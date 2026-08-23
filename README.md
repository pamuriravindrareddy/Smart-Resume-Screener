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
      "createdAt": "2026-08-22T21:42:00.000"
    }
    ```

### 7. Get Match Result by ID
*   **Endpoint**: `GET /api/matches/{id}`
*   **Expected Status**: `200 OK` or `404 Not Found`

---

## Error Handling Catalog
Standardized JSON error structures are returned:
*   **`400 Bad Request`**: Returned on parameter validation failures (blank fields, missing parts, empty uploads).
*   **`404 Not Found`**: Returned when querying candidates, jobs, or matches that do not exist.
*   **`415 Unsupported Media Type`**: Returned when uploading files other than PDF/TXT.
*   **`502 Bad Gateway`**: Returned on LLM network or formatting validation exceptions.
*   **`500 Internal Server Error`**: Generic secure message returned to prevent internal stack leakages.
