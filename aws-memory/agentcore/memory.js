import {
  BedrockAgentCoreClient,
  CreateEventCommand,
  ListEventsCommand,
  RetrieveMemoryRecordsCommand,
} from "@aws-sdk/client-bedrock-agentcore";

// Long-term namespaces the agent searches before answering. They must match the
// namespace_templates of the strategies in infra/memory.tf.
export function longTermNamespaces(actorId) {
  return [`/facts/${actorId}`, `/preferences/${actorId}`];
}

// One conversation turn as an AgentCore Memory event payload. "conversational"
// payloads are what the built-in strategies read to extract long-term records.
export function turnPayload(role, text) {
  return [{ conversational: { role, content: { text } } }];
}

// Flatten an event into something readable for logs and the client stream.
export function describeEvent(event) {
  const turn = event.payload?.find((p) => p.conversational)?.conversational;
  return {
    eventId: event.eventId,
    timestamp: new Date(event.eventTimestamp).toISOString(),
    role: turn?.role,
    text: turn?.content?.text,
  };
}

// Short-term memory -> chat history for the model, oldest turn first.
export function eventsToMessages(events) {
  return [...events]
    .sort((a, b) => new Date(a.eventTimestamp) - new Date(b.eventTimestamp))
    .flatMap((e) => e.payload ?? [])
    .map((p) => p.conversational)
    .filter((c) => c?.content?.text && (c.role === "USER" || c.role === "ASSISTANT"))
    .map((c) => ({ role: c.role === "USER" ? "user" : "assistant", content: c.content.text }));
}

// Long-term memory -> an extra block for the system prompt.
export function recordsToContext(records) {
  const lines = records.map((r) => r.content?.text).filter(Boolean);
  if (lines.length === 0) return "";
  return `Things you remember about this user from earlier sessions:\n${lines.map((l) => `- ${l}`).join("\n")}`;
}

// Thin wrapper over the data-plane API. Every call reports what it did through
// `onOp`, so the agent can both log it and stream it back to the caller.
export class Memory {
  constructor({ memoryId, client = new BedrockAgentCoreClient({}), onOp = () => {} }) {
    this.memoryId = memoryId;
    this.client = client;
    this.onOp = onOp;
  }

  async recentTurns(actorId, sessionId, maxResults = 20) {
    const out = await this.client.send(
      new ListEventsCommand({ memoryId: this.memoryId, actorId, sessionId, includePayloads: true, maxResults })
    );
    const events = out.events ?? [];
    this.onOp({ op: "ListEvents", actorId, sessionId, count: events.length, events: events.map(describeEvent) });
    return events;
  }

  async recall(actorId, query, topK = 5) {
    const perNamespace = await Promise.all(
      longTermNamespaces(actorId).map(async (namespace) => {
        const out = await this.client.send(
          new RetrieveMemoryRecordsCommand({ memoryId: this.memoryId, namespace, searchCriteria: { searchQuery: query, topK } })
        );
        const records = out.memoryRecordSummaries ?? [];
        this.onOp({
          op: "RetrieveMemoryRecords",
          namespace,
          count: records.length,
          records: records.map((r) => ({ id: r.memoryRecordId, score: r.score, text: r.content?.text })),
        });
        return records;
      })
    );
    return perNamespace.flat();
  }

  async saveTurn(actorId, sessionId, role, text) {
    const out = await this.client.send(
      new CreateEventCommand({
        memoryId: this.memoryId,
        actorId,
        sessionId,
        eventTimestamp: new Date(),
        payload: turnPayload(role, text),
      })
    );
    this.onOp({ op: "CreateEvent", actorId, sessionId, event: describeEvent(out.event) });
    return out.event;
  }
}
