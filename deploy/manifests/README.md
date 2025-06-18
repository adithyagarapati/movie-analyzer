# Movie Analyzer Kubernetes Manifests

This directory contains organized Kubernetes manifests for deploying the Movie Analyzer application with **AWS RDS PostgreSQL** database.

## Directory Structure

```
manifests/
├── namespace.yaml              # Namespace definition
├── backend/
│   ├── deployment.yaml         # Backend Spring Boot application (configured for RDS)
│   ├── service.yaml           # Backend service (ClusterIP)
│   └── secret.yaml            # Backend database credentials (for RDS)
├── frontend/
│   ├── deployment.yaml         # Frontend React/Express application
│   ├── service.yaml           # Frontend service (NodePort)
│   └── ingress.yaml           # Ingress for production use (optional)
├── model/
│   ├── deployment.yaml         # Model server (Python Flask)
│   └── service.yaml           # Model service (ClusterIP)
├── deploy.sh                  # Deployment automation script
├── kustomization.yaml         # Kustomize configuration (alternative deployment)
└── README.md                  # This file
```

## Prerequisites

⚠️ **Important**: This deployment requires an external AWS RDS PostgreSQL database.

1. **Set up AWS RDS PostgreSQL** - See `../../RDS_SETUP.md` for detailed instructions
2. **Update RDS Configuration** in `backend/deployment.yaml`:
   ```yaml
   - name: DB_HOST
     value: "your-actual-rds-endpoint.region.rds.amazonaws.com"
   ```

## Quick Start

### 1. Configure RDS Connection

Update the database connection in `backend/deployment.yaml`:
```bash
# Replace this line:
value: "your-rds-endpoint.region.rds.amazonaws.com"
# With your actual RDS endpoint
```

### 2. Deploy the Application

```bash
# Deploy everything
./deploy.sh deploy
```

### 3. Check Status

```bash
./deploy.sh status
```

### 4. Access the Application

Frontend available at: http://localhost:30000

### 5. Clean Up

```bash
./deploy.sh cleanup
```

**Note**: This only removes the Kubernetes resources. Your AWS RDS database remains unaffected.

## Alternative Deployment with Kustomize

You can also deploy using Kustomize:

```bash
# Deploy with kustomize (after updating RDS endpoint)
kubectl apply -k .

# Delete with kustomize
kubectl delete -k .
```

## Troubleshooting

**Common Issues:**
- Backend pods failing to start → Check RDS connectivity and credentials
- Database connection errors → Verify RDS security groups allow traffic
- Health check failures → Ensure RDS database is initialized with schema

**Checking Logs:**
```bash
# Backend logs
./deploy.sh logs backend

# All pod status
kubectl get pods -n movie-analyzer
``` 