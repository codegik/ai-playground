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

output "agent_runtime_arn" {
  description = "ARN used by the client to invoke the agent."
  value       = aws_bedrockagentcore_agent_runtime.agent.agent_runtime_arn
}

output "agent_runtime_id" {
  description = "AgentCore runtime id."
  value       = aws_bedrockagentcore_agent_runtime.agent.agent_runtime_id
}
