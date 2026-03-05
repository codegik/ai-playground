import numpy as np

def sigmoid(x):
    return 1 / (1 + np.exp(-x))

def sigmoid_derivative(x):
    return x * (1 - x)

class NeuralNetwork:
    def __init__(self, input_size, hidden_size, output_size):
        self.weights_input_hidden = np.random.randn(input_size, hidden_size)
        self.weights_hidden_output = np.random.randn(hidden_size, output_size)
        self.bias_hidden = np.zeros((1, hidden_size))
        self.bias_output = np.zeros((1, output_size))

    def forward(self, X):
        self.input = X
        self.hidden_input = np.dot(X, self.weights_input_hidden) + self.bias_hidden
        self.hidden_output = sigmoid(self.hidden_input)
        self.output_input = np.dot(self.hidden_output, self.weights_hidden_output) + self.bias_output
        self.output = sigmoid(self.output_input)
        return self.output

    def backward(self, X, y, learning_rate):
        m = X.shape[0]

        output_error = self.output - y
        output_delta = output_error * sigmoid_derivative(self.output)

        hidden_error = np.dot(output_delta, self.weights_hidden_output.T)
        hidden_delta = hidden_error * sigmoid_derivative(self.hidden_output)

        self.weights_hidden_output -= learning_rate * np.dot(self.hidden_output.T, output_delta) / m
        self.bias_output -= learning_rate * np.sum(output_delta, axis=0, keepdims=True) / m
        self.weights_input_hidden -= learning_rate * np.dot(X.T, hidden_delta) / m
        self.bias_hidden -= learning_rate * np.sum(hidden_delta, axis=0, keepdims=True) / m

    def train(self, X, y, epochs, learning_rate):
        losses = []
        for epoch in range(epochs):
            output = self.forward(X)
            loss = np.mean((output - y) ** 2)
            losses.append(loss)
            self.backward(X, y, learning_rate)

            if epoch % 1000 == 0:
                print(f'Epoch {epoch}, Loss: {loss:.6f}')

        return losses

    def predict(self, X):
        output = self.forward(X)
        return (output > 0.5).astype(int)
