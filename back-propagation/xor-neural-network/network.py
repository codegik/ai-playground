import math
import random

def sigmoid(x):
    return 1 / (1 + math.exp(-x))

def sigmoid_derivative(x):
    return x * (1 - x)

class NeuralNetwork:
    def __init__(self, input_size, hidden_size, output_size):
        self.input_size = input_size
        self.hidden_size = hidden_size
        self.output_size = output_size

        self.weights_input_hidden = [[random.uniform(-1, 1) for _ in range(hidden_size)] for _ in range(input_size)]
        self.weights_hidden_output = [[random.uniform(-1, 1) for _ in range(output_size)] for _ in range(hidden_size)]
        self.bias_hidden = [0.0] * hidden_size
        self.bias_output = [0.0] * output_size

    def forward(self, X):
        self.input = X

        self.hidden_input = [0.0] * self.hidden_size
        for j in range(self.hidden_size):
            for i in range(self.input_size):
                self.hidden_input[j] += X[i] * self.weights_input_hidden[i][j]
            self.hidden_input[j] += self.bias_hidden[j]

        self.hidden_output = [sigmoid(x) for x in self.hidden_input]

        self.output_input = [0.0] * self.output_size
        for k in range(self.output_size):
            for j in range(self.hidden_size):
                self.output_input[k] += self.hidden_output[j] * self.weights_hidden_output[j][k]
            self.output_input[k] += self.bias_output[k]

        self.output = [sigmoid(x) for x in self.output_input]
        return self.output

    def backward(self, X, y, learning_rate):
        output_error = [self.output[k] - y[k] for k in range(self.output_size)]
        output_delta = [output_error[k] * sigmoid_derivative(self.output[k]) for k in range(self.output_size)]

        hidden_error = [0.0] * self.hidden_size
        for j in range(self.hidden_size):
            for k in range(self.output_size):
                hidden_error[j] += output_delta[k] * self.weights_hidden_output[j][k]

        hidden_delta = [hidden_error[j] * sigmoid_derivative(self.hidden_output[j]) for j in range(self.hidden_size)]

        for j in range(self.hidden_size):
            for k in range(self.output_size):
                self.weights_hidden_output[j][k] -= learning_rate * output_delta[k] * self.hidden_output[j]

        for k in range(self.output_size):
            self.bias_output[k] -= learning_rate * output_delta[k]

        for i in range(self.input_size):
            for j in range(self.hidden_size):
                self.weights_input_hidden[i][j] -= learning_rate * hidden_delta[j] * X[i]

        for j in range(self.hidden_size):
            self.bias_hidden[j] -= learning_rate * hidden_delta[j]

    def train(self, X_data, y_data, epochs, learning_rate):
        losses = []
        for epoch in range(epochs):
            epoch_loss = 0.0
            for X, y in zip(X_data, y_data):
                output = self.forward(X)
                loss = sum((output[k] - y[k]) ** 2 for k in range(self.output_size))
                epoch_loss += loss
                self.backward(X, y, learning_rate)

            avg_loss = epoch_loss / len(X_data)
            losses.append(avg_loss)

            if epoch % 1000 == 0:
                print(f'Epoch {epoch}, Loss: {avg_loss:.6f}')

        return losses

    def predict(self, X):
        output = self.forward(X)
        return [1 if o > 0.5 else 0 for o in output]
