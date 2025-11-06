# Lambda Function Summary

## 📦 Deliverables

This directory contains a **production-ready AWS Lambda function** for AI-powered pitch deck generation using Claude Sonnet 4.5.

### Core Components ✅

1. **handler.py** - Main Lambda handler
   - Event validation with Pydantic
   - Orchestration of S3, AI, and callback operations
   - Idempotency checks
   - Error handling and structured logging
   - AWS Lambda Powertools integration (Logger, Tracer)

2. **models.py** - Pydantic schemas
   - `LambdaEvent`: Input validation
   - `PitchDeckOutput`: Result structure
   - `CallbackPayload`: Backend callback
   - Type-safe with UUID and datetime support

3. **ai_client.py** - AI integration
   - Claude Sonnet 4.5 via Anthropic SDK
   - AWS Bedrock support (alternative)
   - Configurable prompts and parameters
   - Token usage tracking
   - JSON extraction from AI responses

4. **s3_utils.py** - S3 operations
   - Download input files
   - Upload result JSON
   - Presigned URL generation
   - Idempotency file checks
   - Error handling

5. **callback_client.py** - HTTP client
   - POST to Spring Boot backend
   - Retry logic (exponential backoff)
   - Success/failure callbacks
   - Timeout handling

6. **requirements.txt** - Dependencies
   - aws-lambda-powertools==2.31.0
   - pydantic==2.5.3
   - boto3==1.34.19
   - httpx==0.26.0
   - anthropic==0.8.1

### Deployment Artifacts ✅

7. **template.yaml** - AWS SAM template
   - Complete CloudFormation template
   - Environment variables
   - IAM policies
   - CloudWatch alarms

8. **iam-policy.json** - IAM permissions
   - S3 read/write
   - Bedrock invoke
   - CloudWatch Logs
   - X-Ray tracing

9. **test-event.json** - Example event
   - Valid test payload
   - Ready for console testing

### Documentation ✅

10. **README.md** - Lambda overview
    - Architecture diagram
    - Component descriptions
    - Configuration guide
    - Monitoring and troubleshooting

11. **DEPLOYMENT.md** - Deployment guide
    - AWS SAM deployment (recommended)
    - Manual ZIP deployment
    - AWS CDK examples
    - Post-deployment configuration

12. **CLOUDFORMATION_CDK_EXAMPLES.md** - IaC examples
    - CloudFormation template
    - AWS CDK (Python)
    - Terraform configuration

13. **SPRING_BOOT_INTEGRATION.md** - Backend integration
    - GenerationService updates
    - Callback controller/service
    - Testing examples
    - Error handling

14. **validate.py** - Validation script
    - Model testing
    - Import verification
    - JSON serialization tests

15. **.gitignore** - Git exclusions
    - Python artifacts
    - AWS SAM build
    - Deployment packages

## 🎯 Key Features

### Production-Ready
- ✅ Error handling and retries
- ✅ Structured JSON logging
- ✅ X-Ray distributed tracing
- ✅ Idempotency support
- ✅ Timeout protection

### Scalable
- ✅ Asynchronous processing
- ✅ Stateless design
- ✅ S3-based file handling
- ✅ Configurable concurrency

### Observable
- ✅ AWS Lambda Powertools
- ✅ CloudWatch metrics
- ✅ X-Ray traces
- ✅ Detailed error logs

### Flexible
- ✅ Anthropic SDK or Bedrock
- ✅ Configurable AI models
- ✅ Custom prompts
- ✅ Presigned URL support

## 📊 Output Structure

The Lambda generates JSON pitch decks with this structure:

```json
{
  "slides": [
    {
      "title": "Company Overview",
      "content": "Detailed slide content...",
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

Stored at S3: `pitchdecks/{jobId}/result.json`

## 🚀 Quick Deployment

### Option 1: AWS SAM (Recommended)

```bash
cd lambda
sam build
sam deploy --guided
```

### Option 2: Manual

```bash
cd lambda
pip install -r requirements.txt -t package/
cp *.py package/
cd package && zip -r ../lambda-deployment.zip .
aws lambda create-function --function-name ai-pitchdeck-generator ...
```

## ⚙️ Required Configuration

### Environment Variables
- `S3_BUCKET` - S3 bucket name
- `ANTHROPIC_API_KEY` - Anthropic API key
- `AWS_REGION` - AWS region

### Lambda Settings
- **Runtime**: Python 3.12
- **Memory**: 2048 MB
- **Timeout**: 600s (10 min)
- **Tracing**: Active (X-Ray)

### IAM Permissions
- S3 read/write
- Bedrock invoke (if using)
- CloudWatch Logs
- X-Ray

## 🧪 Testing

### Validate Code
```bash
python3 validate.py
```

### Test Locally
```bash
sam local invoke -e test-event.json
```

### Integration Test
```bash
aws lambda invoke \
  --function-name ai-pitchdeck-generator \
  --payload file://test-event.json \
  response.json
```

## 📈 Monitoring

### CloudWatch Logs
```bash
aws logs tail /aws/lambda/ai-pitchdeck-generator --follow
```

### Metrics
- Invocations
- Errors
- Duration
- Throttles

### Alarms
- Error rate > 1 (5min)
- Throttle rate > 1 (5min)

## 🔒 Security

- ✅ IAM least-privilege permissions
- ✅ Secrets Manager for API keys (recommended)
- ✅ VPC deployment option
- ✅ S3 encryption support
- ✅ CloudTrail audit logging

## 💰 Cost Optimization

- Right-size memory (test 1-2 GB)
- Use Bedrock for high volume
- Set log retention (30 days)
- Monitor invocation duration
- Consider reserved concurrency

## 🔄 Workflow

```
1. Spring Boot creates job
2. Backend invokes Lambda (async)
3. Lambda validates event
4. Lambda checks idempotency
5. Lambda downloads from S3
6. Lambda generates with Claude
7. Lambda uploads result to S3
8. Lambda POSTs callback
9. Backend updates job status
10. User receives notification
```

## 📋 Checklist for Production

- [ ] Deploy Lambda to production account
- [ ] Configure environment variables
- [ ] Store API key in Secrets Manager
- [ ] Set up CloudWatch alarms
- [ ] Configure X-Ray tracing
- [ ] Test with real documents
- [ ] Verify callback integration
- [ ] Set up monitoring dashboard
- [ ] Document runbook
- [ ] Plan DR/backup strategy

## 🆘 Troubleshooting

### Common Issues

1. **Timeout** → Increase timeout, check AI API latency
2. **Memory error** → Increase memory allocation
3. **Import error** → Verify requirements.txt packages
4. **S3 permission** → Check IAM policy
5. **Callback fails** → Verify backend endpoint

### Debug Commands

```bash
# View logs
sam logs --name ai-pitchdeck-generator --tail

# Get function config
aws lambda get-function-configuration --function-name ai-pitchdeck-generator

# Update environment
aws lambda update-function-configuration \
  --function-name ai-pitchdeck-generator \
  --environment Variables="{...}"
```

## 📚 Resources

- [AWS Lambda Best Practices](https://docs.aws.amazon.com/lambda/latest/dg/best-practices.html)
- [Lambda Powertools Python](https://awslabs.github.io/aws-lambda-powertools-python/)
- [Anthropic Claude API](https://docs.anthropic.com/claude/reference)
- [AWS Bedrock](https://docs.aws.amazon.com/bedrock/)
- [Pydantic Documentation](https://docs.pydantic.dev/)

## ✅ Quality Assurance

All components have been validated:
- ✅ Python syntax checked
- ✅ Pydantic models tested
- ✅ Event schema validated
- ✅ JSON serialization verified
- ✅ Import paths confirmed

## 🎉 Ready for Deployment

This Lambda function is **production-ready** and includes:
- Complete implementation
- Comprehensive documentation
- Deployment templates (SAM, CloudFormation, CDK, Terraform)
- Testing and validation
- Integration guide
- Monitoring and observability

Deploy with confidence! 🚀
