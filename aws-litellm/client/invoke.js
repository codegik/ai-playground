import { randomUUID } from "node:crypto";
import {
  BedrockAgentCoreClient,
  InvokeAgentRuntimeCommand,
} from "@aws-sdk/client-bedrock-agentcore";

// Usage: node invoke.js "your prompt here"
// Config comes from env vars (see Makefile `invoke` target):
//   AGENT_RUNTIME_ARN - ARN of the deployed AgentCore runtime (required)
//   AWS_REGION        - region of the runtime (default us-east-1)
const arn = process.env.AGENT_RUNTIME_ARN;
const region = process.env.AWS_REGION || "us-east-1";
const prompt = process.argv.slice(2).join(" ") || "Say hello and tell me which model you are.";

if (!arn) {
  console.error("Set AGENT_RUNTIME_ARN (from `terraform output -raw agent_runtime_arn`).");
  process.exit(1);
}

const client = new BedrockAgentCoreClient({ region });

// runtimeSessionId must be 33-256 chars; a UUID (36 chars) satisfies that.
const runtimeSessionId = `session-${randomUUID()}`;

const command = new InvokeAgentRuntimeCommand({
  agentRuntimeArn: arn,
  runtimeSessionId,
  contentType: "application/json",
  // The agent streams SSE events (the bedrock-agentcore SDK encodes yields as SSE).
  accept: "text/event-stream",
  // The payload is opaque bytes to AgentCore; our agent expects { prompt }.
  payload: new TextEncoder().encode(JSON.stringify({ prompt })),
});

// The `response` body is a streaming blob; collect it into a string.
async function readBody(body) {
  if (!body) return "";
  if (typeof body.transformToString === "function") return body.transformToString();
  if (body instanceof Uint8Array) return new TextDecoder().decode(body);
  const chunks = [];
  for await (const chunk of body) chunks.push(chunk);
  return Buffer.concat(chunks).toString("utf-8");
}

// Reassemble the agent's SSE stream: each `data:` line is one { event, data }
// object the agent yielded; concatenate the text deltas into the full answer.
function reassemble(sse) {
  let text = "";
  for (const line of sse.split("\n")) {
    const trimmed = line.trim();
    if (!trimmed.startsWith("data:")) continue;
    const json = trimmed.slice(5).trim();
    if (!json || json === "[DONE]") continue;
    try {
      const evt = JSON.parse(json);
      text += evt?.data?.text ?? evt?.text ?? "";
    } catch {
      /* ignore non-JSON keepalive lines */
    }
  }
  return text;
}

const out = await client.send(command);
const raw = await readBody(out.response);

console.log(`session: ${runtimeSessionId}`);
const answer = reassemble(raw);
console.log("\nagent response:\n" + (answer || raw));
