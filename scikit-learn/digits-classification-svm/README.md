# Digit classification with a Support Vector Machine

Recognize handwritten digits (0-9) from tiny 8x8 images, using a **Support
Vector Machine (SVM)** with an RBF kernel.

## The idea in one sentence

Find the boundary that separates the digit classes with the widest possible
margin, and let an RBF kernel curve that boundary so it can split digits that a
straight line never could.

## What this POC teaches

The same five supervised-learning steps as the Iris POCs, now on image-like
data:

1. **Load** the data — 64 pixel values `X` and the true digit `y`.
2. **Split** into a training set and a held-out test set.
3. **Preprocess** — scale the pixels so the SVM is not skewed by raw brightness
   (`StandardScaler`).
4. **Train** the estimator — `SVC(kernel="rbf").fit(X_train, y_train)`.
5. **Evaluate & predict** — accuracy, a confusion matrix, and a look at the
   first image the model got wrong (drawn as ASCII art).

The Digits dataset has 1797 images, each an 8x8 grid (64 pixels) with values
from 0 (white) to 16 (black).

## Run it

Every POC exposes the same three Makefile targets:

```shell
make build   # clean, create a fresh .venv and install requirements
make run     # run the POC
make test    # run the tests
```

Expected output: ~98% accuracy on the held-out test set, a 10x10 confusion
matrix, and an ASCII drawing of the first misclassified digit.

## Things to try (great for learning)

- Switch `kernel="rbf"` to `kernel="linear"` and compare accuracy.
- Lower `C` to `0.1` (more tolerant of mistakes) or raise it to `100`.
- Remove the `StandardScaler` step and watch the accuracy drop.
- Print `model.decision_function(X_test)` to see the per-class scores.

## Key scikit-learn concepts

| Concept     | What it is                                                          |
| ----------- | ------------------------------------------------------------------- |
| SVM         | Classifier that maximizes the margin between classes                |
| Kernel      | Trick that lets the boundary curve without explicit new features    |
| `C`         | Penalty for misclassified training points (bias/variance dial)      |
| `gamma`     | How far the influence of a single training point reaches            |

## Reference

- https://scikit-learn.org/stable/modules/svm.html#classification
