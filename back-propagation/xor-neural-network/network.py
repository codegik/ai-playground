import math
import random

# is a mathematical function that squashes any input value into a range between 0 and 1.
# It's used as an activation function in neural networks.
# Input (x)    →    sigmoid(x)
# -10          →    0.00005  (almost 0)
# -2           →    0.12
# -1           →    0.27
#  0           →    0.50
#  1           →    0.73
#  2           →    0.88
#  10          →    0.99995  (almost 1)
def sigmoid(x):
    return 1 / (1 + math.exp(-x))

def sigmoid_derivative(x):
    return x * (1 - x)

class NeuralNetwork:
    def __init__(self, input_size, hidden_size, output_size):
        self.output = None
        self.hidden_output = None
        self.output_input = None
        self.input = None
        self.hidden_input = None
        self.input_size = input_size
        self.hidden_size = hidden_size
        self.output_size = output_size

        self.weights_input_hidden = [[random.uniform(-1, 1) for _ in range(hidden_size)] for _ in range(input_size)]
        self.weights_hidden_output = [[random.uniform(-1, 1) for _ in range(output_size)] for _ in range(hidden_size)]
        self.bias_hidden = [0.0] * hidden_size
        self.bias_output = [0.0] * output_size

    def forward(self, x):
        self.input = x

        self.hidden_input = [0.0] * self.hidden_size
        for j in range(self.hidden_size):
            for i in range(self.input_size):
                self.hidden_input[j] += x[i] * self.weights_input_hidden[i][j]
            self.hidden_input[j] += self.bias_hidden[j]

        self.hidden_output = [sigmoid(x) for x in self.hidden_input]

        self.output_input = [0.0] * self.output_size
        for k in range(self.output_size):
            for j in range(self.hidden_size):
                self.output_input[k] += self.hidden_output[j] * self.weights_hidden_output[j][k]
            self.output_input[k] += self.bias_output[k]

        self.output = [sigmoid(x) for x in self.output_input]
        return self.output

    def backward(self, x, y, learning_rate):
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
                self.weights_input_hidden[i][j] -= learning_rate * hidden_delta[j] * x[i]

        for j in range(self.hidden_size):
            self.bias_hidden[j] -= learning_rate * hidden_delta[j]

    def train(self, x_data, y_data, epochs, learning_rate):
        losses = []
        for epoch in range(epochs):
            epoch_loss = 0.0
            for X, y in zip(x_data, y_data):
                output = self.forward(X)
                loss = sum((output[k] - y[k]) ** 2 for k in range(self.output_size))
                epoch_loss += loss
                self.backward(X, y, learning_rate)

            avg_loss = epoch_loss / len(x_data)
            losses.append(avg_loss)

            if epoch % 1000 == 0:
                print(f'Epoch {epoch}, Loss: {avg_loss:.6f}')

        return losses

    def predict(self, x):
        output = self.forward(x)
        return [1 if o > 0.5 else 0 for o in output]
