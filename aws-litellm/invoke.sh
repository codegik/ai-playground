#!/usr/bin/env bash
set -euo pipefail

# Invoke the deployed AgentCore runtime from your local machine using only the
# AWS CLI + jq (no Node). One of the four scripts in the engineer flow:
#   ./deploy.sh  → ./invoke.sh  → ./verify.sh  → ./destroy.sh
#
# Usage: ./invoke.sh "your prompt here"
#
# Config: reads the runtime ARN and region from `terraform output` by default,
# or from AGENT_RUNTIME_ARN / AWS_REGION if those env vars are set.

cd "$(dirname "$0")"
INFRA_DIR="infra"

PROMPT="${*:-Say hello and tell me which model you are.}"

ARN="${AGENT_RUNTIME_ARN:-$(terraform -chdir="$INFRA_DIR" output -raw agent_runtime_arn)}"
REGION="${AWS_REGION:-$(terraform -chdir="$INFRA_DIR" output -raw region)}"

if [ -z "$ARN" ]; then
  echo "No runtime ARN. Deploy first, or set AGENT_RUNTIME_ARN." >&2
  exit 1
fi

# runtimeSessionId must be 33-256 chars; two UUIDs comfortably satisfy that.
SESSION="session-$(uuidgen)-$(uuidgen)"

# Build the JSON payload the agent expects: {"prompt": "..."}. jq -R -s turns the
# raw prompt into a properly escaped JSON string.
PAYLOAD_FILE="$(mktemp)"
OUT_FILE="$(mktemp)"
trap 'rm -f "$PAYLOAD_FILE" "$OUT_FILE"' EXIT
printf '{"prompt":%s}' "$(printf '%s' "$PROMPT" | jq -R -s '.')" > "$PAYLOAD_FILE"

echo "session: $SESSION"

# The agent streams SSE, so we accept text/event-stream. The streaming response
# body is written to OUT_FILE (last positional arg to the CLI).
aws bedrock-agentcore invoke-agent-runtime \
  --region "$REGION" \
  --agent-runtime-arn "$ARN" \
  --runtime-session-id "$SESSION" \
  --content-type "application/json" \
  --accept "text/event-stream" \
  --payload "fileb://$PAYLOAD_FILE" \
  "$OUT_FILE" >/dev/null

# Reassemble the SSE stream: each `data:` line is one JSON event the agent
# yielded; concatenate the text deltas into the full answer.
answer=""
while IFS= read -r line; do
  data="${line#data:}"; data="${data# }"
  [ -z "$data" ] && continue
  [ "$data" = "[DONE]" ] && continue
  chunk="$(printf '%s' "$data" | jq -r '(.data.text // .text // "")' 2>/dev/null || true)"
  answer+="$chunk"
done < <(grep '^data:' "$OUT_FILE" || true)

echo
echo "agent response:"
if [ -n "$answer" ]; then
  printf '%s\n' "$answer"
else
  # Fallback: not an SSE stream we recognized — show the raw body.
  cat "$OUT_FILE"; echo
fi
