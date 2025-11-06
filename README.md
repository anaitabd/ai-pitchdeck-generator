# AI PitchDeck Generator - Backend API

A production-ready Spring Boot backend for an AI-powered pitch deck generation SaaS platform. This system uses Claude Sonnet 4.5 via LangChain4j to automatically generate professional pitch decks from uploaded documents.

## 🏗️ Architecture Overview

This backend follows **Clean Architecture** principles with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────┐
│                    Controllers Layer                     │
│     (REST APIs - Auth, Projects, Uploads, AI)          │
└───────────────────┬─────────────────────────────────────┘
                    │
┌───────────────────▼─────────────────────────────────────┐
│                   Services Layer                         │
│  (Business Logic - Auth, Project, S3, AI, Generation)   │
└───────────────────┬─────────────────────────────────────┘
                    │
┌───────────────────▼─────────────────────────────────────┐
│              Repository Layer (JPA)                      │
│    (Data Access - User, Project, FileUpload, etc.)      │
└───────────────────┬─────────────────────────────────────┘
                    │
┌───────────────────▼─────────────────────────────────────┐
│                  Database (PostgreSQL)                   │
└─────────────────────────────────────────────────────────┘
```

### Key Components:
- **Authentication**: JWT-based auth with refresh tokens
- **File Storage**: AWS S3 with presigned URLs
- **AI Generation**: LangChain4j + Claude Sonnet 4.5
- **Async Processing**: Spring Async for background jobs
- **Database**: PostgreSQL with Flyway migrations

## 📋 Tech Stack

- **Java 17**
- **Spring Boot 3.5.7**
- **PostgreSQL 16**
- **AWS SDK v2** (S3, Lambda, STS)
- **LangChain4j** (AI integration)
- **JWT** (Authentication)
- **Flyway** (Database migrations)
- **Docker Compose** (Local development)
- **Maven** (Build tool)

## 🗄️ Database Schema

### Core Tables:
- `users` - User accounts and authentication
- `projects` - Pitch deck projects
- `file_uploads` - Uploaded document metadata
- `generation_jobs` - AI generation job tracking
- `pitch_decks` - Generated pitch decks with version history
- `refresh_tokens` - JWT refresh token management
- `audit_logs` - System audit trail (optional)

See [V1__init.sql](src/main/resources/db/migration/V1__init.sql) for complete schema.

## 🚀 Getting Started

### Prerequisites
- Java 17 or higher
- Docker & Docker Compose
- Maven 3.6+ (or use included wrapper)

### 1. Clone and Build

```bash
git clone <repository-url>
cd ai-pitchdeck-generator
./mvnw clean install
```

### 2. Start Infrastructure

Start PostgreSQL, pgAdmin, and LocalStack:

```bash
docker-compose up -d
```

This will start:
- PostgreSQL on `localhost:5432`
- pgAdmin on `http://localhost:5050`
- LocalStack (S3, Lambda) on `localhost:4566`

### 3. Configure Environment

Create `.env` or set environment variables:

```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=ai_pitchdeck
DB_USER=pitchdeck_user
DB_PASSWORD=pitchdeck_pass

# JWT
JWT_SECRET_KEY=your-secret-key-change-in-production
JWT_EXPIRATION_MS=86400000

# AWS (LocalStack for local dev)
AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=test
AWS_SECRET_ACCESS_KEY=test
AWS_S3_ENDPOINT=http://localhost:4566
AWS_S3_BUCKET=ai-pitchdeck-uploads

# AI (Claude API)
ANTHROPIC_API_KEY=your-anthropic-api-key
ANTHROPIC_MODEL=claude-sonnet-4-20250514
```

### 4. Run the Application

```bash
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080`

## 🔑 API Documentation

### Authentication

#### Register
```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "firstName": "John",
  "lastName": "Doe",
  "company": "Acme Inc"
}
```

Response:
```json
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "user": {
    "id": "uuid",
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe"
  }
}
```

#### Login
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePass123!"
}
```

#### Refresh Token
```http
POST /api/v1/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGc..."
}
```

### Projects

#### Create Project
```http
POST /api/v1/projects
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "name": "My Startup Pitch",
  "description": "Innovative AI solution for healthcare",
  "industry": "Healthcare",
  "targetAudience": "Venture Capitalists"
}
```

#### List Projects
```http
GET /api/v1/projects?page=0&size=20
Authorization: Bearer {accessToken}
```

#### Get Project
```http
GET /api/v1/projects/{projectId}
Authorization: Bearer {accessToken}
```

#### Update Project
```http
PUT /api/v1/projects/{projectId}
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "name": "Updated Name",
  "status": "ACTIVE"
}
```

### File Uploads

#### Request Presigned Upload URL
```http
POST /api/v1/uploads/presigned-url
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "projectId": "uuid",
  "filename": "business-plan.pdf",
  "contentType": "application/pdf"
}
```

Response:
```json
{
  "uploadUrl": "https://s3.amazonaws.com/...",
  "fileKey": "uploads/user-id/project-id/uuid_business-plan.pdf",
  "uploadId": "uuid",
  "expiresIn": 900
}
```

#### Confirm Upload
```http
POST /api/v1/uploads/{uploadId}/confirm?fileSize=1024000
Authorization: Bearer {accessToken}
```

#### List Project Files
```http
GET /api/v1/uploads/project/{projectId}
Authorization: Bearer {accessToken}
```

### AI Generation

#### Start Generation
```http
POST /api/v1/generate/start
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "projectId": "uuid",
  "fileIds": ["uuid1", "uuid2"],
  "promptTemplate": "default"
}
```

Response:
```json
{
  "id": "uuid",
  "projectId": "uuid",
  "status": "QUEUED",
  "aiModel": "claude-sonnet-4-20250514",
  "createdAt": "2025-11-06T13:00:00Z"
}
```

#### Check Job Status
```http
GET /api/v1/generate/job/{jobId}
Authorization: Bearer {accessToken}
```

#### List Project Jobs
```http
GET /api/v1/generate/project/{projectId}/jobs
Authorization: Bearer {accessToken}
```

### Pitch Decks

#### Get Current Version
```http
GET /api/v1/pitch-decks/project/{projectId}/current
Authorization: Bearer {accessToken}
```

#### Get Specific Version
```http
GET /api/v1/pitch-decks/{pitchDeckId}
Authorization: Bearer {accessToken}
```

#### List All Versions
```http
GET /api/v1/pitch-decks/project/{projectId}/versions
Authorization: Bearer {accessToken}
```

## 🔧 Configuration

### Database Connection

The application uses Flyway for database migrations. On startup, it will automatically:
1. Create tables if they don't exist
2. Run pending migrations
3. Create default admin user (change password in production!)

### AWS S3 Configuration

For local development, the app uses LocalStack to simulate S3:

```bash
# Initialize S3 bucket (done automatically via localstack-init/init-s3.sh)
aws --endpoint-url=http://localhost:4566 s3 mb s3://ai-pitchdeck-uploads
```

For production, configure real AWS credentials:
```yaml
aws:
  region: us-east-1
  access-key-id: ${AWS_ACCESS_KEY_ID}
  secret-access-key: ${AWS_SECRET_ACCESS_KEY}
  s3:
    bucket-name: your-production-bucket
    endpoint: # leave empty for real AWS
```

### pgAdmin Access

1. Navigate to `http://localhost:5050`
2. Login with:
   - Email: `admin@pitchdeck.com`
   - Password: `admin`
3. Add server:
   - Host: `postgres`
   - Port: `5432`
   - Database: `ai_pitchdeck`
   - Username: `pitchdeck_user`
   - Password: `pitchdeck_pass`

## 🧪 Testing

Run all tests:
```bash
./mvnw test
```

Run specific test:
```bash
./mvnw test -Dtest=AuthServiceTest
```

## 📦 Building for Production

### Create JAR
```bash
./mvnw clean package -DskipTests
```

### Run JAR
```bash
java -jar target/ai-pitchdeck-generator-0.0.1-SNAPSHOT.jar
```

### Docker Build
```bash
docker build -t ai-pitchdeck-backend .
docker run -p 8080:8080 ai-pitchdeck-backend
```

## 🔐 Security

### JWT Configuration
- Tokens expire after 24 hours (configurable)
- Refresh tokens last 7 days
- All endpoints except `/api/v1/auth/**` require authentication

### CORS
Configure allowed origins in `application.yml`:
```yaml
security:
  cors:
    allowed-origins: http://localhost:3000,https://yourdomain.com
```

## 📊 Monitoring

Health check endpoint:
```http
GET /actuator/health
```

Metrics (if enabled):
```http
GET /actuator/metrics
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Write/update tests
5. Submit a pull request

## 📄 License

[Your License Here]

## 🆘 Support

For issues and questions:
- GitHub Issues: [repository-url]/issues
- Documentation: [docs-url]
- Email: support@yourdomain.com

## 🎯 Roadmap

- [ ] Email verification
- [ ] Password reset flow
- [ ] Batch file processing
- [ ] Export to PowerPoint
- [ ] AI model selection
- [ ] Custom prompt templates
- [ ] Collaboration features
- [ ] Analytics dashboard
