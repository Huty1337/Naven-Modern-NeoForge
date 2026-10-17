package tech.naven.modules.impl.render;

import tech.naven.Naven;
import tech.naven.events.api.EventTarget;
import tech.naven.events.api.types.EventType;
import tech.naven.events.impl.EventRunTicks;
import tech.naven.modules.Category;
import tech.naven.modules.Module;
import tech.naven.modules.ModuleInfo;
import tech.naven.modules.impl.combat.Aura;
import tech.naven.utils.InventoryUtils;
import tech.naven.values.ValueBuilder;
import tech.naven.values.impl.BooleanValue;
import tech.naven.values.impl.FloatValue;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Quaternionf;

@ModuleInfo(
   name = "Animations",
   description = "Old versions hitting animations with multiple styles",
   category = Category.RENDER
)
public class Animations extends Module {
   public final FloatValue positionX = ValueBuilder.create(this, "Position X")
           .setDefaultFloatValue(0.0F)
           .setMinFloatValue(-1.0F)
           .setMaxFloatValue(1.0F)
           .setFloatStep(0.01F)
           .build()
           .getFloatValue();
   public final FloatValue positionY = ValueBuilder.create(this, "Position Y")
           .setDefaultFloatValue(0.0F)
           .setMinFloatValue(-1.0F)
           .setMaxFloatValue(1.0F)
           .setFloatStep(0.01F)
           .build()
           .getFloatValue();
   public final FloatValue positionZ = ValueBuilder.create(this, "Position Z")
           .setDefaultFloatValue(0.0F)
           .setMinFloatValue(-1.0F)
           .setMaxFloatValue(1.0F)
           .setFloatStep(0.01F)
           .build()
           .getFloatValue();
   public final BooleanValue customEquippedProgress = ValueBuilder.create(this, "Custom Equipped Progress")
           .setDefaultBooleanValue(false)
           .build()
           .getBooleanValue();
   public final FloatValue equippedProgress = ValueBuilder.create(this, "Equipped Progress")
           .setDefaultFloatValue(1.0F)
           .setFloatStep(0.01F)
           .setMinFloatValue(0.0F)
           .setMaxFloatValue(1.0F)
           .setVisibility(() -> this.customEquippedProgress.getCurrentValue())
           .build()
           .getFloatValue();
   public final BooleanValue onlykillaura = ValueBuilder.create(this, "Only KillAura").setDefaultBooleanValue(false).build().getBooleanValue();

   private boolean isBlocking;
   private float blockProgress;
   private float oBlockProgress;
   private boolean busRegistered;

   @Override
   public void onEnable() {
      this.isBlocking = false;
      this.blockProgress = 0.0F;
      this.oBlockProgress = 0.0F;
      if (!this.busRegistered) {
         NeoForge.EVENT_BUS.register(this);
         this.busRegistered = true;
      }
   }

   @Override
   public void onDisable() {
      if (this.busRegistered) {
         NeoForge.EVENT_BUS.unregister(this);
         this.busRegistered = false;
      }
   }

   @EventTarget
   public void onTick(EventRunTicks event) {
      if (event.getType() != EventType.PRE || mc.player == null) {
         return;
      }

      this.updateBlockingState();
      this.updateBlockProgress();
   }

   @SubscribeEvent
   public void onRenderHand(RenderHandEvent event) {
      if (!this.isEnabled() || mc.player == null || mc.level == null) {
         return;
      }

      if (event.getHand() != InteractionHand.MAIN_HAND || !this.shouldShowBlockAnimation()) {
         return;
      }

      event.setCanceled(true);
      this.renderBlockingArm(
              event.getPartialTick(),
              event.getEquipProgress(),
              event.getSwingProgress(),
              event.getItemStack(),
              event.getPoseStack(),
              event.getSubmitNodeCollector(),
              event.getPackedLight()
      );
   }

   private void renderBlockingArm(
           float partialTicks,
           float equipProgress,
           float swingProgress,
           ItemStack itemStack,
           PoseStack poseStack,
           SubmitNodeCollector collector,
           int light
   ) {
      AbstractClientPlayer player = mc.player;
      if (player == null || itemStack.isEmpty()) {
         return;
      }

      HumanoidArm arm = player.getMainArm();
      int side = arm == HumanoidArm.RIGHT ? 1 : -1;
      boolean leftHand = arm != HumanoidArm.RIGHT;

      poseStack.pushPose();
      this.applyCustomBlockAnimation(poseStack, partialTicks, equipProgress, swingProgress, side);
      this.drawItem(player, itemStack, leftHand, poseStack, collector, light);
      poseStack.popPose();
   }

   private void drawItem(
           AbstractClientPlayer player,
           ItemStack stack,
           boolean leftHand,
           PoseStack poseStack,
           SubmitNodeCollector collector,
           int light
   ) {
      if (stack.isEmpty()) {
         return;
      }

      ItemStackRenderState renderState = new ItemStackRenderState();
      ItemDisplayContext context = leftHand ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
      mc.getItemModelResolver().updateForLiving(renderState, stack, context, player);
      renderState.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, 0);
   }

   private void updateBlockingState() {
      if (mc.player == null) {
         return;
      }

      boolean shouldBlock = mc.options.keyUse.isDown();
      if (this.getAuraTarget() != null) {
         shouldBlock = true;
      }

      if (this.onlykillaura.getCurrentValue()) {
         shouldBlock = this.getAuraTarget() != null;
      }

      this.isBlocking = shouldBlock;
   }

   private void updateBlockProgress() {
      this.oBlockProgress = this.blockProgress;
      float target = this.shouldShowBlockAnimation() ? 1.0F : 0.0F;
      this.blockProgress += Mth.clamp(target - this.blockProgress, -0.4F, 0.4F);
   }

   private boolean shouldShowBlockAnimation() {
      if (!this.isEnabled()) {
         return false;
      }

      LocalPlayer player = mc.player;
      if (player == null) {
         return false;
      }

      if (!InventoryUtils.isSword(player.getMainHandItem())) {
         return false;
      }

      boolean isOffhandUsing = false;
      if (player.isUsingItem() && player.getUsedItemHand() == InteractionHand.OFF_HAND) {
         if (!player.getOffhandItem().is(Items.SHIELD)) {
            isOffhandUsing = true;
         }
      }

      boolean isKillauraBlocking = this.getAuraTarget() != null;
      if (this.onlykillaura.getCurrentValue()) {
         return isKillauraBlocking;
      }

      if (isKillauraBlocking) {
         return true;
      }

      if (isOffhandUsing) {
         return false;
      }

      return mc.options.keyUse.isDown() || this.isBlocking;
   }

   private LivingEntity getAuraTarget() {
      Aura aura = (Aura) Naven.getInstance().getModuleManager().getModule(Aura.class);
      if (aura == null || !aura.isEnabled()) {
         return null;
      }

      return Aura.target instanceof LivingEntity living ? living : null;
   }

   private void applyCustomBlockAnimation(PoseStack poseStack, float partialTicks, float equippedProg, float swingProgress, int side) {
      float blockProg = Mth.lerp(partialTicks, this.oBlockProgress, this.blockProgress);
      if (this.customEquippedProgress.getCurrentValue()) {
         equippedProg *= this.equippedProgress.getCurrentValue();
         blockProg *= this.equippedProgress.getCurrentValue();
      }

      poseStack.translate(this.positionX.getCurrentValue(), this.positionY.getCurrentValue(), this.positionZ.getCurrentValue());
      poseStack.translate((float) side * 0.56F, -0.52F + equippedProg * -0.6F, -0.72F);
      float f = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
      float f1 = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
      poseStack.mulPose(Axis.YP.rotation((float) side * (45.0F + f * -20.0F) * (float) Math.PI / 180.0F));
      poseStack.mulPose(Axis.ZP.rotation((float) side * f1 * -10.0F * (float) Math.PI / 180.0F));
      poseStack.mulPose(Axis.XP.rotation(f1 * -80.0F * (float) Math.PI / 180.0F));
      poseStack.mulPose(Axis.YP.rotation((float) side * -45.0F * (float) Math.PI / 180.0F));
      poseStack.scale(0.9F, 0.9F, 0.9F);
      poseStack.translate(-0.2F, 0.126F, 0.2F);
      poseStack.mulPose(Axis.XP.rotation(-102.25F * (float) Math.PI / 180.0F));
      poseStack.mulPose(Axis.YP.rotation((float) side * 15.0F * (float) Math.PI / 180.0F));
      poseStack.mulPose(Axis.ZP.rotation((float) side * 80.0F * (float) Math.PI / 180.0F));
   }
}