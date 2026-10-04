#version 330 core

precision lowp float;

in vec2 uv;
out vec4 color;

uniform sampler2D uTexture;
uniform sampler2D uMaskTexture;
uniform vec3 uColor;
uniform int uRainbow;
uniform float uHueOffset;
uniform float uRainbowSaturation;
uniform float uRainbowBands;
uniform float uRainbowSteps;
uniform float uIntensity;
uniform float uDepth;

vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

void main() {
    float blurA = clamp(texture(uTexture, uv).a, 0.0, 1.0);
    float maskA = clamp(texture(uMaskTexture, uv).a, 0.0, 1.0);

    float a = max(blurA - maskA, 0.0);
    float d = max(uDepth, 0.01);
    a = smoothstep(0.0, 1.0, a);

    float inner = pow(a, d);
    float outer = pow(a, max(d * 0.45, 0.01));
    float outA = (inner * 0.65 + outer * 0.55) * uIntensity;

    vec3 rgb = uColor;
    if (uRainbow != 0) {
        float bands = max(uRainbowBands, 1.0);
        float steps = max(uRainbowSteps, 1.0);
        float p = uv.x + uv.y * 0.35;
        float x = p * bands;
        float i = floor(x);
        float f = fract(x);
        f = floor(f * steps) / steps;
        float hue = fract((i + f) / bands + uHueOffset);
        rgb = hsv2rgb(vec3(hue, clamp(uRainbowSaturation, 0.0, 1.0), 1.0));
    }

    color = vec4(rgb, clamp(outA, 0.0, 1.0));
}
