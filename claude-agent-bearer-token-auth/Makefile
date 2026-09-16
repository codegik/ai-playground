# Standard POC Makefile (Node/TypeScript variant)
# ------------------------------------------------
#   make build -> clean, then install dependencies (node_modules)
#   make run   -> run the POC (src/main.ts)
#   make test  -> run the tests (node --test)
#
# run/test install dependencies automatically if node_modules is missing.
# Node >= 22 runs the .ts files directly via built-in type stripping.

NODE_MODULES := node_modules

.PHONY: build run test clean

$(NODE_MODULES):
	npm install --silent

build: clean $(NODE_MODULES)

run: $(NODE_MODULES)
	node src/main.ts

test: $(NODE_MODULES)
	node --test test/*.test.ts

clean:
	rm -rf $(NODE_MODULES)
