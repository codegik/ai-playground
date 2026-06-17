# Repository conventions

This repo is a collection of small POCs, grouped into topic folders (e.g.
`scikit-learn/`, `back-propagation/`, `chromadb/`). Each topic folder holds many
POCs about that topic.

## POC folder naming

Name each POC folder descriptively so they stay easy to browse as the collection
grows. For scikit-learn POCs the convention is:

```
{dataset}-{task}-{algorithm}
```

e.g. `iris-classification-knn`, `digits-classification-svm`.

## Every POC ships a standard Makefile

Each POC folder must include a `Makefile` exposing the same three targets:

- `make build` — clean, then create a fresh venv and install `requirements.txt`
- `make run` — run the POC (`main.py`)
- `make test` — run the tests (`pytest`)

Use the pattern in `scikit-learn/iris-classification-knn/Makefile`: a per-POC
`.venv`, where `run`/`test` auto-create the venv and `build` depends on `clean`.
Also add a `.gitignore` for `.venv/`, `__pycache__/`, `.pytest_cache/`, and at
least one test so `make test` has something to run.

## No redundant file headers

Do not put a large module-level docstring or comment header at the top of a
POC's Python file (e.g. `main.py`) that explains what the POC is, how to run it,
or restates its overview — that already lives in the POC's README. Start the
file with the imports. Concise *inline* comments next to the code explaining
individual steps/concepts are wanted; just skip the big header block.

## Git

Never add a `Co-Authored-By: Claude` trailer to commit messages.
