import { existsSync } from "node:fs";
import { runChat } from "./chat.ts";

// Load .env into process.env (Node 20.6+ built-in) BEFORE anything reads it, so
// the CLI the SDK spawns inherits ANTHROPIC_AUTH_TOKEN / ANTHROPIC_BASE_URL.
if (existsSync(".env")) process.loadEnvFile(".env");

await runChat();
