# API Documentation - AI PitchDeck Generator

Base URL: `http://localhost:8080/api/v1`

## Authentication Endpoints

### 1. Register New User

Creates a new user account and returns access tokens.

**Endpoint:** `POST /auth/register`

**Request:**
```json
{
  "email": "john.doe@example.com",
  "password": "SecurePass123!",
  "firstName": "John",
  "lastName": "Doe",
  "company": "Acme Inc"
}
```

**Response:** `200 OK`
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "john.doe@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "company": "Acme Inc",
    "role": "USER",
    "isActive": true,
    "emailVerified": false,
    "createdAt": "2025-11-06T13:00:00Z",
    "lastLoginAt": null
  }
}
```

**Validation Errors:** `400 Bad Request`
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

### 2. Login

Authenticates existing user and returns tokens.

**Endpoint:** `POST /auth/login`

**Request:**
```json
{
  "email": "john.doe@example.com",
  "password": "SecurePass123!"
}
```

**Response:** `200 OK`
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "user": { ... }
}
```

**Error:** `401 Unauthorized`
```json
{
  "status": 401,
  "message": "Invalid email or password",
  "timestamp": "2025-11-06T13:00:00Z"
}
```

### 3. Refresh Access Token

Gets a new access token using refresh token.

**Endpoint:** `POST /auth/refresh`

**Request:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response:** `200 OK`
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400
}
```

### 4. Logout

Revokes all refresh tokens for the user.

**Endpoint:** `POST /auth/logout`

**Headers:** `Authorization: Bearer {accessToken}`

**Response:** `204 No Content`

---

## Project Endpoints

All project endpoints require authentication.

### 1. Create Project

**Endpoint:** `POST /projects`

**Headers:** `Authorization: Bearer {accessToken}`

**Request:**
```json
{
  "name": "My Startup Pitch Deck",
  "description": "Revolutionary AI-powered healthcare solution targeting hospital inefficiencies",
  "industry": "Healthcare",
  "targetAudience": "Venture Capitalists"
}
```

**Response:** `201 Created`
```json
{
  "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "My Startup Pitch Deck",
  "description": "Revolutionary AI-powered healthcare solution...",
  "industry": "Healthcare",
  "targetAudience": "Venture Capitalists",
  "status": "DRAFT",
  "createdAt": "2025-11-06T13:00:00Z",
  "updatedAt": "2025-11-06T13:00:00Z"
}
```

### 2. List User Projects

**Endpoint:** `GET /projects?page=0&size=20&sort=createdAt,desc`

**Headers:** `Authorization: Bearer {accessToken}`

**Query Parameters:**
- `page`: Page number (default: 0)
- `size`: Items per page (default: 20, max: 100)
- `sort`: Sort field and direction (e.g., `createdAt,desc`)

**Response:** `200 OK`
```json
{
  "content": [
    {
      "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
      "userId": "550e8400-e29b-41d4-a716-446655440000",
      "name": "My Startup Pitch Deck",
      "description": "Revolutionary AI-powered healthcare solution...",
      "industry": "Healthcare",
      "targetAudience": "Venture Capitalists",
      "status": "DRAFT",
      "createdAt": "2025-11-06T13:00:00Z",
      "updatedAt": "2025-11-06T13:00:00Z"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": {
      "sorted": true,
      "unsorted": false,
      "empty": false
    }
  },
  "totalElements": 1,
  "totalPages": 1,
  "last": true,
  "first": true,
  "size": 20,
  "number": 0
}
```

### 3. Get Project by ID

**Endpoint:** `GET /projects/{projectId}`

**Headers:** `Authorization: Bearer {accessToken}`

**Response:** `200 OK`
```json
{
  "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "My Startup Pitch Deck",
  ...
}
```

**Error:** `404 Not Found`
```json
{
  "status": 404,
  "message": "Project not found with id: 7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "timestamp": "2025-11-06T13:00:00Z"
}
```

### 4. Update Project

**Endpoint:** `PUT /projects/{projectId}`

**Headers:** `Authorization: Bearer {accessToken}`

**Request:**
```json
{
  "name": "Updated Project Name",
  "description": "Updated description",
  "status": "ACTIVE"
}
```

**Response:** `200 OK`
```json
{
  "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "name": "Updated Project Name",
  "status": "ACTIVE",
  ...
}
```

### 5. Delete Project

**Endpoint:** `DELETE /projects/{projectId}`

**Headers:** `Authorization: Bearer {accessToken}`

**Response:** `204 No Content`

---

## File Upload Endpoints

### 1. Request Presigned Upload URL

**Endpoint:** `POST /uploads/presigned-url`

**Headers:** `Authorization: Bearer {accessToken}`

**Request:**
```json
{
  "projectId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "filename": "business-plan.pdf",
  "contentType": "application/pdf"
}
```

**Response:** `200 OK`
```json
{
  "uploadUrl": "http://localhost:4566/ai-pitchdeck-uploads/uploads/550e8400.../uuid_business-plan.pdf?X-Amz-...",
  "fileKey": "uploads/550e8400-e29b-41d4-a716-446655440000/7c9e6679.../uuid_business-plan.pdf",
  "uploadId": "123e4567-e89b-12d3-a456-426614174000",
  "expiresIn": 900
}
```

**Usage:**
```bash
# Upload file directly to S3 using presigned URL
curl -X PUT \
  -H "Content-Type: application/pdf" \
  --data-binary @business-plan.pdf \
  "{uploadUrl}"
```

### 2. Confirm Upload

After uploading to S3, confirm the upload was successful.

**Endpoint:** `POST /uploads/{uploadId}/confirm?fileSize=1024000`

**Headers:** `Authorization: Bearer {accessToken}`

**Query Parameters:**
- `fileSize`: Size of uploaded file in bytes

**Response:** `200 OK`
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "projectId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "originalFilename": "business-plan.pdf",
  "fileType": "PDF",
  "fileSize": 1024000,
  "uploadStatus": "COMPLETED",
  "s3Key": "uploads/550e8400.../uuid_business-plan.pdf",
  "createdAt": "2025-11-06T13:00:00Z"
}
```

### 3. List Project Files

**Endpoint:** `GET /uploads/project/{projectId}`

**Headers:** `Authorization: Bearer {accessToken}`

**Response:** `200 OK`
```json
[
  {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "projectId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
    "originalFilename": "business-plan.pdf",
    "fileType": "PDF",
    "fileSize": 1024000,
    "uploadStatus": "COMPLETED",
    "s3Key": "uploads/.../business-plan.pdf",
    "createdAt": "2025-11-06T13:00:00Z"
  }
]
```

### 4. Get File Upload Details

**Endpoint:** `GET /uploads/{uploadId}`

**Headers:** `Authorization: Bearer {accessToken}`

**Response:** `200 OK` (same as confirm response)

### 5. Delete File Upload

**Endpoint:** `DELETE /uploads/{uploadId}`

**Headers:** `Authorization: Bearer {accessToken}`

**Response:** `204 No Content`

---

## AI Generation Endpoints

### 1. Start Pitch Deck Generation

**Endpoint:** `POST /generate/start`

**Headers:** `Authorization: Bearer {accessToken}`

**Request:**
```json
{
  "projectId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "fileIds": [
    "123e4567-e89b-12d3-a456-426614174000",
    "234e5678-e89b-12d3-a456-426614174001"
  ],
  "promptTemplate": "default"
}
```

**Response:** `202 Accepted`
```json
{
  "id": "345e6789-e89b-12d3-a456-426614174002",
  "projectId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "status": "QUEUED",
  "aiModel": "claude-sonnet-4-20250514",
  "retryCount": 0,
  "errorMessage": null,
  "createdAt": "2025-11-06T13:00:00Z",
  "startedAt": null,
  "completedAt": null
}
```

### 2. Check Job Status

**Endpoint:** `GET /generate/job/{jobId}`

**Headers:** `Authorization: Bearer {accessToken}`

**Response:** `200 OK`
```json
{
  "id": "345e6789-e89b-12d3-a456-426614174002",
  "projectId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "status": "COMPLETED",
  "aiModel": "claude-sonnet-4-20250514",
  "retryCount": 0,
  "errorMessage": null,
  "createdAt": "2025-11-06T13:00:00Z",
  "startedAt": "2025-11-06T13:00:05Z",
  "completedAt": "2025-11-06T13:00:45Z"
}
```

**Status Values:**
- `QUEUED`: Job is waiting to be processed
- `PROCESSING`: Job is currently being processed
- `COMPLETED`: Job completed successfully
- `FAILED`: Job failed (check errorMessage)
- `CANCELLED`: Job was cancelled by user

### 3. List Project Generation Jobs

**Endpoint:** `GET /generate/project/{projectId}/jobs`

**Headers:** `Authorization: Bearer {accessToken}`

**Response:** `200 OK`
```json
[
  {
    "id": "345e6789-e89b-12d3-a456-426614174002",
    "projectId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
    "status": "COMPLETED",
    ...
  }
]
```

### 4. Cancel Generation Job

**Endpoint:** `POST /generate/job/{jobId}/cancel`

**Headers:** `Authorization: Bearer {accessToken}`

**Response:** `204 No Content`

---

## Pitch Deck Endpoints

### 1. Get Pitch Deck by ID

**Endpoint:** `GET /pitch-decks/{pitchDeckId}`

**Headers:** `Authorization: Bearer {accessToken}`

**Response:** `200 OK`
```json
{
  "id": "456e7890-e89b-12d3-a456-426614174003",
  "projectId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "generationJobId": "345e6789-e89b-12d3-a456-426614174002",
  "title": "Healthcare AI Solution Pitch",
  "version": 1,
  "content": "{\"title\":\"Healthcare AI Solution\",\"slides\":[...]}",
  "slideCount": 12,
  "templateUsed": "default",
  "isCurrentVersion": true,
  "createdAt": "2025-11-06T13:00:45Z",
  "updatedAt": "2025-11-06T13:00:45Z"
}
```

### 2. Get Current Version

Gets the latest version of the pitch deck for a project.

**Endpoint:** `GET /pitch-decks/project/{projectId}/current`

**Headers:** `Authorization: Bearer {accessToken}`

**Response:** `200 OK` (same as Get by ID)

### 3. List All Versions

Gets version history for a project.

**Endpoint:** `GET /pitch-decks/project/{projectId}/versions`

**Headers:** `Authorization: Bearer {accessToken}`

**Response:** `200 OK`
```json
[
  {
    "id": "456e7890-e89b-12d3-a456-426614174003",
    "title": "Healthcare AI Solution Pitch",
    "version": 2,
    "slideCount": 12,
    "isCurrentVersion": true,
    "createdAt": "2025-11-06T14:00:00Z"
  },
  {
    "id": "567e8901-e89b-12d3-a456-426614174004",
    "title": "Healthcare AI Solution Pitch",
    "version": 1,
    "slideCount": 10,
    "isCurrentVersion": false,
    "createdAt": "2025-11-06T13:00:45Z"
  }
]
```

---

## Error Responses

### Standard Error Format

```json
{
  "status": 400,
  "message": "Error description",
  "timestamp": "2025-11-06T13:00:00Z"
}
```

### Common Status Codes

- `200 OK`: Request successful
- `201 Created`: Resource created
- `202 Accepted`: Request accepted for async processing
- `204 No Content`: Successful deletion
- `400 Bad Request`: Validation error
- `401 Unauthorized`: Missing or invalid authentication
- `403 Forbidden`: Insufficient permissions
- `404 Not Found`: Resource not found
- `500 Internal Server Error`: Server error

---

## Rate Limiting (Future)

Not currently implemented, but recommended for production:
- 100 requests per minute per user
- 10 generation jobs per hour per user
- Headers: `X-RateLimit-Limit`, `X-RateLimit-Remaining`

---

## Webhooks (Future)

For notifying clients about async job completion:
```json
POST {webhookUrl}
{
  "event": "generation.completed",
  "jobId": "uuid",
  "projectId": "uuid",
  "status": "COMPLETED",
  "timestamp": "2025-11-06T13:00:00Z"
}
```
