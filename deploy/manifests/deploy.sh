#!/bin/bash

# Movie Analyzer Kubernetes Deployment Script
# This script deploys the movie analyzer application using organized manifests
# Database: Uses external AWS RDS PostgreSQL (not containerized)

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

# Check if kubectl is available
if ! command -v kubectl &> /dev/null; then
    print_error "kubectl is not installed or not in PATH"
    exit 1
fi

NAMESPACE="movie-analyzer"
MANIFEST_DIR="$(dirname "$0")"

# Function to apply manifests
apply_manifests() {
    local component=$1
    local dir="$MANIFEST_DIR/$component"
    
    if [ -d "$dir" ]; then
        print_status "Applying $component manifests..."
        for file in "$dir"/*.yaml; do
            if [ -f "$file" ]; then
                print_status "Applying $(basename "$file")"
                kubectl apply -f "$file"
            fi
        done
        print_success "$component manifests applied successfully"
    else
        print_warning "Directory $dir not found, skipping $component"
    fi
}

# Function to delete all resources
cleanup() {
    print_warning "This will delete the entire Movie Analyzer deployment!"
    print_warning "Note: This does NOT affect your AWS RDS database"
    read -p "Are you sure you want to continue? (y/N): " -n 1 -r
    echo
    
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        print_status "Cleaning up Movie Analyzer deployment..."
        kubectl delete namespace "$NAMESPACE" --ignore-not-found=true
        print_success "Cleanup completed successfully!"
        print_status "Your AWS RDS database remains unaffected"
    else
        print_status "Cleanup cancelled."
    fi
}

# Function to check deployment status
status() {
    print_status "Checking deployment status..."
    echo
    
    print_status "Namespace:"
    kubectl get namespace "$NAMESPACE" 2>/dev/null || print_warning "Namespace not found"
    
    echo
    print_status "Pods:"
    kubectl get pods -n "$NAMESPACE" 2>/dev/null || print_warning "No pods found"
    
    echo
    print_status "Services:"
    kubectl get services -n "$NAMESPACE" 2>/dev/null || print_warning "No services found"
    
    echo
    print_status "Frontend NodePort access:"
    NODE_PORT=$(kubectl get svc frontend -n "$NAMESPACE" -o jsonpath='{.spec.ports[0].nodePort}' 2>/dev/null || echo "N/A")
    if [ "$NODE_PORT" != "N/A" ]; then
        print_success "Frontend available at: http://localhost:$NODE_PORT"
    else
        print_warning "Frontend service not accessible"
    fi
    
    echo
    print_status "Database Connection:"
    print_warning "Using external AWS RDS PostgreSQL - ensure your RDS instance is configured and accessible"
}

# Function to get logs
logs() {
    local service=$1
    if [ -z "$service" ]; then
        print_error "Please specify a service: backend, frontend, or model"
        exit 1
    fi
    
    print_status "Getting logs for $service..."
    kubectl logs -l app="$service" -n "$NAMESPACE" --tail=50
}

# Main deployment function
deploy() {
    print_status "Starting Movie Analyzer deployment..."
    print_warning "Make sure your AWS RDS PostgreSQL database is configured and accessible!"
    echo
    
    # Apply in order: namespace first, then dependencies, then applications
    print_status "Creating namespace..."
    kubectl apply -f "$MANIFEST_DIR/namespace.yaml"
    
    # Apply backend secrets and services
    apply_manifests "backend"
    
    # Apply application services
    apply_manifests "model"
    apply_manifests "frontend"
    
    # Wait for all deployments to be ready
    print_status "Waiting for all deployments to be ready..."
    kubectl wait --for=condition=available deployment --all -n "$NAMESPACE" --timeout=300s
    
    echo
    print_success "Movie Analyzer deployed successfully!"
    print_warning "Remember to configure backend environment variables to point to your RDS endpoint"
    echo
    status
}

# Command handling
case "$1" in
    "deploy"|"")
        deploy
        ;;
    "cleanup"|"clean")
        cleanup
        ;;
    "status"|"info")
        status
        ;;
    "logs")
        logs "$2"
        ;;
    *)
        echo "Usage: $0 {deploy|cleanup|status|logs <service>}"
        echo
        echo "Commands:"
        echo "  deploy    - Deploy the movie analyzer application"
        echo "  cleanup   - Remove the entire deployment"
        echo "  status    - Show deployment status"
        echo "  logs      - Show logs for a specific service (backend|frontend|model)"
        echo
        echo "Note: This deployment uses external AWS RDS PostgreSQL database"
        exit 1
        ;;
esac 