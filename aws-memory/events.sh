#!/usr/bin/env bash
set -euo pipefail

# Show the short-term memory EVENTS the agent wrote, with their full content,
# read straight from the AgentCore Memory service (not from the agent).
#
# Usage:
#   ./events.sh                 # every session of $ACTOR (default demo-user)
#   ./events.sh <session-id>    # just that session
#   RAW=1 ./events.sh ...       # dump the raw event JSON as returned by the API

cd "$(dirname "$0")"
INFRA_DIR="infra"

ACTOR="${ACTOR:-demo-user}"
MEMORY_ID="$(terraform -chdir="$INFRA_DIR" output -raw memory_id)"
REGION="${AWS_REGION:-$(terraform -chdir="$INFRA_DIR" output -raw region)}"

if [ -n "${1:-}" ]; then
  SESSIONS="$1"
else
  SESSIONS="$(aws bedrock-agentcore list-sessions --region "$REGION" \
    --memory-id "$MEMORY_ID" --actor-id "$ACTOR" \
    --query 'sessionSummaries[].sessionId' --output text | tr '\t' '\n' | grep -v '^None$' || true)"
fi

echo "memory : $MEMORY_ID"
echo "actor  : $ACTOR"
[ -z "$SESSIONS" ] && { echo "no sessions yet — run ./invoke.sh first"; exit 0; }

for s in $SESSIONS; do
  EVENTS="$(aws bedrock-agentcore list-events --region "$REGION" \
    --memory-id "$MEMORY_ID" --actor-id "$ACTOR" --session-id "$s" \
    --include-payloads --output json)"
  echo
  echo "== session $s ($(jq '.events | length' <<<"$EVENTS") events)"
  if [ "${RAW:-0}" = 1 ]; then
    jq '.events' <<<"$EVENTS"
  else
    jq -r '.events | sort_by(.eventTimestamp)[] |
      "\(.eventTimestamp)  \(.eventId)",
      (.payload[] | if .conversational then
          "    [\(.conversational.role)] \(.conversational.content.text)"
        else "    \(tojson)" end)' <<<"$EVENTS"
  fi
done
