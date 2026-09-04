data "aws_caller_identity" "current" {}
data "aws_region" "current" {}

# Execution role the AgentCore Runtime assumes to pull the image, write logs,
# emit traces, and obtain its workload identity token.
data "aws_iam_policy_document" "agent_assume" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["bedrock-agentcore.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "agent" {
  name               = "${var.name_prefix}-agentcore-role"
  assume_role_policy = data.aws_iam_policy_document.agent_assume.json
}

data "aws_iam_policy_document" "agent_permissions" {
  statement {
    sid       = "EcrAuth"
    effect    = "Allow"
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }

  statement {
    sid       = "EcrPull"
    effect    = "Allow"
    actions   = ["ecr:BatchGetImage", "ecr:GetDownloadUrlForLayer"]
    resources = [aws_ecr_repository.agent.arn]
  }

  statement {
    sid    = "Logs"
    effect = "Allow"
    actions = [
      "logs:CreateLogGroup",
      "logs:CreateLogStream",
      "logs:PutLogEvents",
      "logs:DescribeLogGroups",
      "logs:DescribeLogStreams",
    ]
    resources = ["arn:aws:logs:${data.aws_region.current.region}:${data.aws_caller_identity.current.account_id}:log-group:/aws/bedrock-agentcore/*"]
  }

  statement {
    sid    = "Telemetry"
    effect = "Allow"
    actions = [
      "xray:PutTraceSegments",
      "xray:PutTelemetryRecords",
      "xray:GetSamplingRules",
      "xray:GetSamplingTargets",
      "cloudwatch:PutMetricData",
    ]
    resources = ["*"]
  }

  statement {
    sid    = "WorkloadIdentity"
    effect = "Allow"
    actions = [
      "bedrock-agentcore:GetWorkloadAccessToken",
      "bedrock-agentcore:GetWorkloadAccessTokenForJWT",
      "bedrock-agentcore:GetWorkloadAccessTokenForUserId",
    ]
    resources = ["*"]
  }
}

resource "aws_iam_role_policy" "agent" {
  name   = "${var.name_prefix}-agentcore-policy"
  role   = aws_iam_role.agent.id
  policy = data.aws_iam_policy_document.agent_permissions.json
}

resource "aws_bedrockagentcore_agent_runtime" "agent" {
  agent_runtime_name = replace("${var.name_prefix}_runtime", "-", "_")
  role_arn           = aws_iam_role.agent.arn

  agent_runtime_artifact {
    container_configuration {
      container_uri = "${aws_ecr_repository.agent.repository_url}:${var.agent_image_tag}"
    }
  }

  network_configuration {
    network_mode = "PUBLIC"
  }

  # Wire the agent to the LiteLLM proxy. The agent reads these at runtime.
  environment_variables = {
    LITELLM_BASE_URL = "http://${aws_eip.litellm.public_ip}:4000"
    LITELLM_API_KEY  = local.litellm_master_key
    LITELLM_MODEL    = var.litellm_model_alias
  }

  # The image must already be pushed to ECR before creating the runtime.
  depends_on = [aws_iam_role_policy.agent]
}
