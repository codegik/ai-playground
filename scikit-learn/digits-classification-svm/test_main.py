import main


def test_main_runs_end_to_end(capsys):
    main.main()
    out = capsys.readouterr().out

    assert "Accuracy" in out
    assert "Confusion matrix" in out


def test_dataset_shape():
    from sklearn.datasets import load_digits

    digits = load_digits()
    assert digits.data.shape == (1797, 64)
    assert set(digits.target) == set(range(10))
