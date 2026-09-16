import { test } from "node:test";
import assert from "node:assert/strict";
import { mask, resolveAuthMode } from "../src/auth.ts";
import { assistantText, isResult, sessionIdOf } from "../src/messages.ts";
import { byRecent, titleFrom, upsertSession } from "../src/store.ts";

test("resolveAuthMode: bearer token -> bearer mode", () => {
  const cfg = resolveAuthMode({ ANTHROPIC_AUTH_TOKEN: "sk-bearer-abcdef" });
  assert.equal(cfg.mode, "bearer");
});

test("resolveAuthMode: api key -> api-key mode", () => {
  const cfg = resolveAuthMode({ ANTHROPIC_API_KEY: "sk-ant-123456" });
  assert.equal(cfg.mode, "api-key");
});

test("resolveAuthMode: bearer wins when both are set", () => {
  const cfg = resolveAuthMode({
    ANTHROPIC_AUTH_TOKEN: "bearer-tok-1234",
    ANTHROPIC_API_KEY: "sk-ant-9999",
  });
  assert.equal(cfg.mode, "bearer");
});

test("resolveAuthMode: nothing set -> cli-default", () => {
  const cfg = resolveAuthMode({});
  assert.equal(cfg.mode, "cli-default");
  assert.equal(cfg.baseUrl, null);
});

test("resolveAuthMode: base url is captured", () => {
  const cfg = resolveAuthMode({
    ANTHROPIC_AUTH_TOKEN: "t-abcdefgh",
    ANTHROPIC_BASE_URL: "https://gateway.example.com",
  });
  assert.equal(cfg.baseUrl, "https://gateway.example.com");
});

test("mask hides the middle of a secret", () => {
  assert.equal(mask("sk-1234567890"), "sk-1…7890");
  assert.equal(mask("short"), "*****");
});

test("sessionIdOf reads session_id from a message", () => {
  assert.equal(sessionIdOf({ session_id: "abc" }), "abc");
  assert.equal(sessionIdOf({}), null);
});

test("assistantText flattens text content blocks", () => {
  const msg = {
    type: "assistant",
    message: { content: [{ type: "text", text: "Hello " }, { type: "text", text: "world" }] },
  };
  assert.equal(assistantText(msg), "Hello world");
});

test("assistantText ignores non-text blocks", () => {
  const msg = {
    message: { content: [{ type: "tool_use", id: "x" }, { type: "text", text: "kept" }] },
  };
  assert.equal(assistantText(msg), "kept");
});

test("isResult narrows result messages", () => {
  assert.equal(isResult({ type: "result", subtype: "success", session_id: "s" }), true);
  assert.equal(isResult({ type: "assistant" }), false);
});

test("titleFrom collapses whitespace and truncates long prompts", () => {
  assert.equal(titleFrom("  hello   world \n"), "hello world");
  assert.equal(titleFrom("x".repeat(60)).length, 48);
  assert.ok(titleFrom("x".repeat(60)).endsWith("…"));
});

test("upsertSession inserts a new record with turns=1", () => {
  const out = upsertSession([], { id: "s1", title: "hi", costUsd: 0.01 }, "2026-01-01T00:00:00Z");
  assert.equal(out.length, 1);
  assert.equal(out[0].turns, 1);
  assert.equal(out[0].costUsd, 0.01);
  assert.equal(out[0].createdAt, "2026-01-01T00:00:00Z");
});

test("upsertSession accumulates turns/cost and keeps the original title", () => {
  const first = upsertSession([], { id: "s1", title: "first", costUsd: 0.01 }, "2026-01-01T00:00:00Z");
  const second = upsertSession(first, { id: "s1", title: "second", costUsd: 0.02 }, "2026-01-02T00:00:00Z");
  assert.equal(second.length, 1);
  assert.equal(second[0].turns, 2);
  assert.ok(Math.abs(second[0].costUsd - 0.03) < 1e-9);
  assert.equal(second[0].title, "first"); // title preserved
  assert.equal(second[0].createdAt, "2026-01-01T00:00:00Z"); // createdAt preserved
  assert.equal(second[0].lastUsedAt, "2026-01-02T00:00:00Z"); // lastUsed advanced
});

test("upsertSession does not mutate the input array", () => {
  const input = upsertSession([], { id: "s1", title: "a", costUsd: 0 }, "2026-01-01T00:00:00Z");
  const snapshot = JSON.stringify(input);
  upsertSession(input, { id: "s2", title: "b", costUsd: 0 }, "2026-01-02T00:00:00Z");
  assert.equal(JSON.stringify(input), snapshot);
});

test("byRecent sorts most-recently-used first", () => {
  const recs = [
    { id: "a", title: "a", createdAt: "", lastUsedAt: "2026-01-01T00:00:00Z", turns: 1, costUsd: 0 },
    { id: "b", title: "b", createdAt: "", lastUsedAt: "2026-01-03T00:00:00Z", turns: 1, costUsd: 0 },
    { id: "c", title: "c", createdAt: "", lastUsedAt: "2026-01-02T00:00:00Z", turns: 1, costUsd: 0 },
  ];
  assert.deepEqual(byRecent(recs).map((r) => r.id), ["b", "c", "a"]);
});
