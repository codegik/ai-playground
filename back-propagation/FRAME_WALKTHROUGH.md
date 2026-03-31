# Backpropagation Frame-by-Frame Walkthrough

## Frame 1: First Forward Pass

**Input:** [0, 0]
**Target:** [0]
**Phase:** Forward Pass

### What's Happening

The network receives its first training pattern: both inputs are 0, and we expect output to be 0 (XOR rule).

### Step-by-Step Calculation

**1. Input Layer**
- I0 = 0
- I1 = 0

**2. Hidden Layer Computation**

For H0: It's calculating the `sigmoid` function, in this case the `sigmoid` of 0 is 0.50.

For H1: It's doing the same thing, so the result is also 0.50 given the same input.

**3. Output Layer Computation**

The output is defined by calculating the sigmoid of the input, weights and hidden layer.

So we got 0.60.

### Result

- **Prediction:** 0.60
- **Target:** 0.00
- **Loss:** (0.60 - 0.00)² = 0.36

**Problem:** Network predicted 0.60 but should predict 0. The weights need adjustment!

---

## Between Frame 1 and Frame 2: Network State

### What the Network Stores

Before starting the backward pass, the network **saves critical information** from the forward pass:

- Stored Activations
- Stored Weights
- Computed Error
- Computed Loss

### Why This Matters

- The backward pass **requires** these stored values to compute gradients.

- This is the key insight of backpropagation.
- During forward pass, the network remembers everything it computed. During backward pass, it uses those memories to calculate how to adjust each weight.

---

## Frame 2: First Backward Pass

**Input:** [0, 0]
**Target:** [0]
**Phase:** Backward Pass (Gradient Descent)

### What's Happening

The error (0.60 - 0.00 = 0.60) flows backward through the network, adjusting weights to reduce this error.

### Step-by-Step Calculation

**1. Output Error**
```
error = prediction - target
error = 0.60 - 0.00 = 0.60
```

**2. Output Delta (Gradient)**
```
delta_out = error × sigmoid_derivative(0.60)
delta_out = 0.60 × (0.60 × (1 - 0.60))
delta_out = 0.60 × 0.24
delta_out = 0.144
```

**3. Update Hidden→Output Weights**

For H0→OUT:
```
gradient = delta_out × H0
gradient = 0.144 × 0.50 = 0.072
new_weight = 0.47 - (0.5 × 0.072)
new_weight = 0.47 - 0.036 = 0.44
```

For H1→OUT:
```
gradient = delta_out × H1
gradient = 0.144 × 0.50 = 0.072
new_weight = 0.35 - (0.5 × 0.072)
new_weight = 0.35 - 0.036 = 0.32
```

**4. Propagate Error to Hidden Layer**

The error continues flowing backward to update Input→Hidden weights (values too small to see major changes in this frame).

### Result

**Weight Changes:**
- H0→OUT: 0.47 → 0.44 (decreased by 0.03)
- H1→OUT: 0.35 → 0.32 (decreased by 0.03)

**Why decreased?** The output was too high (0.60 instead of 0), so we reduce the weights to make the output smaller next time.

**Visual Cue:** Red/orange colors indicate gradient flow backward.

---

## Frame 3: Second Forward Pass

**Input:** [0, 1]
**Target:** [1]
**Phase:** Forward Pass

### What's Happening

The network moves to the second training pattern: I0=0, I1=1, expecting output 1 (XOR rule). Now using the **updated weights** from Frame 1.

### Step-by-Step Calculation

**1. Input Layer**
- I0 = 0
- I1 = 1

**2. Hidden Layer Computation**

For H0:
```
H0 = sigmoid(I0 × 0.28 + I1 × (-0.45))
H0 = sigmoid(0 × 0.28 + 1 × (-0.45))
H0 = sigmoid(-0.45)
H0 = 0.39
```

For H1:
```
H1 = sigmoid(I0 × (-0.95) + I1 × (-0.55))
H1 = sigmoid(0 × (-0.95) + 1 × (-0.55))
H1 = sigmoid(-0.55)
H1 = 0.37
```

**3. Output Layer Computation (with updated weights)**

```
OUT = sigmoid(H0 × 0.44 + H1 × 0.32)
OUT = sigmoid(0.39 × 0.44 + 0.37 × 0.32)
OUT = sigmoid(0.172 + 0.118)
OUT = sigmoid(0.29)
OUT = 0.57
```

### Result

- **Prediction:** 0.57
- **Target:** 1.00
- **Loss:** (0.57 - 1.00)² = 0.18

**Problem:** Network predicted 0.57 but should predict 1.00. Next backward pass will increase the weights to boost the output.

---

## Key Insights

### Pattern Recognition

1. **Frame 0→1:** Output too high (0.60 vs 0) → weights decreased
2. **Frame 2→3:** Output too low (0.57 vs 1) → weights will increase

### The Learning Process

**Forward Pass (green):**
- Data flows forward: Input → Hidden → Output
- Makes prediction
- Computes loss

**Backward Pass (red):**
- Error flows backward: Output → Hidden → Input
- Computes gradients using chain rule
- Updates weights to reduce loss

### Weight Update Formula

```
new_weight = old_weight - learning_rate × gradient
```

- If output too high → gradient positive → weight decreases
- If output too low → gradient negative → weight increases

### Why It Works

After many iterations of forward and backward passes through all 4 XOR patterns, the weights converge to values that correctly predict:
- [0,0] → 0
- [0,1] → 1
- [1,0] → 1
- [1,1] → 0

This is **backpropagation** - the algorithm that powers modern neural networks!
