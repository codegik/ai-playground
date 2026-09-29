output "region" {
  description = "Region everything is deployed in."
  value       = var.aws_region
}

output "ecr_repository_url" {
  description = "ECR repo to push the agent image to."
  value       = aws_ecr_repository.agent.repository_url
}

output "litellm_url" {
  description = "Public LiteLLM proxy endpoint."
  value       = "http://${aws_eip.litellm.public_ip}:4000"
}

output "litellm_master_key" {
  description = "LiteLLM master key (provided or auto-generated). Use as the API key when calling the proxy."
  value       = local.litellm_master_key
  sensitive   = true
}

output "agent_runtime_arn" {
  description = "ARN used by the client to invoke the agent."
  value       = aws_bedrockagentcore_agent_runtime.agent.agent_runtime_arn
}

output "agent_runtime_id" {
  description = "AgentCore runtime id."
  value       = aws_bedrockagentcore_agent_runtime.agent.agent_runtime_id
}

output "memory_id" {
  description = "AgentCore Memory id the agent reads/writes events to."
  value       = aws_bedrockagentcore_memory.agent.id
}

output "memory_strategy_ids" {
  description = "Long-term memory strategies and their ids (records are tagged with these)."
  value = {
    facts       = aws_bedrockagentcore_memory_strategy.semantic.memory_strategy_id
    preferences = aws_bedrockagentcore_memory_strategy.preferences.memory_strategy_id
    summary     = aws_bedrockagentcore_memory_strategy.summary.memory_strategy_id
  }
}
