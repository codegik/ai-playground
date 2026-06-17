"""Smoke tests for the Iris decision-tree POC."""

import main


def test_main_runs_end_to_end(capsys):
    main.main()
    out = capsys.readouterr().out

    assert "Accuracy" in out
    assert "Feature importances" in out
    # The made-up flower at the end is clearly a setosa.
    assert "setosa" in out


def test_dataset_shape():
    from sklearn.datasets import load_iris

    iris = load_iris()
    assert iris.data.shape == (150, 4)
    assert set(iris.target) == {0, 1, 2}
