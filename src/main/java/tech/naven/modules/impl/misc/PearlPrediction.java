package tech.naven.modules.impl.misc;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import tech.naven.events.api.EventTarget;
import tech.naven.events.api.types.EventType;
import tech.naven.events.impl.EventPacket;
import tech.naven.events.impl.EventRender2D;
import tech.naven.events.impl.EventRunTicks;
import tech.naven.events.impl.EventShader;
import tech.naven.modules.Category;
import tech.naven.modules.Module;
import tech.naven.modules.ModuleInfo;
import tech.naven.modules.impl.render.NameProtect;
import tech.naven.utils.PearlPhysicsUtil;
import tech.naven.utils.ProjectionUtils;
import tech.naven.utils.RenderUtils;
import tech.naven.utils.Vector2f;
import tech.naven.utils.renderer.Fonts;
import tech.naven.values.ValueBuilder;
import tech.naven.values.impl.FloatValue;

@ModuleInfo(
   name = "PearlPrediction",
   description = "Renders prediction and can automatically counter thrown ender pearls.",
   category = Category.MISC
)
public class PearlPrediction extends Module {
   public static Vector2f rotations;
   public static boolean isThrowing;

   private final FloatValue hudSize = ValueBuilder.create(this, "HUD Size")
      .setDefaultFloatValue(0.35F)
      .setFloatStep(0.01F)
      .setMinFloatValue(0.1F)
      .setMaxFloatValue(1.0F)
      .build()
      .getFloatValue();
   private final Map<Integer, PredictedPearlInfo> predictedPearls = new HashMap<>();
   private final Set<UUID> selfPearls = new HashSet<>();
   private static final long SELF_PEARL_WINDOW_MS = 800L;
   private long pendingSelfPearlTime = 0L;
   private Vec3 pendingSelfPearlPos = null;
   private long lastHeldPearlTime = 0L;
   private Vec3 lastHeldPearlPos = null;

   @Override
   public void onEnable() {
      this.reset();
   }

   @Override
   public void onDisable() {
      this.predictedPearls.values().forEach(PredictedPearlInfo::resetAnimation);
      this.predictedPearls.clear();
      this.reset();
   }

   @EventTarget
   public void onShader(EventShader event) {
      if (this.predictedPearls.isEmpty()) {
         return;
      }

      EventType type = event.getType();
      if (type != EventType.BLUR && type != EventType.SHADOW && type != EventType.GLOW) {
         return;
      }

      int bgColor = new Color(0, 0, 0, 55).getRGB();
      for (PredictedPearlInfo info : this.predictedPearls.values()) {
         if (info.finalWidth > 0.1F && info.finalHeight > 0.1F) {
            RenderUtils.fill(
               event.getStack(), info.finalX, info.finalY, info.finalX + info.finalWidth, info.finalY + info.finalHeight, bgColor
            );
         }
      }
   }

   @EventTarget
   public void onTick(EventRunTicks event) {
      if (event.getType() != EventType.PRE || mc.player == null || mc.level == null) {
         return;
      }

      this.updateLastHeldPearl();
      this.updatePearlLogicStates();
   }

   @EventTarget
   public void onRender2D(EventRender2D event) {
      if (mc.player == null || this.predictedPearls.isEmpty()) {
         return;
      }

      GuiGraphics guiGraphics = event.getGuiGraphics();
      float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
      for (PredictedPearlInfo info : this.predictedPearls.values()) {
         info.calculateAnimation(partialTick);
         if (info.finalWidth > 0.1F && info.finalHeight > 0.1F) {
            this.renderLandingMark(guiGraphics, info);
         }
      }
   }

   @EventTarget
   public void onPacket(EventPacket event) {
      if (event.getType() != EventType.SEND || mc.player == null) {
         return;
      }

      if (event.getPacket() instanceof ServerboundUseItemPacket packet) {
         InteractionHand hand = packet.getHand();
         if (hand != null) {
            this.recordSelfPearlThrow(hand);
         }
      }
   }

   private void updateLastHeldPearl() {
      if (mc.player.getMainHandItem().is(Items.ENDER_PEARL) || mc.player.getOffhandItem().is(Items.ENDER_PEARL)) {
         this.lastHeldPearlTime = System.currentTimeMillis();
         this.lastHeldPearlPos = mc.player.position();
      }
   }

   private void recordSelfPearlThrow(InteractionHand hand) {
      if (!mc.player.getItemInHand(hand).is(Items.ENDER_PEARL)) {
         return;
      }

      this.pendingSelfPearlTime = System.currentTimeMillis();
      this.pendingSelfPearlPos = mc.player.position();
   }

   private boolean isSelfPearl(ThrownEnderpearl pearl) {
      if (Objects.equals(pearl.getOwner(), mc.player)) {
         return true;
      }

      if (this.selfPearls.contains(pearl.getUUID())) {
         return true;
      }

      long now = System.currentTimeMillis();
      if (this.pendingSelfPearlTime == 0L || this.pendingSelfPearlPos == null) {
         if (this.lastHeldPearlTime == 0L || this.lastHeldPearlPos == null) {
            return false;
         }

         if (now - this.lastHeldPearlTime > SELF_PEARL_WINDOW_MS) {
            this.lastHeldPearlTime = 0L;
            this.lastHeldPearlPos = null;
            return false;
         }

         if (pearl.position().distanceTo(this.lastHeldPearlPos) > 4.0) {
            return false;
         }

         this.selfPearls.add(pearl.getUUID());
         return true;
      }

      if (now - this.pendingSelfPearlTime > SELF_PEARL_WINDOW_MS) {
         this.pendingSelfPearlTime = 0L;
         this.pendingSelfPearlPos = null;
         return false;
      }

      if (pearl.position().distanceTo(this.pendingSelfPearlPos) > 4.0) {
         return false;
      }

      this.selfPearls.add(pearl.getUUID());
      return true;
   }

   private void updatePearlLogicStates() {
      Set<Integer> visible = new HashSet<>();

      for (Entity entity : mc.level.entitiesForRendering()) {
         if (!(entity instanceof ThrownEnderpearl pearl)) {
            continue;
         }

         int pearlId = pearl.getId();
         visible.add(pearlId);
         PredictedPearlInfo info = this.predictedPearls.get(pearlId);
         if (info == null) {
            this.isSelfPearl(pearl);
            info = new PredictedPearlInfo(pearl);
            this.predictedPearls.put(pearlId, info);
         } else {
            info.refreshCountdown(pearl);
         }
      }

      List<Integer> toRemove = new ArrayList<>();
      for (Integer pearlId : this.predictedPearls.keySet()) {
         if (!visible.contains(pearlId)) {
            toRemove.add(pearlId);
         }
      }

      for (Integer pearlId : toRemove) {
         PredictedPearlInfo info = this.predictedPearls.remove(pearlId);
         if (info != null) {
            this.selfPearls.remove(info.pearlUuid);
         }
      }

      if (this.selfPearls.size() > 100) {
         this.selfPearls.clear();
      }
   }

   private void reset() {
      rotations = null;
      isThrowing = false;
      this.pendingSelfPearlTime = 0L;
      this.pendingSelfPearlPos = null;
      this.lastHeldPearlTime = 0L;
      this.lastHeldPearlPos = null;
      if (this.selfPearls.size() > 50) {
         this.selfPearls.clear();
      }
   }

   private void renderLandingMark(GuiGraphics guiGraphics, PredictedPearlInfo info) {
      if (info.projectedPos == null) {
         return;
      }

      int bgColor = new Color(0, 0, 0, 55).getRGB();
      RenderUtils.fill(
         guiGraphics, info.finalX, info.finalY, info.finalX + info.finalWidth, info.finalY + info.finalHeight, bgColor
      );

      float textY = info.finalY + 2.0F;
      float rowHeight = (float)Fonts.harmony.getHeight(true, this.hudSize.getCurrentValue());
      Fonts.harmony.setAlpha(1.0F);

      for (String line : info.lines) {
         float textXOffset = (info.finalWidth - Fonts.harmony.getWidth(line, this.hudSize.getCurrentValue())) / 2.0F;
         Fonts.harmony.render(
            guiGraphics, line, info.finalX + textXOffset, textY, Color.WHITE, true, this.hudSize.getCurrentValue()
         );
         textY += rowHeight * 0.875F;
      }
   }

   private class PredictedPearlInfo {
      private final UUID pearlUuid;
      private final Vec3 landingPos;
      private final int launchTicks;
      private final int startTickCount;
      private final String ownerName;
      private int ticksToLand;
      private Vector2f projectedPos;
      private final List<String> lines = new ArrayList<>();
      private float totalTextHeight;
      private float finalWidth;
      private float finalHeight;
      private float finalX;
      private float finalY;

      private PredictedPearlInfo(ThrownEnderpearl pearl) {
         this.pearlUuid = pearl.getUUID();
         this.startTickCount = pearl.tickCount;

         Object[] data = PearlPhysicsUtil.predictPearlLandingWithTicks(pearl, mc.level);
         this.landingPos = (Vec3)data[0];
         this.launchTicks = (Integer)data[1];
         this.ticksToLand = this.launchTicks;

         Entity owner = pearl.getOwner();
         String name;
         if (owner != null) {
            name = owner.getName().getString();
         } else {
            boolean treatAsSelf = mc.player != null && selfPearls.contains(this.pearlUuid);
            name = treatAsSelf ? mc.player.getName().getString() : "Unknown";
         }

         this.ownerName = NameProtect.getName(name);
      }

      private void refreshCountdown(ThrownEnderpearl pearl) {
         int elapsed = Math.max(0, pearl.tickCount - this.startTickCount);
         this.ticksToLand = Math.max(0, this.launchTicks - elapsed);
      }

      private void calculateAnimation(float partialTick) {
         if (this.landingPos == null) {
            this.finalWidth = 0.0F;
            this.finalHeight = 0.0F;
            return;
         }

         this.projectedPos = ProjectionUtils.project(this.landingPos.x, this.landingPos.y, this.landingPos.z, partialTick);
         if (this.projectedPos == null
            || this.projectedPos.x == Float.MAX_VALUE
            || this.projectedPos.y == Float.MAX_VALUE) {
            this.finalWidth = 0.0F;
            this.finalHeight = 0.0F;
            return;
         }

         this.lines.clear();
         this.lines.add("Thrown by: " + this.ownerName);
         this.lines.add(String.format("Lands in: %.1f s", (double)this.ticksToLand / 20.0));
         this.lines.add(
            String.format(
               "Distance: %.1f m", mc.player == null ? 0.0 : mc.player.position().distanceTo(this.landingPos)
            )
         );

         float textHeight = (float)Fonts.harmony.getHeight(true, hudSize.getCurrentValue());
         this.totalTextHeight = textHeight * 0.875F * (float)(this.lines.size() - 1) + textHeight;

         float width = 0.0F;
         for (String line : this.lines) {
            width = Math.max(width, Fonts.harmony.getWidth(line, hudSize.getCurrentValue()));
         }

         this.finalWidth = width + 8.0F;
         this.finalHeight = this.totalTextHeight + 4.0F;
         this.finalX = this.projectedPos.x - this.finalWidth / 2.0F;
         this.finalY = this.projectedPos.y - this.finalHeight / 2.0F;
      }

      private void resetAnimation() {
         this.finalWidth = 0.0F;
         this.finalHeight = 0.0F;
      }
   }
}
