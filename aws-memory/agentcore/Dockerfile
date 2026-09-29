# AgentCore Runtime requires linux/arm64 (AWS Graviton). Building on x86
# without --platform produces images that fail to start on the runtime.
FROM --platform=linux/arm64 node:20-slim

WORKDIR /app

COPY package.json ./
# openai declares an optional peer on zod@^3, but bedrock-agentcore needs zod@^4
# and our code only uses zod v4 (never openai's zod helpers), so bypass the
# optional-peer conflict.
RUN npm install --omit=dev --legacy-peer-deps

COPY server.js memory.js ./

EXPOSE 8080
CMD ["node", "server.js"]
