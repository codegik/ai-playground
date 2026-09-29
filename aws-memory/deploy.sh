#!/usr/bin/env bash
set -euo pipefail

# End-to-end deploy. The AgentCore runtime can only be created after its image
# exists in ECR, so we apply the ECR repo first, build+push, then apply the rest.
cd "$(dirname "$0")"
INFRA_DIR="infra"
# Use an IMMUTABLE, unique image tag per deploy. AgentCore pins the image when
# the runtime is created/updated; pushing a new image to a fixed tag like
# ":latest" does NOT roll the runtime over (terraform sees the same container_uri
# and makes no change). A unique tag forces container_uri to change, so
# `terraform apply` updates the runtime to a new version running the new image.
IMAGE_TAG="${AGENT_IMAGE_TAG:-$(git rev-parse --short HEAD 2>/dev/null || echo nogit)-$(date +%s)}"

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
# buildx is required to build linux/arm64 on an x86 host. Cross-building arm64
# on x86 also needs QEMU binfmt handlers registered, or `RUN` steps fail with
# "exec /bin/sh: exec format error". Register them idempotently.
if [ "$(uname -m)" != "aarch64" ] && [ "$(uname -m)" != "arm64" ]; then
  if [ ! -e /proc/sys/fs/binfmt_misc/qemu-aarch64 ]; then
    echo "==> registering QEMU arm64 emulation (binfmt)"
    docker run --privileged --rm tonistiigi/binfmt --install arm64
  fi
fi
echo "==> image tag: ${IMAGE_TAG}"
docker buildx build --platform linux/arm64 \
  -t "${REPO_URL}:${IMAGE_TAG}" \
  --push \
  agentcore

echo "==> full terraform apply (LiteLLM host + AgentCore Memory + runtime)"
# Pass the unique tag so the runtime's container_uri changes and rolls over.
terraform -chdir="$INFRA_DIR" apply -input=false -auto-approve \
  -var="agent_image_tag=${IMAGE_TAG}"

echo
echo "==> done. outputs:"
terraform -chdir="$INFRA_DIR" output
