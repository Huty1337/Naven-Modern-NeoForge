#version 330 core

precision lowp float;

in vec2 uv;
out vec4 color;

uniform sampler2D uTexture;
uniform vec2 uDirection;
uniform float uRadius;
uniform float uSigma;

const int MAX_RADIUS = 64;

void main() {
    vec2 texSize = vec2(textureSize(uTexture, 0));
    vec2 texel = 1.0 / texSize;

    float radiusF = clamp(uRadius, 1.0, float(MAX_RADIUS));
    int radius = int(radiusF);
    float sigma = uSigma > 0.0 ? uSigma : max(radiusF * 0.5, 1.0);
    float twoSigma2 = 2.0 * sigma * sigma;

    vec4 sum = vec4(0.0);
    float weightSum = 0.0;

    for (int i = -radius; i <= radius; ++i) {
        float w = exp(-float(i * i) / twoSigma2);
        sum += texture(uTexture, uv + texel * uDirection * float(i)) * w;
        weightSum += w;
    }

    color = sum / max(weightSum, 0.0001);
}
