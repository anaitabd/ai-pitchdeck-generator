# AI Pitch Deck Generator - AWS Lambda Function

Production-ready AWS Lambda function for generating AI-powered pitch decks using Claude Sonnet 4.5.

## 📋 Overview

This Lambda function is triggered asynchronously by the Spring Boot backend to generate professional pitch decks from uploaded business documents. It uses Claude Sonnet 4.5 (via Anthropic SDK or AWS Bedrock) to analyze documents and create structured pitch deck presentations.

### Key Features

✅ **Asynchronous Processing** - Non-blocking, event-driven architecture  
✅ **Idempotent** - Safe to retry, prevents duplicate processing  
✅ **Production-Ready** - Error handling, logging, tracing, timeouts  
✅ **Scalable** - Handles concurrent requests efficiently  
✅ **Observability** - AWS Lambda Powertools (structured logs, X-Ray traces)  
✅ **Flexible AI** - Supports both Anthropic API and AWS Bedrock  

## 🏗️ Architecture

```
┌─────────────────┐
│  Spring Boot    │
│    Backend      │
└────────┬────────┘
         │ (async invoke)
         ▼
┌─────────────────────────────────────┐
│  AWS Lambda                         │
│  ┌─────────────────────────────┐   │
│  │ 1. Validate Event           │   │
│  │ 2. Check Idempotency (S3)   │   │
│  │ 3. Download from S3         │   │
│  │ 4. Extract Text             │   │
│  │ 5. Call Claude AI           │   │
│  │ 6. Upload Result to S3      │   │
│  │ 7. Callback to Backend      │   │
│  └─────────────────────────────┘   │
└─────────────────────────────────────┘
         │                   │
         ▼                   ▼
   ┌─────────┐         ┌──────────┐
   │   S3    │         │ Backend  │
   │ Bucket  │         │ Callback │
   └─────────┘         └──────────┘
```

## 📁 File Structure

```
lambda/
├── handler.py              # Main Lambda handler (orchestration)
├── models.py               # Pydantic schemas for validation
├── ai_client.py           # Claude AI client wrapper
├── s3_utils.py            # S3 download/upload utilities
├── callback_client.py     # HTTP callback to backend
├── requirements.txt       # Python dependencies
├── template.yaml          # AWS SAM template
├── iam-policy.json        # IAM policy document
├── test-event.json        # Example test event
├── DEPLOYMENT.md          # Deployment instructions
└── README.md              # This file
```

## 🔧 Components

### handler.py
Main Lambda handler with:
- Event validation using Pydantic
- Idempotency checks
- Error handling and retries
- Structured logging (JSON)
- X-Ray tracing
- Timeout protection

### models.py
Pydantic models for:
- **LambdaEvent**: Input event schema validation
- **PitchDeckOutput**: Output JSON structure
- **CallbackPayload**: Backend callback payload

### ai_client.py
AI integration supporting:
- **Anthropic SDK**: Direct Claude API calls
- **AWS Bedrock**: Alternative AI platform
- Configurable models, prompts, temperature
- Token usage tracking

### s3_utils.py
S3 operations:
- Download input files
- Upload result JSON
- Presigned URL support
- Idempotency file checks

### callback_client.py
HTTP client with:
- Retry logic (exponential backoff)
- Success/failure callbacks
- Timeout handling

## 🚀 Quick Start

### Prerequisites

```bash
# Install AWS SAM CLI
brew install aws-sam-cli  # macOS
pip install aws-sam-cli   # Linux/Windows

# Configure AWS credentials
aws configure
```

### Deploy with SAM

```bash
# Navigate to lambda directory
cd lambda

# Build
sam build

# Deploy (guided)
sam deploy --guided
```

### Test Locally

```bash
# Invoke locally with test event
sam local invoke -e test-event.json

# Start local API
sam local start-api
```

## 📝 Event Schema

**Input Event:**
```json
{
  "jobId": "550e8400-e29b-41d4-a716-446655440000",
  "projectId": "660e8400-e29b-41d4-a716-446655440001",
  "userId": "770e8400-e29b-41d4-a716-446655440002",
  "s3Key": "uploads/user-id/project-id/file.pdf",
  "callbackUrl": "http://localhost:8080/api/v1/generate/callback",
  "llmModel": "claude-sonnet-4-20250514",
  "systemPrompt": "Optional custom system prompt",
  "userPrompt": "Optional custom user prompt"
}
```

**Output (S3: pitchdecks/{jobId}/result.json):**
```json
{
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
    "generatedAt": "2025-11-06T16:00:00Z",
    "inputTokens": 1500,
    "outputTokens": 3000
  }
}
```

**Callback Payload:**
```json
{
  "jobId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "COMPLETED",
  "outputS3Key": "pitchdecks/550e8400-.../result.json",
  "generatedAt": "2025-11-06T16:00:00Z"
}
```

## ⚙️ Configuration

### Environment Variables

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `S3_BUCKET` | S3 bucket for uploads/results | `ai-pitchdeck-uploads` | Yes |
| `AWS_REGION` | AWS region | `us-east-1` | Yes |
| `ANTHROPIC_API_KEY` | Anthropic API key | - | Yes* |
| `USE_BEDROCK` | Use Bedrock instead of Anthropic | `false` | No |
| `MAX_RETRIES` | Callback retry attempts | `3` | No |
| `CALLBACK_TIMEOUT` | Callback timeout (seconds) | `30` | No |
| `POWERTOOLS_SERVICE_NAME` | Service name for logs | - | No |
| `POWERTOOLS_LOG_LEVEL` | Log level (DEBUG/INFO/WARN/ERROR) | `INFO` | No |

*Required if `USE_BEDROCK=false`

### Lambda Configuration

- **Runtime**: Python 3.12
- **Memory**: 2048 MB (recommended)
- **Timeout**: 600 seconds (10 minutes)
- **Concurrency**: As needed (default unlimited)

## 🔐 IAM Permissions

Required permissions (see `iam-policy.json`):

- **CloudWatch Logs**: Create/write logs
- **S3**: Read input files, write results
- **Bedrock**: Invoke AI models (if using Bedrock)
- **X-Ray**: Write traces

## 📊 Monitoring

### CloudWatch Logs

```bash
# View logs
aws logs tail /aws/lambda/ai-pitchdeck-generator --follow

# Or with SAM
sam logs --name ai-pitchdeck-generator --tail
```

### X-Ray Traces

View performance traces in AWS X-Ray console to identify bottlenecks.

### CloudWatch Metrics

- **Invocations**: Total function calls
- **Errors**: Failed executions
- **Duration**: Execution time
- **Throttles**: Rate-limited requests

### Custom Metrics (via Powertools)

```python
from aws_lambda_powertools import Metrics

metrics = Metrics(namespace="AIPitchDeck")
metrics.add_metric(name="PitchDeckGenerated", unit="Count", value=1)
```

## 🧪 Testing

### Unit Tests (Local)

```bash
# Install dev dependencies
pip install pytest pytest-cov moto

# Run tests
pytest tests/ -v --cov=.
```

### Integration Test

```bash
# Invoke with test event
aws lambda invoke \
  --function-name ai-pitchdeck-generator \
  --payload file://test-event.json \
  --cli-binary-format raw-in-base64-out \
  response.json

# Check response
cat response.json
```

## 🐛 Troubleshooting

### Common Issues

1. **Timeout Errors**
   - Increase Lambda timeout (max 15 minutes)
   - Check AI API response time

2. **Memory Errors**
   - Increase memory allocation
   - Optimize document parsing

3. **API Key Errors**
   - Verify `ANTHROPIC_API_KEY` environment variable
   - Check API key validity

4. **S3 Permission Errors**
   - Verify IAM policy
   - Check bucket name and keys

5. **Callback Failures**
   - Check backend endpoint availability
   - Review callback retry logs

## 🔄 Idempotency

The function checks if `pitchdecks/{jobId}/result.json` already exists before processing. If found, it skips generation and returns the existing result, ensuring safe retries.

## 💰 Cost Optimization

1. **Memory Tuning**: Right-size memory (test 1GB vs 2GB)
2. **Bedrock vs Anthropic**: Bedrock may be cheaper for high volume
3. **Reserved Concurrency**: Use for predictable workloads
4. **Log Retention**: Set appropriate retention period (e.g., 30 days)

## 🔒 Security Best Practices

1. **Secrets Manager**: Store API keys in AWS Secrets Manager
2. **VPC**: Deploy in VPC for private resources
3. **KMS Encryption**: Encrypt S3 objects with KMS
4. **Least Privilege**: Use minimal IAM permissions
5. **CloudTrail**: Enable audit logging

## 📚 Additional Resources

- [AWS Lambda Best Practices](https://docs.aws.amazon.com/lambda/latest/dg/best-practices.html)
- [AWS Lambda Powertools](https://awslabs.github.io/aws-lambda-powertools-python/)
- [Anthropic Claude API](https://docs.anthropic.com/claude/reference)
- [AWS Bedrock](https://docs.aws.amazon.com/bedrock/)

## 📄 License

See main repository LICENSE file.

## 🆘 Support

For issues:
- Check CloudWatch Logs
- Review X-Ray traces
- Open GitHub issue
