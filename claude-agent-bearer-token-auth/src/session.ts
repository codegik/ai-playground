import { query } from "@anthropic-ai/claude-agent-sdk";
import { assistantText, isResult, sessionIdOf } from "./messages.ts";

export interface Turn {
  text: string; // the assistant's final answer for this turn
  sessionId: string; // id to resume this conversation later
  costUsd: number; // cost reported by the result message
  numTurns: number; // agentic turns the SDK took internally
}

// A thin wrapper over query() that runs one prompt to completion and remembers
// the session id so the next call can resume the same conversation.
//
// Session model in one line: each query() is one "turn". To keep context across
// turns you must resume the previous session_id — the SDK is otherwise stateless
// between separate query() calls.
export interface SessionManagerOptions {
  model?: string;
  sessionId?: string; // resume an existing conversation from turn 1
}

export class SessionManager {
  private currentSessionId: string | null;
  private readonly model?: string;

  // Note: an explicit field + assignment is used instead of a constructor
  // "parameter property" (constructor(private model)) because Node's built-in
  // type stripping cannot transform parameter properties — only erase types.
  constructor(options: SessionManagerOptions = {}) {
    this.model = options.model;
    this.currentSessionId = options.sessionId ?? null;
  }

  get sessionId(): string | null {
    return this.currentSessionId;
  }

  // First call starts a fresh session; every call after resumes the stored id.
  async ask(prompt: string): Promise<Turn> {
    const response = query({
      prompt,
      options: {
        ...(this.model ? { model: this.model } : {}),
        // resume ties this query to the previous conversation. Omitted on turn 1.
        ...(this.currentSessionId ? { resume: this.currentSessionId } : {}),
      },
    });

    let text = "";
    let costUsd = 0;
    let numTurns = 0;

    for await (const message of response) {
      // session_id is present on every message; capture/refresh it.
      const id = sessionIdOf(message as any);
      if (id) this.currentSessionId = id;

      if ((message as any).type === "assistant") {
        text += assistantText(message);
      } else if (isResult(message)) {
        // The result message carries the authoritative final text + accounting.
        if (message.result) text = message.result;
        costUsd = message.total_cost_usd ?? 0;
        numTurns = message.num_turns ?? 0;
      }
    }

    return {
      text,
      sessionId: this.currentSessionId ?? "",
      costUsd,
      numTurns,
    };
  }
}
