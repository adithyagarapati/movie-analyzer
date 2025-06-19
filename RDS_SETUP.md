# AWS RDS PostgreSQL Setup for Movie Analyzer

This project has been configured to use **AWS RDS PostgreSQL** instead of a containerized database for demonstration purposes.

## 📋 Prerequisites

- AWS Account with RDS access
- AWS CLI configured (optional)
- PostgreSQL client (psql) installed locally

## 🗄️ RDS Database Setup

### 1. Create RDS PostgreSQL Instance

1. **Go to AWS RDS Console**
   - Navigate to AWS Console → RDS → Databases
   - Click "Create database"

2. **Database Configuration**
   - Engine: PostgreSQL
   - Version: 15.x (recommended)
   - Template: Free tier (for learning) or Dev/Test
   - Instance identifier: `movie-analyzer-db`
   - Master username: `postgres`
   - Master password: Choose a secure password

3. **Database Settings**
   - Initial database name: `moviereviews`
   - Port: `5432` (default)
   - Security group: Allow inbound PostgreSQL (5432) from your IP
   - Public access: Yes (for demonstration)

### 2. Configure Database Schema

Once your RDS instance is created:

1. **Get the RDS Endpoint**
   ```bash
   # Example endpoint format:
   # movie-analyzer-db.xxxxx.us-east-1.rds.amazonaws.com
   ```

2. **Connect to RDS and Initialize Database**
   ```bash
   # Connect using psql
   psql -h YOUR_RDS_ENDPOINT -U postgres -d moviereviews
   
   # Run the initialization script
   \i database/init.sql
   ```

   Or copy and paste the contents of `database/init.sql` into your SQL client.

### 3. Update Application Configuration

#### For Docker Compose
```bash
DB_HOST=your-rds-endpoint.region.rds.amazonaws.com
DB_PORT=5432
DB_NAME=moviereviews
DB_USERNAME=movieuser
DB_PASSWORD=moviepass
```

#### For Kubernetes Manifests
Update `deploy/manifests/backend/deployment.yaml`:
```yaml
- name: DB_HOST
  value: "your-actual-rds-endpoint.region.rds.amazonaws.com"
```

#### For Helm Charts
Update `deploy/helm/values.yaml`:
```yaml
backend:
  env:
    DB_HOST: "your-actual-rds-endpoint.region.rds.amazonaws.com"
```

## 🚀 Deployment Commands

### Docker Compose
```bash
# Set environment variables in .env file first
docker-compose up -d --build
```

### Kubernetes
```bash
# Update RDS endpoint in manifests first
cd deploy/manifests
./deploy.sh deploy
```

### Helm
```bash
# Update RDS endpoint in values.yaml first
cd deploy/helm
helm install movie-analyzer . -n movie-analyzer --create-namespace
```

## 🔧 Connection Testing

Test your RDS connection:
```bash
# Test connection
psql -h YOUR_RDS_ENDPOINT -U movieuser -d moviereviews -c "SELECT COUNT(*) FROM reviews;"

# Should return: count: 8 (sample reviews)
```

## 🛡️ Security Notes

- **Never commit real credentials** to version control
- Use AWS Secrets Manager or Kubernetes secrets in production
- Configure proper security groups and VPC settings