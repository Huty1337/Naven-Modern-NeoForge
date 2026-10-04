package com.heypixel.heypixelmod.obsoverlay.events.impl;

import com.heypixel.heypixelmod.obsoverlay.events.api.events.Event;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.mojang.blaze3d.vertex.PoseStack;
import java.math.BigInteger;

public class EventShader implements Event {
   public static Object trash = new BigInteger("fffffffffffffffffffffffffffffffaaffffffffffffffafffaffff09ffcfff", 16);
   private final PoseStack stack;
   private final EventType type;

   public PoseStack getStack() {
      return this.stack;
   }

   public EventType getType() {
      return this.type;
   }

   public EventShader(PoseStack stack, EventType type) {
      this.stack = stack;
      this.type = type;
   }
}
