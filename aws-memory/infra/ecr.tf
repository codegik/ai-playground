resource "aws_ecr_repository" "agent" {
  name         = "${var.name_prefix}-agentcore"
  force_delete = true

  image_scanning_configuration {
    scan_on_push = true
  }
}
