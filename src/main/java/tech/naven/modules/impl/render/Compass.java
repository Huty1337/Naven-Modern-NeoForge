package tech.naven.modules.impl.render;

import tech.naven.events.api.EventTarget;
import tech.naven.events.api.types.EventType;
import tech.naven.events.impl.EventMotion;
import tech.naven.events.impl.EventRender;
import tech.naven.events.impl.EventRender2D;
import tech.naven.modules.Category;
import tech.naven.modules.Module;
import tech.naven.modules.ModuleInfo;
import tech.naven.utils.BlinkingPlayer;
import tech.naven.utils.InventoryUtils;
import tech.naven.utils.RoundedRectRenderState;
import tech.naven.values.ValueBuilder;
import tech.naven.values.impl.BooleanValue;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;

@ModuleInfo(
   name = "Compass",
   description = "Shows a compass",
   category = Category.RENDER
)
public class Compass extends Module {
   public BooleanValue compassOnly = ValueBuilder.create(this, "Compass Only").setDefaultBooleanValue(true).build().getBooleanValue();
   public BooleanValue noPlayerOnly = ValueBuilder.create(this, "No Player Only").setDefaultBooleanValue(true).build().getBooleanValue();
   private boolean hasCompass = false;
   private BlockPos spawnPosition;
   private float renderYaw;
   private double renderX;
   private double renderZ;

   private BlockPos getSpawnPosition(ClientLevel p_117922_) {
      return p_117922_.dimensionType().natural() ? p_117922_.getRespawnData().globalPos().pos() : null;
   }

   private boolean hasPlayer() {
      for (Entity entity : mc.level.entitiesForRendering()) {
         if (entity != mc.player && !(entity instanceof BlinkingPlayer) && entity instanceof Player) {
            return true;
         }
      }

      return false;
   }

   @EventTarget
   public void onMotion(EventMotion e) {
      if (e.getType() == EventType.PRE) {
         this.hasCompass = InventoryUtils.hasItem(Items.COMPASS);
         this.spawnPosition = this.getSpawnPosition(mc.level);
      }
   }

   @EventTarget
   public void onRender(EventRender e) {
      this.renderX = Mth.lerp((double)e.getRenderPartialTicks(), mc.player.xOld, mc.player.getX());
      this.renderZ = Mth.lerp((double)e.getRenderPartialTicks(), mc.player.zOld, mc.player.getZ());
      this.renderYaw = Mth.lerp(e.getRenderPartialTicks(), mc.player.yRotO, mc.player.getYRot());
   }

   @EventTarget(4)
   public void onRender2D(EventRender2D e) {
      this.draw(e.getGuiGraphics());
   }

   private static final float ANGLE_OFFSET = 0.0F;

   private void draw(GuiGraphics guiGraphics) {
      if (this.hasCompass || !this.compassOnly.getCurrentValue()) {
         if (!this.hasPlayer() || !this.noPlayerOnly.getCurrentValue()) {
            if (this.spawnPosition != null) {
               double dx = (double)this.spawnPosition.getX() - this.renderX;
               double dz = (double)this.spawnPosition.getZ() - this.renderZ;
               if (Math.abs(dx) + Math.abs(dz) < 0.01) {
                  return;
               }

               float bearing = (float)Math.toDegrees(Math.atan2(-dx, dz));
               float angle = Mth.wrapDegrees(bearing - this.renderYaw + ANGLE_OFFSET);
               float centerX = (float)mc.getWindow().getGuiScaledWidth() / 2.0F;
               float centerY = (float)mc.getWindow().getGuiScaledHeight() / 2.0F;
               Matrix3x2fStack pose = guiGraphics.pose();
               pose.pushMatrix();
               pose.translate(centerX, centerY);
               pose.rotate((float)Math.toRadians((double)angle));
               pose.translate(-centerX, -centerY);
               this.drawArrow(guiGraphics, centerX, centerY - 45.0F, 10.0F, 5.0F, -1);
               pose.popMatrix();
            }
         }
      }
   }

   private void drawArrow(GuiGraphics guiGraphics, float tipX, float tipY, float size, float halfWidth, int color) {
      float[] perimeter = new float[]{tipX, tipY, tipX - halfWidth, tipY + size, tipX + halfWidth, tipY + size};
      guiGraphics.submitGuiElementRenderState(
         new RoundedRectRenderState(perimeter, new Matrix3x2f(guiGraphics.pose()), color, guiGraphics.peekScissorStack())
      );
   }
}
