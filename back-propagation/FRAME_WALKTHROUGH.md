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

## Frame 2: Network State

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

## Frame 3: First Backward Pass

**Input:** [0, 0]
**Target:** [0]
**Phase:** Backward Pass (Gradient Descent)

### What's Happening

The error flows backward through the network, adjusting weights to reduce this error.

### Step-by-Step Calculation

1. Output Error 
2. Output Gradient
3. Update Hidden→Output Weights
4. Propagate Error to Hidden Layer

The error continues flowing backward to update Input→Hidden weights.

### Result

**Weight Changes:**
- H0→OUT: 0.47 → 0.44 (decreased by 0.03)
- H1→OUT: 0.35 → 0.32 (decreased by 0.03)

**Why decreased?** The output was too high (0.60 instead of 0), so we reduce the weights to make the output smaller next time.

---

## Frame 4: Second Forward Pass

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

For H0: It's calculating the `sigmoid` function, in this case the `sigmoid` of 0 is 0.50.

For H1: It's doing the same thing, so the result is also 0.50 given the same input.

**3. Output Layer Computation (with updated weights)**

We got 0.57.

### Result

- **Prediction:** 0.57
- **Target:** 1.00
- **Loss:** (0.57 - 1.00)² = 0.18

**Problem:** Network predicted 0.57 but should predict 1.00. Next backward pass will increase the weights to boost the output.

---
