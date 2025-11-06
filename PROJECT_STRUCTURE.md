# AI PitchDeck Generator - Project Structure

```
ai-pitchdeck-generator/
│
├── src/
│   ├── main/
│   │   ├── java/com/naitabdallah/aipitchdeck/
│   │   │   │
│   │   │   ├── AiPitchdeckGeneratorApplication.java    # Main Spring Boot application
│   │   │   │
│   │   │   ├── config/                                 # Configuration classes
│   │   │   │   ├── AsyncConfig.java                    # Async task executor
│   │   │   │   ├── AwsConfig.java                      # AWS SDK beans (S3, Lambda)
│   │   │   │   ├── LangChain4jConfig.java              # AI model configuration
│   │   │   │   └── SecurityConfig.java                 # Spring Security + JWT
│   │   │   │
│   │   │   ├── controller/                             # REST API Controllers
│   │   │   │   ├── AuthController.java                 # /api/v1/auth/**
│   │   │   │   ├── ProjectController.java              # /api/v1/projects/**
│   │   │   │   ├── FileUploadController.java           # /api/v1/uploads/**
│   │   │   │   ├── GenerationController.java           # /api/v1/generate/**
│   │   │   │   └── PitchDeckController.java            # /api/v1/pitch-decks/**
│   │   │   │
│   │   │   ├── service/                                # Business Logic Layer
│   │   │   │   ├── AuthService.java                    # Authentication & JWT
│   │   │   │   ├── ProjectService.java                 # Project CRUD
│   │   │   │   ├── FileUploadService.java              # File upload management
│   │   │   │   ├── S3Service.java                      # AWS S3 operations
│   │   │   │   ├── AIService.java                      # AI generation via LangChain4j
│   │   │   │   ├── GenerationService.java              # Generation workflow
│   │   │   │   └── PitchDeckService.java               # Pitch deck retrieval
│   │   │   │
│   │   │   ├── repository/                             # Data Access Layer (JPA)
│   │   │   │   ├── UserRepository.java
│   │   │   │   ├── ProjectRepository.java
│   │   │   │   ├── FileUploadRepository.java
│   │   │   │   ├── GenerationJobRepository.java
│   │   │   │   ├── PitchDeckRepository.java
│   │   │   │   └── RefreshTokenRepository.java
│   │   │   │
│   │   │   ├── entity/                                 # JPA Entities
│   │   │   │   ├── User.java
│   │   │   │   ├── Project.java
│   │   │   │   ├── FileUpload.java
│   │   │   │   ├── GenerationJob.java
│   │   │   │   ├── PitchDeck.java
│   │   │   │   └── RefreshToken.java
│   │   │   │
│   │   │   ├── dto/                                    # Data Transfer Objects (Records)
│   │   │   │   ├── AuthDto.java                        # Auth request/response DTOs
│   │   │   │   ├── UserResponse.java
│   │   │   │   ├── ProjectDto.java                     # Project DTOs
│   │   │   │   ├── FileUploadDto.java                  # Upload DTOs
│   │   │   │   ├── GenerationDto.java                  # Generation DTOs
│   │   │   │   └── PitchDeckDto.java                   # PitchDeck DTOs
│   │   │   │
│   │   │   ├── mapper/                                 # Entity-DTO Mappers
│   │   │   │   ├── UserMapper.java
│   │   │   │   ├── ProjectMapper.java
│   │   │   │   ├── FileUploadMapper.java
│   │   │   │   ├── GenerationJobMapper.java
│   │   │   │   └── PitchDeckMapper.java
│   │   │   │
│   │   │   ├── security/                               # Security Components
│   │   │   │   ├── JwtAuthenticationFilter.java        # JWT validation filter
│   │   │   │   └── UserPrincipal.java                  # Authenticated user principal
│   │   │   │
│   │   │   ├── exception/                              # Exception Handling
│   │   │   │   ├── GlobalExceptionHandler.java         # @RestControllerAdvice
│   │   │   │   ├── ResourceNotFoundException.java
│   │   │   │   ├── AuthenticationException.java
│   │   │   │   ├── FileUploadException.java
│   │   │   │   └── GenerationException.java
│   │   │   │
│   │   │   └── util/                                   # Utilities
│   │   │       └── JwtUtil.java                        # JWT generation/validation
│   │   │
│   │   └── resources/
│   │       ├── application.yaml                        # Main configuration
│   │       └── db/migration/                           # Flyway migrations
│   │           └── V1__init.sql                        # Initial schema
│   │
│   └── test/
│       └── java/com/naitabdallah/aipitchdeck/
│           └── AiPitchdeckGeneratorApplicationTests.java
│
├── docker-compose.yml                                  # Local development stack
├── localstack-init/                                    # LocalStack initialization
│   └── init-s3.sh                                      # S3 bucket setup
│
├── pom.xml                                             # Maven dependencies
├── Dockerfile                                          # Container image
│
├── README.md                                           # Getting started guide
├── ARCHITECTURE.md                                     # Architecture documentation
├── API_DOCUMENTATION.md                                # API reference
└── PROJECT_STRUCTURE.md                                # This file
```

## Layer Responsibilities

### Controllers (Presentation Layer)
- Handle HTTP requests/responses
- Input validation (via `@Valid`)
- Extract user principal from JWT
- Delegate to service layer
- Map service responses to DTOs

### Services (Business Logic Layer)
- Implement business rules
- Transaction management (`@Transactional`)
- Cross-cutting concerns (auth, validation)
- Orchestrate between repositories
- Call external services (S3, AI)

### Repositories (Data Access Layer)
- Database operations via JPA
- Custom queries
- No business logic
- Extend `JpaRepository<Entity, UUID>`

### Entities (Domain Model)
- JPA entities with annotations
- Database table mapping
- Relationships (via IDs, not @OneToMany)
- Enums for constrained values

### DTOs (Data Transfer Objects)
- Java records (immutable)
- Request/response objects
- Validation annotations
- No business logic

### Mappers
- Convert entities to DTOs
- Convert DTOs to entities
- Stateless components

## Package Dependencies

```
Controllers → Services → Repositories → Entities
     ↓           ↓
   DTOs      Mappers
     ↓           ↓
 Validation   Entities
```

## Configuration Files

- **application.yaml**: Main Spring Boot configuration
- **pom.xml**: Maven dependencies and build config
- **docker-compose.yml**: Local infrastructure
- **V1__init.sql**: Database schema migration

## Key Technologies

| Layer | Technology |
|-------|-----------|
| Web Framework | Spring Boot 3.5.7 |
| Security | Spring Security + JWT |
| Database | PostgreSQL + JPA |
| Migrations | Flyway |
| Cloud | AWS SDK v2 (S3, Lambda) |
| AI | LangChain4j + Claude |
| Async | Spring @Async |
| Validation | Jakarta Validation |
| Build | Maven |
| Containerization | Docker |

## File Naming Conventions

- **Entities**: Singular noun (User, Project)
- **Repositories**: EntityNameRepository
- **Services**: EntityNameService or FunctionalityService
- **Controllers**: EntityNameController
- **DTOs**: Nested in XyzDto class (e.g., AuthDto.LoginRequest)
- **Mappers**: EntityNameMapper

## Code Organization Principles

1. **Single Responsibility**: Each class has one clear purpose
2. **Dependency Injection**: Constructor-based DI
3. **Interface Segregation**: Repositories extend JpaRepository
4. **Immutability**: DTOs as records, entities with Lombok
5. **Clean Code**: Meaningful names, small methods
