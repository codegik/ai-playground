import math
import random

class SimpleNetwork:
    def __init__(self):
        random.seed(42)
        self.w_ih = [[random.uniform(-1, 1) for _ in range(2)] for _ in range(2)]
        self.w_ho = [[random.uniform(-1, 1)] for _ in range(2)]
        self.lr = 0.5

    def sigmoid(self, x):
        return 1 / (1 + math.exp(-max(min(x, 500), -500)))

    def forward(self, x):
        self.x = x
        self.h = [self.sigmoid(sum(x[i] * self.w_ih[i][j] for i in range(2))) for j in range(2)]
        self.y = self.sigmoid(sum(self.h[j] * self.w_ho[j][0] for j in range(2)))
        return self.y

    def backward(self, target):
        out_err = self.y - target
        out_delta = out_err * self.y * (1 - self.y)

        h_err = [out_delta * self.w_ho[j][0] for j in range(2)]
        h_delta = [h_err[j] * self.h[j] * (1 - self.h[j]) for j in range(2)]

        for j in range(2):
            self.w_ho[j][0] -= self.lr * out_delta * self.h[j]

        for i in range(2):
            for j in range(2):
                self.w_ih[i][j] -= self.lr * h_delta[j] * self.x[i]

        return (self.y - target) ** 2

def create_bridge_frame(net, x, target, iteration, loss):
    width, height = 800, 600

    svg = f'''<svg width="{width}" height="{height}" xmlns="http://www.w3.org/2000/svg">
    <defs>
        <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" style="stop-color:#6A1B9A;stop-opacity:1" />
            <stop offset="100%" style="stop-color:#8E24AA;stop-opacity:1" />
        </linearGradient>
        <filter id="glow">
            <feGaussianBlur stdDeviation="3" result="coloredBlur"/>
            <feMerge>
                <feMergeNode in="coloredBlur"/>
                <feMergeNode in="SourceGraphic"/>
            </feMerge>
        </filter>
    </defs>

    <rect width="{width}" height="{height}" fill="url(#bg)"/>

    <text x="400" y="40" font-family="Arial" font-size="28" fill="white" text-anchor="middle" font-weight="bold">
        Network State Storage
    </text>

    <text x="400" y="70" font-family="Arial" font-size="16" fill="white" text-anchor="middle" opacity="0.8">
        Iteration {iteration} - Preparing for Backward Pass
    </text>
'''

    input_x = [150, 150]
    input_y = [220, 380]
    hidden_x = [400, 400]
    hidden_y = [200, 400]
    output_x = 650
    output_y = 300

    line_color = "#FFB300"
    neuron_color = "#FFA726"

    for i in range(2):
        for j in range(2):
            weight = net.w_ih[i][j]
            width_line = abs(weight) * 4 + 1
            svg += f'<line x1="{input_x[i]}" y1="{input_y[i]}" x2="{hidden_x[j]}" y2="{hidden_y[j]}" '
            svg += f'stroke="{line_color}" stroke-width="{width_line}" opacity="0.6" stroke-dasharray="5,5"/>\n'

            mid_x = (input_x[i] + hidden_x[j]) / 2
            mid_y = (input_y[i] + hidden_y[j]) / 2
            svg += f'<text x="{mid_x}" y="{mid_y-5}" font-family="Arial" font-size="11" fill="white" text-anchor="middle">'
            svg += f'{weight:.2f}</text>\n'

    for j in range(2):
        weight = net.w_ho[j][0]
        width_line = abs(weight) * 4 + 1
        svg += f'<line x1="{hidden_x[j]}" y1="{hidden_y[j]}" x2="{output_x}" y2="{output_y}" '
        svg += f'stroke="{line_color}" stroke-width="{width_line}" opacity="0.6" stroke-dasharray="5,5"/>\n'

        mid_x = (hidden_x[j] + output_x) / 2
        mid_y = (hidden_y[j] + output_y) / 2
        svg += f'<text x="{mid_x}" y="{mid_y-5}" font-family="Arial" font-size="11" fill="white" text-anchor="middle">'
        svg += f'{weight:.2f}</text>\n'

    for i in range(2):
        svg += f'<circle cx="{input_x[i]}" cy="{input_y[i]}" r="35" fill="{neuron_color}" '
        svg += f'opacity="0.8" stroke="white" stroke-width="3" stroke-dasharray="5,3" filter="url(#glow)"/>\n'
        svg += f'<text x="{input_x[i]}" y="{input_y[i]-5}" font-family="Arial" font-size="14" fill="white" '
        svg += f'text-anchor="middle" font-weight="bold">I{i}</text>\n'
        svg += f'<text x="{input_x[i]}" y="{input_y[i]+12}" font-family="Arial" font-size="13" fill="white" '
        svg += f'text-anchor="middle">{x[i]:.2f}</text>\n'

    for j in range(2):
        svg += f'<circle cx="{hidden_x[j]}" cy="{hidden_y[j]}" r="35" fill="{neuron_color}" '
        svg += f'opacity="0.8" stroke="white" stroke-width="3" stroke-dasharray="5,3" filter="url(#glow)"/>\n'
        svg += f'<text x="{hidden_x[j]}" y="{hidden_y[j]-5}" font-family="Arial" font-size="14" fill="white" '
        svg += f'text-anchor="middle" font-weight="bold">H{j}</text>\n'
        svg += f'<text x="{hidden_x[j]}" y="{hidden_y[j]+12}" font-family="Arial" font-size="13" fill="white" '
        svg += f'text-anchor="middle">{net.h[j]:.2f}</text>\n'

    svg += f'<circle cx="{output_x}" cy="{output_y}" r="35" fill="{neuron_color}" '
    svg += f'opacity="0.8" stroke="white" stroke-width="3" stroke-dasharray="5,3" filter="url(#glow)"/>\n'
    svg += f'<text x="{output_x}" y="{output_y-5}" font-family="Arial" font-size="14" fill="white" '
    svg += f'text-anchor="middle" font-weight="bold">OUT</text>\n'
    svg += f'<text x="{output_x}" y="{output_y+12}" font-family="Arial" font-size="13" fill="white" '
    svg += f'text-anchor="middle">{net.y:.2f}</text>\n'

    svg += f'<text x="150" y="120" font-family="Arial" font-size="16" fill="white" text-anchor="middle" font-weight="bold">Input</text>\n'
    svg += f'<text x="400" y="120" font-family="Arial" font-size="16" fill="white" text-anchor="middle" font-weight="bold">Hidden</text>\n'
    svg += f'<text x="650" y="120" font-family="Arial" font-size="16" fill="white" text-anchor="middle" font-weight="bold">Output</text>\n'

    error = net.y - target
    svg += f'''<rect x="20" y="480" width="760" height="100" fill="rgba(255,255,255,0.15)" rx="10" stroke="#FFD54F" stroke-width="2"/>
    <text x="400" y="505" font-family="Arial" font-size="16" fill="#FFD54F" font-weight="bold" text-anchor="middle">STORED NETWORK STATE</text>
    <text x="40" y="530" font-family="Arial" font-size="14" fill="white" font-weight="bold">Activations: I=[{x[0]}, {x[1]}], H=[{net.h[0]:.2f}, {net.h[1]:.2f}], OUT={net.y:.2f}</text>
    <text x="40" y="555" font-family="Arial" font-size="14" fill="white" font-weight="bold">Error: {error:.4f} (Prediction {net.y:.2f} - Target {target})</text>
    <text x="40" y="572" font-family="Arial" font-size="13" fill="#FFD54F">Ready for gradient computation in backward pass</text>
'''

    svg += '</svg>'
    return svg

def create_svg_frame(net, x, target, iteration, phase, loss):
    width, height = 800, 600

    svg = f'''<svg width="{width}" height="{height}" xmlns="http://www.w3.org/2000/svg">
    <defs>
        <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" style="stop-color:#1e3c72;stop-opacity:1" />
            <stop offset="100%" style="stop-color:#2a5298;stop-opacity:1" />
        </linearGradient>
        <filter id="glow">
            <feGaussianBlur stdDeviation="3" result="coloredBlur"/>
            <feMerge>
                <feMergeNode in="coloredBlur"/>
                <feMergeNode in="SourceGraphic"/>
            </feMerge>
        </filter>
    </defs>

    <rect width="{width}" height="{height}" fill="url(#bg)"/>

    <text x="400" y="40" font-family="Arial" font-size="28" fill="white" text-anchor="middle" font-weight="bold">
        Backpropagation: XOR Training
    </text>

    <text x="400" y="70" font-family="Arial" font-size="16" fill="white" text-anchor="middle" opacity="0.8">
        Iteration {iteration} - {phase}
    </text>
'''

    input_x = [150, 150]
    input_y = [220, 380]
    hidden_x = [400, 400]
    hidden_y = [200, 400]
    output_x = 650
    output_y = 300

    y = net.forward(x)

    if phase == "Forward Pass":
        line_color = "#4CAF50"
        neuron_color = "#2196F3"
    else:
        line_color = "#FF5722"
        neuron_color = "#FF7043"

    for i in range(2):
        for j in range(2):
            weight = net.w_ih[i][j]
            width_line = abs(weight) * 4 + 1
            svg += f'<line x1="{input_x[i]}" y1="{input_y[i]}" x2="{hidden_x[j]}" y2="{hidden_y[j]}" '
            svg += f'stroke="{line_color}" stroke-width="{width_line}" opacity="0.6"/>\n'

            mid_x = (input_x[i] + hidden_x[j]) / 2
            mid_y = (input_y[i] + hidden_y[j]) / 2
            svg += f'<text x="{mid_x}" y="{mid_y-5}" font-family="Arial" font-size="11" fill="white" text-anchor="middle">'
            svg += f'{weight:.2f}</text>\n'

    for j in range(2):
        weight = net.w_ho[j][0]
        width_line = abs(weight) * 4 + 1
        svg += f'<line x1="{hidden_x[j]}" y1="{hidden_y[j]}" x2="{output_x}" y2="{output_y}" '
        svg += f'stroke="{line_color}" stroke-width="{width_line}" opacity="0.6"/>\n'

        mid_x = (hidden_x[j] + output_x) / 2
        mid_y = (hidden_y[j] + output_y) / 2
        svg += f'<text x="{mid_x}" y="{mid_y-5}" font-family="Arial" font-size="11" fill="white" text-anchor="middle">'
        svg += f'{weight:.2f}</text>\n'

    for i in range(2):
        svg += f'<circle cx="{input_x[i]}" cy="{input_y[i]}" r="35" fill="{neuron_color}" '
        svg += f'opacity="{0.3 + x[i] * 0.7}" stroke="white" stroke-width="2" filter="url(#glow)"/>\n'
        svg += f'<text x="{input_x[i]}" y="{input_y[i]-5}" font-family="Arial" font-size="14" fill="white" '
        svg += f'text-anchor="middle" font-weight="bold">I{i}</text>\n'
        svg += f'<text x="{input_x[i]}" y="{input_y[i]+12}" font-family="Arial" font-size="13" fill="white" '
        svg += f'text-anchor="middle">{x[i]:.2f}</text>\n'

    for j in range(2):
        svg += f'<circle cx="{hidden_x[j]}" cy="{hidden_y[j]}" r="35" fill="{neuron_color}" '
        svg += f'opacity="{0.3 + net.h[j] * 0.7}" stroke="white" stroke-width="2" filter="url(#glow)"/>\n'
        svg += f'<text x="{hidden_x[j]}" y="{hidden_y[j]-5}" font-family="Arial" font-size="14" fill="white" '
        svg += f'text-anchor="middle" font-weight="bold">H{j}</text>\n'
        svg += f'<text x="{hidden_x[j]}" y="{hidden_y[j]+12}" font-family="Arial" font-size="13" fill="white" '
        svg += f'text-anchor="middle">{net.h[j]:.2f}</text>\n'

    svg += f'<circle cx="{output_x}" cy="{output_y}" r="35" fill="{neuron_color}" '
    svg += f'opacity="{0.3 + net.y * 0.7}" stroke="white" stroke-width="2" filter="url(#glow)"/>\n'
    svg += f'<text x="{output_x}" y="{output_y-5}" font-family="Arial" font-size="14" fill="white" '
    svg += f'text-anchor="middle" font-weight="bold">OUT</text>\n'
    svg += f'<text x="{output_x}" y="{output_y+12}" font-family="Arial" font-size="13" fill="white" '
    svg += f'text-anchor="middle">{net.y:.2f}</text>\n'

    svg += f'<text x="150" y="120" font-family="Arial" font-size="16" fill="white" text-anchor="middle" font-weight="bold">Input</text>\n'
    svg += f'<text x="400" y="120" font-family="Arial" font-size="16" fill="white" text-anchor="middle" font-weight="bold">Hidden</text>\n'
    svg += f'<text x="650" y="120" font-family="Arial" font-size="16" fill="white" text-anchor="middle" font-weight="bold">Output</text>\n'

    svg += f'''<rect x="20" y="500" width="760" height="80" fill="rgba(255,255,255,0.1)" rx="10"/>
    <text x="40" y="530" font-family="Arial" font-size="15" fill="white" font-weight="bold">Input: [{x[0]}, {x[1]}]</text>
    <text x="40" y="555" font-family="Arial" font-size="15" fill="white" font-weight="bold">Target: [{target}]</text>
    <text x="400" y="530" font-family="Arial" font-size="15" fill="white" font-weight="bold">Prediction: {net.y:.4f}</text>
    <text x="400" y="555" font-family="Arial" font-size="15" fill="white" font-weight="bold">Loss: {loss:.4f}</text>
'''

    svg += '</svg>'
    return svg

def generate_video():
    print("Generating backpropagation video...\n")

    net = SimpleNetwork()
    X_data = [[0, 0], [0, 1], [1, 0], [1, 1]]
    y_data = [0, 1, 1, 0]

    frames = []
    frame_count = 0

    for iteration in range(5):
        for idx in range(len(X_data)):
            x = X_data[idx]
            target = y_data[idx]

            net.forward(x)
            loss = (net.y - target) ** 2

            svg = create_svg_frame(net, x, target, iteration, "Forward Pass", loss)
            filename = f"./frames/frame_{frame_count:04d}.svg"
            with open(filename, 'w') as f:
                f.write(svg)
            frames.append(filename)
            frame_count += 1
            print(f"Frame {frame_count}: Forward pass - Input {x}, Loss {loss:.4f}")

            svg = create_bridge_frame(net, x, target, iteration, loss)
            filename = f"./frames/frame_{frame_count:04d}.svg"
            with open(filename, 'w') as f:
                f.write(svg)
            frames.append(filename)
            frame_count += 1
            print(f"Frame {frame_count}: Network state stored - Ready for backward pass")

            loss = net.backward(target)

            svg = create_svg_frame(net, x, target, iteration, "Backward Pass", loss)
            filename = f"./frames/frame_{frame_count:04d}.svg"
            with open(filename, 'w') as f:
                f.write(svg)
            frames.append(filename)
            frame_count += 1
            print(f"Frame {frame_count}: Backward pass - Weights updated")

    print(f"\nGenerated {frame_count} frames!")
    print("\nTo create video, run:")
    print("cd xor-neural-network/frames")
    print("for f in *.svg; do convert $f ${f%.svg}.png; done")
    print("ffmpeg -framerate 2 -pattern_type glob -i '*.png' -c:v libx264 -pix_fmt yuv420p ../backpropagation.mp4")

    return frames

import os
os.makedirs("./frames", exist_ok=True)
generate_video()
