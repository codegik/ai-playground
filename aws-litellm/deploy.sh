#!/usr/bin/env bash
set -euo pipefail

# End-to-end deploy. The AgentCore runtime can only be created after its image
# exists in ECR, so we apply the ECR repo first, build+push, then apply the rest.
cd "$(dirname "$0")"
INFRA_DIR="infra"
IMAGE_TAG="${AGENT_IMAGE_TAG:-latest}"

echo "==> terraform init"
terraform -chdir="$INFRA_DIR" init -input=false

echo "==> create ECR repository (targeted apply)"
terraform -chdir="$INFRA_DIR" apply -input=false -auto-approve -target=aws_ecr_repository.agent

REPO_URL="$(terraform -chdir="$INFRA_DIR" output -raw ecr_repository_url)"
REGISTRY="${REPO_URL%%/*}"
# Registry host is <acct>.dkr.ecr.<region>.amazonaws.com — region is field 4.
REGION="$(echo "$REGISTRY" | cut -d. -f4)"

echo "==> docker login to ECR ($REGISTRY)"
aws ecr get-login-password --region "$REGION" | docker login --username AWS --password-stdin "$REGISTRY"

echo "==> build ARM64 agent image and push"
# buildx is required to build linux/arm64 on an x86 host.
docker buildx build --platform linux/arm64 \
  -t "${REPO_URL}:${IMAGE_TAG}" \
  --push \
  agentcore

echo "==> full terraform apply (LiteLLM host + AgentCore runtime)"
terraform -chdir="$INFRA_DIR" apply -input=false -auto-approve

echo
echo "==> done. outputs:"
terraform -chdir="$INFRA_DIR" output
