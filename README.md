# Resume Analysis and Job Matching System

## Overview

A full-stack resume analysis system built with JavaFX, Spring Boot, JDBC, and a Python ML service. It parses PDF/DOCX resumes, compares them with job descriptions using NLP, and presents animated results with skill match insights and recommendations.

## Key Features

- JavaFX login, upload, analysis progress, results dashboard, and admin analytics.
- PDF and DOCX parsing with clean text normalization.
- ML service with TF-IDF similarity, skill extraction, experience and education detection.
- Asynchronous processing with caching and robust logging.
- Resume ranking and PDF report export.

## Project Structure

- backend: Spring Boot REST API and JDBC storage.
- core: Reusable parsing utilities for PDF/DOCX.
- frontend: JavaFX UI and controllers.
- ml-service: Python FastAPI ML engine.

## Requirements

- Java 17
- Maven
- Python 3.10+
- PostgreSQL

## Database Setup

1. Create a PostgreSQL database named resume_analyser.
2. Update database credentials if needed in backend/src/main/resources/application.yml.

## Run the ML Service

1. Open a terminal at ml-service.
2. Create a virtual environment and install dependencies:
   - python -m venv .venv
   - .venv\Scripts\activate
   - pip install -r requirements.txt
3. Start the service:
   - uvicorn app:app --host 0.0.0.0 --port 8000

## Run the Backend

1. Open a terminal at the project root.
2. Start Spring Boot:
   - mvn -pl backend spring-boot:run

## Run the Frontend

1. Open a terminal at the project root.
2. Start JavaFX:
   - mvn -pl frontend javafx:run

## Usage

1. Login with any email and password to create an account.
2. Upload a resume and add a job description or job file.
3. Start analysis and wait for the animated results dashboard.
4. Export the analysis report as PDF or compare multiple resumes.

## Notes

- The backend uses the ML service endpoint set in application.yml.
- Analysis results are cached for faster repeat evaluations.
