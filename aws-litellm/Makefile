# Convenience wrapper around deploy.sh, the client, and terraform destroy.
# Prereqs: terraform, docker (with buildx), aws CLI (configured), node.

INFRA_DIR := infra

.PHONY: deploy invoke destroy outputs client-install litellm-logs

# Provision everything and push the agent image (see deploy.sh for ordering).
deploy:
	./deploy.sh

# Invoke the deployed agent. Usage: make invoke PROMPT="your question"
invoke: client-install
	AGENT_RUNTIME_ARN="$$(terraform -chdir=$(INFRA_DIR) output -raw agent_runtime_arn)" \
	AWS_REGION="$$(terraform -chdir=$(INFRA_DIR) output -raw region)" \
	node client/invoke.js "$(PROMPT)"

client-install:
	cd client && npm install --silent

outputs:
	terraform -chdir=$(INFRA_DIR) output

# SSH-free peek at LiteLLM startup: prints how to read cloud-init logs.
litellm-logs:
	@echo "SSM/SSH into the instance, then: sudo docker logs litellm"
	@echo "Instance boot log: sudo cat /var/log/cloud-init-output.log"

destroy:
	terraform -chdir=$(INFRA_DIR) destroy
