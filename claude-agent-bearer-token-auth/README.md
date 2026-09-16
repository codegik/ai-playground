# claude-agent-bearer-token-auth

A tiny **interactive chat** (terminal) on top of
[`@anthropic-ai/claude-agent-sdk`](https://www.npmjs.com/package/@anthropic-ai/claude-agent-sdk).
You can start a new session or resume a saved one, then send prompts to Claude.
It also shows how to point the SDK at a **bearer-token** endpoint instead of an
API key.

## What it does

- On launch, lists **saved sessions** and lets you pick one to resume or start a
  new one.
- Each message you type is sent to Claude via the SDK; the reply is printed.
- Every session is tracked in a local `sessions.json` so it survives restarts —
  resuming a session restores the full conversation (Claude still remembers what
  you told it earlier).

### In-chat commands

```
/new           start a fresh session
/list          list saved sessions
/switch <n>    resume saved session number <n>
/help          show this help
/exit          quit
```

## How it works (architecture)

The SDK does **not** call the API itself. It spawns the Claude Code CLI as a
subprocess, talks to it over a local Unix socket (stream-json), and the **CLI**
makes the real HTTPS request to `https://api.anthropic.com/v1/messages`.

```
your input ─▶ SessionManager ─(query)─▶ claude CLI subprocess ─HTTPS─▶ api.anthropic.com
```

**Session management is the core idea:** each `query()` is one turn and is
otherwise stateless. To keep context you capture the `session_id` from the
messages and pass it back via `options.resume`:

```ts
import { query } from "@anthropic-ai/claude-agent-sdk";

let sessionId = "";
for await (const msg of query({ prompt: "Remember 42." }))
  if (msg.session_id) sessionId = msg.session_id;      // present on every message

for await (const msg of query({ prompt: "What number?", options: { resume: sessionId } }))
  // ...still remembers 42

// Or continue the most recent session without tracking an id:
query({ prompt: "...", options: { continue: true } });
```

## Auth modes (resolved in `src/auth.ts`)

| Env var | Header sent | Use case |
|---|---|---|
| `ANTHROPIC_AUTH_TOKEN` | `Authorization: Bearer <token>` | LLM gateway / proxy |
| `ANTHROPIC_API_KEY` | `x-api-key: <key>` | Direct Anthropic API (pay-per-token) |
| *(neither)* | — | Falls back to the CLI's logged-in credentials (Pro/Max subscription) |

`ANTHROPIC_BASE_URL` sets the endpoint for either token mode. These are read
from `process.env` by the spawned CLI — **not** passable via the SDK's `options`.
`src/main.ts` loads a `.env` (if present) into `process.env` before starting.

## Run it

```bash
make build   # clean + npm install
make run     # start the chat (src/main.ts)
make test    # run the unit tests (node --test) — no network/credentials needed
```

`make run` works out of the box if you're logged into Claude Code (it uses the
subscription credentials). To exercise bearer-token auth, copy `.env.example` to
`.env` and set `ANTHROPIC_AUTH_TOKEN` + `ANTHROPIC_BASE_URL`.

## Files

| File | Purpose |
|---|---|
| `src/main.ts` | Entry point: loads `.env`, starts the chat |
| `src/chat.ts` | Interactive REPL: session menu + command loop |
| `src/session.ts` | `SessionManager` — wraps `query()`, tracks/resumes `session_id` |
| `src/store.ts` | `sessions.json` persistence (pure upsert/sort helpers + fs I/O) |
| `src/auth.ts` | Resolves bearer / api-key / cli-default auth mode |
| `src/messages.ts` | Pure helpers to read the messages `query()` yields |
| `test/session.test.ts` | Unit tests for the pure logic (auth, messages, store) |

## Notes

- Requires **Node ≥ 22** — the `.ts` files run directly via Node's built-in type
  stripping (no build step, no `tsc`). Constructor *parameter properties* are
  avoided because strip-only mode can't transform them.
- The tests cover the pure logic and need no network or credentials, so
  `make test` always runs green.
- On stdin EOF (Ctrl-D) the chat exits cleanly via an `AbortController` wired to
  readline's `close` (otherwise `question()` strands the process).
- `forkSession` exists in the CLI but isn't part of the documented V2 SDK API;
  this POC sticks to `resume` / `continue`, which are stable.
