"""
Pydantic models for Lambda event validation and response schemas.
"""
from datetime import datetime
from typing import List, Optional
from uuid import UUID
from pydantic import BaseModel, Field, HttpUrl


class LambdaEvent(BaseModel):
    """
    Input event schema for the Lambda function.
    Expected to be triggered asynchronously from Spring Boot backend.
    """
    jobId: UUID = Field(..., description="Unique job identifier")
    projectId: UUID = Field(..., description="Project identifier")
    userId: UUID = Field(..., description="User identifier")
    s3Key: str = Field(..., description="S3 key for the input document")
    callbackUrl: HttpUrl = Field(..., description="Spring Boot backend callback URL")
    llmModel: str = Field(default="claude-sonnet-4-20250514", description="AI model to use")
    systemPrompt: Optional[str] = Field(default=None, description="System prompt for AI")
    userPrompt: Optional[str] = Field(default=None, description="User prompt for AI")

    class Config:
        json_schema_extra = {
            "example": {
                "jobId": "550e8400-e29b-41d4-a716-446655440000",
                "projectId": "660e8400-e29b-41d4-a716-446655440001",
                "userId": "770e8400-e29b-41d4-a716-446655440002",
                "s3Key": "uploads/user-id/project-id/uuid_business-plan.pdf",
                "callbackUrl": "http://localhost:8080/api/v1/generate/callback",
                "llmModel": "claude-sonnet-4-20250514",
                "systemPrompt": "You are an expert pitch deck generator.",
                "userPrompt": "Generate a professional pitch deck."
            }
        }


class SlideContent(BaseModel):
    """Individual slide in the pitch deck."""
    title: str = Field(..., description="Slide title")
    content: str = Field(..., description="Slide content/body")
    type: str = Field(..., description="Slide type (e.g., title, problem, solution)")

    class Config:
        json_schema_extra = {
            "example": {
                "title": "Problem Statement",
                "content": "Our target market faces significant challenges...",
                "type": "problem"
            }
        }


class PitchDeckMetadata(BaseModel):
    """Metadata about the generated pitch deck."""
    model: str = Field(..., description="AI model used for generation")
    durationMs: int = Field(..., description="Generation time in milliseconds")
    generatedAt: datetime = Field(default_factory=datetime.utcnow, description="Timestamp")
    inputTokens: Optional[int] = Field(default=None, description="Input tokens used")
    outputTokens: Optional[int] = Field(default=None, description="Output tokens used")


class PitchDeckOutput(BaseModel):
    """Complete pitch deck output structure."""
    slides: List[SlideContent] = Field(..., description="List of slides")
    metadata: PitchDeckMetadata = Field(..., description="Generation metadata")

    class Config:
        json_schema_extra = {
            "example": {
                "slides": [
                    {
                        "title": "Company Overview",
                        "content": "We are revolutionizing...",
                        "type": "title"
                    }
                ],
                "metadata": {
                    "model": "claude-sonnet-4-20250514",
                    "durationMs": 5000,
                    "generatedAt": "2025-11-06T16:00:00Z"
                }
            }
        }


class CallbackPayload(BaseModel):
    """Payload sent to Spring Boot backend callback."""
    jobId: UUID = Field(..., description="Job identifier")
    status: str = Field(..., description="Job status: COMPLETED or FAILED")
    outputS3Key: Optional[str] = Field(default=None, description="S3 key for result JSON")
    errorMessage: Optional[str] = Field(default=None, description="Error message if failed")
    generatedAt: datetime = Field(default_factory=datetime.utcnow, description="Completion timestamp")

    class Config:
        json_schema_extra = {
            "example": {
                "jobId": "550e8400-e29b-41d4-a716-446655440000",
                "status": "COMPLETED",
                "outputS3Key": "pitchdecks/550e8400-e29b-41d4-a716-446655440000/result.json",
                "generatedAt": "2025-11-06T16:00:00Z"
            }
        }
