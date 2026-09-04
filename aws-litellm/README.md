# aws-litellm — AgentCore → LiteLLM → OpenAI

A POC where a NodeJS agent runs on **AWS Bedrock AgentCore Runtime** and reaches
the **OpenAI API** through a **LiteLLM** proxy. A client invokes the agent with a
prompt.

```
client (InvokeAgentRuntime)
        │
        ▼
AgentCore Runtime  ──HTTP /v1/chat/completions──►  LiteLLM proxy (EC2)  ──►  OpenAI API
 (Node, container)                                  (Docker, port 4000)
```

## Components

| Path         | What it is                                                              |
|--------------|------------------------------------------------------------------------|
| `agentcore/` | NodeJS agent built on the official **`bedrock-agentcore`** runtime SDK (`BedrockAgentCoreApp`). The SDK serves `POST /invocations` + `GET /ping` on `:8080`; the handler streams tokens from LiteLLM via the OpenAI SDK. ARM64 container. |
| `litellm/`   | LiteLLM proxy config (also embedded in the EC2 user-data).             |
| `infra/`     | Terraform: VPC, ECR, IAM, the LiteLLM EC2 host, and the AgentCore runtime. |
| `deploy.sh`  | Orchestrates: create ECR → build/push ARM64 image → apply the rest.    |
| `invoke.sh`  | Invoke the deployed agent with a prompt (AWS CLI + jq, no Node).       |
| `verify.sh`  | Prove the current image is live and traffic routes through LiteLLM.    |
| `destroy.sh` | Tear down everything this project created on AWS.                      |

## Engineer flow

Run the four scripts in order to test this POC:

```bash
./deploy.sh                                  # provision + build/push + apply
./invoke.sh "Explain LiteLLM in one line."   # call the deployed agent
./verify.sh                                   # confirm image is live + routes via LiteLLM
./destroy.sh                                  # tear it all down
```

## How the pieces connect

- The agent talks to LiteLLM instead of OpenAI directly. Terraform injects
  `LITELLM_BASE_URL`, `LITELLM_API_KEY`, and `LITELLM_MODEL` into the runtime as
  environment variables (`infra/agentcore.tf`).
- LiteLLM holds the real `OPENAI_API_KEY` and maps the model alias
  (`gpt-4o-mini`) to `openai/gpt-4o-mini` (`litellm/config.yaml`).
- The agent runs in AgentCore `PUBLIC` network mode, so its outbound call
  reaches the LiteLLM host's public Elastic IP on port 4000.

## Prerequisites

- Terraform ≥ 1.6 with the AWS provider ≥ 6.15 (includes
  `aws_bedrockagentcore_agent_runtime`).
- Docker with `buildx` (to build a `linux/arm64` image on any host).
- AWS CLI configured with credentials; run in a region where AgentCore Runtime
  is available (default `us-east-1`).
- `jq` and `uuidgen` (used by `invoke.sh` / `verify.sh`).

## Deploy

```bash
cd aws-litellm
cp infra/terraform.tfvars.example infra/terraform.tfvars
# edit infra/terraform.tfvars: set openai_api_key and litellm_master_key
./deploy.sh
```

`deploy.sh` runs `terraform init`, creates the ECR repo, builds + pushes the
ARM64 agent image, then applies the full stack (LiteLLM host + AgentCore
runtime). The LiteLLM container needs ~1–2 min after boot to be reachable.

## Invoke

```bash
./invoke.sh "Explain what LiteLLM does in one sentence."
```

This reads the runtime ARN from `terraform output` and calls
`InvokeAgentRuntime`. Example client output:

```
session: session-3b2f...-...
agent response:
LiteLLM is a proxy that exposes many LLM providers behind one OpenAI-compatible API.
```

## Run the agent locally (optional)

```bash
# terminal 1 — LiteLLM
OPENAI_API_KEY=sk-... LITELLM_MASTER_KEY=sk-litellm-master \
  litellm --config litellm/config.yaml --port 4000 --host 0.0.0.0

# terminal 2 — the agent (BedrockAgentCoreApp serves :8080 automatically)
cd agentcore && npm install
LITELLM_BASE_URL=http://localhost:4000 LITELLM_API_KEY=sk-litellm-master \
  node server.js

# terminal 3 — the agent streams SSE, so responses arrive as `data:` events
curl -sN localhost:8080/invocations \
  -H 'content-type: application/json' \
  -d '{"prompt":"hello"}'
```

## Tear down

```bash
./destroy.sh
```

## POC caveats (not production-ready)

- The OpenAI key and LiteLLM master key are passed via Terraform variables into
  EC2 user-data — visible in state and the EC2 console. For real use, store them
  in SSM Parameter Store / Secrets Manager and fetch at boot.
- LiteLLM's port 4000 is open to `0.0.0.0/0` (guarded only by the master key).
  Restrict the security group CIDR for anything beyond a demo.
- LiteLLM runs as a single EC2 container with no TLS. Front it with an ALB +
  ACM cert (HTTPS) for anything real.
