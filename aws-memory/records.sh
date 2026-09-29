#!/usr/bin/env bash
set -euo pipefail

# Show the LONG-TERM memory records AgentCore Memory extracted from the agent's
# events, one block per strategy (facts / preferences / summary). Extraction is
# asynchronous: records usually appear ~1 minute after the events are written.
#
# Usage: ./records.sh        (RAW=1 ./records.sh dumps the raw API JSON)

cd "$(dirname "$0")"
INFRA_DIR="infra"

MEMORY_ID="$(terraform -chdir="$INFRA_DIR" output -raw memory_id)"
REGION="${AWS_REGION:-$(terraform -chdir="$INFRA_DIR" output -raw region)}"
STRATEGIES="$(terraform -chdir="$INFRA_DIR" output -json memory_strategy_ids)"

echo "memory : $MEMORY_ID"
for name in $(jq -r 'keys[]' <<<"$STRATEGIES"); do
  id="$(jq -r --arg n "$name" '.[$n]' <<<"$STRATEGIES")"
  # A namespace is mandatory and acts as a prefix; every strategy's namespace
  # starts with "/", so "/" returns all of that strategy's records.
  RECORDS="$(aws bedrock-agentcore list-memory-records --region "$REGION" \
    --memory-id "$MEMORY_ID" --memory-strategy-id "$id" --namespace / --output json)"
  echo
  echo "== $name ($id): $(jq '.memoryRecordSummaries | length' <<<"$RECORDS") record(s)"
  if [ "${RAW:-0}" = 1 ]; then
    jq '.memoryRecordSummaries' <<<"$RECORDS"
  else
    jq -r '.memoryRecordSummaries | sort_by(.createdAt)[] |
      "  \(.createdAt)  \(.namespaces | join(","))", "    \(.content.text)"' <<<"$RECORDS"
  fi
done
