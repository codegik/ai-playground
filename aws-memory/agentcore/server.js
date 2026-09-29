import { BedrockAgentCoreApp } from "bedrock-agentcore/runtime";
import OpenAI from "openai";
import { z } from "zod";
import { Memory, eventsToMessages, recordsToContext } from "./memory.js";

const LITELLM_BASE_URL = process.env.LITELLM_BASE_URL || "http://localhost:4000";
const LITELLM_API_KEY = process.env.LITELLM_API_KEY || "sk-litellm-master";
const MODEL = process.env.LITELLM_MODEL || "gpt-4o-mini";
const MEMORY_ID = process.env.MEMORY_ID;
const SYSTEM_PROMPT =
  process.env.SYSTEM_PROMPT ||
  "You are a helpful assistant running on AWS Bedrock AgentCore. You have memory of past conversations with this user.";

// LiteLLM is OpenAI-compatible, so the OpenAI SDK points at the proxy.
const client = new OpenAI({
  baseURL: `${LITELLM_BASE_URL.replace(/\/$/, "")}/v1`,
  apiKey: LITELLM_API_KEY,
});

const app = new BedrockAgentCoreApp({
  invocationHandler: {
    // actorId identifies the user across sessions (long-term memory is per actor).
    requestSchema: z.object({ prompt: z.string(), actorId: z.string().default("demo-user") }),

    process: async function* (request, context) {
      const { prompt, actorId } = request;
      // The AgentCore runtime session doubles as the memory session, so reusing
      // a runtime session id continues the same short-term conversation.
      const sessionId = context.sessionId;

      // Every memory call is logged to CloudWatch (filter on "memory-op") and
      // queued so it can be streamed to the client alongside the answer.
      const ops = [];
      const memory = new Memory({
        memoryId: MEMORY_ID,
        onOp: (op) => {
          console.log(JSON.stringify({ marker: "memory-op", ...op }));
          ops.push(op);
        },
      });
      const flush = function* () {
        while (ops.length) yield { type: "memory", ...ops.shift() };
      };

      // 1. Read memory: this session's recent turns + long-term records about the actor.
      const [history, remembered] = await Promise.all([
        memory.recentTurns(actorId, sessionId),
        memory.recall(actorId, prompt),
      ]);
      yield* flush();

      // 2. Persist the user's turn as a short-term event.
      await memory.saveTurn(actorId, sessionId, "USER", prompt);
      yield* flush();

      // 3. Answer with the remembered context, streaming tokens to the client.
      const system = [SYSTEM_PROMPT, recordsToContext(remembered)].filter(Boolean).join("\n\n");
      const stream = await client.chat.completions.create({
        model: MODEL,
        stream: true,
        messages: [{ role: "system", content: system }, ...eventsToMessages(history), { role: "user", content: prompt }],
      });
      let answer = "";
      for await (const chunk of stream) {
        const text = chunk.choices?.[0]?.delta?.content;
        if (text) {
          answer += text;
          yield { type: "text", text };
        }
      }

      // 4. Persist the assistant's turn. Strategies extract long-term records from these asynchronously.
      await memory.saveTurn(actorId, sessionId, "ASSISTANT", answer);
      yield* flush();
    },
  },
});

app.run();
