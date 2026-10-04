package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.google.common.collect.Lists;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventShader;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.SmoothAnimationTimer;
import com.heypixel.heypixelmod.obsoverlay.utils.StencilUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.text.CustomTextRenderer;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.gui.Gui;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringUtil;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import org.joml.Vector4f;

@ModuleInfo(
   name = "EffectDisplay",
   description = "Displays potion effects on the HUD",
   category = Category.RENDER
)
public class EffectDisplay extends Module {
   private List<Runnable> list;
   private final Map<Holder<MobEffect>, EffectDisplay.MobEffectInfo> infos = new ConcurrentHashMap<>();
   private final Color headerColor = new Color(150, 45, 45, 255);
   private final Color bodyColor = new Color(0, 0, 0, 50);
   private final List<Vector4f> blurMatrices = new ArrayList<>();

   @EventTarget(4)
   public void renderIcons(EventRender2D e) {
      this.list.forEach(Runnable::run);
   }

   @EventTarget
   public void onShader(EventShader e) {
      if (e.getType() != EventType.BLUR && e.getType() != EventType.SHADOW) {
         return;
      }

      for (Vector4f matrix : this.blurMatrices) {
         if (matrix.z() > 0.0F && matrix.w() > 0.0F) {
            RenderUtils.drawRoundedRect(e.getStack(), matrix.x(), matrix.y(), matrix.z(), matrix.w(), 5.0F, 1073741824);
         }
      }
   }

   @EventTarget
   public void onRender(EventRender2D e) {
      if (mc.player == null) {
         return;
      }

      GuiGraphics guiGraphics = e.getGuiGraphics();
      for (MobEffectInstance effect : mc.player.getActiveEffects()) {
         EffectDisplay.MobEffectInfo info;
         if (this.infos.containsKey(effect.getEffect())) {
            info = this.infos.get(effect.getEffect());
         } else {
            info = new EffectDisplay.MobEffectInfo();
            this.infos.put(effect.getEffect(), info);
         }

         info.maxDuration = Math.max(info.maxDuration, effect.getDuration());
         info.duration = effect.getDuration();
         info.amplifier = effect.getAmplifier();
         info.shouldDisappear = false;
      }

      int startY = mc.getWindow().getGuiScaledHeight() / 2 - this.infos.size() * 16;
      this.list = Lists.newArrayListWithExpectedSize(this.infos.size());
      this.blurMatrices.clear();

      for (Entry<Holder<MobEffect>, EffectDisplay.MobEffectInfo> entry : this.infos.entrySet()) {
         EffectDisplay.MobEffectInfo effectInfo = entry.getValue();
         String text = this.getDisplayName(entry.getKey(), effectInfo);
         if (effectInfo.yTimer.value == -1.0F) {
            effectInfo.yTimer.value = (float)startY;
         }

         CustomTextRenderer harmony = Fonts.harmony;
         effectInfo.width = 25.0F + harmony.getWidth(text, 0.3) + 20.0F;
         float x = effectInfo.xTimer.value;
         float y = effectInfo.yTimer.value;
         effectInfo.shouldDisappear = !mc.player.hasEffect(entry.getKey());
         if (effectInfo.shouldDisappear) {
            effectInfo.xTimer.target = -effectInfo.width - 20.0F;
            if (x <= -effectInfo.width - 20.0F) {
               this.infos.remove(entry.getKey());
            }
         } else {
            effectInfo.durationTimer.target = (float)effectInfo.duration / (float)effectInfo.maxDuration * effectInfo.width;
            if (effectInfo.durationTimer.value <= 0.0F) {
               effectInfo.durationTimer.value = effectInfo.durationTimer.target;
            }

            effectInfo.xTimer.target = 10.0F;
            effectInfo.yTimer.target = (float)startY;
            effectInfo.yTimer.update(true);
         }

         effectInfo.durationTimer.update(true);
         effectInfo.xTimer.update(true);
         this.blurMatrices.add(new Vector4f(x + 2.0F, y + 2.0F, effectInfo.width - 2.0F, 28.0F));
         StencilUtils.write(false);
         RenderUtils.drawRoundedRect(guiGraphics, x + 2.0F, y + 2.0F, effectInfo.width - 2.0F, 28.0F, 5.0F, -1);
         StencilUtils.erase(true);
         RenderUtils.fillBound(guiGraphics, x, y, effectInfo.width, 30.0F, this.bodyColor.getRGB());
         RenderUtils.fillBound(guiGraphics, x, y, effectInfo.durationTimer.value, 30.0F, this.bodyColor.getRGB());
         StencilUtils.dispose();
         RenderUtils.drawRoundedRect(guiGraphics, x + effectInfo.width - 10.0F, y + 7.0F, 5.0F, 18.0F, 2.0F, this.headerColor.getRGB());
         harmony.render(guiGraphics, text, (double)(x + 27.0F), (double)(y + 7.0F), this.headerColor, true, 0.3);
         String duration = StringUtil.formatTickDuration(effectInfo.duration, mc.level.tickRateManager().tickrate());
         harmony.render(guiGraphics, duration, (double)(x + 27.0F), (double)(y + 17.0F), Color.WHITE, true, 0.25);
         startY += 34;
      }
   }

   public String getDisplayName(Holder<MobEffect> effect, EffectDisplay.MobEffectInfo info) {
      String effectName = effect.value().getDisplayName().getString();
      String amplifierName;
      if (info.amplifier == 0) {
         amplifierName = "";
      } else if (info.amplifier == 1) {
         amplifierName = " " + I18n.get("enchantment.level.2", new Object[0]);
      } else if (info.amplifier == 2) {
         amplifierName = " " + I18n.get("enchantment.level.3", new Object[0]);
      } else if (info.amplifier == 3) {
         amplifierName = " " + I18n.get("enchantment.level.4", new Object[0]);
      } else {
         amplifierName = " " + info.amplifier;
      }

      return effectName + amplifierName;
   }

   public static class MobEffectInfo {
      public SmoothAnimationTimer xTimer = new SmoothAnimationTimer(-60.0F, 0.2F);
      public SmoothAnimationTimer yTimer = new SmoothAnimationTimer(-1.0F, 0.2F);
      public SmoothAnimationTimer durationTimer = new SmoothAnimationTimer(-1.0F, 0.2F);
      public int maxDuration = -1;
      public int duration = 0;
      public int amplifier = 0;
      public boolean shouldDisappear = false;
      public float width;
   }
}
