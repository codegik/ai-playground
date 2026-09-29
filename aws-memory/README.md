# aws-memory — AgentCore Memory, seen from the inside

A POC to test **Amazon Bedrock AgentCore Memory** (the managed memory service)
and to *see* what an agent does with it: which memory events it triggers and
what those events contain.

It reuses the infrastructure of [`../aws-litellm`](../aws-litellm) (VPC, ECR,
LiteLLM on EC2, the NodeJS agent on AgentCore Runtime) and adds a Memory
resource with three long-term strategies. The agent reads and writes that memory
on every invocation.

```
client (invoke.sh)
   │ InvokeAgentRuntime {prompt, actorId}
   ▼
AgentCore Runtime (Node agent) ──► LiteLLM (EC2) ──► OpenAI
   │  ListEvents / RetrieveMemoryRecords / CreateEvent
   ▼
AgentCore Memory
   ├─ short-term: events (one per turn, per actor + session)
   └─ long-term : records extracted async by strategies
        facts        (SEMANTIC)        /facts/{actorId}
        preferences  (USER_PREFERENCE) /preferences/{actorId}
        summary      (SUMMARIZATION)   /summaries/{actorId}/{sessionId}
```

## What the agent does per invocation

| Step | Memory API                         | Purpose                                            |
|------|------------------------------------|----------------------------------------------------|
| 1    | `ListEvents` (actor, session)      | load this session's earlier turns as chat history  |
| 1    | `RetrieveMemoryRecords` ×2         | semantic search of facts + preferences for the actor |
| 2    | `CreateEvent` role=`USER`          | store the prompt                                   |
| 3    | —                                  | call the model via LiteLLM with history + records  |
| 4    | `CreateEvent` role=`ASSISTANT`     | store the answer                                   |

After step 4 the service runs the strategies over the new events in the
background and writes long-term **memory records** (~1 min later).

The memory session is the AgentCore **runtime session id**, so reusing a
`SESSION` continues a conversation; `actorId` identifies the user across
sessions.

## Components

| Path          | What it is                                                                  |
|---------------|-----------------------------------------------------------------------------|
| `agentcore/`  | Node agent: `server.js` (runtime handler) + `memory.js` (Memory API wrapper, `@aws-sdk/client-bedrock-agentcore`) + unit tests. |
| `infra/`      | Terraform. `memory.tf` = Memory + strategies; `agentcore.tf` adds memory IAM + `MEMORY_ID`. The rest is from aws-litellm. |
| `deploy.sh`   | Create ECR → build/push ARM64 image → apply everything.                      |
| `invoke.sh`   | Call the agent; prints every memory call it made and the answer.             |
| `events.sh`   | Read the **events** (and their content) straight from AgentCore Memory.      |
| `records.sh`  | Read the **long-term records** the strategies extracted.                     |
| `destroy.sh`  | Tear everything down.                                                        |

## Engineer flow

```bash
cp infra/terraform.tfvars.example infra/terraform.tfvars   # set openai_api_key
./deploy.sh

# 1st turn — new session. Prints the memory calls + answer + the session id.
./invoke.sh "Hi, I'm Ana. I live in Lisbon and I prefer very short answers."

# 2nd turn in the SAME session — ListEvents now returns the 2 earlier events.
SESSION=session-... ./invoke.sh "Where do I live?"

./events.sh                 # all sessions of the actor, every event + content
./events.sh session-...     # one session;  RAW=1 for the raw API JSON

sleep 60; ./records.sh      # long-term records extracted from those events

# New session: no short-term history, but RetrieveMemoryRecords brings back
# what was learned before.
./invoke.sh "What do you know about me?"

./destroy.sh
```

`make deploy | invoke | events | records | destroy` wrap the same scripts
(`make invoke PROMPT='"hi"'`, `make events SESSION=...`).

### Example output

```
$ ./invoke.sh "Where do I live?"
actor  : demo-user
session: session-6f1c...

memory calls made by the agent:
  ListEvents             -> 2 event(s) already in this session
      [USER] Hi, I'm Ana. I live in Lisbon and I prefer very short answers.
      [ASSISTANT] Nice to meet you, Ana!
  RetrieveMemoryRecords  -> 0 record(s) in /facts/demo-user
  RetrieveMemoryRecords  -> 0 record(s) in /preferences/demo-user
  CreateEvent            -> 0000001790...#a1b2  [USER] Where do I live?
  CreateEvent            -> 0000001790...#c3d4  [ASSISTANT] Lisbon.

$ ./events.sh session-6f1c...
== session session-6f1c... (4 events)
2026-09-29T10:00:01Z  0000001790...
    [USER] Hi, I'm Ana. I live in Lisbon and I prefer very short answers.
...
```

## Where else to look at the events

- **CloudWatch** — the agent logs every memory call as one JSON line with
  `"marker":"memory-op"` in `/aws/bedrock-agentcore/runtimes/<runtime-id>-DEFAULT`:
  ```bash
  aws logs tail /aws/bedrock-agentcore/runtimes/$(terraform -chdir=infra output -raw agent_runtime_id)-DEFAULT \
    --follow --filter-pattern '"memory-op"'
  ```
- **Console** — Bedrock AgentCore → Memory → `memory_agent_memory`.

## Tests

```bash
make build && make test     # unit tests for memory.js (no AWS needed)
```

## Run the agent locally (optional)

Needs a deployed Memory (for `MEMORY_ID`) and local AWS credentials:

```bash
OPENAI_API_KEY=sk-... LITELLM_MASTER_KEY=sk-litellm-master \
  litellm --config ../aws-litellm/litellm/config.yaml --port 4000   # terminal 1

MEMORY_ID=$(terraform -chdir=infra output -raw memory_id) \
LITELLM_API_KEY=sk-litellm-master make run                           # terminal 2

curl -sN localhost:8080/invocations -H 'content-type: application/json' \
  -H 'x-amzn-bedrock-agentcore-runtime-session-id: local-session-0000000000000000000001' \
  -d '{"prompt":"hello","actorId":"demo-user"}'                      # terminal 3
```

## POC caveats

Same as aws-litellm (secrets in Terraform state/user-data, LiteLLM open on
port 4000 without TLS), plus:

- Events expire after `memory_event_expiry_days` (default 7); records persist
  until the memory is destroyed.
- Long-term extraction is asynchronous — `records.sh` right after an invoke
  may show nothing yet.
