# Quick Reference Card

## 🎯 Lambda Function Overview

**Name**: `ai-pitchdeck-generator`  
**Runtime**: Python 3.12  
**Purpose**: Generate AI pitch decks using Claude Sonnet 4.5  
**ARN**: `arn:aws:lambda:us-east-1:166225579913:function:ai-pitchdeck-generator`

## 📦 Files (16 total)

### Core Code (5 files)
```
handler.py          - Main Lambda handler (300 lines)
models.py           - Pydantic schemas (130 lines)
ai_client.py        - Claude AI client (260 lines)
s3_utils.py         - S3 utilities (150 lines)
callback_client.py  - HTTP client (150 lines)
```

### Configuration (3 files)
```
requirements.txt    - Dependencies (9 packages)
iam-policy.json     - IAM permissions
test-event.json     - Example test payload
```

### Deployment (2 files)
```
template.yaml       - AWS SAM template
.gitignore          - Git exclusions
```

### Documentation (5 files)
```
README.md                         - Overview & features
DEPLOYMENT.md                     - Deployment guide
SPRING_BOOT_INTEGRATION.md        - Backend integration
CLOUDFORMATION_CDK_EXAMPLES.md    - IaC examples
SUMMARY.md                        - Complete summary
```

### Testing (1 file)
```
validate.py         - Validation script
```

## ⚡ Quick Commands

### Deploy
```bash
cd lambda
sam build && sam deploy --guided
```

### Test Locally
```bash
sam local invoke -e test-event.json
```

### Validate
```bash
python3 validate.py
```

### View Logs
```bash
sam logs --name ai-pitchdeck-generator --tail
```

### Update Code
```bash
sam build && sam deploy
```

## 🔧 Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| S3_BUCKET | ✅ | ai-pitchdeck-uploads | S3 bucket name |
| ANTHROPIC_API_KEY | ✅* | - | Anthropic API key |
| AWS_REGION | ✅ | us-east-1 | AWS region |
| USE_BEDROCK | ❌ | false | Use Bedrock instead |
| MAX_RETRIES | ❌ | 3 | Callback retries |
| CALLBACK_TIMEOUT | ❌ | 30 | Callback timeout (s) |

*Required if USE_BEDROCK=false

## 📊 Input Event

```json
{
  "jobId": "uuid",
  "projectId": "uuid",
  "userId": "uuid",
  "s3Key": "uploads/.../file.pdf",
  "callbackUrl": "http://backend/callback",
  "llmModel": "claude-sonnet-4-20250514",
  "systemPrompt": "optional",
  "userPrompt": "optional"
}
```

## 📤 Output

**Location**: `s3://bucket/pitchdecks/{jobId}/result.json`

**Structure**:
```json
{
  "slides": [
    {"title": "...", "content": "...", "type": "..."}
  ],
  "metadata": {
    "model": "...",
    "durationMs": 5000,
    "generatedAt": "...",
    "inputTokens": 1500,
    "outputTokens": 3000
  }
}
```

## 🔄 Callback

**Endpoint**: POST {callbackUrl}

**Payload**:
```json
{
  "jobId": "uuid",
  "status": "COMPLETED|FAILED",
  "outputS3Key": "pitchdecks/{jobId}/result.json",
  "errorMessage": "...",
  "generatedAt": "2025-11-06T16:00:00Z"
}
```

## 🏗️ Lambda Config

```yaml
Runtime: python3.12
Memory: 2048 MB
Timeout: 600s (10 min)
Tracing: Active (X-Ray)
Concurrency: Unlimited
```

## 🔐 IAM Permissions

- ✅ CloudWatch Logs (write)
- ✅ S3 (read uploads/*, write pitchdecks/*)
- ✅ Bedrock (invoke model)
- ✅ X-Ray (tracing)

## 📈 Monitoring

**CloudWatch Logs**: `/aws/lambda/ai-pitchdeck-generator`  
**X-Ray**: Enabled  
**Alarms**: Errors > 1, Throttles > 1  

## 🧪 Testing Checklist

- [x] Python syntax valid
- [x] Pydantic models work
- [x] Test event validates
- [x] JSON serialization works
- [ ] Deploy to AWS
- [ ] Invoke with real event
- [ ] Verify S3 upload
- [ ] Test callback
- [ ] Check CloudWatch logs

## 💰 Estimated Costs

**Lambda**: ~$0.20 per 1000 requests (2GB, 1min avg)  
**Anthropic API**: ~$0.015 per request (3K tokens)  
**S3**: Negligible  
**CloudWatch**: ~$1/month  

**Total**: ~$16 per 1000 generations

## 🚨 Troubleshooting

| Issue | Solution |
|-------|----------|
| Timeout | Increase timeout, check AI latency |
| Memory | Increase to 3GB or 4GB |
| Import error | Verify requirements.txt |
| S3 permission | Check IAM policy |
| Callback fails | Verify backend URL reachable |

## 📞 Support

- **Logs**: `sam logs --name ai-pitchdeck-generator --tail`
- **Config**: `aws lambda get-function --function-name ai-pitchdeck-generator`
- **Metrics**: CloudWatch Console → Lambda → ai-pitchdeck-generator
- **Traces**: X-Ray Console → Service Map

## 🎓 Learn More

1. Read `README.md` for overview
2. Follow `DEPLOYMENT.md` for setup
3. Use `SPRING_BOOT_INTEGRATION.md` for backend
4. Reference `CLOUDFORMATION_CDK_EXAMPLES.md` for IaC
5. Check `SUMMARY.md` for complete details

## ✅ Production Checklist

Before going live:

- [ ] Deploy to production account
- [ ] Store API key in Secrets Manager
- [ ] Configure CloudWatch alarms
- [ ] Set up monitoring dashboard
- [ ] Test with production backend
- [ ] Document runbook
- [ ] Set up log retention
- [ ] Configure VPC (if needed)
- [ ] Enable Reserved Concurrency
- [ ] Test DR procedures

---

**Version**: 1.0  
**Last Updated**: 2025-11-06  
**Python**: 3.12  
**AWS Region**: us-east-1
