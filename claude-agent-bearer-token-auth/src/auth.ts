// The Agent SDK does not authenticate by itself: it spawns the Claude Code CLI,
// and the CLI reads these env vars from process.env at startup. So "configuring
// auth" means putting the right vars in the environment before calling query().
//
//   ANTHROPIC_AUTH_TOKEN -> sent as `Authorization: Bearer <token>` (LLM gateway/proxy)
//   ANTHROPIC_API_KEY    -> sent as `x-api-key: <key>`              (direct Anthropic API)
//   ANTHROPIC_BASE_URL   -> overrides the endpoint for either mode
//
// If neither is set, the CLI falls back to its own logged-in credentials
// (the OAuth token from a Claude Pro/Max subscription).

export type AuthMode = "bearer" | "api-key" | "cli-default";

export interface AuthConfig {
  mode: AuthMode;
  baseUrl: string | null;
  detail: string;
}

// Pure function so it can be unit-tested without touching the network or the SDK.
export function resolveAuthMode(env: Record<string, string | undefined>): AuthConfig {
  const bearer = env.ANTHROPIC_AUTH_TOKEN?.trim();
  const apiKey = env.ANTHROPIC_API_KEY?.trim();
  const baseUrl = env.ANTHROPIC_BASE_URL?.trim() || null;

  // Bearer token wins when both are present (that's what a gateway expects).
  if (bearer) {
    return {
      mode: "bearer",
      baseUrl,
      detail: `Authorization: Bearer <token> (${mask(bearer)})`,
    };
  }
  if (apiKey) {
    return {
      mode: "api-key",
      baseUrl,
      detail: `x-api-key: <key> (${mask(apiKey)})`,
    };
  }
  return {
    mode: "cli-default",
    baseUrl,
    detail: "no token in env — using the Claude Code CLI's logged-in credentials",
  };
}

// Never print a full secret; show only enough to identify it.
export function mask(secret: string): string {
  if (secret.length <= 8) return "*".repeat(secret.length);
  return `${secret.slice(0, 4)}…${secret.slice(-4)}`;
}
