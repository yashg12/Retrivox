# Retrivox

> **Your Notes. Your Knowledge. Your Next Level.**

Retrivox is an AI-powered study and revision assistant that transforms uploaded PDFs and lecture notes into executive summaries, practice quizzes, and document-grounded question answering (RAG).

---

## 🚀 Features

- **Document Ingestion & Text Extraction**: Upload PDFs up to 5 MB with robust Apache PDFBox parsing.
- **AI Executive Summaries**: Concise, structured summaries focusing on key concepts and definitions.
- **Interactive Quiz Arena**: Generates customizable MCQs with difficulty levels, instant grading, and historical attempt tracking.
- **Document-Grounded Q&A (RAG)**: Chat with your notes backed by semantic chunking, embedding vectors, and source citations.
- **User Ownership & Isolation**: Secure authentication with stateless JWT, BCrypt encryption, and strict multi-tenant data isolation.
- **Modern Responsive UI**: Clean, accessible interface with fluid dark/light modes and glassmorphic aesthetics.

---

## 🛠 Tech Stack

- **Frontend**: React 19, Vite, Lucide Icons, Modern Vanilla CSS Design System.
- **Backend**: Java 21, Spring Boot 4.1.1, Spring Security (Stateless JWT), Spring Data JPA, Apache PDFBox.
- **Database**: PostgreSQL hosted on Supabase.
- **AI / Embeddings**: OpenAI-compatible LLM & Embedding endpoints.

---

## 🏁 Quickstart

### Prerequisites
- Java 21+
- Node.js 18+ & npm
- PostgreSQL database (e.g. Supabase)

### Backend Setup
1. Navigate to the backend directory:
   ```bash
   cd backend
   ```
2. Configure your environment variables:
   ```bash
   export DB_URL="jdbc:postgresql://<host>:5432/postgres?sslmode=require"
   export DB_USERNAME="<username>"
   export DB_PASSWORD="<password>"
   export LLM_API_KEY="<api-key>" # Optional if using mock/custom LLM
   export JWT_SECRET="<secret-at-least-256-bits>"
   ```
3. Run the Spring Boot server:
   ```bash
   ./mvnw spring-boot:run
   ```

### Frontend Setup
1. Navigate to the frontend directory:
   ```bash
   cd frontend
   ```
2. Install dependencies:
   ```bash
   npm install
   ```
3. Run dev server:
   ```bash
   npm run dev
   ```

---

## 📄 License
This project is licensed under the MIT License.
