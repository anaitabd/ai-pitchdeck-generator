"""
S3 utilities for downloading input files and uploading results.
Includes presigned URL support and idempotency checks.
"""
import json
from typing import Optional
import boto3
from botocore.exceptions import ClientError
from aws_lambda_powertools import Logger

logger = Logger(service="s3-utils")


class S3Client:
    """S3 client wrapper with helper methods for Lambda function."""

    def __init__(self, bucket_name: str, region: str = "us-east-1"):
        """
        Initialize S3 client.

        Args:
            bucket_name: Default S3 bucket name
            region: AWS region
        """
        self.bucket_name = bucket_name
        self.s3_client = boto3.client("s3", region_name=region)
        logger.info(f"S3 client initialized for bucket: {bucket_name}, region: {region}")

    def download_file(self, s3_key: str) -> bytes:
        """
        Download a file from S3.

        Args:
            s3_key: S3 object key

        Returns:
            File content as bytes

        Raises:
            ClientError: If download fails
        """
        try:
            logger.info(f"Downloading file from S3: s3://{self.bucket_name}/{s3_key}")
            response = self.s3_client.get_object(Bucket=self.bucket_name, Key=s3_key)
            content = response["Body"].read()
            logger.info(f"Successfully downloaded {len(content)} bytes from {s3_key}")
            return content
        except ClientError as e:
            logger.error(f"Failed to download file from S3: {s3_key}", exc_info=True)
            raise

    def upload_json(self, s3_key: str, data: dict, metadata: Optional[dict] = None) -> str:
        """
        Upload JSON data to S3.

        Args:
            s3_key: S3 object key
            data: Dictionary to upload as JSON
            metadata: Optional S3 object metadata

        Returns:
            S3 URI of uploaded object

        Raises:
            ClientError: If upload fails
        """
        try:
            logger.info(f"Uploading JSON to S3: s3://{self.bucket_name}/{s3_key}")
            json_content = json.dumps(data, indent=2, default=str)
            
            put_args = {
                "Bucket": self.bucket_name,
                "Key": s3_key,
                "Body": json_content.encode("utf-8"),
                "ContentType": "application/json"
            }
            
            if metadata:
                put_args["Metadata"] = metadata
            
            self.s3_client.put_object(**put_args)
            s3_uri = f"s3://{self.bucket_name}/{s3_key}"
            logger.info(f"Successfully uploaded JSON to {s3_uri}")
            return s3_uri
        except ClientError as e:
            logger.error(f"Failed to upload JSON to S3: {s3_key}", exc_info=True)
            raise

    def file_exists(self, s3_key: str) -> bool:
        """
        Check if a file exists in S3 (for idempotency).

        Args:
            s3_key: S3 object key

        Returns:
            True if file exists, False otherwise
        """
        try:
            self.s3_client.head_object(Bucket=self.bucket_name, Key=s3_key)
            logger.info(f"File exists in S3: {s3_key}")
            return True
        except ClientError as e:
            if e.response["Error"]["Code"] == "404":
                logger.info(f"File does not exist in S3: {s3_key}")
                return False
            else:
                logger.error(f"Error checking file existence: {s3_key}", exc_info=True)
                raise

    def get_presigned_url(self, s3_key: str, expiration: int = 3600, method: str = "get_object") -> str:
        """
        Generate a presigned URL for S3 object access.

        Args:
            s3_key: S3 object key
            expiration: URL expiration time in seconds (default 1 hour)
            method: S3 method (get_object or put_object)

        Returns:
            Presigned URL string
        """
        try:
            logger.info(f"Generating presigned URL for {s3_key}, method: {method}")
            url = self.s3_client.generate_presigned_url(
                method,
                Params={"Bucket": self.bucket_name, "Key": s3_key},
                ExpiresIn=expiration
            )
            logger.info("Presigned URL generated successfully")
            return url
        except ClientError as e:
            logger.error(f"Failed to generate presigned URL for {s3_key}", exc_info=True)
            raise

    def download_file_with_presigned_url(self, presigned_url: str) -> bytes:
        """
        Download a file using a presigned URL (alternative to direct download).

        Args:
            presigned_url: Presigned S3 URL

        Returns:
            File content as bytes
        """
        import requests
        
        try:
            logger.info("Downloading file using presigned URL")
            response = requests.get(presigned_url, timeout=60)
            response.raise_for_status()
            logger.info(f"Successfully downloaded {len(response.content)} bytes via presigned URL")
            return response.content
        except Exception as e:
            logger.error("Failed to download file via presigned URL", exc_info=True)
            raise
