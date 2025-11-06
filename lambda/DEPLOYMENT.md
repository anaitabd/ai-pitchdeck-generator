# AWS Lambda Deployment Instructions

## Overview
This document provides step-by-step instructions for deploying the AI Pitch Deck Generator Lambda function to AWS.

## Prerequisites

- AWS CLI installed and configured (`aws configure`)
- Python 3.12 installed locally
- AWS account with appropriate permissions
- Anthropic API key (if not using Bedrock)

## Deployment Options

### Option 1: AWS SAM (Recommended)

#### 1.1 Install AWS SAM CLI

```bash
# macOS
brew install aws-sam-cli

# Linux/WSL
pip install aws-sam-cli

# Verify installation
sam --version
```

#### 1.2 Create SAM Template

Create `template.yaml` in the lambda directory:

```yaml
AWSTemplateFormatVersion: '2010-09-09'
Transform: AWS::Serverless-2016-10-31
Description: AI Pitch Deck Generator Lambda Function

Parameters:
  S3BucketName:
    Type: String
    Default: ai-pitchdeck-uploads
    Description: S3 bucket for uploads and results
  
  AnthropicApiKey:
    Type: String
    NoEcho: true
    Description: Anthropic API key for Claude
  
  UseBedrock:
    Type: String
    Default: 'false'
    AllowedValues: ['true', 'false']
    Description: Use AWS Bedrock instead of direct Anthropic API

Resources:
  PitchDeckGeneratorFunction:
    Type: AWS::Serverless::Function
    Properties:
      FunctionName: ai-pitchdeck-generator
      Runtime: python3.12
      Handler: handler.lambda_handler
      CodeUri: .
      Timeout: 600  # 10 minutes
      MemorySize: 2048  # 2GB for AI processing
      Tracing: Active  # Enable X-Ray
      Environment:
        Variables:
          S3_BUCKET: !Ref S3BucketName
          AWS_REGION: !Ref AWS::Region
          ANTHROPIC_API_KEY: !Ref AnthropicApiKey
          USE_BEDROCK: !Ref UseBedrock
          MAX_RETRIES: '3'
          CALLBACK_TIMEOUT: '30'
          POWERTOOLS_SERVICE_NAME: ai-pitchdeck-generator
          POWERTOOLS_LOG_LEVEL: INFO
          POWERTOOLS_LOGGER_SAMPLE_RATE: 0.1
          POWERTOOLS_TRACE_DISABLED: 'false'
      Policies:
        - S3ReadPolicy:
            BucketName: !Ref S3BucketName
        - S3WritePolicy:
            BucketName: !Ref S3BucketName
        - Statement:
            - Effect: Allow
              Action:
                - bedrock:InvokeModel
                - bedrock:InvokeModelWithResponseStream
              Resource:
                - !Sub 'arn:aws:bedrock:${AWS::Region}::foundation-model/anthropic.claude-3-5-sonnet-*'
        - CloudWatchPutMetricPolicy: {}
        - XRayDaemonWriteAccess: {}

Outputs:
  FunctionArn:
    Description: Lambda Function ARN
    Value: !GetAtt PitchDeckGeneratorFunction.Arn
  
  FunctionName:
    Description: Lambda Function Name
    Value: !Ref PitchDeckGeneratorFunction
```

#### 1.3 Deploy with SAM

```bash
# Navigate to lambda directory
cd lambda

# Build the Lambda function
sam build

# Deploy (first time - guided)
sam deploy --guided

# Deploy (subsequent deployments)
sam deploy
```

Follow the prompts:
- Stack Name: `ai-pitchdeck-generator-stack`
- AWS Region: `us-east-1`
- Parameter S3BucketName: `ai-pitchdeck-uploads`
- Parameter AnthropicApiKey: `[your-api-key]`
- Confirm changes before deploy: `Y`
- Allow SAM CLI IAM role creation: `Y`
- Save arguments to configuration file: `Y`

---

### Option 2: Manual Deployment (ZIP Package)

#### 2.1 Create Deployment Package

```bash
# Navigate to lambda directory
cd lambda

# Create a clean deployment directory
mkdir -p package
cd package

# Install dependencies
pip install -r ../requirements.txt -t .

# Copy Lambda function files
cp ../handler.py .
cp ../models.py .
cp ../s3_utils.py .
cp ../ai_client.py .
cp ../callback_client.py .

# Create ZIP file
zip -r ../lambda-deployment.zip .

# Go back to lambda directory
cd ..
```

#### 2.2 Create IAM Role

```bash
# Create trust policy file
cat > trust-policy.json << 'EOF'
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "lambda.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
EOF

# Create IAM role
aws iam create-role \
  --role-name ai-pitchdeck-lambda-role \
  --assume-role-policy-document file://trust-policy.json

# Attach the custom policy
aws iam put-role-policy \
  --role-name ai-pitchdeck-lambda-role \
  --policy-name ai-pitchdeck-lambda-policy \
  --policy-document file://iam-policy.json

# Attach AWS managed policy for basic Lambda execution
aws iam attach-role-policy \
  --role-name ai-pitchdeck-lambda-role \
  --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole
```

#### 2.3 Create Lambda Function

```bash
# Get the role ARN (replace with actual account ID)
ROLE_ARN="arn:aws:iam::166225579913:role/ai-pitchdeck-lambda-role"

# Create the Lambda function
aws lambda create-function \
  --function-name ai-pitchdeck-generator \
  --runtime python3.12 \
  --role $ROLE_ARN \
  --handler handler.lambda_handler \
  --zip-file fileb://lambda-deployment.zip \
  --timeout 600 \
  --memory-size 2048 \
  --environment Variables="{
    S3_BUCKET=ai-pitchdeck-uploads,
    AWS_REGION=us-east-1,
    ANTHROPIC_API_KEY=your-api-key-here,
    USE_BEDROCK=false,
    MAX_RETRIES=3,
    CALLBACK_TIMEOUT=30,
    POWERTOOLS_SERVICE_NAME=ai-pitchdeck-generator,
    POWERTOOLS_LOG_LEVEL=INFO
  }" \
  --tracing-config Mode=Active
```

#### 2.4 Update Lambda Function (for code changes)

```bash
# Rebuild the package
cd package
zip -r ../lambda-deployment.zip .
cd ..

# Update function code
aws lambda update-function-code \
  --function-name ai-pitchdeck-generator \
  --zip-file fileb://lambda-deployment.zip
```

---

### Option 3: AWS CDK (Infrastructure as Code)

#### 3.1 Create CDK App Structure

```bash
# Install AWS CDK
npm install -g aws-cdk

# Create CDK app
mkdir cdk
cd cdk
cdk init app --language python
```

#### 3.2 CDK Stack Definition

Create `cdk/cdk_stack.py`:

```python
from aws_cdk import (
    Stack,
    Duration,
    aws_lambda as lambda_,
    aws_iam as iam,
    aws_s3 as s3,
    CfnOutput
)
from constructs import Construct

class PitchDeckGeneratorStack(Stack):
    def __init__(self, scope: Construct, construct_id: str, **kwargs) -> None:
        super().__init__(scope, construct_id, **kwargs)

        # S3 Bucket (or reference existing)
        bucket = s3.Bucket.from_bucket_name(
            self, "PitchDeckBucket",
            bucket_name="ai-pitchdeck-uploads"
        )

        # Lambda function
        lambda_function = lambda_.Function(
            self, "PitchDeckGenerator",
            function_name="ai-pitchdeck-generator",
            runtime=lambda_.Runtime.PYTHON_3_12,
            handler="handler.lambda_handler",
            code=lambda_.Code.from_asset("../lambda"),
            timeout=Duration.minutes(10),
            memory_size=2048,
            tracing=lambda_.Tracing.ACTIVE,
            environment={
                "S3_BUCKET": bucket.bucket_name,
                "ANTHROPIC_API_KEY": "your-api-key",  # Use Secrets Manager in production
                "USE_BEDROCK": "false",
                "MAX_RETRIES": "3",
                "CALLBACK_TIMEOUT": "30",
                "POWERTOOLS_SERVICE_NAME": "ai-pitchdeck-generator",
                "POWERTOOLS_LOG_LEVEL": "INFO"
            }
        )

        # Grant S3 permissions
        bucket.grant_read_write(lambda_function)

        # Grant Bedrock permissions (if using)
        lambda_function.add_to_role_policy(
            iam.PolicyStatement(
                actions=[
                    "bedrock:InvokeModel",
                    "bedrock:InvokeModelWithResponseStream"
                ],
                resources=[
                    "arn:aws:bedrock:*::foundation-model/anthropic.claude-3-5-sonnet-*"
                ]
            )
        )

        # Outputs
        CfnOutput(self, "FunctionArn", value=lambda_function.function_arn)
        CfnOutput(self, "FunctionName", value=lambda_function.function_name)
```

#### 3.3 Deploy with CDK

```bash
cd cdk
pip install -r requirements.txt
cdk bootstrap  # First time only
cdk deploy
```

---

## Post-Deployment Configuration

### 1. Test the Function

```bash
# Invoke with test event
aws lambda invoke \
  --function-name ai-pitchdeck-generator \
  --payload file://test-event.json \
  --cli-binary-format raw-in-base64-out \
  response.json

# View the response
cat response.json
```

### 2. View Logs

```bash
# Get recent logs
aws logs tail /aws/lambda/ai-pitchdeck-generator --follow

# Or use SAM
sam logs --name ai-pitchdeck-generator --tail
```

### 3. Configure Environment Variables (if needed)

```bash
aws lambda update-function-configuration \
  --function-name ai-pitchdeck-generator \
  --environment Variables="{
    S3_BUCKET=ai-pitchdeck-uploads,
    ANTHROPIC_API_KEY=new-key
  }"
```

### 4. Enable CloudWatch Alarms

```bash
# Create alarm for errors
aws cloudwatch put-metric-alarm \
  --alarm-name ai-pitchdeck-generator-errors \
  --alarm-description "Alert on Lambda errors" \
  --metric-name Errors \
  --namespace AWS/Lambda \
  --statistic Sum \
  --period 300 \
  --evaluation-periods 1 \
  --threshold 1 \
  --comparison-operator GreaterThanThreshold \
  --dimensions Name=FunctionName,Value=ai-pitchdeck-generator
```

---

## Integration with Spring Boot Backend

The Spring Boot backend can invoke the Lambda function using:

```java
// In your GenerationService.java
@Autowired
private LambdaClient lambdaClient;

public void triggerAIGeneration(GenerationJob job) {
    Map<String, Object> payload = Map.of(
        "jobId", job.getId(),
        "projectId", job.getProjectId(),
        "userId", job.getUserId(),
        "s3Key", job.getInputS3Key(),
        "callbackUrl", callbackUrl,
        "llmModel", job.getAiModel()
    );
    
    InvokeRequest request = InvokeRequest.builder()
        .functionName("ai-pitchdeck-generator")
        .invocationType(InvocationType.EVENT)  // Async
        .payload(SdkBytes.fromUtf8String(new ObjectMapper().writeValueAsString(payload)))
        .build();
    
    lambdaClient.invoke(request);
}
```

---

## Monitoring and Troubleshooting

### CloudWatch Logs
- Log Group: `/aws/lambda/ai-pitchdeck-generator`
- Structured JSON logs via AWS Lambda Powertools

### X-Ray Tracing
- View traces in AWS X-Ray console
- Analyze performance bottlenecks

### Common Issues

1. **Timeout errors**: Increase Lambda timeout (max 15 minutes)
2. **Memory errors**: Increase memory size (up to 10GB)
3. **API key errors**: Check ANTHROPIC_API_KEY environment variable
4. **S3 permission errors**: Verify IAM policy

---

## Cost Optimization

1. **Use Provisioned Concurrency** for consistent performance
2. **Enable Lambda Insights** for detailed monitoring
3. **Use Bedrock** instead of Anthropic API for cost savings
4. **Configure Reserved Capacity** for predictable workloads

---

## Security Best Practices

1. **Use AWS Secrets Manager** for API keys
2. **Enable VPC** for private resources
3. **Use KMS** for S3 encryption
4. **Implement least-privilege IAM** policies
5. **Enable CloudTrail** for audit logging

---

## Cleanup

```bash
# Delete SAM stack
sam delete

# Or delete manually
aws lambda delete-function --function-name ai-pitchdeck-generator
aws iam delete-role --role-name ai-pitchdeck-lambda-role
```
