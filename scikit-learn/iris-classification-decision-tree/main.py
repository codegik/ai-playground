from sklearn.datasets import load_iris
from sklearn.model_selection import train_test_split
from sklearn.tree import DecisionTreeClassifier, export_text
from sklearn.metrics import accuracy_score, confusion_matrix, classification_report


def main():
    # Load the toy Iris dataset: 150 flowers, 4 measurements, 3 species.
    iris = load_iris()
    X = iris.data
    y = iris.target

    print(f"Dataset: {X.shape[0]} flowers, {X.shape[1]} measurements each")
    print(f"Species (classes): {list(iris.target_names)}\n")

    # Same 80/20 split as the KNN POC, so the two are comparable.
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, stratify=y, random_state=42
    )
    print(f"Training on {len(X_train)} flowers, testing on {len(X_test)}.\n")

    # No scaling needed: a decision tree splits one feature at a time on a
    # threshold, so the numeric scale of each feature does not matter.
    #
    # max_depth limits how many questions the tree may ask. A shallow tree is
    # easier to read and less likely to overfit; try changing it to see the
    # accuracy/readability trade-off.
    model = DecisionTreeClassifier(max_depth=3, random_state=42)
    model.fit(X_train, y_train)

    y_pred = model.predict(X_test)

    accuracy = accuracy_score(y_test, y_pred)
    print(f"Accuracy on the unseen test set: {accuracy:.1%}\n")

    print("Confusion matrix (rows = true species, columns = predicted):")
    print(confusion_matrix(y_test, y_pred))
    print()

    print("Detailed report:")
    print(classification_report(y_test, y_pred, target_names=iris.target_names))

    # The big advantage of a tree: you can read exactly how it decides.
    print("Learned decision rules:")
    print(export_text(model, feature_names=list(iris.feature_names)))

    # Feature importances show which measurements the tree actually relied on.
    print("Feature importances:")
    for name, importance in zip(iris.feature_names, model.feature_importances_):
        print(f"  {name}: {importance:.2f}")
    print()

    # Classify one brand-new flower (no scaling step, unlike the KNN POC).
    new_flower = [[5.1, 3.5, 1.4, 0.2]]
    prediction = model.predict(new_flower)[0]
    print(f"New flower {new_flower[0]} -> {iris.target_names[prediction]}")


if __name__ == "__main__":
    main()
