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

Install the dependencies for any POC from its own `requirements.txt`:

```shell
pip install -r <poc-folder>/requirements.txt
python <poc-folder>/main.py
```

## POCs

- [`iris-classification-knn`](iris-classification-knn/) — the "hello world" of
  scikit-learn: classify Iris flowers with K-Nearest Neighbors. **Start here.**

## References

- https://scikit-learn.org/stable/getting_started.html
- https://scikit-learn.org/stable/tutorial/basic/tutorial.html
