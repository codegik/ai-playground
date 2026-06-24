# Standard POC Makefile
# ---------------------
# Every scikit-learn POC in this repo exposes the same three targets:
#   make build  -> clean, then create a fresh venv and install requirements
#   make run    -> run the POC (main.py)
#   make test   -> run the tests (pytest)
#
# run/test create the venv automatically if it does not exist yet.

VENV   := .venv
PYTHON := $(VENV)/bin/python
PIP    := $(VENV)/bin/pip

.PHONY: build run test clean

# Create the venv and install dependencies (only re-runs if the venv is gone).
$(PYTHON):
	python3 -m venv $(VENV)
	$(PIP) install --quiet --upgrade pip
	$(PIP) install --quiet -r requirements.txt

build: clean $(PYTHON)

run: $(PYTHON)
	$(PYTHON) main.py

test: $(PYTHON)
	$(PYTHON) -m pytest -v

clean:
	rm -rf $(VENV) __pycache__ .pytest_cache
