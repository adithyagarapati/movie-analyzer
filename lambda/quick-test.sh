#!/bin/bash
echo "🧪 Quick Lambda Test..."

# Test 1: Health Check
echo "1️⃣ Health Check:"
aws lambda invoke \
  --function-name movie-analyzer-sentiment \
  --payload '{"action":"health"}' \
  --region ap-south-1 \
  --cli-binary-format raw-in-base64-out \
  health.json > /dev/null 2>&1

if [ -f health.json ]; then
  cat health.json
  echo -e "\n"
else
  echo "❌ Health test failed"
fi

# Test 2: Sentiment Analysis
echo "2️⃣ Sentiment Analysis:"
aws lambda invoke \
  --function-name movie-analyzer-sentiment \
  --payload '{"action":"analyze","text":"This movie was fantastic!"}' \
  --region ap-south-1 \
  --cli-binary-format raw-in-base64-out \
  sentiment.json > /dev/null 2>&1

if [ -f sentiment.json ]; then
  cat sentiment.json
  echo -e "\n"
else
  echo "❌ Sentiment test failed"
fi

echo "✅ Quick test complete!"
rm -f health.json sentiment.json 