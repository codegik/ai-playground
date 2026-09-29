import { test } from "node:test";
import assert from "node:assert/strict";
import { Memory, describeEvent, eventsToMessages, recordsToContext, turnPayload } from "../memory.js";

const event = (id, ts, role, text) => ({
  eventId: id,
  eventTimestamp: new Date(ts),
  payload: turnPayload(role, text),
});

test("turnPayload builds a conversational payload", () => {
  assert.deepEqual(turnPayload("USER", "hi"), [{ conversational: { role: "USER", content: { text: "hi" } } }]);
});

test("describeEvent flattens the conversational payload", () => {
  assert.deepEqual(describeEvent(event("e1", "2026-01-01T00:00:00Z", "USER", "hi")), {
    eventId: "e1",
    timestamp: "2026-01-01T00:00:00.000Z",
    role: "USER",
    text: "hi",
  });
});

test("eventsToMessages orders oldest first and maps roles", () => {
  const events = [
    event("e2", "2026-01-01T00:00:02Z", "ASSISTANT", "hello"),
    event("e1", "2026-01-01T00:00:01Z", "USER", "hi"),
    event("e3", "2026-01-01T00:00:03Z", "TOOL", "ignored"),
  ];
  assert.deepEqual(eventsToMessages(events), [
    { role: "user", content: "hi" },
    { role: "assistant", content: "hello" },
  ]);
});

test("recordsToContext is empty without records", () => {
  assert.equal(recordsToContext([]), "");
  assert.match(recordsToContext([{ content: { text: "likes tea" } }]), /- likes tea/);
});

test("Memory reports every call through onOp", async () => {
  const ops = [];
  const sent = [];
  const client = {
    send: async (cmd) => {
      sent.push(cmd.constructor.name);
      // Like the real service: the CreateEvent response carries no payload.
      if (cmd.constructor.name === "CreateEventCommand") return { event: { eventId: "e9", eventTimestamp: new Date() } };
      if (cmd.constructor.name === "ListEventsCommand") return { events: [] };
      return { memoryRecordSummaries: [{ memoryRecordId: "r1", score: 0.9, content: { text: "likes tea" } }] };
    },
  };
  const memory = new Memory({ memoryId: "m", client, onOp: (op) => ops.push(op) });

  await memory.recentTurns("alice", "s1");
  const records = await memory.recall("alice", "drinks?");
  await memory.saveTurn("alice", "s1", "USER", "hi");

  assert.deepEqual(sent, ["ListEventsCommand", "RetrieveMemoryRecordsCommand", "RetrieveMemoryRecordsCommand", "CreateEventCommand"]);
  assert.equal(records.length, 2);
  assert.deepEqual(ops.map((o) => o.op), ["ListEvents", "RetrieveMemoryRecords", "RetrieveMemoryRecords", "CreateEvent"]);
  assert.deepEqual(ops.filter((o) => o.namespace).map((o) => o.namespace), ["/facts/alice", "/preferences/alice"]);
  assert.equal(ops[3].event.text, "hi");
});
