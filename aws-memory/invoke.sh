#!/usr/bin/env bash
set -euo pipefail

# Invoke the deployed agent and show, in order, every AgentCore Memory call it
# made (ListEvents / RetrieveMemoryRecords / CreateEvent) plus its answer.
#
# Usage:
#   ./invoke.sh "your prompt"                       # new session
#   SESSION=<id> ./invoke.sh "follow-up prompt"     # continue a session (short-term memory)
#   ACTOR=alice  ./invoke.sh "prompt"               # pick the user (long-term memory is per actor)

cd "$(dirname "$0")"
INFRA_DIR="infra"

PROMPT="${*:-Hi! My name is Ana, I live in Lisbon and I prefer short answers.}"
ACTOR="${ACTOR:-demo-user}"

ARN="${AGENT_RUNTIME_ARN:-$(terraform -chdir="$INFRA_DIR" output -raw agent_runtime_arn)}"
REGION="${AWS_REGION:-$(terraform -chdir="$INFRA_DIR" output -raw region)}"

# runtimeSessionId must be 33-256 chars; it is also used as the memory sessionId.
SESSION="${SESSION:-session-$(uuidgen)}"

PAYLOAD_FILE="$(mktemp)"
OUT_FILE="$(mktemp)"
trap 'rm -f "$PAYLOAD_FILE" "$OUT_FILE"' EXIT
jq -n --arg p "$PROMPT" --arg a "$ACTOR" '{prompt: $p, actorId: $a}' > "$PAYLOAD_FILE"

echo "actor  : $ACTOR"
echo "session: $SESSION"

aws bedrock-agentcore invoke-agent-runtime \
  --region "$REGION" \
  --agent-runtime-arn "$ARN" \
  --runtime-session-id "$SESSION" \
  --content-type "application/json" \
  --accept "text/event-stream" \
  --payload "fileb://$PAYLOAD_FILE" \
  "$OUT_FILE" >/dev/null

# Each SSE `data:` line is one JSON object the agent yielded:
#   {"type":"memory", "op":..., ...}  -> a memory call the agent made
#   {"type":"text",   "text":...}     -> a token of the answer
DATA="$(grep '^data:' "$OUT_FILE" | sed 's/^data: \{0,1\}//' || true)"
if [ -z "$DATA" ]; then
  cat "$OUT_FILE"; echo; exit 1
fi

echo
echo "memory calls made by the agent:"
jq -r 'select(.type == "memory") |
  if .op == "CreateEvent" then
    "  CreateEvent            -> \(.event.eventId)  [\(.event.role)] \(.event.text | .[0:100])"
  elif .op == "ListEvents" then
    "  ListEvents             -> \(.count) event(s) already in this session",
    (.events[] | "      [\(.role)] \(.text | .[0:100])")
  elif .op == "RetrieveMemoryRecords" then
    "  RetrieveMemoryRecords  -> \(.count) record(s) in \(.namespace)",
    (.records[] | "      (\(.score // "-")) \(.text)")
  else "  \(.op)" end' <<<"$DATA"

echo
echo "agent response:"
jq -j 'select(.type == "text") | .text' <<<"$DATA"; echo

echo
echo "next: SESSION=$SESSION ./invoke.sh \"...\"   |   ./events.sh $SESSION   |   ./records.sh"
