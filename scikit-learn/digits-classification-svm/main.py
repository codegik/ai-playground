from sklearn.datasets import load_digits
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from sklearn.svm import SVC
from sklearn.metrics import accuracy_score, confusion_matrix, classification_report


def main():
    # 1. LOAD THE DATA ---------------------------------------------------------
    # The Digits dataset is 1797 tiny 8x8 grayscale images of handwritten
    # numbers (0-9). Each image is flattened into 64 numbers (one per pixel),
    # where every pixel is a brightness from 0 (white) to 16 (black).
    digits = load_digits()

    # X = the 64 pixel values per image. y = the digit it actually shows (0-9).
    X = digits.data
    y = digits.target

    print(f"Dataset: {X.shape[0]} images, {X.shape[1]} pixels each (8x8)")
    print(f"Digits (classes): {list(digits.target_names)}\n")

    # 2. SPLIT THE DATA --------------------------------------------------------
    # Hold back 20% as a test set the model never sees while learning, so we can
    # honestly measure how well it generalizes to new handwriting.
    #
    # - stratify=y keeps each digit equally represented in both sets.
    # - random_state makes the split reproducible.
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, stratify=y, random_state=42
    )
    print(f"Training on {len(X_train)} images, testing on {len(X_test)}.\n")

    # 3. PREPROCESS ------------------------------------------------------------
    # An SVM with an RBF kernel is sensitive to feature scale, so we rescale
    # every pixel to mean 0 and standard deviation 1.
    #
    # IMPORTANT: .fit the scaler on the TRAINING data only, then apply the same
    # transform to the test data. Fitting on the test set would be data leakage
    # and make the score look better than it really is.
    scaler = StandardScaler()
    X_train = scaler.fit_transform(X_train)
    X_test = scaler.transform(X_test)

    # 4. TRAIN THE MODEL -------------------------------------------------------
    # A Support Vector Machine finds the boundary that best separates the
    # classes with the widest possible margin. The RBF kernel lets that
    # boundary curve, so it can separate digits that are not linearly separable.
    #
    # - C controls how much we punish misclassifications (higher = stricter).
    # - gamma="scale" sets the reach of each training point automatically.
    model = SVC(kernel="rbf", C=10, gamma="scale")
    model.fit(X_train, y_train)

    # 5. EVALUATE & PREDICT ----------------------------------------------------
    y_pred = model.predict(X_test)

    accuracy = accuracy_score(y_test, y_pred)
    print(f"Accuracy on the unseen test set: {accuracy:.1%}\n")

    # The confusion matrix shows, per digit, how many were classified correctly
    # (the diagonal) vs. confused with another digit (off-diagonal).
    print("Confusion matrix (rows = true digit, columns = predicted):")
    print(confusion_matrix(y_test, y_pred))
    print()

    # precision / recall / f1 per digit.
    print("Detailed report:")
    print(classification_report(y_test, y_pred))

    # Find the first image the model got wrong and show it as ASCII art, so we
    # can see the kind of messy handwriting that fools the model.
    for i in range(len(y_test)):
        if y_pred[i] != y_test[i]:
            print(f"First mistake: image #{i} is a {y_test[i]}, "
                  f"model guessed {y_pred[i]}.")
            print(render_digit(X_test[i], scaler))
            break
    else:
        print("No mistakes on the test set.")


def render_digit(scaled_pixels, scaler):
    # Undo the scaling to get the original 0-16 pixel values back, then draw the
    # 8x8 grid with shading characters so it looks like the handwritten digit.
    original = scaler.inverse_transform([scaled_pixels])[0]
    shades = " .:-=+*#%@"
    lines = []
    for row in range(8):
        line = ""
        for col in range(8):
            value = original[row * 8 + col]
            index = min(int(value / 16 * (len(shades) - 1)), len(shades) - 1)
            line += shades[index] * 2
        lines.append(line)
    return "\n".join(lines)


if __name__ == "__main__":
    main()
