from fastapi import FastAPI
from pydantic import BaseModel
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
import re
from pathlib import Path
from collections import Counter

app = FastAPI()

SKILLS_PATH = Path(__file__).parent / "data" / "skills.txt"

# Preload skills on startup for better performance
_SKILLS_CACHE = None
_SKILLS_SINGLE = set()
_SKILLS_MULTI = []
_SKILLS_MULTI_PATTERNS = {}

class AnalyzeRequest(BaseModel):
    resume_text: str
    job_text: str

class AnalyzeResponse(BaseModel):
    match_score: float
    confidence_score: float
    matched_skills: list[str]
    missing_skills: list[str]
    weaknesses: list[str]
    recommendations: list[str]
    experience_years: float
    education: list[str]
    seniority: str


@app.on_event("startup")
async def startup_event():
    """Preload skills cache on startup"""
    _init_skills_cache()


def _init_skills_cache() -> None:
    global _SKILLS_CACHE, _SKILLS_SINGLE, _SKILLS_MULTI, _SKILLS_MULTI_PATTERNS
    if not SKILLS_PATH.exists():
        _SKILLS_CACHE = []
        _SKILLS_SINGLE = set()
        _SKILLS_MULTI = []
        _SKILLS_MULTI_PATTERNS = {}
        return

    raw_skills = [line.strip() for line in SKILLS_PATH.read_text(encoding="utf-8").splitlines() if line.strip()]
    normalized = sorted(set(normalize(skill) for skill in raw_skills))
    _SKILLS_CACHE = normalized
    _SKILLS_SINGLE = {skill for skill in normalized if " " not in skill}
    _SKILLS_MULTI = [skill for skill in normalized if " " in skill]
    _SKILLS_MULTI_PATTERNS = {
        skill: re.compile(r"\b" + re.escape(skill) + r"\b")
        for skill in _SKILLS_MULTI
    }


def load_skills() -> list[str]:
    """Get preloaded skills from cache"""
    global _SKILLS_CACHE
    if _SKILLS_CACHE is None:
        _init_skills_cache()
    return _SKILLS_CACHE



def normalize(text: str) -> str:
    text = text.lower()
    text = re.sub(r"[^a-z0-9\s]", " ", text)
    text = re.sub(r"\s+", " ", text).strip()
    return text


def extract_skills(text: str, skills: list[str]) -> list[str]:
    matched = set()
    tokens = set(text.split())
    matched.update(tokens.intersection(_SKILLS_SINGLE))
    for skill in _SKILLS_MULTI:
        if skill in text:
            matched.add(skill)
    return sorted(matched)


def extract_skill_frequencies(text: str, skills: list[str]) -> dict:
    """Extract skills with their mention frequencies"""
    frequencies = {}
    token_counts = Counter(text.split())

    for skill in _SKILLS_SINGLE:
        count = token_counts.get(skill, 0)
        if count > 0:
            frequencies[skill] = count

    for skill in _SKILLS_MULTI:
        pattern = _SKILLS_MULTI_PATTERNS.get(skill)
        count = len(pattern.findall(text)) if pattern else 0
        if count > 0:
            frequencies[skill] = count

    return frequencies


def extract_experience_years(text: str) -> float:
    """Extract total years of experience"""
    matches = re.findall(r"(\d+\.?\d*)\s+years", text)
    if not matches:
        return 0.0
    values = [float(m) for m in matches]
    return max(values)


def extract_education(text: str) -> list[str]:
    """Extract education qualifications"""
    patterns = {
        "phd": r"\bphd\b|doctorate",
        "master": r"\bmaster\b|m\.?sc|m\.?tech|mba",
        "bachelor": r"\bbachelor\b|b\.?sc|b\.?tech|bs\b"
    }
    found = []
    for edu_type, pattern in patterns.items():
        if re.search(pattern, text):
            found.append(edu_type)
    return sorted(set(found), reverse=True)


def classify_seniority(years: float) -> str:
    """Classify seniority level based on experience"""
    if years >= 8:
        return "senior"
    if years >= 3:
        return "mid-level"
    if years > 0:
        return "junior"
    return "entry-level"


def extract_job_requirements(text: str) -> dict:
    """Extract job requirements section"""
    req_section = ""
    
    # Try to find requirements section
    patterns = [
        r"requirements?\s*:?(.*?)(?=\n[A-Z]|$)",
        r"must have\s*:?(.*?)(?=\n[A-Z]|$)",
        r"qualifications?\s*:?(.*?)(?=\n[A-Z]|$)"
    ]
    
    for pattern in patterns:
        match = re.search(pattern, text, re.IGNORECASE | re.DOTALL)
        if match:
            req_section = match.group(1)
            break
    
    return {"requirements": req_section if req_section else text}


def calculate_ats_score(resume_text: str, job_text: str, skills: list[str]) -> tuple:
    """
    Calculate ATS-style match score using multiple factors
    Returns: (total_score, confidence, weighted_scores)
    """
    
    # 1. SKILLS MATCHING (40% of score)
    resume_skills = extract_skills(resume_text, skills)
    job_skills = extract_skills(job_text, skills)
    resume_skill_freq = extract_skill_frequencies(resume_text, skills)
    job_skill_freq = extract_skill_frequencies(job_text, skills)
    
    if job_skills:
        skills_matched = len(set(resume_skills).intersection(job_skills))
        skills_score = (skills_matched / len(job_skills)) * 100
    else:
        skills_score = 0
    
    # Penalize for missing critical skills
    missing_skills = set(job_skills) - set(resume_skills)
    missing_penalty = len(missing_skills) * 5  # 5% penalty per missing skill
    skills_score = max(0, skills_score - missing_penalty)
    
    skills_weight = 40
    
    # 2. CONTENT SIMILARITY (30% of score)
    # Use TF-IDF for overall content matching
    vectorizer = TfidfVectorizer(stop_words="english", max_features=100)
    vectors = vectorizer.fit_transform([resume_text, job_text])
    similarity_score = cosine_similarity(vectors[0:1], vectors[1:2])[0][0] * 100
    similarity_weight = 30
    
    # 3. EXPERIENCE MATCHING (15% of score)
    resume_years = extract_experience_years(resume_text)
    job_years_match = re.search(r"(\d+\.?\d*)[+-]?\s*years?(?:\s+(?:of|experience))?", job_text, re.IGNORECASE)
    job_years = float(job_years_match.group(1)) if job_years_match else 0
    
    if job_years > 0:
        if resume_years >= job_years:
            experience_score = 100
        else:
            experience_score = (resume_years / job_years) * 100
    else:
        # If no experience requirement, just check if candidate has any
        experience_score = min(100, (resume_years / 3) * 100) if resume_years > 0 else 50
    
    experience_weight = 15
    
    # 4. EDUCATION MATCHING (15% of score)
    resume_education = extract_education(resume_text)
    job_education = extract_education(job_text)
    
    education_score = 100 if resume_education and job_education else (70 if resume_education else 40)
    education_weight = 15
    
    # Calculate weighted total score
    total_score = (
        (skills_score * skills_weight +
         similarity_score * similarity_weight +
         experience_score * experience_weight +
         education_score * education_weight) / 100
    )
    
    # Cap at 100
    total_score = min(100, max(0, total_score))
    
    # Confidence based on data quality
    confidence = 70 + (min(30, (len(resume_skills) + len(job_skills)) / 2))
    
    return (total_score, confidence, {
        "skills": skills_score,
        "similarity": similarity_score,
        "experience": experience_score,
        "education": education_score
    })


def build_weaknesses(resume_text: str, job_text: str, resume_skills: list[str], 
                      job_skills: list[str], resume_years: float, 
                      job_years: float, scores: dict) -> list[str]:
    """Build detailed weakness analysis"""
    weaknesses = []
    
    # Skills gaps
    missing_skills = set(job_skills) - set(resume_skills)
    if missing_skills:
        if len(missing_skills) <= 3:
            weaknesses.append(f"Missing skills: {', '.join(list(missing_skills)[:3])}")
        else:
            weaknesses.append(f"Missing {len(missing_skills)} required technical skills")
    
    # Experience gap
    if job_years > 0 and resume_years < job_years:
        gap = job_years - resume_years
        weaknesses.append(f"Experience gap: {gap:.1f} years below requirement ({job_years:.0f} required)")
    
    # Content similarity
    if scores.get("similarity", 0) < 40:
        weaknesses.append("Resume content poorly matches job description keywords")
    elif scores.get("similarity", 0) < 60:
        weaknesses.append("Resume has limited alignment with job requirements")
    
    # Skills coverage
    if job_skills and len(resume_skills) < len(job_skills) * 0.5:
        weaknesses.append("Low coverage of required technical skills")
    
    # Education
    if "master" in job_text.lower() and "master" not in resume_text.lower():
        if "bachelor" not in resume_text.lower():
            weaknesses.append("Below required education level")
    
    return weaknesses


def build_recommendations(missing_skills: list[str], resume_years: float, 
                          job_years: float, weaknesses: list[str], scores: dict) -> list[str]:
    """Build actionable recommendations"""
    recommendations = []
    
    # Skills recommendations
    if missing_skills:
        top_missing = missing_skills[:5]
        recommendations.append(f"Highlight experience with: {', '.join(top_missing)}")
    
    # Experience recommendations
    if job_years > 0 and resume_years < job_years:
        recommendations.append(f"If possible, gain {job_years - resume_years:.1f} more years of relevant experience")
    
    if resume_years > 0:
        recommendations.append("Quantify impacts and results from past roles with metrics")
    
    # Content alignment
    if scores.get("similarity", 0) < 60:
        recommendations.append("Adjust resume language to better match job description keywords")
        recommendations.append("Use industry-specific terminology from the job posting")
    
    # General improvements
    if "low coverage" in " ".join(weaknesses).lower():
        recommendations.append("Consider certifications or training for critical missing skills")
    
    if len(recommendations) == 0:
        recommendations.append("Strong match! Ensure portfolio/projects are linked on resume")
        recommendations.append("Consider adding metrics and quantifiable achievements")
    
    return recommendations[:5]  # Return top 5 recommendations


@app.post("/analyze", response_model=AnalyzeResponse)
def analyze(req: AnalyzeRequest) -> AnalyzeResponse:
    # Normalize texts
    resume_text = normalize(req.resume_text)
    job_text = normalize(req.job_text)
    
    # Load skills database
    skills = load_skills()
    
    # Extract components
    resume_skills = extract_skills(resume_text, skills)
    job_skills = extract_skills(job_text, skills)
    matched_skills = sorted(set(resume_skills).intersection(job_skills))
    missing_skills = sorted(set(job_skills).difference(resume_skills))
    
    resume_years = extract_experience_years(resume_text)
    job_years_match = re.search(r"(\d+\.?\d*)[+-]?\s*years?", job_text)
    job_years = float(job_years_match.group(1)) if job_years_match else 0
    
    # Calculate ATS score
    total_score, confidence, scores = calculate_ats_score(resume_text, job_text, skills)
    
    # Extract education
    education = extract_education(resume_text)
    seniority = classify_seniority(resume_years)
    
    # Build weaknesses and recommendations
    weaknesses = build_weaknesses(resume_text, job_text, resume_skills, job_skills, 
                                  resume_years, job_years, scores)
    recommendations = build_recommendations(missing_skills, resume_years, job_years, 
                                           weaknesses, scores)
    
    return AnalyzeResponse(
        match_score=float(round(total_score, 2)),
        confidence_score=float(round(confidence, 2)),
        matched_skills=matched_skills[:20],
        missing_skills=missing_skills[:10],
        weaknesses=weaknesses[:5],
        recommendations=recommendations,
        experience_years=resume_years,
        education=education,
        seniority=seniority,
    )

