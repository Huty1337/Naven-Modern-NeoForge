#version 330

// Screen space motion blur: blends the current frame with the previous frame that is kept in the
// "history" render target by assets/minecraft/post_effect/motion_blur.json.
//
// Both samplers and the two uniform blocks are declared by PostChain#createPass:
//   * "<input sampler_name>Sampler" for every input of the pass (Curr, Prev),
//   * "SamplerInfo" (OutSize followed by one vec2 per input, in declaration order),
//   * one block per entry of the pass' "uniforms" object (MotionBlurConfig).

uniform sampler2D CurrSampler;
uniform sampler2D PrevSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 CurrSize;
    vec2 PrevSize;
};

layout(std140) uniform MotionBlurConfig {
    float BlendFactor;
};

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 curr = texture(CurrSampler, texCoord);
    vec4 prev = texture(PrevSampler, texCoord);

    // A (re)created history target is cleared to transparent black, so on the very first frame after
    // a (re)load there is no previous frame to blend with; fade in from the current frame instead of
    // fading in from black.
    vec3 previous = mix(curr.rgb, prev.rgb, step(0.5, prev.a));

    // BlendFactor is the weight of the current frame: 1.0 = no blur, 0.3 = default strength 7.
    // MotionBlur#uploadStrength overwrites the value baked into the post effect json whenever the
    // user moves the Strength slider.
    fragColor = vec4(mix(previous, curr.rgb, BlendFactor), 1.0);
}
