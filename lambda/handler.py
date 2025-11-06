"""
AWS Lambda handler for AI pitch deck generation.
Production-ready with error handling, logging, and idempotency.
"""
import json
import os
import time
from datetime import datetime
from typing import Dict, Any
from aws_lambda_powertools import Logger, Tracer
from aws_lambda_powertools.utilities.typing import LambdaContext
from aws_lambda_powertools.utilities.validation import validator
from pydantic import ValidationError

from models import LambdaEvent, PitchDeckOutput, PitchDeckMetadata, SlideContent
from s3_utils import S3Client
from ai_client import ClaudeClient
from callback_client import CallbackClient

# Initialize AWS Lambda Powertools
logger = Logger(service="ai-pitchdeck-generator")
tracer = Tracer(service="ai-pitchdeck-generator")

# Environment variables
S3_BUCKET = os.getenv("S3_BUCKET", "ai-pitchdeck-uploads")
AWS_REGION = os.getenv("AWS_REGION", "us-east-1")
ANTHROPIC_API_KEY = os.getenv("ANTHROPIC_API_KEY")
USE_BEDROCK = os.getenv("USE_BEDROCK", "false").lower() == "true"
MAX_RETRIES = int(os.getenv("MAX_RETRIES", "3"))
CALLBACK_TIMEOUT = int(os.getenv("CALLBACK_TIMEOUT", "30"))

# Initialize clients (reused across warm starts)
s3_client = None
callback_client = None


def get_s3_client() -> S3Client:
    """Get or create S3 client (cached for Lambda warm starts)."""
    global s3_client
    if s3_client is None:
        s3_client = S3Client(bucket_name=S3_BUCKET, region=AWS_REGION)
    return s3_client


def get_callback_client() -> CallbackClient:
    """Get or create callback client (cached for Lambda warm starts)."""
    global callback_client
    if callback_client is None:
        callback_client = CallbackClient(
            max_retries=MAX_RETRIES,
            timeout=CALLBACK_TIMEOUT
        )
    return callback_client


@tracer.capture_method
def extract_text_from_document(content: bytes, s3_key: str) -> str:
    """
    Extract text from document content.
    Currently supports plain text; can be extended for PDF/DOCX.

    Args:
        content: Document bytes
        s3_key: S3 key (used to determine file type)

    Returns:
        Extracted text content
    """
    file_extension = s3_key.lower().split('.')[-1]
    logger.info(f"Extracting text from document, type: {file_extension}")

    if file_extension in ['txt', 'md']:
        # Plain text files
        return content.decode('utf-8')
    elif file_extension == 'pdf':
        # For PDF, you should use pypdf or pdfplumber
        # For now, we'll raise an informative error
        logger.warning("PDF text extraction requires pypdf/pdfplumber library")
        raise ValueError(
            "PDF parsing not implemented. Please install 'pypdf' or 'pdfplumber' "
            "and implement PDF text extraction, or use plain text files."
        )
    elif file_extension in ['doc', 'docx']:
        # For Word docs, you should use python-docx
        logger.warning("Word document parsing requires python-docx library")
        raise ValueError(
            "Word document parsing not implemented. Please install 'python-docx' "
            "and implement DOCX text extraction, or use plain text files."
        )
    else:
        # Try to decode as text with error handling
        logger.warning(f"Unsupported file type: {file_extension}, attempting text decode")
        try:
            return content.decode('utf-8', errors='replace')
        except Exception as e:
            logger.error(f"Failed to decode content as text: {e}")
            raise ValueError(f"Unable to extract text from file type: {file_extension}")


@tracer.capture_method
def check_idempotency(s3: S3Client, job_id: str) -> bool:
    """
    Check if this job has already been processed (idempotency).

    Args:
        s3: S3 client instance
        job_id: Job identifier

    Returns:
        True if result already exists, False otherwise
    """
    output_key = f"pitchdecks/{job_id}/result.json"
    exists = s3.file_exists(output_key)
    if exists:
        logger.info(f"Job {job_id} already processed (idempotent)", extra={
            "job_id": job_id,
            "output_key": output_key
        })
    return exists


@tracer.capture_method
def process_generation(event_data: LambdaEvent) -> Dict[str, Any]:
    """
    Main processing logic for pitch deck generation.

    Args:
        event_data: Validated Lambda event

    Returns:
        Dictionary with status and output S3 key

    Raises:
        Exception: If generation fails
    """
    start_time = time.time()
    s3 = get_s3_client()

    # Check idempotency
    output_s3_key = f"pitchdecks/{event_data.jobId}/result.json"
    if check_idempotency(s3, str(event_data.jobId)):
        logger.info("Job already completed, skipping processing")
        return {
            "status": "COMPLETED",
            "output_s3_key": output_s3_key,
            "idempotent": True
        }

    # Download input document from S3
    logger.info(f"Downloading input file: {event_data.s3Key}")
    document_bytes = s3.download_file(event_data.s3Key)

    # Extract text from document
    document_text = extract_text_from_document(document_bytes, event_data.s3Key)
    logger.info(f"Extracted {len(document_text)} characters from document")

    # Initialize AI client
    ai_client = ClaudeClient(
        api_key=ANTHROPIC_API_KEY,
        model=event_data.llmModel,
        use_bedrock=USE_BEDROCK,
        region=AWS_REGION
    )

    # Generate pitch deck
    logger.info("Starting AI generation")
    generation_result = ai_client.generate_pitch_deck(
        document_content=document_text,
        system_prompt=event_data.systemPrompt,
        user_prompt=event_data.userPrompt
    )

    # Build output structure
    duration_ms = int((time.time() - start_time) * 1000)
    pitch_deck_data = generation_result["pitch_deck"]

    # Validate and structure output
    slides = [
        SlideContent(**slide) for slide in pitch_deck_data.get("slides", [])
    ]

    metadata = PitchDeckMetadata(
        model=generation_result["model"],
        durationMs=duration_ms,
        generatedAt=datetime.utcnow(),
        inputTokens=generation_result.get("input_tokens"),
        outputTokens=generation_result.get("output_tokens")
    )

    output = PitchDeckOutput(slides=slides, metadata=metadata)

    # Upload result to S3
    logger.info(f"Uploading result to S3: {output_s3_key}")
    s3.upload_json(
        s3_key=output_s3_key,
        data=output.model_dump(mode='json'),
        metadata={
            "job-id": str(event_data.jobId),
            "project-id": str(event_data.projectId),
            "user-id": str(event_data.userId),
            "model": event_data.llmModel
        }
    )

    logger.info("Pitch deck generation completed successfully", extra={
        "job_id": str(event_data.jobId),
        "duration_ms": duration_ms,
        "slides_count": len(slides),
        "input_tokens": generation_result.get("input_tokens"),
        "output_tokens": generation_result.get("output_tokens")
    })

    return {
        "status": "COMPLETED",
        "output_s3_key": output_s3_key,
        "duration_ms": duration_ms,
        "slides_count": len(slides)
    }


@logger.inject_lambda_context(log_event=True)
@tracer.capture_lambda_handler
def lambda_handler(event: Dict[str, Any], context: LambdaContext) -> Dict[str, Any]:
    """
    AWS Lambda handler function.

    Args:
        event: Lambda event payload
        context: Lambda context object

    Returns:
        Response dictionary with status and details
    """
    logger.info("Lambda function invoked", extra={
        "request_id": context.request_id,
        "function_name": context.function_name,
        "remaining_time_ms": context.get_remaining_time_in_millis()
    })

    callback_url = None
    job_id = None

    try:
        # Validate event payload
        logger.info("Validating event payload")
        event_data = LambdaEvent(**event)
        callback_url = str(event_data.callbackUrl)
        job_id = event_data.jobId

        logger.info("Event validation successful", extra={
            "job_id": str(job_id),
            "project_id": str(event_data.projectId),
            "user_id": str(event_data.userId),
            "s3_key": event_data.s3Key,
            "llm_model": event_data.llmModel
        })

        # Process generation
        result = process_generation(event_data)

        # Send success callback
        logger.info("Sending success callback to backend")
        callback = get_callback_client()
        callback_sent = callback.send_success_callback(
            callback_url=callback_url,
            job_id=job_id,
            output_s3_key=result["output_s3_key"]
        )

        if not callback_sent:
            logger.warning("Callback failed but processing succeeded")

        return {
            "statusCode": 200,
            "body": json.dumps({
                "status": "COMPLETED",
                "jobId": str(job_id),
                "outputS3Key": result["output_s3_key"],
                "callbackSent": callback_sent
            })
        }

    except ValidationError as e:
        error_msg = f"Event validation failed: {str(e)}"
        logger.error(error_msg, exc_info=True)

        # Send failure callback if we have the callback URL
        if callback_url and job_id:
            callback = get_callback_client()
            callback.send_failure_callback(
                callback_url=callback_url,
                job_id=job_id,
                error_message=error_msg
            )

        return {
            "statusCode": 400,
            "body": json.dumps({
                "status": "FAILED",
                "error": error_msg
            })
        }

    except Exception as e:
        error_msg = f"Pitch deck generation failed: {str(e)}"
        logger.error(error_msg, exc_info=True)

        # Send failure callback
        if callback_url and job_id:
            callback = get_callback_client()
            callback.send_failure_callback(
                callback_url=callback_url,
                job_id=job_id,
                error_message=error_msg
            )

        return {
            "statusCode": 500,
            "body": json.dumps({
                "status": "FAILED",
                "error": error_msg
            })
        }
