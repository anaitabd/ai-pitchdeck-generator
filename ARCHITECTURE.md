# AI PitchDeck Generator - Architecture Documentation

## System Overview

The AI PitchDeck Generator is a production-ready SaaS backend built with Spring Boot 3.5.7 that enables users to automatically generate professional pitch decks from uploaded documents using AI (Claude Sonnet 4.5).

## Architecture Patterns

### Clean Architecture
The application follows Clean Architecture principles with clear separation between:
- **Controllers** (API/Presentation Layer)
- **Services** (Business Logic Layer)
- **Repositories** (Data Access Layer)
- **Entities** (Domain Model)

### Design Patterns Used
1. **Repository Pattern**: Data access abstraction
2. **Service Layer Pattern**: Business logic encapsulation
3. **DTO Pattern**: Data transfer with records
4. **Mapper Pattern**: Entity-DTO conversion
5. **Builder Pattern**: Entity construction (via Lombok)
6. **Strategy Pattern**: AI model selection (extensible)

## System Components

### 1. Authentication & Authorization

**JWT-Based Authentication**
- Stateless authentication using JSON Web Tokens
- Refresh token mechanism for extended sessions
- Role-based access control (USER, ADMIN, PREMIUM)

**Flow:**
```
Client → Register/Login → JWT Token Generated → 
Client Stores Token → Subsequent Requests with Bearer Token → 
JwtAuthenticationFilter Validates → Request Processed
```

**Security Features:**
- BCrypt password hashing
- Token expiration (24h access, 7d refresh)
- CORS protection
- CSRF disabled (stateless API)

### 2. File Upload System

**S3 Presigned URLs**
The system uses presigned URLs for direct client-to-S3 uploads:

```
┌────────┐           ┌──────────┐           ┌─────┐
│ Client │──request──│ Backend  │──create───│ S3  │
│        │           │          │           │     │
│        │◄─presigned│          │           │     │
│        │    URL    │          │           │     │
│        │───upload──────────────────────────►     │
│        │           │          │           │     │
│        │─confirm───►          │──verify───►     │
└────────┘           └──────────┘           └─────┘
```

**Benefits:**
- Reduced server load
- Faster uploads
- Scalable architecture
- Cost-effective

**File Types Supported:**
- PDF
- DOC/DOCX
- TXT
- MD

### 3. AI Generation Pipeline

**Async Processing Architecture**

```
┌──────────────┐
│ Start Gen    │
│ Request      │
└──────┬───────┘
       │
       ▼
┌──────────────┐
│ Create Job   │
│ (QUEUED)     │
└──────┬───────┘
       │
       ▼
┌──────────────┐     ┌─────────────┐
│ Async Worker │────►│ Extract     │
│ Executor     │     │ File Content│
└──────┬───────┘     └─────────────┘
       │
       ▼
┌──────────────┐     ┌─────────────┐
│ AI Service   │────►│ Claude API  │
│ LangChain4j  │     │ (Anthropic) │
└──────┬───────┘     └─────────────┘
       │
       ▼
┌──────────────┐
│ Parse &      │
│ Store Result │
│ (PitchDeck)  │
└──────┬───────┘
       │
       ▼
┌──────────────┐
│ Update Job   │
│ (COMPLETED)  │
└──────────────┘
```

**Features:**
- Async processing to avoid blocking
- Retry logic (up to 3 attempts)
- Job status tracking
- Error handling and logging

**AI Prompt Structure:**
```
Project Description + Industry + Target Audience + Document Content
    ↓
Claude Sonnet 4.5
    ↓
JSON Pitch Deck (10-15 slides)
    ↓
Stored in Database as JSONB
```

### 4. Data Model

**Entity Relationships:**

```
┌─────────┐
│  Users  │
└────┬────┘
     │ 1
     │
     │ N
┌────▼──────┐     1     ┌──────────────┐
│ Projects  ├───────────┤ PitchDecks   │
└────┬──────┘     N     └──────────────┘
     │ 1                        ▲
     │                          │ N
     │ N                        │ 1
┌────▼──────────┐     1   ┌────┴────────────┐
│ FileUploads   ├─────────┤ GenerationJobs  │
└───────────────┘     N   └─────────────────┘
```

**Key Features:**
- UUID primary keys for better distribution
- Soft deletes via CASCADE constraints
- JSONB for flexible content storage
- Version tracking for pitch decks
- Audit trail support

### 5. Database Layer

**Technology:** PostgreSQL 16
**Migration Tool:** Flyway

**Schema Features:**
- Optimized indexes for common queries
- Foreign key constraints for data integrity
- Check constraints for enum values
- JSONB columns for flexible data
- Timestamp tracking (created_at, updated_at)
- Automatic timestamp updates via triggers

**Performance Optimizations:**
- Indexes on foreign keys
- Indexes on query columns (status, created_at)
- GIN indexes on JSONB columns
- Connection pooling (HikariCP)

### 6. AWS Integration

**Services Used:**
1. **S3**: File storage with presigned URLs
2. **Lambda**: (Future) Async file processing
3. **STS**: Temporary credentials

**LocalStack for Development:**
- Simulates AWS services locally
- No cloud costs during development
- Same API as production AWS

### 7. API Design

**RESTful Principles:**
- Resource-based URLs
- HTTP methods (GET, POST, PUT, DELETE)
- Proper status codes
- JSON request/response
- Pagination support

**Error Handling:**
```json
{
  "status": 404,
  "message": "Project not found with id: xxx",
  "timestamp": "2025-11-06T13:00:00Z"
}
```

**Validation Errors:**
```json
{
  "status": 400,
  "message": "Validation failed",
  "timestamp": "2025-11-06T13:00:00Z",
  "errors": {
    "email": "Email must be valid",
    "password": "Password must be at least 8 characters"
  }
}
```

## Data Flow Examples

### 1. User Registration Flow

```
1. POST /api/v1/auth/register
   ↓
2. Validate request (email format, password strength)
   ↓
3. Check if email exists
   ↓
4. Hash password (BCrypt)
   ↓
5. Save user to database
   ↓
6. Generate JWT + Refresh Token
   ↓
7. Save refresh token
   ↓
8. Return tokens + user info
```

### 2. Pitch Deck Generation Flow

```
1. Upload files to S3 (presigned URLs)
   ↓
2. POST /api/v1/generate/start
   ↓
3. Validate project & files
   ↓
4. Create GenerationJob (QUEUED)
   ↓
5. Start async processing
   ↓
6. Extract text from files
   ↓
7. Build AI prompt
   ↓
8. Call Claude API via LangChain4j
   ↓
9. Parse JSON response
   ↓
10. Create PitchDeck entity
   ↓
11. Update job status (COMPLETED)
   ↓
12. Client polls GET /api/v1/generate/job/{id}
```

## Scalability Considerations

### Horizontal Scaling
- **Stateless Design**: No server-side sessions
- **Database Connection Pooling**: Efficient resource usage
- **Async Processing**: Non-blocking operations

### Vertical Scaling
- **JVM Tuning**: Heap size configuration
- **Thread Pools**: Configurable executors
- **Connection Pools**: Adjustable limits

### Bottlenecks & Solutions

| Component | Potential Bottleneck | Solution |
|-----------|---------------------|----------|
| Database | Query performance | Indexing, caching |
| AI API | Rate limits | Queue management, retry logic |
| File Storage | S3 bandwidth | CloudFront CDN |
| Generation | Long processing | Async + notifications |

## Security Architecture

### Authentication
- JWT with short expiration
- Refresh token rotation
- Secure password hashing (BCrypt)

### Authorization
- Role-based access control
- Resource ownership validation
- Method-level security

### Data Protection
- HTTPS only in production
- SQL injection prevention (JPA)
- XSS prevention (content validation)
- CORS configuration

### Secrets Management
- Environment variables
- AWS Secrets Manager (production)
- Never commit secrets to Git

## Monitoring & Observability

### Health Checks
- `/actuator/health`: Application health
- Database connectivity
- External service availability

### Logging
- Structured logging
- Log levels: DEBUG, INFO, WARN, ERROR
- Request/response logging (configurable)
- AI request/response logging

### Metrics (Future)
- Request rates
- Response times
- Error rates
- Generation success/failure rates
- Database query performance

## Deployment Architecture

### Development
```
Docker Compose:
- PostgreSQL
- pgAdmin
- LocalStack (S3, Lambda)
- Application (via ./mvnw)
```

### Production (Recommended)
```
AWS/Cloud Platform:
- RDS PostgreSQL
- ECS/EKS for containers
- S3 for file storage
- Lambda for async processing
- CloudWatch for monitoring
- Route53 + ALB for routing
```

## Technology Choices Rationale

| Technology | Why Chosen |
|-----------|------------|
| Spring Boot 3.5.7 | Latest LTS, production-ready |
| Java 17 | LTS, modern features |
| PostgreSQL | JSONB support, reliability |
| JWT | Stateless, scalable |
| AWS S3 | Scalable file storage |
| LangChain4j | AI integration abstraction |
| Flyway | Version-controlled migrations |
| Docker Compose | Easy local development |

## Future Enhancements

1. **Caching Layer**: Redis for frequently accessed data
2. **Message Queue**: RabbitMQ/SQS for job processing
3. **WebSocket**: Real-time job status updates
4. **Export Features**: PDF/PPTX generation
5. **Analytics**: Usage metrics and insights
6. **Multi-tenancy**: Organization support
7. **CDN Integration**: CloudFront for static assets
8. **Search**: Elasticsearch for content search
