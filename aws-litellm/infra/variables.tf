variable "aws_region" {
  description = "AWS region. Must be a region where Bedrock AgentCore Runtime is available."
  type        = string
  default     = "us-east-1"
}

variable "name_prefix" {
  description = "Prefix for resource names."
  type        = string
  default     = "litellm-agent"
}

variable "openai_api_key" {
  description = "Real OpenAI API key. LiteLLM uses this to reach the OpenAI API."
  type        = string
  sensitive   = true
}

variable "litellm_master_key" {
  description = "Shared secret between the agent and LiteLLM (LiteLLM master_key). Leave empty to auto-generate one at provision time."
  type        = string
  sensitive   = true
  default     = ""
}

variable "litellm_model_alias" {
  description = "Model alias exposed by LiteLLM and requested by the agent."
  type        = string
  default     = "gpt-4o-mini"
}

variable "openai_model" {
  description = "Upstream OpenAI model LiteLLM maps the alias to."
  type        = string
  default     = "gpt-4o-mini"
}

variable "litellm_instance_type" {
  description = "EC2 instance type for the LiteLLM proxy host."
  type        = string
  default     = "t3.small"
}

variable "agent_image_tag" {
  description = "Tag of the agent image pushed to ECR."
  type        = string
  default     = "latest"
}
