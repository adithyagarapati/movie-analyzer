# Lambda Function Manual Testing Guide

Complete guide for testing the `movie-analyzer-sentiment` Lambda function manually before EKS integration.

## 🎯 Testing Overview

After creating the Lambda function via AWS console, you should test:
- ✅ **Health Check** - Function is running correctly
- ✅ **Sentiment Analysis** - Core functionality works
- ✅ **Error Handling** - Invalid inputs are handled
- ✅ **Performance** - Response times are acceptable

## 📋 Method 1: AWS Console Testing (Recommended)

### Step 1: Access Test Interface
1. Go to **AWS Lambda Console**
2. Find your `movie-analyzer-sentiment` function
3. Click on the function name
4. Go to **"Test"** tab
5. Click **"Create new test event"**

### Step 2: Test Event Templates

**Test 1: Health Check**
```json
{
  "action": "health"
}
```
**Expected Response:**
```json
{
  "status": "healthy",
  "service": "lambda-model",
  "timestamp": 1701234567.89,
  "version": "1.0.0",
  "message": "Lambda sentiment analysis service"
}
```

**Test 2: Positive Sentiment**
```json
{
  "action": "analyze",
  "text": "This movie was absolutely fantastic! The acting was superb and the plot was engaging."
}
```
**Expected Response:**
```json
{
  "sentiment": "positive",
  "score": 0.75,
  "confidence": "high",
  "rating": 4.5,
  "timestamp": 1701234567.89,
  "text_length": 86,
  "processed_by": "lambda-textblob"
}
```

**Test 3: Negative Sentiment**
```json
{
  "action": "analyze",
  "text": "Terrible movie, waste of time. Poor acting and boring storyline."
}
```
**Expected Response:**
```json
{
  "sentiment": "negative",
  "score": -0.8,
  "confidence": "high",
  "rating": 1.2,
  "timestamp": 1701234567.89,
  "text_length": 62,
  "processed_by": "lambda-textblob"
}
```

**Test 4: Neutral Sentiment**
```json
{
  "action": "analyze",
  "text": "It was an okay movie. Nothing special but not bad either."
}
```
**Expected Response:**
```json
{
  "sentiment": "neutral",
  "score": 0.0,
  "confidence": "medium",
  "rating": 3.0,
  "timestamp": 1701234567.89,
  "text_length": 55,
  "processed_by": "lambda-textblob"
}
```

**Test 5: Service Status**
```json
{
  "action": "status"
}
```
**Expected Response:**
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

### Step 3: Error Testing

**Test 6: Invalid Action**
```json
{
  "action": "invalid"
}
```
**Expected Response:**
```json
{
  "error": "Unknown action: invalid",
  "valid_actions": ["analyze", "health", "status"]
}
```

**Test 7: Missing Text**
```json
{
  "action": "analyze"
}
```
**Expected Response:**
```json
{
  "error": "Text is required for analysis"
}
```

**Test 8: Empty Text**
```json
{
  "action": "analyze",
  "text": ""
}
```
**Expected Response:**
```json
{
  "error": "Text cannot be empty"
}
```

## 🖥️ Method 2: AWS CLI Testing

### Prerequisites
```bash
# Ensure AWS CLI is configured
aws sts get-caller-identity

# Should show your account details
```

### CLI Test Commands

**Test 1: Health Check**
```bash
aws lambda invoke \
  --function-name movie-analyzer-sentiment \
  --payload '{"action":"health"}' \
  --region ap-south-1 \
  --cli-binary-format raw-in-base64-out \
  health-response.json

# View response
cat health-response.json
```

**Test 2: Positive Sentiment Analysis**
```bash
aws lambda invoke \
  --function-name movie-analyzer-sentiment \
  --payload '{"action":"analyze","text":"Amazing movie with incredible acting!"}' \
  --region ap-south-1 \
  --cli-binary-format raw-in-base64-out \
  positive-response.json

# View response
cat positive-response.json
```

**Test 3: Negative Sentiment Analysis**
```bash
aws lambda invoke \
  --function-name movie-analyzer-sentiment \
  --payload '{"action":"analyze","text":"Worst movie ever, completely boring and terrible acting."}' \
  --region ap-south-1 \
  --cli-binary-format raw-in-base64-out \
  negative-response.json

# View response
cat negative-response.json
```

**Test 4: Batch Testing Script**
```bash
# Create a test script
cat > test-lambda.sh << 'EOF'
#!/bin/bash

echo "🧪 Testing Lambda Function..."

# Test 1: Health
echo "1️⃣ Testing Health Check..."
aws lambda invoke \
  --function-name movie-analyzer-sentiment \
  --payload '{"action":"health"}' \
  --region ap-south-1 \
  --cli-binary-format raw-in-base64-out \
  health.json > /dev/null

echo "Health Response:"
cat health.json
echo -e "\n"

# Test 2: Positive Sentiment
echo "2️⃣ Testing Positive Sentiment..."
aws lambda invoke \
  --function-name movie-analyzer-sentiment \
  --payload '{"action":"analyze","text":"This movie was fantastic!"}' \
  --region ap-south-1 \
  --cli-binary-format raw-in-base64-out \
  positive.json > /dev/null

echo "Positive Response:"
cat positive.json
echo -e "\n"

# Test 3: Negative Sentiment
echo "3️⃣ Testing Negative Sentiment..."
aws lambda invoke \
  --function-name movie-analyzer-sentiment \
  --payload '{"action":"analyze","text":"Terrible movie, waste of time."}' \
  --region ap-south-1 \
  --cli-binary-format raw-in-base64-out \
  negative.json > /dev/null

echo "Negative Response:"
cat negative.json
echo -e "\n"

# Test 4: Status
echo "4️⃣ Testing Status..."
aws lambda invoke \
  --function-name movie-analyzer-sentiment \
  --payload '{"action":"status"}' \
  --region ap-south-1 \
  --cli-binary-format raw-in-base64-out \
  status.json > /dev/null

echo "Status Response:"
cat status.json
echo -e "\n"

# Test 5: Error case
echo "5️⃣ Testing Error Handling..."
aws lambda invoke \
  --function-name movie-analyzer-sentiment \
  --payload '{"action":"invalid"}' \
  --region ap-south-1 \
  --cli-binary-format raw-in-base64-out \
  error.json > /dev/null

echo "Error Response:"
cat error.json
echo -e "\n"

echo "✅ Testing Complete!"

# Clean up
rm -f health.json positive.json negative.json status.json error.json
EOF

# Make executable and run
chmod +x test-lambda.sh
./test-lambda.sh
```

## 📊 Method 3: Performance Testing

### Basic Performance Test
```bash
# Test response time
time aws lambda invoke \
  --function-name movie-analyzer-sentiment \
  --payload '{"action":"analyze","text":"Performance test message"}' \
  --region ap-south-1 \
  --cli-binary-format raw-in-base64-out \
  perf-response.json

# View response
cat perf-response.json
```

### Load Testing (Multiple Invocations)
```bash
# Simple load test - 10 concurrent invocations
for i in {1..10}; do
  aws lambda invoke \
    --function-name movie-analyzer-sentiment \
    --payload "{\"action\":\"analyze\",\"text\":\"Load test message $i\"}" \
    --region ap-south-1 \
    --cli-binary-format raw-in-base64-out \
    load-test-$i.json &
done

# Wait for all to complete
wait

# Check results
echo "Load test results:"
for i in {1..10}; do
  echo "Test $i:"
  cat load-test-$i.json
  echo ""
done

# Clean up
rm -f load-test-*.json
```

## 🔍 Method 4: CloudWatch Monitoring

### Check Logs
```bash
# List log groups
aws logs describe-log-groups \
  --log-group-name-prefix "/aws/lambda/movie-analyzer" \
  --region ap-south-1

# Get recent logs
aws logs filter-log-events \
  --log-group-name "/aws/lambda/movie-analyzer-sentiment" \
  --start-time $(date -d '10 minutes ago' +%s)000 \
  --region ap-south-1
```

### Monitor Metrics via Console
1. Go to **CloudWatch Console**
2. Navigate to **Logs** → **Log groups**
3. Find `/aws/lambda/movie-analyzer-sentiment`
4. Click to view log streams
5. Check for any errors or warnings

## ✅ Validation Checklist

### ✅ **Basic Functionality**
- [ ] Health check returns `"status": "healthy"`
- [ ] Sentiment analysis works for positive text
- [ ] Sentiment analysis works for negative text
- [ ] Sentiment analysis works for neutral text
- [ ] Status check returns capability information

### ✅ **Response Format**
- [ ] All responses are valid JSON
- [ ] Timestamp fields are present
- [ ] Sentiment scores are in expected range (-1 to 1)
- [ ] Ratings are in expected range (1 to 5)
- [ ] Error responses include helpful messages

### ✅ **Performance**
- [ ] Cold start time < 5 seconds
- [ ] Warm invocation time < 2 seconds
- [ ] No timeout errors (under 30 seconds)
- [ ] Memory usage within limits

### ✅ **Error Handling**
- [ ] Invalid actions return proper error
- [ ] Missing text returns proper error
- [ ] Empty text returns proper error
- [ ] Malformed JSON handled gracefully

## 🎯 Real Movie Review Tests

Test with actual movie review examples:

**Positive Movie Review:**
```json
{
  "action": "analyze",
  "text": "The Shawshank Redemption is a masterpiece of storytelling. Morgan Freeman's narration is absolutely captivating, and Tim Robbins delivers a powerful performance. The film explores themes of hope, friendship, and redemption in a way that feels both profound and accessible. Every scene is carefully crafted, and the emotional payoff is tremendous. This is easily one of the greatest films ever made."
}
```

**Negative Movie Review:**
```json
{
  "action": "analyze",
  "text": "I was really disappointed with this movie. The plot was confusing and full of holes, the acting felt forced and unnatural, and the pacing was incredibly slow. I found myself checking my watch multiple times wondering when it would end. The special effects looked cheap and the dialogue was cringe-worthy. Would not recommend to anyone."
}
```

**Mixed Movie Review:**
```json
{
  "action": "analyze",
  "text": "The movie had some good moments and the cinematography was decent, but overall it felt like a missed opportunity. Some of the actors did well while others seemed to be just going through the motions. The story had potential but wasn't executed as well as it could have been. It's not terrible, but it's not great either."
}
```

## 🚀 Ready for EKS Integration?

If all tests pass successfully, your Lambda function is ready for EKS integration! 

**Next Steps:**
1. ✅ Verify all test cases pass
2. ✅ Check CloudWatch logs for any errors  
3. ✅ Note the function ARN for backend configuration
4. ✅ Ensure IAM permissions are set correctly
5. ✅ Proceed with backend integration in EKS

**Function ARN:** You'll need this for backend configuration:
```bash
# Get your function ARN
aws lambda get-function \
  --function-name movie-analyzer-sentiment \
  --region ap-south-1 \
  --query 'Configuration.FunctionArn' \
  --output text
```

🎉 **Your Lambda function is now tested and ready for production use!** 