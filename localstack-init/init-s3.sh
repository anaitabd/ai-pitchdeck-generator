#!/bin/bash

echo "Initializing LocalStack S3 buckets..."

# Create S3 bucket for file uploads
awslocal s3 mb s3://ai-pitchdeck-uploads
awslocal s3api put-bucket-cors --bucket ai-pitchdeck-uploads --cors-configuration '{
  "CORSRules": [
    {
      "AllowedOrigins": ["*"],
      "AllowedMethods": ["GET", "POST", "PUT", "DELETE", "HEAD"],
      "AllowedHeaders": ["*"],
      "ExposeHeaders": ["ETag"]
    }
  ]
}'

echo "LocalStack initialization complete!"
