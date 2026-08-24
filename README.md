# Smart Resume Screener

An automated, LLM-powered resume screener application built with Spring Boot, MySQL, and OpenRouter APIs. This application extracts structured profile data from PDF/Text resumes and conducts semantic match analysis against specific job requirements.

---

## Main Features
*   **Resume Upload & Text Extraction**: Accepts PDF resumes (using Apache PDFBox) and standard Text resumes (.txt).
*   **AI-Based Resume Parsing**: Automatically extracts names, emails, phones, education, experience, and skills list via OpenRouter LLMs.
*   **Upsert Persistence Logic**: Dedupes candidates on their email addresses, updating fields automatically for subsequent uploads.
*   **Job Description Management**: Store and retrieve target job roles with built-in parameter constraints.
*   **Semantic Match Analysis**: Semantically compares candidate profiles against job descriptions using LLMs, returning an evidence-based match score (1–10), shortlist/reject decision, matched/missing skills, and detailed justification.

---

## Technology Stack
*   **Java**: Version 21 (targeted release) / JDK 25 runtime compatible.
*   **Framework**: Spring Boot 3.4.2 (Spring Web, Spring Data JPA, Bean Validation).
*   **Database**: MySQL 8.0.46 (with Hibernate-managed schema updates).
*   **Parsing Engine**: Apache PDFBox 3.x.
*   **AI Integration**: OpenRouter API (`openrouter/free`).

---

## Project Structure
```text
Smart-Resume-Screener/
├── .env.example                     # Reference file for environment variables
├── .gitignore                       # Standard Git exclusions (target, local credentials)
├── pom.xml                          # Maven build descriptors & JVM flags
└── src/
    ├── main/
    │   ├── java/com/resume/screener/
    │   │   ├── SmartResumeScreenerApplication.java
    │   │   ├── config/
    │   │   │   └── AppConfig.java
    │   │   ├── controller/
    │   │   │   ├── CandidateController.java
    │   │   │   ├── JobDescriptionController.java
    │   │   │   └── MatchController.java
    │   │   ├── dto/
    │   │   │   ├── CandidateResponseDto.java
    │   │   │   ├── ErrorResponseDto.java
    │   │   │   ├── LlmMatchResponse.java
    │   │   │   ├── LlmResumeParseResponse.java
    │   │   │   ├── JobDescriptionRequestDto.java
    │   │   │   ├── JobDescriptionResponseDto.java
    │   │   │   ├── MatchRequestDto.java
    │   │   │   └── MatchResponseDto.java
    │   │   ├── entity/
    │   │   │   ├── Candidate.java
    │   │   │   ├── JobDescription.java
    │   │   │   └── MatchResult.java
    │   │   ├── exception/
    │   │   │   ├── EmptyFileException.java
    │   │   │   ├── GlobalExceptionHandler.java
    │   │   │   ├── LlmException.java
    │   │   │   ├── PdfException.java
    │   │   │   ├── ResourceNotFoundException.java
    │   │   │   └── UnsupportedFileTypeException.java
    │   │   ├── repository/
    │   │   │   ├── CandidateRepository.java
    │   │   │   ├── JobDescriptionRepository.java
    │   │   │   └── MatchResultRepository.java
    │   │   └── service/
    │   │       ├── CandidateService.java
    │   │       ├── JobDescriptionService.java
    │   │       ├── LlmService.java
    │   │       ├── MatchService.java
    │   │       ├── PdfService.java
    │   │       └── TxtService.java
    │   └── resources/
    │       └── application.yml      # Base configurations & environment mappings
    └── test/
        └── java/com/resume/screener/
            ├── controller/
            │   ├── CandidateControllerTest.java
            │   ├── JobDescriptionControllerTest.java
            │   └── MatchControllerTest.java
            ├── exception/
            │   └── GlobalExceptionHandlerTest.java
            └── service/
                ├── CandidateServiceTest.java
                ├── JobDescriptionServiceTest.java
                ├── LlmServiceTest.java
                ├── MatchServiceTest.java
                ├── PdfServiceTest.java
                └── TxtServiceTest.java
```

---

## Database Schema Overview
The application connects to a MySQL database named `smart_resume_screener`. Hibernate automatically provisions the following table structures:

### 1. `candidates`
*   `id` (`BIGINT AUTO_INCREMENT`): Primary Key.
*   `name` (`VARCHAR(255)`): Candidate name.
*   `email` (`VARCHAR(255) UNIQUE`): Unique candidate identifier used for upserts.
*   `phone` (`VARCHAR(50)`): Phone number.
*   `skills` (`TEXT`): Candidate skills returned by the parser.
*   `experience` (`TEXT`): Experience history.
*   `education` (`TEXT`): Education details.
*   `resume_text` (`TEXT`): Raw text extracted from file.
*   `created_at` (`TIMESTAMP`): Automatically set to current timestamp on creation.

### 2. `job_descriptions`
*   `id` (`BIGINT AUTO_INCREMENT`): Primary Key.
*   `title` (`VARCHAR(255)`): Job title. Cannot be blank and max size is 255.
*   `description` (`TEXT`): Job requirements/details.
*   `created_at` (`TIMESTAMP`): Automatically set on creation.

### 3. `match_results`
*   `id` (`BIGINT AUTO_INCREMENT`): Primary Key.
*   `candidate_id` (`BIGINT`): Foreign Key referencing `candidates(id)`.
*   `job_description_id` (`BIGINT`): Foreign Key referencing `job_descriptions(id)`.
*   `score` (`INT`): Semantic match score (1 to 10).
*   `decision` (`VARCHAR(50)`): Match decision (`SHORTLIST` or `REJECT`).
*   `matched_skills` (`TEXT`): JSON array string of matched skills.
*   `missing_skills` (`TEXT`): JSON array string of missing skills.
*   `justification` (`TEXT`): Evidence-based matching explanation.
*   `created_at` (`TIMESTAMP`): Automatically set on creation.

---

## Architecture & LLM Prompts

The Smart Resume Screener application uses a decoupled, service-oriented architecture to pipeline resume ingestion, structural metadata parsing, and semantic suitability evaluations.

### Data & Evaluation Flow Diagram
The lifecycle of a resume and evaluation runs through the following stages:
```text
[Resume Ingestion] 
       │ (PDF / TXT Upload)
       ▼
[Text Ingestion Services] ──► (Apache PDFBox for PDFs / TxtService for TXT)
       │ (Raw Text Extraction)
       ▼
[LlmService: parseResume] ──► (OpenRouter Completion Request with Resume Parser Prompt)
       │ (Structured JSON Profile Extraction)
       ▼
[Candidate Entity Creation] ──► (Saved & Upserted in MySQL `candidates` Table)
       │ (Candidate ID & Job Description ID)
       ▼
[MatchService: matchAndSave]
       │
       ├─► [Duplicate Check] ──► (Queries `match_results` for existing Candidate-Job Pair)
       │         │
       │         ├─► YES ──► (Returns cached result directly with isDuplicate = true)
       │         │
       │         └─► NO  ──► [LlmService: matchCandidateWithJob] ──► (OpenRouter Matching Prompt)
       │                                 │ (Structured JSON Score & Justification)
       │                                 ▼
       │                          [Score & Decision Validation]
       │                                 │
       │                                 ▼
       │                          [MySQL Persistence] ──► (Saved to `match_results`)
       │                                 │
       │                                 ▼
       │                          [MatchResponseDto] ──► (Returns result with isDuplicate = false)
```

---

### 1. Resume Ingestion & Parsing Prompt

When a resume document is uploaded, its raw text is extracted and passed to the `LlmService.parseResume(String resumeText)` method.

#### System Prompt Template
```text
You are an expert resume parsing system. Analyze the raw text of the candidate's resume and extract structured information.
You must output a valid JSON object matching this schema exactly:
{
  "name": "Full Name (default 'Unknown' if not found)",
  "email": "Email Address (default 'Unknown' if not found)",
  "phone": "Phone Number (default 'Unknown' if not found)",
  "skills": ["Skill1", "Skill2", ...],
  "experience": "Synthesized summary of work experience and companies",
  "education": "Synthesized summary of degree, institution, graduation year"
}
Rules:
1. Extract only information actually present in the resume. Do not invent information.
2. If a field is unavailable, use 'Unknown' or an empty array.
3. Skills should be returned as a clean JSON array of strings.
4. Return JSON only. Do NOT return Markdown code blocks (like ```json). Do NOT return explanations outside the JSON object.
```

#### User Prompt Template
```text
Resume Text:
[Raw Extracted Resume Text]
```

*   **Sent Information**: The system instructions establish the target JSON schema and extraction guidelines. The raw text of the resume is supplied as the user input.
*   **Expected JSON Response**: A JSON object matching the keys `name`, `email`, `phone`, `skills`, `experience`, and `education`.

---

### 2. Candidate-Job Matching Prompt

When a match is requested, candidate details and the target job description are passed to the `LlmService.matchCandidateWithJob(Candidate candidate, JobDescription jobDescription)` method.

#### System Prompt Template
```text
You are an expert recruiter evaluating candidate suitability for a job.
Analyze the candidate's skills, experience, and education, and compare them with the target Job Title and Job Description.
You must output a valid JSON object matching this schema exactly:
{
  "score": <Integer from 1 to 10>,
  "decision": "<Either 'SHORTLIST' or 'REJECT'>",
  "matchedSkills": ["SkillA", "SkillB", ...],
  "missingSkills": ["SkillC", "SkillD", ...],
  "justification": "<A concise, evidence-based explanation of why this score and decision was chosen. Mention key strengths and critical missing skills.>"
}
Rules:
1. Only use information actually present in the candidate profile and job description. Do not invent skills, experience, education, or requirements.
2. The score must be strictly evidence-based from 1 to 10.
3. matchedSkills must contain only skills actually supported by the candidate that match the job description.
4. missingSkills must contain important job requirements/skills requested in the job description that are absent from the candidate.
5. The justification must explicitly explain the main strengths and weaknesses.
6. Return JSON only. Do NOT return Markdown code blocks (like ```json). Do NOT return explanations outside the JSON object.
```

#### User Prompt Template
```text
Candidate Profile:
Name: [Candidate Name]
Skills: [Candidate Skills List]
Experience: [Candidate Experience Description]
Education: [Candidate Education Description]

Job Requirements:
Title: [Job Title]
Description: [Job Description]
```

*   **Supplied Information**: Candidate Name, Skills, Experience, and Education, alongside the Job Title and Job Description.
*   **Evaluation Criteria**: The LLM compares candidate attributes against job parameters, assessing technical skills, depth of experience, and academic relevance.
*   **Expected JSON Response**:
    *   `score`: An integer from 1 to 10.
    *   `decision`: Either `"SHORTLIST"` or `"REJECT"`.
    *   `matchedSkills`: List of intersecting skills.
    *   `missingSkills`: List of skills requested in the job description but missing from the candidate.
    *   `justification`: Detailed recruiter textual explanation.

---

### 3. Prompt Design Decisions

*   **Semantic Matching**: Evaluates overlapping competencies rather than doing strict keyword searches, allowing synonyms or conceptual alignments to be mapped.
*   **Structured JSON Output**: Enabled via API settings (`"response_format": {"type": "json_object"}`) and strict system prompt guidelines to ensure response consistency.
*   **Grounding (No Hallucinations)**: System rules strictly instruct the LLM: *"Only use information actually present... Do not invent skills, experience, education, or requirements."* to ensure objectivity.
*   **Post-processing Guardrails**: The output JSON is sanitized to find the first `{` and last `}` brace to protect against conversational wrappers, and parsed DTO values are programmatically checked to confirm `score` is strictly between `1` and `10` and `decision` is exactly `"SHORTLIST"` or `"REJECT"`, rejecting malformed completion blocks with a `502 Bad Gateway` error.

---

## Configuration & Environment Variables
Copy `.env.example` to establish local configurations. The application connects locally using the following properties:

| Variable | Description | Default |
| :--- | :--- | :--- |
| `MYSQL_HOST` | Host of your MySQL server | `localhost` |
| `MYSQL_PORT` | Port of your MySQL server | `3306` |
| `MYSQL_USER` | MySQL login username | `root` |
| `MYSQL_PASSWORD` | MySQL user password | *Required* |
| `MYSQL_USE_SSL` | Enable database SSL connections | `false` |
| `MYSQL_ALLOW_PUBLIC_KEY_RETRIEVAL` | Allow dynamic key fetching | `true` |
| `OPENROUTER_API_KEY` | OpenRouter developer key | *Required* |
| `OPENROUTER_MODEL` | Match and parse target model | `openrouter/free` |
| `OPENROUTER_MAX_TOKENS` | Completion output limit | `2048` |
| `OPENROUTER_REFERER` | Site referer mapping header | `http://localhost:8080` |
| `OPENROUTER_CONNECT_TIMEOUT_MS` | API connection threshold | `5000` (5s) |
| `OPENROUTER_READ_TIMEOUT_MS` | API response read threshold | `30000` (30s) |

---

## How to Run Locally

### 1. Provision Database
Ensure MySQL is running on port `3306` and create the schema database:
```sql
CREATE DATABASE smart_resume_screener;
```

### 2. Launch Application
In your PowerShell console, export variables and run the boot script:
```powershell
$env:MYSQL_PASSWORD="YOUR_LOCAL_MYSQL_PASSWORD"
$env:OPENROUTER_API_KEY="YOUR_OPENROUTER_API_KEY"
mvn spring-boot:run
```

---

## Maven Commands
*   **Compile Code**: `mvn clean compile`
*   **Run All Tests**: `mvn clean test`
*   **Package as JAR**: `mvn clean package` (located in `./target/screener-0.0.1-SNAPSHOT.jar`)

---

## API Endpoint Documentation

### 1. Resume Upload
*   **Endpoint**: `POST /api/resumes/upload`
*   **Body Type**: `form-data`
*   **Payload Key**: `file` (multipart file)
*   **Expected Status**: `201 Created`
*   **Response Payload**:
    ```json
    {
      "id": 1,
      "name": "Jane Doe",
      "email": "jane.doe@example.com",
      "phone": "555-0199",
      "skills": ["Java", "Spring Boot", "SQL"],
      "experience": "5+ years of software development experience...",
      "education": "B.S. in Computer Science",
      "createdAt": "2026-08-22T21:25:00.000"
    }
    ```

### 2. Get All Candidates
*   **Endpoint**: `GET /api/candidates`
*   **Expected Status**: `200 OK`
*   **Response Payload**: Array of candidate response objects.

### 3. Get Candidate by ID
*   **Endpoint**: `GET /api/candidates/{id}`
*   **Expected Status**: `200 OK` or `404 Not Found`

### 4. Create Job Description
*   **Endpoint**: `POST /api/jobs`
*   **Headers**: `Content-Type: application/json`
*   **Payload**:
    ```json
    {
      "title": "Senior Java Developer",
      "description": "Must have 5+ years of experience with Java, Spring Boot, and MySQL."
    }
    ```
*   **Expected Status**: `201 Created`
*   **Response Payload**: Persisted Job Description object.

### 5. Get Job Description by ID
*   **Endpoint**: `GET /api/jobs/{id}`
*   **Expected Status**: `200 OK` or `404 Not Found`

### 6. Candidate Matching Analysis
*   **Endpoint**: `POST /api/matches`
*   **Headers**: `Content-Type: application/json`
*   **Payload**:
    ```json
    {
      "candidateId": 1,
      "jobDescriptionId": 1
    }
    ```
*   **Expected Status**: `201 Created`
*   **Response Payload**:
    ```json
    {
      "id": 1,
      "candidateId": 1,
      "jobDescriptionId": 1,
      "score": 8,
      "decision": "SHORTLIST",
      "matchedSkills": ["Java", "Spring Boot"],
      "missingSkills": ["MySQL"],
      "justification": "Candidate matches core skills. Needs additional database expertise.",
      "createdAt": "2026-08-22T21:42:00.000",
      "isDuplicate": false
    }
    ```
*   **Duplicate Match Prevention**: If a match result already exists for the candidate + job description combination, the application immediately returns the pre-existing result with `"isDuplicate": true` instead of calling OpenRouter.

### 7. Get Match Result by ID
*   **Endpoint**: `GET /api/matches/{id}`
*   **Expected Status**: `200 OK` or `404 Not Found`

### 8. Get Filtered Match History
*   **Endpoint**: `GET /api/matches`
*   **Query Parameters (Optional)**:
    *   `candidateId`: Filter by Candidate ID (e.g. `?candidateId=1`)
    *   `jobDescriptionId`: Filter by Job Description ID (e.g. `?jobDescriptionId=2`)
*   **Expected Status**: `200 OK`
*   **Response Payload**: Array of match response objects.
*   **Description**: Used to fetch the list of past match evaluations, supporting optional filtering by candidate or job role. Used to populate the AI Matcher history dashboard.

---

## Error Handling Catalog
Standardized JSON error structures are returned:
*   **`400 Bad Request`**: Returned on parameter validation failures (blank fields, missing parts, empty uploads).
*   **`404 Not Found`**: Returned when querying candidates, jobs, or matches that do not exist.
*   **`415 Unsupported Media Type`**: Returned when uploading files other than PDF/TXT.
*   **`502 Bad Gateway`**: Returned on LLM network or formatting validation exceptions.
*   **`500 Internal Server Error`**: Generic secure message returned to prevent internal stack leakages.
