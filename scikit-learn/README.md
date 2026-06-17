# scikit-learn playground

My playground for studying [scikit-learn](https://scikit-learn.org/), the most
popular machine-learning library for classical (non–deep-learning) ML in Python.

This folder collects **many small POCs about the same topic**. To keep them easy
to browse as the collection grows, every POC lives in its own folder named with
this convention:

```
{dataset}-{task}-{algorithm}
```

- **dataset**   — the data we learn from (`iris`, `digits`, `wine`, ...)
- **task**      — `classification`, `regression`, `clustering`, ...
- **algorithm** — the estimator used (`knn`, `decision-tree`, `svm`, ...)

Examples of how future POCs slot in:

| Folder                              | What it demonstrates                         |
| ----------------------------------- | -------------------------------------------- |
| `iris-classification-knn`           | K-Nearest Neighbors on the Iris flowers      |
| `iris-classification-decision-tree` | A decision tree on the same dataset          |
| `digits-classification-svm`         | Support Vector Machine on handwritten digits |
| `wine-regression-linear`            | Linear regression on the wine dataset        |

## Environment

Built with Python 3.12.

Every POC ships a standard `Makefile` with the same three targets, so you run
them all the same way:

```shell
cd <poc-folder>
make build   # clean, create a fresh .venv and install requirements.txt
make run     # run the POC (main.py)
make test    # run the tests (pytest)
```

## POCs

- [`iris-classification-knn`](iris-classification-knn/) — the "hello world" of
  scikit-learn: classify Iris flowers with K-Nearest Neighbors. **Start here.**
- [`iris-classification-decision-tree`](iris-classification-decision-tree/) —
  the same task with a Decision Tree: no feature scaling, plus readable rules
  and feature importances.

## References

- https://scikit-learn.org/stable/getting_started.html
- https://scikit-learn.org/stable/tutorial/basic/tutorial.html
