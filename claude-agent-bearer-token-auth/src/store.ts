import { existsSync, readFileSync, writeFileSync } from "node:fs";

// A local index of sessions this app has created, so the user can pick an
// existing conversation on the next run. The SDK/CLI stores the actual message
// history; here we only track ids + a friendly label to resume by.
export interface SessionRecord {
  id: string;
  title: string;
  createdAt: string;
  lastUsedAt: string;
  turns: number;
  costUsd: number;
}

// A one-line label derived from the first prompt of a session.
export function titleFrom(prompt: string): string {
  const clean = prompt.replace(/\s+/g, " ").trim();
  return clean.length <= 48 ? clean : `${clean.slice(0, 47)}…`;
}

// Insert a new session or update an existing one, keeping the original title
// and createdAt. Pure: returns a new array, never mutates the input.
export function upsertSession(
  records: SessionRecord[],
  entry: { id: string; title: string; costUsd: number },
  nowIso: string,
): SessionRecord[] {
  const existing = records.find((r) => r.id === entry.id);
  if (existing) {
    const updated: SessionRecord = {
      ...existing,
      lastUsedAt: nowIso,
      turns: existing.turns + 1,
      costUsd: existing.costUsd + entry.costUsd,
    };
    return records.map((r) => (r.id === entry.id ? updated : r));
  }
  const created: SessionRecord = {
    id: entry.id,
    title: entry.title,
    createdAt: nowIso,
    lastUsedAt: nowIso,
    turns: 1,
    costUsd: entry.costUsd,
  };
  return [...records, created];
}

// Most-recently-used first, for display.
export function byRecent(records: SessionRecord[]): SessionRecord[] {
  return [...records].sort((a, b) => b.lastUsedAt.localeCompare(a.lastUsedAt));
}

export function loadSessions(path: string): SessionRecord[] {
  if (!existsSync(path)) return [];
  try {
    const parsed = JSON.parse(readFileSync(path, "utf8"));
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return []; // a corrupt store shouldn't crash the chat
  }
}

export function saveSessions(path: string, records: SessionRecord[]): void {
  writeFileSync(path, `${JSON.stringify(records, null, 2)}\n`);
}
