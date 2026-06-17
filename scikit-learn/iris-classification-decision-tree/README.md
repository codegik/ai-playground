# Iris classification with a Decision Tree

The same Iris species-prediction task as
[`iris-classification-knn`](../iris-classification-knn/), but using a **Decision
Tree** instead of K-Nearest Neighbors. Doing the same task with a second
algorithm is a great way to learn what makes each one different.

## The idea in one sentence

A decision tree learns a flowchart of yes/no questions about the measurements
(e.g. "is petal length < 2.45 cm?") and follows it down to a species.

## How it differs from the KNN POC

| | KNN | Decision Tree |
| --- | --- | --- |
| Needs feature scaling? | Yes (distance-based) | **No** (splits one feature at a time) |
| Interpretable? | Not really | **Yes** — you can print the exact rules |
| Tells you which features matter? | No | **Yes** — via `feature_importances_` |

So this POC has **no `StandardScaler` step**, and it prints the learned rules
(`export_text`) plus feature importances.

## Run it

```shell
make build   # clean, create a fresh .venv and install requirements
make run     # run the POC
make test    # run the tests
```

Expected output: ~93–97% accuracy, the tree's decision rules, the feature
importances (petal measurements dominate), and a prediction for a made-up
flower (*setosa*).

## Things to try

- Change `max_depth=3` to `1`, `2`, or `None` and watch the rules and accuracy
  change. `None` lets the tree grow until it perfectly fits the training data —
  a classic way to see overfitting.
- Compare the feature importances here with how KNN treats all four features
  equally.

## Key scikit-learn concepts

| Concept                 | What it is                                            |
| ----------------------- | ----------------------------------------------------- |
| `DecisionTreeClassifier`| Tree-based estimator; trained with `.fit()`           |
| `max_depth`             | Limits tree size — trades accuracy for simplicity     |
| `export_text`           | Prints the learned tree as readable rules             |
| `feature_importances_`  | How much each feature contributed to the splits       |

## Reference

- https://scikit-learn.org/stable/modules/tree.html#classification
