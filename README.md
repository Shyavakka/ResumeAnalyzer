# AI Resume Analyzer

A simple Spring Boot app that lets a user upload a PDF resume and get back
AI-generated structured feedback: overall score, profile summary, key
strengths, areas for improvement, missing skills/sections, and suggestions.

Built for the NavaDhiti fresher assignment.

## Stack

- Java 17, Spring Boot 3.2 (Web + Thymeleaf)
- Apache PDFBox for PDF text extraction
- Google Gemini API for the AI analysis (model: `gemini-1.5-flash`, free tier)

## How it works

1. `/` shows an upload form (`upload.html`).
2. `POST /analyze` receives the PDF, and:
   - `PdfExtractionService` extracts raw text with PDFBox (rejects non-PDFs,
     password-protected files, or files with no readable text).
   - `ResumeAnalysisService` sends that text to the Gemini API with a prompt
     asking for a strict JSON response, and parses it into an `AnalysisResult`.
3. `result.html` renders the structured feedback.
4. Any expected failure (bad file, missing API key, API error) is caught and
   shown back to the user on the upload page instead of crashing.

## Running locally

**1. Get a Gemini API key (free)**
Go to https://aistudio.google.com/apikey, sign in with a Google account, and
click "Create API key". No billing setup required for the free tier. (Any
provider works if you'd rather swap in a different one - see "Swapping the
AI provider" below.)

**2. Set the API key as an environment variable** (never put it in the code
or commit it):

```bash
# macOS / Linux
export GEMINI_API_KEY=your-key-here

# Windows PowerShell
$env:GEMINI_API_KEY="your-key-here"
```

**3. Run the app**

```bash
mvn spring-boot:run
```

**4. Open** http://localhost:8080 and upload a PDF resume.

## Swapping the AI provider

If you'd rather use a different model provider, everything AI-related lives
in `ResumeAnalysisService.java` — change the URL, headers, and request body
format to match that provider's chat/completions API. The rest of the app
(PDF extraction, controller, templates) doesn't need to change.

## Deploying a demo link

For a quick, free deployment so you have a live link to submit:

- **Render.com** (easiest for Spring Boot):
  1. Push this repo to GitHub.
  2. On Render, create a new **Web Service**, connect the repo.
  3. Build command: `mvn clean package -DskipTests`
     Start command: `java -jar target/resume-analyzer-1.0.0.jar`
  4. Add an environment variable `GEMINI_API_KEY` in Render's dashboard.
  5. Deploy — Render gives you a public URL.

- **Railway.app** works the same way and also auto-detects Spring Boot.

## Submitting

You need to send NavaDhiti:
1. Your own GitHub repo link (push this code there first).
2. The demo/deployment link from Render or Railway.

## Security note

No API keys or secrets are committed anywhere in this repo. The key is only
ever read from the `OPENAI_API_KEY` environment variable at runtime.
