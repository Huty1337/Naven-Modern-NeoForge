#version 330 core

out vec4 color;

in vec2 v_TexCoord;
in vec2 v_OneTexel;

uniform sampler2D u_Texture;
uniform vec2 u_Direction;

const int u_Radius = 3;
const float u_Weights[7] = float[](0.05199096, 0.054712396, 0.056413162, 0.056991756, 0.056413162, 0.054712396, 0.05199096);

void main() {
    vec4 finalColor = vec4(0.0);
    float totalWeight = 0.0;

    for (int i = -u_Radius; i <= u_Radius; ++i) {
        float weight = u_Weights[i + u_Radius];
        finalColor += texture(u_Texture, v_TexCoord + v_OneTexel * float(i) * u_Direction) * weight;
        totalWeight += weight;
    }

    color = finalColor / totalWeight;
}
