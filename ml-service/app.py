from fastapi import FastAPI
from pydantic import BaseModel
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
import re
from pathlib import Path

app = FastAPI()

SKILLS_PATH = Path(__file__).parent / "data" / "skills.txt"

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
    global _SKILLS_CACHE
    if _SKILLS_CACHE is None:
        _init_skills_cache()
    return _SKILLS_CACHE



def normalize(text: str) -> str:
    text = text.lower()
    text = re.sub(r"[^a-z0-9\s]", " ", text)
    text = re.sub(r"\s+", " ", text).strip()
    return text


def get_skills(text: str) -> list[str]:
    matched = set()
    tokens = set(text.split())
    matched.update(tokens.intersection(_SKILLS_SINGLE))
    for skill in _SKILLS_MULTI:
        if skill in text:
            matched.add(skill)
    return sorted(matched)


def get_years(text: str) -> float:
    matches = re.findall(r"(\d+\.?\d*)\s+years", text)
    if not matches:
        return 0.0
    values = [float(m) for m in matches]
    return max(values)


def get_education(text: str) -> list[str]:
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


def get_seniority(years: float) -> str:
    if years >= 8:
        return "senior"
    if years >= 3:
        return "mid-level"
    if years > 0:
        return "junior"
    return "entry-level"


def get_job_years(text: str) -> float:
    years_match = re.search(r"(\d+\.?\d*)[+-]?\s*years?(?:\s+(?:of|experience))?", text, re.IGNORECASE)
    return float(years_match.group(1)) if years_match else 0


def score_skills(resume_skills: list[str], job_skills: list[str]) -> float:
    if not job_skills:
        return 0
    matched = len(set(resume_skills).intersection(job_skills))
    base_score = (matched / len(job_skills)) * 100
    missing = len(set(job_skills) - set(resume_skills))
    return max(0, base_score - (missing * 5))


def score_similarity(resume_text: str, job_text: str) -> float:
    vectorizer = TfidfVectorizer(stop_words="english", max_features=100)
    vectors = vectorizer.fit_transform([resume_text, job_text])
    return cosine_similarity(vectors[0:1], vectors[1:2])[0][0] * 100


def score_experience(resume_years: float, job_years: float) -> float:
    if job_years > 0:
        if resume_years >= job_years:
            return 100
        return (resume_years / job_years) * 100
    if resume_years > 0:
        return min(100, (resume_years / 3) * 100)
    return 50


def score_education(resume_text: str, job_text: str) -> float:
    resume_education = get_education(resume_text)
    job_education = get_education(job_text)
    return 100 if resume_education and job_education else (70 if resume_education else 40)


def calculate_score(resume_text: str, job_text: str) -> tuple:
    resume_skills = get_skills(resume_text)
    job_skills = get_skills(job_text)
    resume_years = get_years(resume_text)
    job_years = get_job_years(job_text)

    skills_score = score_skills(resume_skills, job_skills)
    similarity_score = score_similarity(resume_text, job_text)
    experience_score = score_experience(resume_years, job_years)
    education_score = score_education(resume_text, job_text)

    total_score = (
        (skills_score * 40 +
         similarity_score * 30 +
         experience_score * 15 +
         education_score * 15) / 100
    )

    total_score = min(100, max(0, total_score))

    confidence = 70 + (min(30, (len(resume_skills) + len(job_skills)) / 2))

    return (total_score, confidence, {
        "skills": skills_score,
        "similarity": similarity_score,
        "experience": experience_score,
        "education": education_score
    })


def build_response(resume_text: str, job_text: str) -> AnalyzeResponse:
    load_skills()

    resume_skills = get_skills(resume_text)
    job_skills = get_skills(job_text)
    matched_skills = sorted(set(resume_skills).intersection(job_skills))
    missing_skills = sorted(set(job_skills).difference(resume_skills))

    resume_years = get_years(resume_text)
    job_years = get_job_years(job_text)
    total_score, confidence, scores = calculate_score(resume_text, job_text)

    weaknesses = get_weaknesses(resume_text, job_text, resume_skills, job_skills,
                                resume_years, job_years, scores)
    recommendations = get_recommendations(missing_skills, resume_years, job_years,
                                          weaknesses, scores)

    return AnalyzeResponse(
        match_score=float(round(total_score, 2)),
        confidence_score=float(round(confidence, 2)),
        matched_skills=matched_skills[:20],
        missing_skills=missing_skills[:10],
        weaknesses=weaknesses[:5],
        recommendations=recommendations,
        experience_years=resume_years,
        education=get_education(resume_text),
        seniority=get_seniority(resume_years),
    )


def get_weaknesses(resume_text: str, job_text: str, resume_skills: list[str], 
                   job_skills: list[str], resume_years: float, 
                   job_years: float, scores: dict) -> list[str]:
    weaknesses = []

    missing_skills = set(job_skills) - set(resume_skills)
    if missing_skills:
        if len(missing_skills) <= 3:
            weaknesses.append(f"Missing skills: {', '.join(list(missing_skills)[:3])}")
        else:
            weaknesses.append(f"Missing {len(missing_skills)} required technical skills")

    if job_years > 0 and resume_years < job_years:
        gap = job_years - resume_years
        weaknesses.append(f"Experience gap: {gap:.1f} years below requirement ({job_years:.0f} required)")

    if scores.get("similarity", 0) < 40:
        weaknesses.append("Resume content poorly matches job description keywords")
    elif scores.get("similarity", 0) < 60:
        weaknesses.append("Resume has limited alignment with job requirements")

    if job_skills and len(resume_skills) < len(job_skills) * 0.5:
        weaknesses.append("Low coverage of required technical skills")

    if "master" in job_text.lower() and "master" not in resume_text.lower():
        if "bachelor" not in resume_text.lower():
            weaknesses.append("Below required education level")

    return weaknesses


def get_recommendations(missing_skills: list[str], resume_years: float, 
                        job_years: float, weaknesses: list[str], scores: dict) -> list[str]:
    recommendations = []

    if missing_skills:
        top_missing = missing_skills[:5]
        recommendations.append(f"Highlight experience with: {', '.join(top_missing)}")

    if job_years > 0 and resume_years < job_years:
        recommendations.append(f"If possible, gain {job_years - resume_years:.1f} more years of relevant experience")

    if resume_years > 0:
        recommendations.append("Quantify impacts and results from past roles with metrics")

    if scores.get("similarity", 0) < 60:
        recommendations.append("Adjust resume language to better match job description keywords")
        recommendations.append("Use industry-specific terminology from the job posting")

    if "low coverage" in " ".join(weaknesses).lower():
        recommendations.append("Consider certifications or training for critical missing skills")

    if len(recommendations) == 0:
        recommendations.append("Strong match! Ensure portfolio/projects are linked on resume")
        recommendations.append("Consider adding metrics and quantifiable achievements")

    return recommendations[:5]


@app.post("/analyze", response_model=AnalyzeResponse)
def analyze(req: AnalyzeRequest) -> AnalyzeResponse:
    resume_text = normalize(req.resume_text)
    job_text = normalize(req.job_text)
    return build_response(resume_text, job_text)

