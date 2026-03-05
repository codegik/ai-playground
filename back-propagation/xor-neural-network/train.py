import numpy as np
from network import NeuralNetwork

X = np.array([[0, 0],
              [0, 1],
              [1, 0],
              [1, 1]])

y = np.array([[0],
              [1],
              [1],
              [0]])

np.random.seed(42)
nn = NeuralNetwork(input_size=2, hidden_size=4, output_size=1)

print("Training XOR Neural Network\n")
print("Initial predictions:")
for i in range(len(X)):
    pred = nn.forward(X[i:i+1])
    print(f"Input: {X[i]} -> Output: {pred[0][0]:.4f}")

print("\n" + "="*50 + "\n")

losses = nn.train(X, y, epochs=10000, learning_rate=0.5)

print("\n" + "="*50 + "\n")
print("Final predictions:")
for i in range(len(X)):
    pred = nn.forward(X[i:i+1])
    binary_pred = nn.predict(X[i:i+1])
    print(f"Input: {X[i]} -> Output: {pred[0][0]:.4f} -> Predicted: {binary_pred[0][0]}")

print(f"\nFinal Loss: {losses[-1]:.6f}")

accuracy = np.mean(nn.predict(X) == y) * 100
print(f"Accuracy: {accuracy:.2f}%")
