package client.nilore.event.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Generated;
import client.nilore.event.EventMarker;

public record RenderEvent(PoseStack poseStack, float partialTick)
        implements EventMarker {
    @Override
    @Generated
    public PoseStack poseStack() {
        return this.poseStack;
    }

    @Override
    @Generated
    public float partialTick() {
        return this.partialTick;
    }

    @Generated
    public RenderEvent {
    }
}