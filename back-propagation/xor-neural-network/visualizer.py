import random
from network import NeuralNetwork

class VisualizedNetwork(NeuralNetwork):
    def visualize_forward(self, X):
        print("\n" + "="*60)
        print("FORWARD PASS")
        print("="*60)

        print(f"\n1. Input Layer: {X}")

        self.input = X
        self.hidden_input = [0.0] * self.hidden_size

        print(f"\n2. Computing Hidden Layer (Input → Hidden):")
        for j in range(self.hidden_size):
            total = 0.0
            for i in range(self.input_size):
                weight = self.weights_input_hidden[i][j]
                contribution = X[i] * weight
                total += contribution
                print(f"   Neuron {j}: input[{i}]({X[i]:.2f}) × weight({weight:.3f}) = {contribution:.4f}")
            total += self.bias_hidden[j]
            self.hidden_input[j] = total
            print(f"   Neuron {j}: sum = {total:.4f}")

        print(f"\n3. Apply Sigmoid Activation to Hidden Layer:")
        self.hidden_output = []
        for j, val in enumerate(self.hidden_input):
            activated = 1 / (1 + 2.71828 ** (-val))
            self.hidden_output.append(activated)
            print(f"   Hidden[{j}]: sigmoid({val:.4f}) = {activated:.4f}")

        print(f"\n4. Computing Output Layer (Hidden → Output):")
        self.output_input = [0.0] * self.output_size
        for k in range(self.output_size):
            total = 0.0
            for j in range(self.hidden_size):
                weight = self.weights_hidden_output[j][k]
                contribution = self.hidden_output[j] * weight
                total += contribution
                print(f"   hidden[{j}]({self.hidden_output[j]:.4f}) × weight({weight:.3f}) = {contribution:.4f}")
            total += self.bias_output[k]
            self.output_input[k] = total
            print(f"   Output sum = {total:.4f}")

        print(f"\n5. Apply Sigmoid Activation to Output:")
        self.output = []
        for k, val in enumerate(self.output_input):
            activated = 1 / (1 + 2.71828 ** (-val))
            self.output.append(activated)
            print(f"   Output[{k}]: sigmoid({val:.4f}) = {activated:.4f}")

        return self.output

    def visualize_backward(self, X, y, learning_rate):
        print("\n" + "="*60)
        print("BACKWARD PASS (BACKPROPAGATION)")
        print("="*60)

        print(f"\n1. Compute Output Error:")
        output_error = []
        for k in range(self.output_size):
            error = self.output[k] - y[k]
            output_error.append(error)
            print(f"   Error[{k}]: prediction({self.output[k]:.4f}) - target({y[k]}) = {error:.4f}")

        print(f"\n2. Compute Output Delta (Error × Sigmoid Derivative):")
        output_delta = []
        for k in range(self.output_size):
            sig_deriv = self.output[k] * (1 - self.output[k])
            delta = output_error[k] * sig_deriv
            output_delta.append(delta)
            print(f"   Delta[{k}]: {output_error[k]:.4f} × {sig_deriv:.4f} = {delta:.4f}")

        print(f"\n3. Propagate Error Back to Hidden Layer:")
        hidden_error = [0.0] * self.hidden_size
        for j in range(self.hidden_size):
            for k in range(self.output_size):
                contribution = output_delta[k] * self.weights_hidden_output[j][k]
                hidden_error[j] += contribution
                print(f"   Hidden[{j}] error += delta[{k}]({output_delta[k]:.4f}) × weight({self.weights_hidden_output[j][k]:.3f}) = {contribution:.4f}")
            print(f"   Hidden[{j}] total error = {hidden_error[j]:.4f}")

        print(f"\n4. Compute Hidden Delta:")
        hidden_delta = []
        for j in range(self.hidden_size):
            sig_deriv = self.hidden_output[j] * (1 - self.hidden_output[j])
            delta = hidden_error[j] * sig_deriv
            hidden_delta.append(delta)
            print(f"   Delta[{j}]: {hidden_error[j]:.4f} × {sig_deriv:.4f} = {delta:.4f}")

        print(f"\n5. Update Hidden→Output Weights:")
        for j in range(self.hidden_size):
            for k in range(self.output_size):
                old_weight = self.weights_hidden_output[j][k]
                gradient = output_delta[k] * self.hidden_output[j]
                new_weight = old_weight - learning_rate * gradient
                self.weights_hidden_output[j][k] = new_weight
                print(f"   W[{j}→{k}]: {old_weight:.4f} - {learning_rate}×{gradient:.4f} = {new_weight:.4f}")

        print(f"\n6. Update Input→Hidden Weights:")
        for i in range(self.input_size):
            for j in range(self.hidden_size):
                old_weight = self.weights_input_hidden[i][j]
                gradient = hidden_delta[j] * X[i]
                new_weight = old_weight - learning_rate * gradient
                self.weights_input_hidden[i][j] = new_weight
                print(f"   W[{i}→{j}]: {old_weight:.4f} - {learning_rate}×{gradient:.4f} = {new_weight:.4f}")

random.seed(42)
nn = VisualizedNetwork(input_size=2, hidden_size=2, output_size=1)

X = [1, 0]
y = [1]

print("XOR BACKPROPAGATION VISUALIZATION")
print("Training pattern: [1, 0] → [1]")

nn.visualize_forward(X)
nn.visualize_backward(X, y, learning_rate=0.5)

print("\n" + "="*60)
print("KEY INSIGHT:")
print("="*60)
print("Backpropagation flows gradients BACKWARD from output to input.")
print("Each weight's update is: weight -= learning_rate × gradient")
print("Gradient = (error from next layer) × (activation of previous layer)")
print("="*60)
