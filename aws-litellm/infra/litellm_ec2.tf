# Latest Amazon Linux 2023 AMI (matches the default x86_64 instance types).
data "aws_ssm_parameter" "al2023" {
  name = "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64"
}

resource "aws_security_group" "litellm" {
  name        = "${var.name_prefix}-litellm-sg"
  description = "Allow inbound to the LiteLLM proxy port"
  vpc_id      = aws_vpc.main.id

  # POC: LiteLLM is reachable on the internet (AgentCore PUBLIC egress needs
  # to reach it) but is protected by the LiteLLM master key. Lock this CIDR
  # down for anything beyond a POC.
  ingress {
    description = "LiteLLM proxy"
    from_port   = 4000
    to_port     = 4000
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${var.name_prefix}-litellm-sg" }
}

resource "aws_instance" "litellm" {
  ami                    = data.aws_ssm_parameter.al2023.value
  instance_type          = var.litellm_instance_type
  subnet_id              = aws_subnet.public.id
  vpc_security_group_ids = [aws_security_group.litellm.id]

  user_data = templatefile("${path.module}/user_data.sh.tftpl", {
    litellm_model_alias = var.litellm_model_alias
    openai_model        = var.openai_model
    openai_api_key      = var.openai_api_key
    litellm_master_key  = var.litellm_master_key
  })

  tags = { Name = "${var.name_prefix}-litellm" }
}

# Stable public address for the agent to call.
resource "aws_eip" "litellm" {
  instance = aws_instance.litellm.id
  domain   = "vpc"
  tags     = { Name = "${var.name_prefix}-litellm-eip" }
}
