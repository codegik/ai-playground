import { BedrockAgentCoreApp } from "bedrock-agentcore/runtime";
import OpenAI from "openai";
import { z } from "zod";

// BedrockAgentCoreApp is the official AgentCore Runtime SDK. It implements the
// runtime contract for us: it serves POST /invocations and GET /ping on :8080,
// validates the payload against requestSchema, and SSE-encodes whatever the
// async-generator `process` yields.
const LITELLM_BASE_URL = process.env.LITELLM_BASE_URL || "http://localhost:4000";
const LITELLM_API_KEY = process.env.LITELLM_API_KEY || "sk-litellm-master";
const MODEL = process.env.LITELLM_MODEL || "gpt-4o-mini";
const SYSTEM_PROMPT =
  process.env.SYSTEM_PROMPT || "You are a helpful assistant running on AWS Bedrock AgentCore.";

// LiteLLM is OpenAI-compatible, so we point the OpenAI SDK at the LiteLLM proxy.
// The proxy holds the real OpenAI key and forwards the request upstream.
const client = new OpenAI({
  baseURL: `${LITELLM_BASE_URL.replace(/\/$/, "")}/v1`,
  apiKey: LITELLM_API_KEY,
});

const app = new BedrockAgentCoreApp({
  invocationHandler: {
    // The InvokeAgentRuntime payload must match this shape: { "prompt": "..." }.
    requestSchema: z.object({ prompt: z.string() }),

    // Stream the model's tokens back through the runtime as they arrive.
    process: async function* (request) {
      // Log an identifiable marker per invocation so requests can be correlated
      // in CloudWatch (filter on "agent-invoke"). Also records the upstream the
      // OpenAI client is pointed at — this is the LiteLLM gateway, not OpenAI.
      console.log(
        JSON.stringify({
          marker: "agent-invoke",
          model: MODEL,
          upstream: client.baseURL,
          prompt: request.prompt,
        })
      );

      const stream = await client.chat.completions.create({
        model: MODEL,
        stream: true,
        messages: [
          { role: "system", content: SYSTEM_PROMPT },
          { role: "user", content: request.prompt },
        ],
      });

      for await (const chunk of stream) {
        const text = chunk.choices?.[0]?.delta?.content;
        if (text) yield { event: "message", data: { text } };
      }
    },
  },
});

app.run();
