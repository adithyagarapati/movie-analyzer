# AWS Lambda Sentiment Analysis Service

This directory contains the AWS Lambda implementation of the movie review sentiment analysis service, replacing the containerized Flask model server with a serverless function.

## 🏗️ Architecture

```
Frontend → Backend → AWS Lambda Function → Response
              ↘ AWS RDS PostgreSQL
```

## 📂 Files

- `lambda_function.py` - Main Lambda handler with sentiment analysis logic
- `requirements.txt` - Python dependencies (TextBlob, NLTK)
- `deploy.sh` - Automated deployment script
- `lambda-deployment-package.zip` - Pre-built deployment package (ready for manual upload)
- `README.md` - This documentation

## 🚀 Quick Deployment

### Prerequisites

1. **AWS CLI installed and configured**:
   ```bash
   # Install AWS CLI
   curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
   unzip awscliv2.zip
   sudo ./aws/install
   
   # Configure credentials
   aws configure
   ```

2. **Python 3 and pip3 installed**
3. **AWS credentials with Lambda permissions**

### Option 1: Quick Manual Deployment

**Use the pre-built deployment package:**
The `lambda-deployment-package.zip` file is ready for immediate deployment. You can:

1. **Upload via AWS Console:**
   - Go to AWS Lambda console
   - Create new function or update existing
   - Upload `lambda-deployment-package.zip` (2.8MB)

2. **Deploy via AWS CLI:**
   ```bash
   # Create new function
   aws lambda create-function \
     --function-name movie-analyzer-sentiment \
     --runtime python3.12 \
     --role arn:aws:iam::YOUR_ACCOUNT:role/lambda-execution-role \
     --handler lambda_function.lambda_handler \
     --zip-file fileb://lambda-deployment-package.zip \
     --timeout 30 \
     --memory-size 256 \
     --region ap-south-1
   
   # Update existing function
   aws lambda update-function-code \
     --function-name movie-analyzer-sentiment \
     --zip-file fileb://lambda-deployment-package.zip \
     --region ap-south-1
   ```

### Option 2: Automated Deployment Script

```bash
# Navigate to lambda directory
cd lambda

# Create Lambda function
./deploy.sh create

# Update existing function
./deploy.sh update

# Test the function
./deploy.sh test

# Get function info
./deploy.sh info

# Delete function
./deploy.sh delete
```

## 🔧 Manual Deployment (Step-by-Step)

For users who want complete control over the deployment process, here's a detailed step-by-step guide:

### Step 1: Verify Prerequisites

**Check AWS CLI Installation:**
```bash
# Verify AWS CLI is installed
aws --version

# Should output something like: aws-cli/2.x.x
```

**Check Python and pip:**
```bash
# Verify Python 3.9+ is available
python3 --version

# Verify pip3 is available
pip3 --version
```

**Verify AWS Credentials:**
```bash
# Check current AWS configuration
aws sts get-caller-identity

# Should output your AWS account details
```

### Step 2: Create IAM Role for Lambda

**Create the IAM role:**
```bash
# Create trust policy file
cat > lambda-trust-policy.json << EOF
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

# Create the IAM role
aws iam create-role \
  --role-name lambda-execution-role \
  --assume-role-policy-document file://lambda-trust-policy.json

# Attach basic Lambda execution policy
aws iam attach-role-policy \
  --role-name lambda-execution-role \
  --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole

# Get the role ARN (save this for later)
aws iam get-role \
  --role-name lambda-execution-role \
  --query 'Role.Arn' \
  --output text
```

**Alternative - Custom IAM Policy:**
If you prefer more restrictive permissions:
```bash
# Create custom policy
cat > lambda-custom-policy.json << EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "arn:aws:logs:*:*:*"
    }
  ]
}
EOF

# Create and attach custom policy
aws iam create-policy \
  --policy-name lambda-movie-analyzer-policy \
  --policy-document file://lambda-custom-policy.json

# Get your account ID
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)

# Attach custom policy
aws iam attach-role-policy \
  --role-name lambda-execution-role \
  --policy-arn arn:aws:iam::${ACCOUNT_ID}:policy/lambda-movie-analyzer-policy
```

### Step 3: Build Deployment Package

**Option A: Use Pre-built Package**
```bash
# Navigate to lambda directory
cd lambda

# Verify the pre-built package exists
ls -la lambda-deployment-package.zip

# Should show: lambda-deployment-package.zip (2.8MB)
```

**Option B: Build Your Own Package**
```bash
# Navigate to lambda directory
cd lambda

# Create package directory
mkdir -p package

# Install dependencies
pip3 install -r requirements.txt -t package/

# Copy Lambda function
cp lambda_function.py package/

# Create deployment package
cd package && zip -r ../custom-lambda-package.zip . && cd ..
```

### Step 4: Deploy Lambda Function

**Get your IAM role ARN:**
```bash
# Get the role ARN you created earlier
ROLE_ARN=$(aws iam get-role \
  --role-name lambda-execution-role \
  --query 'Role.Arn' \
  --output text)

echo "Role ARN: $ROLE_ARN"
```

**Create the Lambda function:**
```bash
# Using pre-built package
aws lambda create-function \
  --function-name movie-analyzer-sentiment \
  --runtime python3.12 \
  --role $ROLE_ARN \
  --handler lambda_function.lambda_handler \
  --zip-file fileb://lambda-deployment-package.zip \
  --timeout 30 \
  --memory-size 256 \
  --environment Variables='{LAMBDA_HEALTHY=true}' \
  --region ap-south-1 \
  --description "Movie review sentiment analysis service"

# Or using custom package
aws lambda create-function \
  --function-name movie-analyzer-sentiment \
  --runtime python3.12 \
  --role $ROLE_ARN \
  --handler lambda_function.lambda_handler \
  --zip-file fileb://custom-lambda-package.zip \
  --timeout 30 \
  --memory-size 256 \
  --environment Variables='{LAMBDA_HEALTHY=true}' \
  --region ap-south-1 \
  --description "Movie review sentiment analysis service"
```

### Step 5: Verify Deployment

**Check function details:**
```bash
# Get function information
aws lambda get-function \
  --function-name movie-analyzer-sentiment \
  --region ap-south-1

# Check function configuration
aws lambda get-function-configuration \
  --function-name movie-analyzer-sentiment \
  --region ap-south-1
```

**Test the function:**
```bash
# Test with health check
aws lambda invoke \
  --function-name movie-analyzer-sentiment \
  --payload '{"action":"health"}' \
  --region ap-south-1 \
  --cli-binary-format raw-in-base64-out \
  test-response.json

# View the response
cat test-response.json

# Test with sentiment analysis
aws lambda invoke \
  --function-name movie-analyzer-sentiment \
  --payload '{"action":"analyze","text":"This movie was fantastic!"}' \
  --region ap-south-1 \
  --cli-binary-format raw-in-base64-out \
  sentiment-response.json

# View the sentiment response
cat sentiment-response.json
```

### Step 6: Configure Environment Variables (Optional)

```bash
# Update environment variables
aws lambda update-function-configuration \
  --function-name movie-analyzer-sentiment \
  --environment Variables='{LAMBDA_HEALTHY=true,LOG_LEVEL=INFO}' \
  --region ap-south-1
```

### Step 7: Set Up CloudWatch Logging (Automatic)

The function automatically creates CloudWatch log groups. To view logs:

```bash
# List log groups
aws logs describe-log-groups \
  --log-group-name-prefix "/aws/lambda/movie-analyzer" \
  --region ap-south-1

# Get recent log events
aws logs filter-log-events \
  --log-group-name "/aws/lambda/movie-analyzer-sentiment" \
  --start-time $(date -d '10 minutes ago' +%s)000 \
  --region ap-south-1
```

### Step 8: Update Function Code (Future Updates)

```bash
# Update function code with new package
aws lambda update-function-code \
  --function-name movie-analyzer-sentiment \
  --zip-file fileb://lambda-deployment-package.zip \
  --region ap-south-1

# Publish a new version (optional)
aws lambda publish-version \
  --function-name movie-analyzer-sentiment \
  --description "Updated sentiment analysis logic" \
  --region ap-south-1
```

### Step 9: Clean Up (If Needed)

```bash
# Delete Lambda function
aws lambda delete-function \
  --function-name movie-analyzer-sentiment \
  --region ap-south-1

# Delete IAM role
aws iam detach-role-policy \
  --role-name lambda-execution-role \
  --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole

aws iam delete-role \
  --role-name lambda-execution-role

# Clean up local files
rm -f lambda-trust-policy.json lambda-custom-policy.json
rm -f test-response.json sentiment-response.json
rm -rf package/ custom-lambda-package.zip
```

### Manual Deployment Troubleshooting

**Issue: "User is not authorized to perform: iam:CreateRole"**
```bash
# Solution: Ensure your AWS user has IAM permissions
# Or use an existing role ARN instead of creating one
```

**Issue: "InvalidParameterValueException: The role defined for the function cannot be assumed by Lambda"**
```bash
# Solution: Wait a few seconds after role creation, then retry
sleep 10
# Then retry the create-function command
```

**Issue: "ResourceConflictException: The function already exists"**
```bash
# Solution: Either delete existing function or use update-function-code
aws lambda delete-function --function-name movie-analyzer-sentiment --region ap-south-1
# Then retry create-function
```

**Issue: Package too large errors**
```bash
# Check package size
ls -lh lambda-deployment-package.zip

# If over 50MB, use S3 upload instead:
aws s3 cp lambda-deployment-package.zip s3://your-bucket/lambda-package.zip

aws lambda create-function \
  --function-name movie-analyzer-sentiment \
  --runtime python3.12 \
  --role $ROLE_ARN \
  --code S3Bucket=your-bucket,S3Key=lambda-package.zip \
  --handler lambda_function.lambda_handler \
  --timeout 30 \
  --memory-size 256 \
  --region ap-south-1
```

## 📊 Lambda Function Features

### Core Functionality
- **Sentiment Analysis**: Uses TextBlob for natural language processing
- **Rating Generation**: Converts sentiment to 1-5 star ratings
- **Multi-Action Support**: analyze, health, status actions

### API Actions

#### 1. Sentiment Analysis
```json
{
  "action": "analyze",
  "text": "This movie was absolutely amazing!"
}
```

**Response:**
```json
{
  "sentiment": "positive",
  "score": 0.95,
  "confidence": "high",
  "rating": 4.8,
  "timestamp": 1701234567.89,
  "text_length": 33,
  "processed_by": "lambda-textblob"
}
```

#### 2. Health Check
```json
{
  "action": "health"
}
```

**Response:**
```json
{
  "status": "healthy",
  "service": "lambda-model",
  "timestamp": 1701234567.89,
  "version": "1.0.0",
  "message": "Lambda sentiment analysis service"
}
```

#### 3. Service Status
```json
{
  "action": "status"
}
```

**Response:**
```json
{
  "service": "lambda-model",
  "healthy": true,
  "version": "1.0.0",
  "timestamp": 1701234567.89,
  "runtime": "python3.12",
  "capabilities": {
    "sentiment_analysis": true,
    "rating_generation": true,
    "supported_sentiments": ["positive", "negative", "neutral"]
  }
}
```

## 🔒 IAM Permissions

The Lambda function requires the following IAM role with permissions:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "arn:aws:logs:*:*:*"
    }
  ]
}
```

## 🎛️ Environment Variables

- `LAMBDA_HEALTHY` - Set to "false" to simulate unhealthy state (default: "true")

## 🧪 Testing

### Local Testing
```bash
# Test with AWS CLI
aws lambda invoke \
  --function-name movie-analyzer-sentiment \
  --payload '{"action":"analyze","text":"Great movie!"}' \
  --region ap-south-1 \
  response.json && cat response.json
```

### Test Payloads

**Positive Review:**
```json
{"action": "analyze", "text": "This movie was absolutely fantastic! The acting was superb and the plot was engaging."}
```

**Negative Review:**
```json
{"action": "analyze", "text": "Terrible movie, waste of time. Poor acting and boring storyline."}
```

**Neutral Review:**
```json
{"action": "analyze", "text": "It was an okay movie. Nothing special but not bad either."}
```

## 📈 Monitoring

### CloudWatch Logs
Monitor Lambda execution in CloudWatch:
- Log Group: `/aws/lambda/movie-analyzer-sentiment`
- Metrics: Invocations, Duration, Errors

### Performance Metrics
- **Typical Duration**: 500-1500ms (first run includes package loading)
- **Memory Usage**: ~100-150MB
- **Cold Start**: ~2-3 seconds (rare with regular usage)

## 🔍 Troubleshooting

### Common Issues

**Import Errors:**
- Ensure all dependencies are properly packaged
- Check TextBlob and NLTK are included in the deployment package

**Timeout Errors:**
- Increase Lambda timeout (current: 30 seconds)
- Check for large input text (limit: 5000 characters)

**Permission Errors:**
- Verify IAM role has necessary permissions
- Check AWS credentials configuration

**Cold Start Performance:**
- First invocation takes longer due to package loading
- Consider increasing memory allocation for better performance

### Debug Commands
```bash
# Check Lambda logs
aws logs describe-log-groups --log-group-name-prefix "/aws/lambda/movie-analyzer"

# Get recent log events
aws logs filter-log-events \
  --log-group-name "/aws/lambda/movie-analyzer-sentiment" \
  --start-time $(date -d '10 minutes ago' +%s)000
```

## 🔄 Integration with Backend

The Spring Boot backend will need to be updated to invoke this Lambda function instead of making HTTP calls to the containerized model service. This includes:

1. Adding AWS SDK for Java dependency
2. Configuring Lambda client with credentials
3. Replacing HTTP calls with Lambda invocations
4. Handling Lambda response format

## 💰 Cost Estimation

**AWS Lambda Pricing (ap-south-1):**
- Requests: $0.20 per 1M requests
- Duration: $0.0000166667 per GB-second

**Example for 1000 requests/day:**
- Monthly cost: ~$0.006 (extremely cost-effective for demos)

## 🎯 Benefits

✅ **Serverless**: No container management  
✅ **Auto-scaling**: Handles traffic spikes automatically  
✅ **Cost-effective**: Pay per request  
✅ **Managed**: AWS handles infrastructure  
✅ **Fast deployment**: Simple zip file upload  
✅ **Built-in monitoring**: CloudWatch integration