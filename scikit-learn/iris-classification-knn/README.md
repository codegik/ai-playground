# Iris classification with K-Nearest Neighbors

The classic first scikit-learn project: predict the species of an Iris flower
from four measurements, using the **K-Nearest Neighbors (KNN)** algorithm.

## The idea in one sentence

To classify a new flower, look at the `k` most similar flowers we already know
the answer for, and take a majority vote.

## What this POC teaches

The five steps that show up in almost every supervised-learning project:

1. **Load** the data — features `X` and labels `y`.
2. **Split** into a training set (to learn from) and a test set (to grade on).
3. **Preprocess** — scale the features so distances are fair (`StandardScaler`).
4. **Train** the estimator — `model.fit(X_train, y_train)`.
5. **Evaluate & predict** — `model.predict(...)` plus accuracy and a confusion
   matrix.

The Iris dataset has 150 flowers, 3 species, and 4 measurements each
(sepal length/width, petal length/width).

## Run it

Every POC exposes the same three Makefile targets:

```shell
make build   # clean, create a fresh .venv and install requirements
make run     # run the POC
make test    # run the tests
```

Expected output: ~93% accuracy on the held-out test set, plus a confusion
matrix and a prediction for a made-up flower (which comes out as *setosa*).

## Things to try (great for learning)

- Change `n_neighbors=3` to `1` or `15` and watch the accuracy move.
- Remove the `StandardScaler` step — does it still work? (For Iris it barely
  changes; the habit matters more on real data.)
- Print `model.predict_proba(X_test)` to see the vote percentages per species.

## Key scikit-learn concepts

| Concept            | What it is                                                        |
| ------------------ | ----------------------------------------------------------------- |
| Estimator          | Any model object; trained with `.fit()`                           |
| `train_test_split` | Splits data so you can measure real generalization                |
| Transformer        | Preprocessing object with `.fit_transform()` / `.transform()`     |
| Data leakage       | Letting the model peek at test data — inflates scores dishonestly |

## Reference

- https://scikit-learn.org/stable/modules/neighbors.html#classification
