#!/usr/bin/env bash
set -euo pipefail

# Tear down EVERYTHING this project created on AWS:
#   - the AgentCore runtime, ECR repo (+ images, force_delete=true), EC2 LiteLLM
#     host, Elastic IP, VPC/subnet/route table/IGW/security group, IAM role.
#   - the CloudWatch log group AgentCore auto-creates (NOT terraform-managed, so
#     it would otherwise linger after destroy).
#
# Usage:
#   ./destroy.sh          # shows what will be destroyed, then asks to confirm
#   ./destroy.sh -y       # skip the confirmation prompt (also: FORCE=1)

cd "$(dirname "$0")"
INFRA_DIR="infra"

ASSUME_YES=0
if [ "${1:-}" = "-y" ] || [ "${1:-}" = "--yes" ] || [ "${FORCE:-0}" = "1" ]; then
  ASSUME_YES=1
fi

# Capture the log-group name from state BEFORE destroy removes the outputs.
LOG_GROUP=""
if ARN="$(terraform -chdir="$INFRA_DIR" output -raw agent_runtime_arn 2>/dev/null)" && [ -n "$ARN" ]; then
  LOG_GROUP="/aws/bedrock-agentcore/runtimes/${ARN##*/}-DEFAULT"
fi
REGION="$(terraform -chdir="$INFRA_DIR" output -raw region 2>/dev/null || echo "")"

echo "==> the following AWS resources are managed by terraform and will be destroyed:"
# Drop data.* entries — those are read-only lookups, not resources to delete.
RESOURCES="$(terraform -chdir="$INFRA_DIR" state list 2>/dev/null | grep -v '^data\.' || true)"
if [ -z "$RESOURCES" ]; then
  echo "      (no terraform-managed resources found — nothing to destroy)"; exit 0
fi
printf '%s\n' "$RESOURCES" | sed 's/^/      /'
[ -n "$LOG_GROUP" ] && echo "==> plus CloudWatch log group (created by AgentCore, not terraform):" && echo "      $LOG_GROUP"

if [ "$ASSUME_YES" != 1 ]; then
  echo
  printf 'Type "destroy" to permanently delete all of the above: '
  read -r REPLY
  [ "$REPLY" = "destroy" ] || { echo "aborted."; exit 1; }
fi

echo
echo "==> terraform destroy"
# Pass agent_image_tag so terraform doesn't prompt for it; the value is irrelevant
# for teardown. force_delete=true on the ECR repo lets it delete with images present.
terraform -chdir="$INFRA_DIR" destroy -input=false -auto-approve \
  -var="agent_image_tag=latest"

# Best-effort cleanup of the auto-created log group (ignore if already gone).
if [ -n "$LOG_GROUP" ] && [ -n "$REGION" ]; then
  echo "==> deleting CloudWatch log group $LOG_GROUP"
  aws logs delete-log-group --region "$REGION" --log-group-name "$LOG_GROUP" 2>/dev/null \
    && echo "      deleted." || echo "      already gone (or no permission) — skipping."
fi

echo
echo "==> done. Verify nothing is left:"
echo "      terraform -chdir=$INFRA_DIR state list   # should be empty"
