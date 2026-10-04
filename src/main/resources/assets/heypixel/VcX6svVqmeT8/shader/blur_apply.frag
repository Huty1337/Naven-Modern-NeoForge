#version 330 core

precision lowp float;

in vec2 uv;
out vec4 color;

uniform sampler2D uTexture;
uniform sampler2D uMaskTexture;
uniform float uAlpha;

void main() {
    vec4 blurred = texture(uTexture, uv);
    float maskA = clamp(texture(uMaskTexture, uv).a, 0.0, 1.0);
    // Mask is authored as arbitrary UI colors (historically used with stencil), so treat any non-zero alpha as "on".
    float mask = smoothstep(0.0, 0.01, maskA);
    float a = clamp(mask * uAlpha, 0.0, 1.0);
    color = vec4(blurred.rgb, a);
}
