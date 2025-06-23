# AWS Lambda Integration Guide

Complete guide for AWS Lambda integration in the Movie Analyzer application.

## 🎯 Overview

The Movie Analyzer uses **AWS Lambda for serverless sentiment analysis**, replacing the containerized model server with a cloud-native approach.

### Architecture

<img src="../movie-analyzer-lambda-dark.png" alt="Movie Analyzer Architecture" width="1000"/>

### Benefits
✅ **Serverless**: No container management  
✅ **Auto-scaling**: Handles traffic spikes automatically  
✅ **Cost-effective**: Pay-per-request pricing  
✅ **Reliable**: AWS managed infrastructure  

## 🔐 Authentication Methods

The backend supports **2 simple authentication methods**:

| Method | Use Case | Configuration | Security |
|--------|----------|---------------|----------|
| **`iam`** | **Production** | IAM roles (IRSA/Instance Profile) | ⭐⭐⭐⭐⭐ |
| **`keys`** | **Development/Demo** | AWS Access Keys | ⭐⭐⭐ |

### Configuration Examples

```bash
# Method 1: IAM Roles (Production - Recommended)
LAMBDA_AUTH_METHOD=iam
# No additional credentials needed - uses IAM roles

# Method 2: Access Keys (Development/Demo)
LAMBDA_AUTH_METHOD=keys
AWS_ACCESS_KEY_ID=AKIA...
AWS_SECRET_ACCESS_KEY=wJalr...
```

## 🚀 Deployment Configurations

### Docker Compose (Development)
```yaml
# docker-compose.yml
services:
  backend:
    environment:
      LAMBDA_AUTH_METHOD: keys
      AWS_ACCESS_KEY_ID: ${AWS_ACCESS_KEY_ID}
      AWS_SECRET_ACCESS_KEY: ${AWS_SECRET_ACCESS_KEY}
      LAMBDA_FUNCTION_NAME: movie-analyzer-sentiment
      AWS_REGION: ap-south-1
```

### Kubernetes with IRSA (Production)
```yaml
# deployment.yaml
env:
- name: LAMBDA_AUTH_METHOD
  value: "iam"
- name: LAMBDA_FUNCTION_NAME
  value: "movie-analyzer-sentiment"
- name: AWS_REGION
  value: "ap-south-1"
---
# serviceaccount.yaml (auto-created for IAM method)
apiVersion: v1
kind: ServiceAccount
metadata:
  name: backend-sa
  annotations:
    eks.amazonaws.com/role-arn: arn:aws:iam::ACCOUNT:role/MovieAnalyzerBackendRole
```

### Helm Chart
```yaml
# values.yaml
backend:
  env:
    LAMBDA_AUTH_METHOD: "iam"  # or "keys"
  serviceAccount:
    create: true  # Only creates when method is "iam"
    annotations:
      eks.amazonaws.com/role-arn: "arn:aws:iam::ACCOUNT:role/MovieAnalyzerBackendRole"
```

## 🔧 Environment Variables

### Required
- `LAMBDA_FUNCTION_NAME`: Lambda function name (default: `movie-analyzer-sentiment`)
- `AWS_REGION`: AWS region (default: `ap-south-1`)
- `LAMBDA_AUTH_METHOD`: Authentication method (`iam` or `keys`)

### For KEYS Method Only
- `AWS_ACCESS_KEY_ID`: AWS access key
- `AWS_SECRET_ACCESS_KEY`: AWS secret key

## 🔒 IAM Permissions

### Required IAM Policy
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["lambda:InvokeFunction"],
      "Resource": "*"
    }
  ]
}
```

### IRSA Setup (EKS)
```bash
# 1. Create IAM policy
aws iam create-policy \
  --policy-name MovieAnalyzerLambdaInvoke \
  --policy-document file://lambda-policy.json

# 2. Create IAM role for service account
eksctl create iamserviceaccount \
  --name backend-sa \
  --namespace movie-analyzer \
  --cluster your-cluster-name \
  --attach-policy-arn arn:aws:iam::ACCOUNT:policy/MovieAnalyzerLambdaInvoke \
  --approve
```

The migration from model server pod to lambda function provides **simplified deployment**, **cost optimization**, and **better scalability** while maintaining all functionality. 