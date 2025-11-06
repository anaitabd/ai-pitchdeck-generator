# CloudFormation Snippet for AI Pitch Deck Generator Lambda

## Option 1: Standalone CloudFormation Template

```yaml
AWSTemplateFormatVersion: '2010-09-09'
Description: AI Pitch Deck Generator Lambda Function (CloudFormation)

Parameters:
  S3BucketName:
    Type: String
    Default: ai-pitchdeck-uploads
  
  LambdaCodeS3Bucket:
    Type: String
    Description: S3 bucket containing the Lambda deployment package
  
  LambdaCodeS3Key:
    Type: String
    Default: lambda/ai-pitchdeck-generator.zip
    Description: S3 key for Lambda deployment package
  
  AnthropicApiKey:
    Type: String
    NoEcho: true
    Description: Anthropic API key

Resources:
  # IAM Role
  LambdaExecutionRole:
    Type: AWS::IAM::Role
    Properties:
      RoleName: ai-pitchdeck-lambda-execution-role
      AssumeRolePolicyDocument:
        Version: '2012-10-17'
        Statement:
          - Effect: Allow
            Principal:
              Service: lambda.amazonaws.com
            Action: sts:AssumeRole
      ManagedPolicyArns:
        - arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole
        - arn:aws:iam::aws:policy/AWSXRayDaemonWriteAccess
      Policies:
        - PolicyName: S3Access
          PolicyDocument:
            Version: '2012-10-17'
            Statement:
              - Effect: Allow
                Action:
                  - s3:GetObject
                  - s3:PutObject
                  - s3:HeadObject
                Resource:
                  - !Sub 'arn:aws:s3:::${S3BucketName}/*'
        - PolicyName: BedrockAccess
          PolicyDocument:
            Version: '2012-10-17'
            Statement:
              - Effect: Allow
                Action:
                  - bedrock:InvokeModel
                  - bedrock:InvokeModelWithResponseStream
                Resource:
                  - !Sub 'arn:aws:bedrock:${AWS::Region}::foundation-model/anthropic.claude-3-5-sonnet-*'

  # Lambda Function
  PitchDeckGeneratorFunction:
    Type: AWS::Lambda::Function
    Properties:
      FunctionName: ai-pitchdeck-generator
      Runtime: python3.12
      Handler: handler.lambda_handler
      Role: !GetAtt LambdaExecutionRole.Arn
      Code:
        S3Bucket: !Ref LambdaCodeS3Bucket
        S3Key: !Ref LambdaCodeS3Key
      Timeout: 600
      MemorySize: 2048
      Environment:
        Variables:
          S3_BUCKET: !Ref S3BucketName
          ANTHROPIC_API_KEY: !Ref AnthropicApiKey
          USE_BEDROCK: 'false'
          MAX_RETRIES: '3'
          CALLBACK_TIMEOUT: '30'
          POWERTOOLS_SERVICE_NAME: ai-pitchdeck-generator
          POWERTOOLS_LOG_LEVEL: INFO
      TracingConfig:
        Mode: Active
      Tags:
        - Key: Project
          Value: ai-pitchdeck-generator

  # CloudWatch Log Group
  FunctionLogGroup:
    Type: AWS::Logs::LogGroup
    Properties:
      LogGroupName: !Sub /aws/lambda/${PitchDeckGeneratorFunction}
      RetentionInDays: 30

  # CloudWatch Alarm - Errors
  ErrorAlarm:
    Type: AWS::CloudWatch::Alarm
    Properties:
      AlarmName: !Sub ${PitchDeckGeneratorFunction}-errors
      MetricName: Errors
      Namespace: AWS/Lambda
      Statistic: Sum
      Period: 300
      EvaluationPeriods: 1
      Threshold: 1
      ComparisonOperator: GreaterThanThreshold
      Dimensions:
        - Name: FunctionName
          Value: !Ref PitchDeckGeneratorFunction

Outputs:
  FunctionArn:
    Value: !GetAtt PitchDeckGeneratorFunction.Arn
    Export:
      Name: PitchDeckGeneratorFunctionArn
  
  FunctionName:
    Value: !Ref PitchDeckGeneratorFunction
```

## Option 2: AWS CDK (Python)

```python
from aws_cdk import (
    Stack,
    Duration,
    aws_lambda as lambda_,
    aws_iam as iam,
    aws_logs as logs,
    aws_cloudwatch as cloudwatch,
    CfnOutput
)
from constructs import Construct

class PitchDeckGeneratorStack(Stack):
    def __init__(self, scope: Construct, construct_id: str, **kwargs) -> None:
        super().__init__(scope, construct_id, **kwargs)

        # IAM Role
        lambda_role = iam.Role(
            self, "LambdaExecutionRole",
            assumed_by=iam.ServicePrincipal("lambda.amazonaws.com"),
            managed_policies=[
                iam.ManagedPolicy.from_aws_managed_policy_name(
                    "service-role/AWSLambdaBasicExecutionRole"
                ),
                iam.ManagedPolicy.from_aws_managed_policy_name(
                    "AWSXRayDaemonWriteAccess"
                )
            ]
        )

        # Add S3 permissions
        lambda_role.add_to_policy(
            iam.PolicyStatement(
                effect=iam.Effect.ALLOW,
                actions=[
                    "s3:GetObject",
                    "s3:PutObject",
                    "s3:HeadObject"
                ],
                resources=["arn:aws:s3:::ai-pitchdeck-uploads/*"]
            )
        )

        # Add Bedrock permissions
        lambda_role.add_to_policy(
            iam.PolicyStatement(
                effect=iam.Effect.ALLOW,
                actions=[
                    "bedrock:InvokeModel",
                    "bedrock:InvokeModelWithResponseStream"
                ],
                resources=[
                    f"arn:aws:bedrock:{self.region}::foundation-model/anthropic.claude-3-5-sonnet-*"
                ]
            )
        )

        # Lambda Function
        function = lambda_.Function(
            self, "PitchDeckGenerator",
            function_name="ai-pitchdeck-generator",
            runtime=lambda_.Runtime.PYTHON_3_12,
            handler="handler.lambda_handler",
            code=lambda_.Code.from_asset("lambda"),
            role=lambda_role,
            timeout=Duration.minutes(10),
            memory_size=2048,
            tracing=lambda_.Tracing.ACTIVE,
            environment={
                "S3_BUCKET": "ai-pitchdeck-uploads",
                "ANTHROPIC_API_KEY": "your-api-key",  # Use Secrets Manager
                "USE_BEDROCK": "false",
                "MAX_RETRIES": "3",
                "CALLBACK_TIMEOUT": "30",
                "POWERTOOLS_SERVICE_NAME": "ai-pitchdeck-generator",
                "POWERTOOLS_LOG_LEVEL": "INFO"
            },
            log_retention=logs.RetentionDays.ONE_MONTH
        )

        # CloudWatch Alarms
        function.metric_errors().create_alarm(
            self, "ErrorAlarm",
            threshold=1,
            evaluation_periods=1,
            alarm_name=f"{function.function_name}-errors"
        )

        function.metric_throttles().create_alarm(
            self, "ThrottleAlarm",
            threshold=1,
            evaluation_periods=1,
            alarm_name=f"{function.function_name}-throttles"
        )

        # Outputs
        CfnOutput(self, "FunctionArn", value=function.function_arn)
        CfnOutput(self, "FunctionName", value=function.function_name)
```

## Option 3: Terraform

```hcl
# Lambda IAM Role
resource "aws_iam_role" "lambda_role" {
  name = "ai-pitchdeck-lambda-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "lambda.amazonaws.com"
      }
    }]
  })
}

# Attach policies
resource "aws_iam_role_policy_attachment" "lambda_basic" {
  role       = aws_iam_role.lambda_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

resource "aws_iam_role_policy_attachment" "lambda_xray" {
  role       = aws_iam_role.lambda_role.name
  policy_arn = "arn:aws:iam::aws:policy/AWSXRayDaemonWriteAccess"
}

# S3 and Bedrock policy
resource "aws_iam_role_policy" "lambda_policy" {
  name = "lambda-s3-bedrock-policy"
  role = aws_iam_role.lambda_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "s3:GetObject",
          "s3:PutObject",
          "s3:HeadObject"
        ]
        Resource = "arn:aws:s3:::ai-pitchdeck-uploads/*"
      },
      {
        Effect = "Allow"
        Action = [
          "bedrock:InvokeModel",
          "bedrock:InvokeModelWithResponseStream"
        ]
        Resource = "arn:aws:bedrock:us-east-1::foundation-model/anthropic.claude-3-5-sonnet-*"
      }
    ]
  })
}

# Lambda Function
resource "aws_lambda_function" "pitchdeck_generator" {
  function_name = "ai-pitchdeck-generator"
  role          = aws_iam_role.lambda_role.arn
  handler       = "handler.lambda_handler"
  runtime       = "python3.12"
  timeout       = 600
  memory_size   = 2048

  filename         = "lambda-deployment.zip"
  source_code_hash = filebase64sha256("lambda-deployment.zip")

  environment {
    variables = {
      S3_BUCKET                  = "ai-pitchdeck-uploads"
      ANTHROPIC_API_KEY          = var.anthropic_api_key
      USE_BEDROCK                = "false"
      MAX_RETRIES                = "3"
      CALLBACK_TIMEOUT           = "30"
      POWERTOOLS_SERVICE_NAME    = "ai-pitchdeck-generator"
      POWERTOOLS_LOG_LEVEL       = "INFO"
    }
  }

  tracing_config {
    mode = "Active"
  }

  tags = {
    Project = "ai-pitchdeck-generator"
  }
}

# CloudWatch Log Group
resource "aws_cloudwatch_log_group" "lambda_logs" {
  name              = "/aws/lambda/${aws_lambda_function.pitchdeck_generator.function_name}"
  retention_in_days = 30
}

# CloudWatch Alarms
resource "aws_cloudwatch_metric_alarm" "lambda_errors" {
  alarm_name          = "${aws_lambda_function.pitchdeck_generator.function_name}-errors"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "Errors"
  namespace           = "AWS/Lambda"
  period              = 300
  statistic           = "Sum"
  threshold           = 1

  dimensions = {
    FunctionName = aws_lambda_function.pitchdeck_generator.function_name
  }
}

# Outputs
output "function_arn" {
  value = aws_lambda_function.pitchdeck_generator.arn
}

output "function_name" {
  value = aws_lambda_function.pitchdeck_generator.function_name
}
```

## Deployment Commands

### CloudFormation
```bash
# Package
aws cloudformation package \
  --template-file cloudformation.yaml \
  --s3-bucket my-deployment-bucket \
  --output-template-file packaged.yaml

# Deploy
aws cloudformation deploy \
  --template-file packaged.yaml \
  --stack-name ai-pitchdeck-generator \
  --capabilities CAPABILITY_NAMED_IAM \
  --parameter-overrides AnthropicApiKey=sk-ant-xxx
```

### CDK
```bash
cd cdk
cdk bootstrap
cdk deploy
```

### Terraform
```bash
terraform init
terraform plan
terraform apply
```
