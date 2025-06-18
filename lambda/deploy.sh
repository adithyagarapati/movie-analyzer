#!/bin/bash

# AWS Lambda Deployment Script for Movie Analyzer Sentiment Analysis
# This script helps package and deploy the Lambda function

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Configuration
LAMBDA_DIR="$(dirname "$0")"
FUNCTION_NAME="movie-analyzer-sentiment"
DEFAULT_REGION="ap-south-1"
PACKAGE_FILE="lambda-deployment-package.zip"

# Read configuration
read -p "Enter AWS region (default: $DEFAULT_REGION): " AWS_REGION
AWS_REGION=${AWS_REGION:-$DEFAULT_REGION}

read -p "Enter Lambda function name (default: $FUNCTION_NAME): " FUNC_NAME
FUNC_NAME=${FUNC_NAME:-$FUNCTION_NAME}

print_status "Lambda deployment configuration:"
echo "  Function Name: $FUNC_NAME"
echo "  AWS Region: $AWS_REGION"
echo "  Package File: $PACKAGE_FILE"
echo ""

# Function to check prerequisites
check_prerequisites() {
    print_status "Checking prerequisites..."
    
    if ! command -v aws &> /dev/null; then
        print_error "AWS CLI is not installed. Please install AWS CLI first."
        exit 1
    fi
    
    if ! command -v python3 &> /dev/null; then
        print_error "Python 3 is not installed."
        exit 1
    fi
    
    if ! command -v pip3 &> /dev/null; then
        print_error "pip3 is not installed."
        exit 1
    fi
    
    # Check AWS credentials
    if ! aws sts get-caller-identity &> /dev/null; then
        print_error "AWS credentials not configured. Run 'aws configure' first."
        exit 1
    fi
    
    print_success "Prerequisites check passed!"
}

# Function to package Lambda function
package_lambda() {
    print_status "Packaging Lambda function..."
    
    cd "$LAMBDA_DIR"
    
    # Check if existing package exists
    if [[ -f "$PACKAGE_FILE" ]]; then
        print_status "Found existing deployment package: $PACKAGE_FILE ($(du -h "$PACKAGE_FILE" | cut -f1))"
        read -p "Use existing package? (Y/n): " -n 1 -r
        echo
        
        if [[ $REPLY =~ ^[Nn]$ ]]; then
            print_status "Rebuilding deployment package..."
        else
            print_success "Using existing deployment package: $PACKAGE_FILE"
            return 0
        fi
    fi
    
    # Clean up previous package
    rm -f "$PACKAGE_FILE"
    rm -rf package/
    
    # Create package directory
    mkdir -p package
    
    # Install dependencies
    print_status "Installing Python dependencies..."
    pip3 install -r requirements.txt -t package/
    
    # Copy Lambda function
    cp lambda_function.py package/
    
    # Create deployment package
    print_status "Creating deployment package..."
    cd package
    zip -r "../$PACKAGE_FILE" .
    cd ..
    
    # Clean up package directory
    rm -rf package/
    
    print_success "Lambda package created: $PACKAGE_FILE"
    echo "Package size: $(du -h "$PACKAGE_FILE" | cut -f1)"
}

# Function to create Lambda function
create_lambda() {
    print_status "Creating Lambda function..."
    
    # Create IAM role for Lambda (if it doesn't exist)
    ROLE_NAME="lambda-execution-role"
    ROLE_ARN=""
    
    if aws iam get-role --role-name "$ROLE_NAME" --region "$AWS_REGION" &> /dev/null; then
        print_status "IAM role '$ROLE_NAME' already exists"
        ROLE_ARN=$(aws iam get-role --role-name "$ROLE_NAME" --region "$AWS_REGION" --query 'Role.Arn' --output text)
    else
        print_status "Creating IAM role for Lambda..."
        
        # Create trust policy
        cat > trust-policy.json << EOF
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
        
        # Create role
        aws iam create-role \
            --role-name "$ROLE_NAME" \
            --assume-role-policy-document file://trust-policy.json \
            --region "$AWS_REGION"
        
        # Attach basic execution policy
        aws iam attach-role-policy \
            --role-name "$ROLE_NAME" \
            --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole \
            --region "$AWS_REGION"
        
        # Get role ARN
        ROLE_ARN=$(aws iam get-role --role-name "$ROLE_NAME" --region "$AWS_REGION" --query 'Role.Arn' --output text)
        
        # Clean up temp file
        rm -f trust-policy.json
        
        print_success "IAM role created: $ROLE_ARN"
        
        # Wait for role to be available
        print_status "Waiting for IAM role to be available..."
        sleep 10
    fi
    
    # Create Lambda function
    if aws lambda get-function --function-name "$FUNC_NAME" --region "$AWS_REGION" &> /dev/null; then
        print_warning "Lambda function '$FUNC_NAME' already exists. Use update command to update it."
    else
        print_status "Creating Lambda function '$FUNC_NAME'..."
        
        aws lambda create-function \
            --function-name "$FUNC_NAME" \
            --runtime python3.12 \
            --role "$ROLE_ARN" \
            --handler lambda_function.lambda_handler \
            --zip-file fileb://"$PACKAGE_FILE" \
            --timeout 30 \
            --memory-size 256 \
            --environment Variables='{LAMBDA_HEALTHY=true}' \
            --region "$AWS_REGION"
        
        print_success "Lambda function created successfully!"
    fi
    
    # Get function ARN
    FUNCTION_ARN=$(aws lambda get-function --function-name "$FUNC_NAME" --region "$AWS_REGION" --query 'Configuration.FunctionArn' --output text)
    
    echo ""
    print_success "Lambda function details:"
    echo "  Function Name: $FUNC_NAME"
    echo "  Function ARN: $FUNCTION_ARN"
    echo "  Region: $AWS_REGION"
    echo ""
}

# Function to update Lambda function
update_lambda() {
    print_status "Updating Lambda function..."
    
    if ! aws lambda get-function --function-name "$FUNC_NAME" --region "$AWS_REGION" &> /dev/null; then
        print_error "Lambda function '$FUNC_NAME' does not exist. Use create command first."
        exit 1
    fi
    
    aws lambda update-function-code \
        --function-name "$FUNC_NAME" \
        --zip-file fileb://"$PACKAGE_FILE" \
        --region "$AWS_REGION"
    
    print_success "Lambda function updated successfully!"
}

# Function to test Lambda function
test_lambda() {
    print_status "Testing Lambda function..."
    
    # Test analyze action
    cat > test-payload.json << EOF
{
  "action": "analyze",
  "text": "This movie was absolutely amazing! I loved every minute of it."
}
EOF
    
    print_status "Testing sentiment analysis..."
    aws lambda invoke \
        --function-name "$FUNC_NAME" \
        --payload file://test-payload.json \
        --region "$AWS_REGION" \
        response.json
    
    echo "Response:"
    cat response.json | python3 -m json.tool
    echo ""
    
    # Test health check
    cat > test-health.json << EOF
{
  "action": "health"
}
EOF
    
    print_status "Testing health check..."
    aws lambda invoke \
        --function-name "$FUNC_NAME" \
        --payload file://test-health.json \
        --region "$AWS_REGION" \
        health-response.json
    
    echo "Health Response:"
    cat health-response.json | python3 -m json.tool
    echo ""
    
    # Clean up test files
    rm -f test-payload.json test-health.json response.json health-response.json
    
    print_success "Lambda function test completed!"
}

# Function to delete Lambda function
delete_lambda() {
    print_warning "This will delete the Lambda function '$FUNC_NAME'!"
    read -p "Are you sure you want to continue? (y/N): " -n 1 -r
    echo
    
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        print_status "Deleting Lambda function..."
        aws lambda delete-function \
            --function-name "$FUNC_NAME" \
            --region "$AWS_REGION"
        
        print_success "Lambda function deleted successfully!"
    else
        print_status "Deletion cancelled."
    fi
}

# Function to get function info
info_lambda() {
    print_status "Lambda function information:"
    
    aws lambda get-function \
        --function-name "$FUNC_NAME" \
        --region "$AWS_REGION" \
        --query '{FunctionName:Configuration.FunctionName,Runtime:Configuration.Runtime,Handler:Configuration.Handler,Role:Configuration.Role,FunctionArn:Configuration.FunctionArn,LastModified:Configuration.LastModified}' \
        --output table
}

# Main script logic
case "$1" in
    "package")
        check_prerequisites
        package_lambda
        ;;
    "create")
        check_prerequisites
        package_lambda
        create_lambda
        ;;
    "update")
        check_prerequisites
        package_lambda
        update_lambda
        ;;
    "test")
        test_lambda
        ;;
    "delete")
        delete_lambda
        ;;
    "info")
        info_lambda
        ;;
    *)
        echo "AWS Lambda Deployment Script for Movie Analyzer"
        echo ""
        echo "Usage: $0 {package|create|update|test|delete|info}"
        echo ""
        echo "Commands:"
        echo "  package  - Package the Lambda function into a zip file"
        echo "  create   - Package and create new Lambda function"
        echo "  update   - Package and update existing Lambda function"
        echo "  test     - Test the Lambda function with sample data"
        echo "  delete   - Delete the Lambda function"
        echo "  info     - Show Lambda function information"
        echo ""
        echo "Prerequisites:"
        echo "  - AWS CLI installed and configured"
        echo "  - Python 3 and pip3 installed"
        echo "  - AWS credentials configured (aws configure)"
        echo ""
        exit 1
        ;;
esac 