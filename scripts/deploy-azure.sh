#!/bin/bash

# Azure deployment script for HAI-Indexer Auth Service
# Deploys to Azure Kubernetes Service (AKS) with Istio

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="${SCRIPT_DIR}/.."

# Configuration
ENVIRONMENT="${ENVIRONMENT:-prod}"
RESOURCE_GROUP="hai-indexer-rg"
AKS_CLUSTER="hai-indexer-aks-${ENVIRONMENT}"
ACR_NAME="haiindexeracr${ENVIRONMENT}"
IMAGE_TAG="${IMAGE_TAG:-2.1.1}"

echo "🚀 Deploying HAI-Indexer Auth Service to Azure"
echo "   Environment: ${ENVIRONMENT}"
echo "   Image Tag: ${IMAGE_TAG}"
echo ""

# Step 1: Build and push Docker image
echo "📦 Building Docker image..."
cd "${PROJECT_DIR}"
docker build -t auth-service:${IMAGE_TAG} .

echo "🔐 Logging in to Azure Container Registry..."
az acr login --name ${ACR_NAME}

echo "📤 Pushing image to ACR..."
docker tag auth-service:${IMAGE_TAG} ${ACR_NAME}.azurecr.io/auth-service:${IMAGE_TAG}
docker push ${ACR_NAME}.azurecr.io/auth-service:${IMAGE_TAG}

# Step 2: Get AKS credentials
echo "🔑 Getting AKS credentials..."
az aks get-credentials --resource-group ${RESOURCE_GROUP} --name ${AKS_CLUSTER} --overwrite-existing

# Step 3: Create namespace if not exists
echo "📁 Creating namespace..."
kubectl create namespace hai-indexer --dry-run=client -o yaml | kubectl apply -f -

# Step 4: Create secrets
echo "🔒 Creating secrets..."

# JWT keys
if [ -f "${PROJECT_DIR}/secrets/jwt-private.pem" ]; then
    kubectl create secret generic jwt-keys \
        --from-file=jwt-private.pem=${PROJECT_DIR}/secrets/jwt-private.pem \
        --from-file=jwt-public.pem=${PROJECT_DIR}/secrets/jwt-public.pem \
        --namespace=hai-indexer \
        --dry-run=client -o yaml | kubectl apply -f -
else
    echo "⚠️  Warning: JWT keys not found. Please generate them first."
fi

# Google service account
if [ -f "${PROJECT_DIR}/secrets/service-account.json" ]; then
    kubectl create secret generic google-service-account \
        --from-file=service-account.json=${PROJECT_DIR}/secrets/service-account.json \
        --namespace=hai-indexer \
        --dry-run=client -o yaml | kubectl apply -f -
else
    echo "⚠️  Warning: Google service account not found."
fi

# Redis password (from Terraform output)
REDIS_PASSWORD=$(terraform -chdir="${PROJECT_DIR}/terraform" output -raw redis_primary_key)
kubectl create secret generic redis-secret \
    --from-literal=password=${REDIS_PASSWORD} \
    --namespace=hai-indexer \
    --dry-run=client -o yaml | kubectl apply -f -

# Step 5: Update image in deployment
echo "🔄 Updating deployment manifest..."
export CONTAINER_REGISTRY="${ACR_NAME}.azurecr.io"
envsubst < "${PROJECT_DIR}/k8s/deployment.yaml" | kubectl apply -f -

# Step 6: Apply Kubernetes manifests
echo "☸️  Applying Kubernetes manifests..."
kubectl apply -f "${PROJECT_DIR}/k8s/service.yaml"
kubectl apply -f "${PROJECT_DIR}/k8s/configmap.yaml"

# Step 7: Apply Istio configurations
echo "🌐 Applying Istio configurations..."
kubectl apply -f "${PROJECT_DIR}/k8s/istio/"

# Step 8: Wait for deployment
echo "⏳ Waiting for deployment to be ready..."
kubectl rollout status deployment/auth-service -n hai-indexer --timeout=5m

# Step 9: Verify deployment
echo "✅ Verifying deployment..."
kubectl get pods -n hai-indexer -l app=auth-service
kubectl get svc -n hai-indexer auth-service

echo ""
echo "🎉 Deployment complete!"
echo ""
echo "📊 Check status:"
echo "   kubectl get pods -n hai-indexer -l app=auth-service"
echo "   kubectl logs -n hai-indexer -l app=auth-service --tail=100"
echo ""
echo "🔍 Test endpoints:"
echo "   kubectl port-forward -n hai-indexer svc/auth-service 8080:8080"
echo "   curl http://localhost:8080/actuator/health"

