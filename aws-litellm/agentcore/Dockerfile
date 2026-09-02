# AgentCore Runtime requires linux/arm64 (AWS Graviton). Building on x86
# without --platform produces images that fail to start on the runtime.
FROM --platform=linux/arm64 node:20-slim

WORKDIR /app

COPY package.json ./
RUN npm install --omit=dev

COPY server.js ./

EXPOSE 8080
CMD ["node", "server.js"]
