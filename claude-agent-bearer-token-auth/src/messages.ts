// Small pure helpers to read the messages that query() yields. Kept free of any
// SDK import so they can be unit-tested against plain objects.
//
// The async iterator returned by query() yields these message shapes:
//   { type: "system",    subtype: "init", session_id, ... }  -> session starts
//   { type: "assistant", session_id, message: { content: [...] } }
//   { type: "user",      session_id, message: { role, content } }
//   { type: "result",    subtype, result, session_id, total_cost_usd, num_turns }

export interface ResultMessage {
  type: "result";
  subtype: string;
  result?: string;
  session_id: string;
  total_cost_usd?: number;
  num_turns?: number;
}

// Every message carries session_id; grab it from the first one that has it.
export function sessionIdOf(message: { session_id?: string }): string | null {
  return message.session_id ?? null;
}

// Flatten an assistant message's content blocks into plain text.
export function assistantText(message: any): string {
  const content = message?.message?.content;
  if (typeof content === "string") return content;
  if (!Array.isArray(content)) return "";
  return content
    .filter((block: any) => block?.type === "text")
    .map((block: any) => block.text)
    .join("");
}

export function isResult(message: any): message is ResultMessage {
  return message?.type === "result";
}
