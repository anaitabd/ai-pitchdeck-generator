"""
HTTP client for sending callbacks to Spring Boot backend.
Includes retry logic and error handling.
"""
import time
from typing import Dict
import httpx
from aws_lambda_powertools import Logger
from models import CallbackPayload

logger = Logger(service="callback-client")


class CallbackClient:
    """
    HTTP client for posting job completion status back to Spring Boot backend.
    Implements retry logic for resilience.
    """

    def __init__(self, max_retries: int = 3, retry_delay: int = 2, timeout: int = 30):
        """
        Initialize callback client.

        Args:
            max_retries: Maximum number of retry attempts
            retry_delay: Delay between retries in seconds
            timeout: HTTP request timeout in seconds
        """
        self.max_retries = max_retries
        self.retry_delay = retry_delay
        self.timeout = timeout
        logger.info(
            f"Callback client initialized: max_retries={max_retries}, "
            f"retry_delay={retry_delay}s, timeout={timeout}s"
        )

    def send_callback(self, callback_url: str, payload: CallbackPayload) -> bool:
        """
        Send callback to Spring Boot backend with retry logic.

        Args:
            callback_url: Backend callback endpoint URL
            payload: Callback payload with job status

        Returns:
            True if callback succeeded, False otherwise
        """
        logger.info(f"Sending callback to {callback_url}", extra={
            "job_id": str(payload.jobId),
            "status": payload.status
        })

        # Convert Pydantic model to dict with ISO format dates
        payload_dict = payload.model_dump(mode='json')

        for attempt in range(1, self.max_retries + 1):
            try:
                logger.info(f"Callback attempt {attempt}/{self.max_retries}")
                
                with httpx.Client(timeout=self.timeout) as client:
                    response = client.post(
                        callback_url,
                        json=payload_dict,
                        headers={"Content-Type": "application/json"}
                    )
                    response.raise_for_status()

                logger.info(
                    f"Callback succeeded on attempt {attempt}",
                    extra={"status_code": response.status_code}
                )
                return True

            except httpx.HTTPStatusError as e:
                logger.warning(
                    f"Callback failed with HTTP error on attempt {attempt}: {e.response.status_code}",
                    extra={"response_text": e.response.text[:500]}
                )
                if e.response.status_code < 500 and attempt >= self.max_retries:
                    # Don't retry 4xx errors after max attempts
                    logger.error(f"Callback failed with client error: {e.response.status_code}")
                    return False

            except (httpx.RequestError, httpx.TimeoutException) as e:
                logger.warning(
                    f"Callback failed with request error on attempt {attempt}: {str(e)}"
                )

            except Exception as e:
                logger.error(
                    f"Unexpected error during callback on attempt {attempt}",
                    exc_info=True
                )

            # Wait before retry (except on last attempt)
            if attempt < self.max_retries:
                delay = self.retry_delay * attempt  # Exponential backoff
                logger.info(f"Waiting {delay}s before retry...")
                time.sleep(delay)

        logger.error(f"Callback failed after {self.max_retries} attempts")
        return False

    def send_success_callback(
        self,
        callback_url: str,
        job_id,
        output_s3_key: str
    ) -> bool:
        """
        Send successful completion callback.

        Args:
            callback_url: Backend callback endpoint
            job_id: Job UUID
            output_s3_key: S3 key where result JSON is stored

        Returns:
            True if callback succeeded
        """
        payload = CallbackPayload(
            jobId=job_id,
            status="COMPLETED",
            outputS3Key=output_s3_key,
            errorMessage=None
        )
        return self.send_callback(callback_url, payload)

    def send_failure_callback(
        self,
        callback_url: str,
        job_id,
        error_message: str
    ) -> bool:
        """
        Send failure callback.

        Args:
            callback_url: Backend callback endpoint
            job_id: Job UUID
            error_message: Error description

        Returns:
            True if callback succeeded
        """
        payload = CallbackPayload(
            jobId=job_id,
            status="FAILED",
            outputS3Key=None,
            errorMessage=error_message
        )
        return self.send_callback(callback_url, payload)
