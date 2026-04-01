# Back Propagation: Under the Hood

## What is Back Propagation?

Back propagation is the algorithm that trains neural networks by computing gradients of the loss function with respect to each weight using the chain rule of calculus. It flows backwards through the network, layer by layer, calculating how much each weight contributed to the error.

## How It Works (Deep Dive)

### Forward Pass
1. Input passes through layers: `x → h = σ(W₁x + b₁) → y = σ(W₂h + b₂)`
2. Compute loss: `L = (y - target)²`

### Backward Pass
The chain rule decomposes gradients:
```
∂L/∂W₂ = ∂L/∂y × ∂y/∂W₂
∂L/∂W₁ = ∂L/∂y × ∂y/∂h × ∂h/∂W₁
```

Key insight: Each layer stores its activation derivative during forward pass, then multiplies it with the gradient flowing backward.

**Mathematical Foundation:**
- For sigmoid: `σ'(x) = σ(x)(1 - σ(x))`
- Gradient at layer l: `δₗ = δₗ₊₁ × Wₗ₊₁ᵀ × σ'(zₗ)`
- Weight update: `Wₗ = Wₗ - η × δₗ × aₗ₋₁ᵀ`

## Pros

**Efficient Gradient Computation**
- Computes all gradients in O(n) time where n = number of weights
- Without backprop, finite differences would require O(n²) forward passes

**Compositional**
- Works with any differentiable activation function
- Enables deep architectures through gradient flow


## Cons

**Vanishing Gradients**
- Sigmoid/tanh derivatives approach zero for large inputs
- Deep networks suffer: `∂L/∂W₁ = ∂L/∂y × σ'(z₂) × σ'(z₁)`
- If σ'(z) < 0.25, gradients shrink exponentially with depth

**Exploding Gradients**
- Large weight values cause gradients to grow exponentially
- Network becomes unstable, parameters overflow


## Use Case: Binary Classification

Training a neural network to learn XOR function (non-linearly separable problem):

**Input:** Two binary values → **Output:** XOR result

This requires at least one hidden layer because XOR is not linearly separable. The network learns to create a decision boundary by:
1. Hidden layer projects input to higher dimension
2. Creates two hyperplanes that separate the classes
3. Output layer combines these projections

**Training Process:**
- Forward pass computes predictions
- Loss measures error: Mean Squared Error
- Backward pass computes gradients via chain rule
- Weights updated using gradient descent: `W -= learning_rate × ∂L/∂W`

After ~10,000 iterations, the network achieves >95% accuracy, demonstrating how backprop enables learning complex non-linear functions.

## Key Takeaway

Back propagation transforms an intractable optimization problem (training neural networks) into a practical algorithm by applying the chain rule systematically. It's the computational breakthrough that enabled modern deep learning.