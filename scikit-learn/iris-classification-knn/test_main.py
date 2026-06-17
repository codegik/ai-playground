"""Smoke tests for the Iris KNN POC.

These run the whole pipeline end to end and check it produces sane output,
so `make test` actually exercises the code a beginner just wrote.
"""

import main


def test_main_runs_end_to_end(capsys):
    # Should run without raising and print its results.
    main.main()
    out = capsys.readouterr().out

    assert "Accuracy" in out
    # The made-up flower at the end is clearly a setosa.
    assert "setosa" in out


def test_dataset_shape():
    from sklearn.datasets import load_iris

    iris = load_iris()
    assert iris.data.shape == (150, 4)  # 150 flowers, 4 measurements
    assert set(iris.target) == {0, 1, 2}  # 3 species
