import random
from network import NeuralNetwork

X = [[0, 0],
     [0, 1],
     [1, 0],
     [1, 1]]

y = [[0],
     [1],
     [1],
     [0]]

random.seed(42)
nn = NeuralNetwork(input_size=2, hidden_size=4, output_size=1)

print("Training XOR Neural Network\n")
print("Initial predictions:")
for i in range(len(X)):
    pred = nn.forward(X[i])
    print(f"Input: {X[i]} -> Output: {pred[0]:.4f}")

print("\n" + "="*50 + "\n")

losses = nn.train(X, y, epochs=10000, learning_rate=0.5)

print("\n" + "="*50 + "\n")
print("Final predictions:")
for i in range(len(X)):
    pred = nn.forward(X[i])
    binary_pred = nn.predict(X[i])
    print(f"Input: {X[i]} -> Output: {pred[0]:.4f} -> Predicted: {binary_pred[0]}")

print(f"\nFinal Loss: {losses[-1]:.6f}")

correct = sum(1 for i in range(len(X)) if nn.predict(X[i])[0] == y[i][0])
accuracy = (correct / len(X)) * 100
print(f"Accuracy: {accuracy:.2f}%")
