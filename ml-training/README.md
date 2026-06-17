# Challenge: Loan Default Predictor

**Goal:** Train a tabular classifier that predicts whether a loan applicant will default, save the model to disk, then load it in a separate script that takes user input from the CLI and returns a prediction.

## Dataset
Use the **UCI German Credit** dataset (1,000 rows, 20 features, binary `good`/`bad` credit label). It's small, public, and ships with `sklearn`-friendly tooling.

```bash
curl -O https://archive.ics.uci.edu/ml/machine-learning-databases/statlog/german/german.data
```

Columns are space-separated; schema in `german.doc` at the same URL. Or use a simpler subset (numeric features only) to keep the CLI input manageable.

## Required deliverables

**1. `train.go`**
- Loads the CSV
- Splits into train/test (e.g., 80/20)
- Trains a classifier (`LogisticRegression` or `RandomForestClassifier` from `sklearn`)
- Prints accuracy + confusion matrix on the test set
- Saves the fitted model to `model.pkl` using `joblib`
- Saves the feature schema (column names + types) alongside it so `predict.go` knows what to ask for

**2. `predict.go`**
- Loads `model.pkl`
- Prompts the user at the CLI for each feature value (one at a time)
- Validates input types
- Runs `model.predict()` and prints `APPROVED` or `DENIED` plus the probability

## Acceptance criteria
- Test accuracy >= 70% (baseline is ~70% — a coin flip weighted to majority class gets you there, so push past it)
- `predict.go` runs standalone — does not import `train.go` or re-touch the dataset
- Re-running `train.go` overwrites `model.pkl`; `predict.go` immediately picks up the new model
- Handles a malformed input (e.g., text where a number is expected) without crashing

## Stretch goals
- Cross-validation instead of a single split
- Print top 3 features by importance after training
- Add a `--batch` flag to `predict.go` that reads a CSV instead of prompting

