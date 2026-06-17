from sklearn.datasets import load_iris
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from sklearn.neighbors import KNeighborsClassifier
from sklearn.metrics import accuracy_score, confusion_matrix, classification_report


def main():
    # 1. LOAD THE DATA ---------------------------------------------------------
    # scikit-learn ships with a few small "toy" datasets so you can experiment
    # without downloading anything. The Iris dataset has 150 flowers.
    iris = load_iris()

    # X = the "features": 4 measurements per flower (in centimeters)
    #     [sepal length, sepal width, petal length, petal width]
    # y = the "label" / "target": which species (0, 1 or 2)
    X = iris.data
    y = iris.target

    print(f"Dataset: {X.shape[0]} flowers, {X.shape[1]} measurements each")
    print(f"Feature names: {iris.feature_names}")
    print(f"Species (classes): {list(iris.target_names)}\n")

    # 2. SPLIT THE DATA --------------------------------------------------------
    # We hold back 20% of the flowers as a "test set" the model never sees while
    # learning. That is the only honest way to know if it actually generalizes.
    #
    # - stratify=y keeps the same proportion of each species in both sets.
    # - random_state makes the split reproducible (same result every run).
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, stratify=y, random_state=42
    )
    print(f"Training on {len(X_train)} flowers, testing on {len(X_test)}.\n")

    # 3. PREPROCESS ------------------------------------------------------------
    # KNN measures distances between flowers, so features on bigger numeric
    # scales would unfairly dominate. StandardScaler rescales every feature to
    # mean 0 and standard deviation 1.
    #
    # IMPORTANT: we .fit the scaler on the TRAINING data only, then apply the
    # same transform to the test data. Letting the scaler peek at the test set
    # would be "data leakage" and would make our score look better than reality.
    scaler = StandardScaler()
    X_train = scaler.fit_transform(X_train)
    X_test = scaler.transform(X_test)

    # 4. TRAIN THE MODEL -------------------------------------------------------
    # KNN classifies a flower by looking at its k closest neighbors in the
    # training data and taking a majority vote. Here k = 3.
    #
    # In scikit-learn every model is an "estimator" and you train it the same
    # way: estimator.fit(features, labels).
    model = KNeighborsClassifier(n_neighbors=3)
    model.fit(X_train, y_train)

    # 5. EVALUATE & PREDICT ----------------------------------------------------
    # .predict gives the model's guess for each flower in the test set.
    y_pred = model.predict(X_test)

    accuracy = accuracy_score(y_test, y_pred)
    print(f"Accuracy on the unseen test set: {accuracy:.1%}\n")

    # A confusion matrix shows, per species, how many were classified correctly
    # (the diagonal) vs. confused with another species (off-diagonal).
    print("Confusion matrix (rows = true species, columns = predicted):")
    print(confusion_matrix(y_test, y_pred))
    print()

    # The classification report adds precision / recall / f1 per species.
    print("Detailed report:")
    print(classification_report(y_test, y_pred, target_names=iris.target_names))

    # Finally, let's classify one brand-new flower we make up ourselves.
    # Measurements: sepal length, sepal width, petal length, petal width.
    new_flower = [[5.1, 3.5, 1.4, 0.2]]
    new_flower_scaled = scaler.transform(new_flower)  # same scaling as training
    prediction = model.predict(new_flower_scaled)[0]
    print(f"New flower {new_flower[0]} -> {iris.target_names[prediction]}")


if __name__ == "__main__":
    main()
