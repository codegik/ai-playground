import * as readline from "node:readline/promises";
import { stdin as input, stdout as output } from "node:process";
import { resolveAuthMode } from "./auth.ts";
import { SessionManager } from "./session.ts";
import {
  byRecent,
  loadSessions,
  saveSessions,
  titleFrom,
  upsertSession,
  type SessionRecord,
} from "./store.ts";

const STORE_PATH = "sessions.json";

const nowIso = (): string => new Date().toISOString();

function printSessions(records: SessionRecord[]): void {
  if (records.length === 0) {
    console.log("(no saved sessions yet)");
    return;
  }
  byRecent(records).forEach((r, i) => {
    console.log(
      `  [${i + 1}] ${r.title}  ·  ${r.turns} turns · $${r.costUsd.toFixed(4)} · ${r.id.slice(0, 8)}`,
    );
  });
}

// Startup menu: new session or resume one of the saved ones.
async function pickSession(
  rl: readline.Interface,
  records: SessionRecord[],
  model?: string,
): Promise<SessionManager> {
  console.log("Saved sessions:");
  printSessions(records);
  const answer = (
    await rl.question("\nType a number to resume, or Enter for a new session › ")
  ).trim();

  const idx = Number.parseInt(answer, 10);
  const sorted = byRecent(records);
  if (answer && Number.isInteger(idx) && idx >= 1 && idx <= sorted.length) {
    const chosen = sorted[idx - 1];
    console.log(`Resuming: ${chosen.title}\n`);
    return new SessionManager({ model, sessionId: chosen.id });
  }
  console.log("Starting a new session.\n");
  return new SessionManager({ model });
}

const HELP = `Commands:
  /new           start a fresh session
  /list          list saved sessions
  /switch <n>    resume saved session number <n>
  /help          show this help
  /exit          quit`;

export async function runChat(): Promise<void> {
  const auth = resolveAuthMode(process.env);
  console.log(`Auth: ${auth.mode} — ${auth.detail}`);
  console.log(`Endpoint: ${auth.baseUrl ?? "https://api.anthropic.com"}\n`);

  const model = process.env.ANTHROPIC_MODEL || undefined;
  let records = loadSessions(STORE_PATH);
  const rl = readline.createInterface({ input, output });

  // On stdin close (Ctrl-D / EOF) readline/promises leaves a pending question()
  // unsettled, which strands the process. Abort the pending question so the
  // loop can break cleanly.
  const closed = new AbortController();
  rl.once("close", () => closed.abort());

  let manager = await pickSession(rl, records, model);

  console.log(HELP);
  console.log();

  while (true) {
    let line: string;
    try {
      line = (await rl.question("you › ", { signal: closed.signal })).trim();
    } catch {
      break; // stdin closed (Ctrl-D / EOF)
    }
    if (!line) continue;

    if (line === "/exit") break;
    if (line === "/help") {
      console.log(HELP);
      continue;
    }
    if (line === "/list") {
      printSessions(records);
      continue;
    }
    if (line === "/new") {
      manager = new SessionManager({ model });
      console.log("Started a new session.\n");
      continue;
    }
    if (line.startsWith("/switch")) {
      const n = Number.parseInt(line.split(/\s+/)[1] ?? "", 10);
      const sorted = byRecent(records);
      if (Number.isInteger(n) && n >= 1 && n <= sorted.length) {
        manager = new SessionManager({ model, sessionId: sorted[n - 1].id });
        console.log(`Switched to: ${sorted[n - 1].title}\n`);
      } else {
        console.log("Usage: /switch <n>  (see /list)\n");
      }
      continue;
    }

    // Anything else is a prompt for Claude.
    output.write("claude … ");
    try {
      const turn = await manager.ask(line);
      output.write(`\rclaude › ${turn.text}\n`);
      // Register/refresh this session in the local index and persist it.
      records = upsertSession(
        records,
        { id: turn.sessionId, title: titleFrom(line), costUsd: turn.costUsd },
        nowIso(),
      );
      saveSessions(STORE_PATH, records);
    } catch (err) {
      output.write(`\r`);
      console.error(`error: ${(err as Error).message}\n`);
    }
  }

  rl.close();
  console.log("bye.");
}
