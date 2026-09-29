# AgentCore Memory — the managed service under test.
#
# Short-term memory = raw "events" (one per conversation turn) the agent writes
# with CreateEvent. They are kept for event_expiry_duration days.
#
# Long-term memory = "memory records" that the service extracts asynchronously
# from those events, driven by the strategies below. Each strategy writes into
# its own namespace; {actorId} / {sessionId} are filled in by the service.
resource "aws_bedrockagentcore_memory" "agent" {
  name                  = replace("${var.name_prefix}_memory", "-", "_")
  description           = "Conversation memory for the aws-memory POC agent"
  event_expiry_duration = var.memory_event_expiry_days
}

# Distils durable facts out of the conversation ("the user lives in Lisbon").
resource "aws_bedrockagentcore_memory_strategy" "semantic" {
  name                = "facts"
  memory_id           = aws_bedrockagentcore_memory.agent.id
  type                = "SEMANTIC"
  namespace_templates = ["/facts/{actorId}"]
}

# Captures the user's stated likes/dislikes ("prefers answers in bullet points").
resource "aws_bedrockagentcore_memory_strategy" "preferences" {
  name                = "preferences"
  memory_id           = aws_bedrockagentcore_memory.agent.id
  type                = "USER_PREFERENCE"
  namespace_templates = ["/preferences/{actorId}"]
}

# Keeps a rolling summary per session.
resource "aws_bedrockagentcore_memory_strategy" "summary" {
  name                = "summary"
  memory_id           = aws_bedrockagentcore_memory.agent.id
  type                = "SUMMARIZATION"
  namespace_templates = ["/summaries/{actorId}/{sessionId}"]
}
