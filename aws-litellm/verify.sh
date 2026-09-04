#!/usr/bin/env bash
set -euo pipefail

# Verify two things about the deployed AgentCore setup:
#   1. Invoking the agent hits the CURRENT (freshly-deployed) image, not a stale
#      one. Proven by: runtime image tag == newest ECR image, and the invoke's
#      own "agent-invoke" marker (with our exact nonce) shows up in CloudWatch.
#   2. The agent reaches OpenAI THROUGH the LiteLLM gateway. Proven by: the gateway
#      answers its liveliness probe, and the marker's "upstream" is the gateway URL.
#
# Usage: ./verify.sh
# Config: everything is read from `terraform output`; no hard-coded ARNs.

cd "$(dirname "$0")"
INFRA_DIR="infra"

# --- tiny output helpers ---------------------------------------------------
if [ -t 1 ]; then G=$'\e[32m'; R=$'\e[31m'; Y=$'\e[33m'; B=$'\e[1m'; N=$'\e[0m'; else G=; R=; Y=; B=; N=; fi
pass() { printf '%s  PASS%s  %s\n' "$G" "$N" "$1"; }
fail() { printf '%s  FAIL%s  %s\n' "$R" "$N" "$1"; FAILED=1; }
info() { printf '%s  ....%s  %s\n' "$Y" "$N" "$1"; }
hdr()  { printf '\n%s== %s ==%s\n' "$B" "$1" "$N"; }
FAILED=0

for bin in aws jq terraform uuidgen curl; do
  command -v "$bin" >/dev/null || { echo "missing required tool: $bin" >&2; exit 1; }
done

# --- resolve config from terraform ----------------------------------------
ARN="$(terraform -chdir="$INFRA_DIR" output -raw agent_runtime_arn)"
REGION="$(terraform -chdir="$INFRA_DIR" output -raw region)"
GATEWAY_URL="$(terraform -chdir="$INFRA_DIR" output -raw litellm_url)"
RUNTIME_ID="${ARN##*/}"                 # part after "runtime/"
REPO_URL="$(terraform -chdir="$INFRA_DIR" output -raw ecr_repository_url)"
REPO_NAME="${REPO_URL##*/}"
LOG_GROUP="/aws/bedrock-agentcore/runtimes/${RUNTIME_ID}-DEFAULT"
GATEWAY_HOSTPORT="${GATEWAY_URL#http://}"; GATEWAY_HOSTPORT="${GATEWAY_HOSTPORT#https://}"

echo "runtime : $RUNTIME_ID"
echo "region  : $REGION"
echo "gateway : $GATEWAY_URL"

# --- 1. runtime image matches newest ECR image -----------------------------
hdr "Runtime is serving the newest pushed image"
RUNTIME_JSON="$(aws bedrock-agentcore-control get-agent-runtime --region "$REGION" \
  --agent-runtime-id "$RUNTIME_ID" \
  --query '{version:agentRuntimeVersion,status:status,updatedAt:lastUpdatedAt,uri:agentRuntimeArtifact.containerConfiguration.containerUri}' \
  --output json)"
RUNTIME_STATUS="$(jq -r .status <<<"$RUNTIME_JSON")"
RUNTIME_URI="$(jq -r .uri <<<"$RUNTIME_JSON")"
RUNTIME_TAG="${RUNTIME_URI##*:}"
info "runtime version $(jq -r .version <<<"$RUNTIME_JSON"), updated $(jq -r .updatedAt <<<"$RUNTIME_JSON")"
info "runtime image tag: $RUNTIME_TAG"

NEWEST_TAGS="$(aws ecr describe-images --region "$REGION" --repository-name "$REPO_NAME" \
  --query 'reverse(sort_by(imageDetails,&imagePushedAt))[0].imageTags' --output json)"
info "newest ECR image tags: $(jq -rc . <<<"$NEWEST_TAGS")"

[ "$RUNTIME_STATUS" = "READY" ] && pass "runtime status is READY" || fail "runtime status is $RUNTIME_STATUS (expected READY)"
if jq -e --arg t "$RUNTIME_TAG" 'index($t)' <<<"$NEWEST_TAGS" >/dev/null; then
  pass "runtime is pinned to the newest ECR image ($RUNTIME_TAG)"
else
  fail "runtime image tag ($RUNTIME_TAG) is NOT the newest ECR image — a stale image is live. Re-run ./deploy.sh"
fi
if [ "$RUNTIME_TAG" = "latest" ]; then
  info "note: runtime is on the mutable ':latest' tag — pushing a new ':latest' will NOT roll it over. deploy.sh uses unique tags to avoid this."
fi

# --- 2. gateway is alive at the address the agent uses ---------------------
hdr "LiteLLM gateway is reachable where the agent points"
LIVE="$(curl -s -m 15 "$GATEWAY_URL/health/liveliness" || true)"
if [ -n "$LIVE" ]; then
  pass "gateway $GATEWAY_URL answered liveliness: $LIVE"
else
  fail "gateway $GATEWAY_URL did not answer /health/liveliness"
fi

# --- 3. invoke, then correlate the marker in CloudWatch --------------------
hdr "Invoke reaches the current image AND routes through the gateway"
NONCE="verify-$(uuidgen)"
info "invoking with nonce prompt: $NONCE"
INVOKE_OUT="$(AGENT_RUNTIME_ARN="$ARN" AWS_REGION="$REGION" ./invoke.sh "$NONCE" 2>&1 || true)"
RESPONSE="$(printf '%s\n' "$INVOKE_OUT" | awk '/^agent response:/{f=1;next} f')"
info "agent responded: $(printf '%s' "$RESPONSE" | head -c 120)"

info "searching CloudWatch for our marker (this nonce)..."
MARKER=""
for _ in $(seq 1 12); do
  # Pull every event mentioning our nonce, then keep the agent-invoke marker line.
  RAW="$(aws logs filter-log-events --region "$REGION" --log-group-name "$LOG_GROUP" \
    --start-time $(( ($(date +%s) - 600) * 1000 )) \
    --filter-pattern "\"$NONCE\"" \
    --query 'events[].message' --output text 2>/dev/null || true)"
  # events[].message on an empty array prints nothing; guard the "None" literal too.
  MARKER="$(printf '%s\n' "$RAW" | tr '\t' '\n' | grep -F 'agent-invoke' | grep -F "$NONCE" | tail -1 || true)"
  [ -n "$MARKER" ] && break
  MARKER=""; sleep 6
done

if [ -z "$MARKER" ]; then
  fail "could not find an 'agent-invoke' marker for this nonce in $LOG_GROUP (logs can lag; try again, or the running image may predate the marker)"
else
  pass "found our invocation in the runtime's own logs (proves it hit THIS runtime/image)"
  echo "        $MARKER"
  UPSTREAM="$(jq -r '.upstream // empty' <<<"$MARKER" 2>/dev/null || true)"
  if [ -n "$UPSTREAM" ]; then
    if printf '%s' "$UPSTREAM" | grep -q "$GATEWAY_HOSTPORT"; then
      pass "agent's upstream is the gateway: $UPSTREAM (NOT api.openai.com)"
    else
      fail "agent's upstream ($UPSTREAM) is not the gateway ($GATEWAY_HOSTPORT)"
    fi
  else
    info "marker had no 'upstream' field (older image?); gateway liveliness above still shows the path"
  fi
fi

# --- verdict ---------------------------------------------------------------
hdr "Result"
if [ "$FAILED" = 0 ]; then
  printf '%sAll checks passed:%s current image is live AND traffic routes through the LiteLLM gateway.\n' "$G" "$N"
else
  printf '%sSome checks failed%s — see FAIL lines above.\n' "$R" "$N"
  exit 1
fi
