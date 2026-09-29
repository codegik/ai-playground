# Standard POC Makefile
# ---------------------
# Same three targets as every POC in this repo, adapted to a Node agent:
#   make build  -> clean, then install the agent's node_modules
#   make run    -> run the agent locally (needs LiteLLM + MEMORY_ID, see README)
#   make test   -> run the agent's unit tests (node --test)
#
# AWS lifecycle wrappers: make deploy / invoke / events / records / destroy.

AGENT := agentcore
DEPS  := $(AGENT)/node_modules

.PHONY: build run test clean deploy invoke events records destroy

# Install dependencies (only re-runs if node_modules is gone).
$(DEPS):
	cd $(AGENT) && npm install --legacy-peer-deps --no-audit --no-fund

build: clean $(DEPS)

run: $(DEPS)
	cd $(AGENT) && node server.js

test: $(DEPS)
	cd $(AGENT) && npm test

clean:
	rm -rf $(DEPS)

deploy:
	./deploy.sh

invoke:
	./invoke.sh $(PROMPT)

events:
	./events.sh $(SESSION)

records:
	./records.sh

destroy:
	./destroy.sh
