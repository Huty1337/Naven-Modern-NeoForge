package com.heypixel.heypixelmod.obsoverlay.ui;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventShader;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.utils.Colors;
import com.heypixel.heypixelmod.obsoverlay.utils.FontIcons;
import com.heypixel.heypixelmod.obsoverlay.utils.MathUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.SmoothAnimationTimer;
import com.heypixel.heypixelmod.obsoverlay.utils.StencilUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.StringUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.TimeHelper;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.text.CustomTextRenderer;
import com.heypixel.heypixelmod.obsoverlay.values.Value;
import com.heypixel.heypixelmod.obsoverlay.values.ValueType;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class ClickGUI extends Screen {
   private static final Minecraft mc = Minecraft.getInstance();
   public static float windowX = 100.0F;
   public static float windowY = 100.0F;
   public static float windowWidth = 400.0F;
   public static float windowHeight = 250.0F;
   Category selectedCategory = null;
   Module selectedModule = null;
   int[] dragMousePosition = new int[]{-1, -1};
   boolean hoveringBack = false;
   SmoothAnimationTimer widthAnimation = new SmoothAnimationTimer(100.0F);
   SmoothAnimationTimer heightAnimation = new SmoothAnimationTimer(140.0F);
   SmoothAnimationTimer titleAnimation = new SmoothAnimationTimer(100.0F);
   SmoothAnimationTimer titleHoverAnimation = new SmoothAnimationTimer(0.0F);
   SmoothAnimationTimer categoryMotionY = new SmoothAnimationTimer(0.0F);
   SmoothAnimationTimer moduleValuesMotionY = new SmoothAnimationTimer(0.0F);
   HashMap<Category, SmoothAnimationTimer> categoryXAnimation = new HashMap<Category, SmoothAnimationTimer>() {
      {
         for (Category value : Category.values()) {
            this.put(value, new SmoothAnimationTimer(0.0F));
         }
      }
   };
   HashMap<Category, SmoothAnimationTimer> categoryYAnimation = new HashMap<Category, SmoothAnimationTimer>() {
      {
         for (Category value : Category.values()) {
            this.put(value, new SmoothAnimationTimer(0.0F));
         }
      }
   };
   HashMap<Category, List<Module>> modules = new HashMap<Category, List<Module>>() {
      {
         for (Category value : Category.values()) {
            this.put(value, Naven.getInstance().getModuleManager().getModulesByCategory(value));
         }
      }
   };
   HashMap<Module, SmoothAnimationTimer> modulesAnimation = new HashMap<Module, SmoothAnimationTimer>() {
      {
         for (Module value : Naven.getInstance().getModuleManager().getModules()) {
            this.put(value, new SmoothAnimationTimer(0.0F, 255.0F));
         }
      }
   };
   HashMap<Module, SmoothAnimationTimer> modulesToggleAnimation = new HashMap<Module, SmoothAnimationTimer>() {
      {
         for (Module value : Naven.getInstance().getModuleManager().getModules()) {
            this.put(value, new SmoothAnimationTimer(0.0F));
         }
      }
   };
   HashMap<Value, SmoothAnimationTimer> valuesAnimation = new HashMap<Value, SmoothAnimationTimer>() {
      {
         for (Value value : Naven.getInstance().getValueManager().getValues()) {
            this.put(value, new SmoothAnimationTimer(0.0F));
         }
      }
   };
   String titleDisplayName = "";
   float finalModuleHeight;
   float finalValueHeight;
   boolean clickReturnModules = false;
   boolean clickReturnCategories = false;
   boolean clickOpenCategoryModules = false;
   boolean clickResizeWindow = false;
   boolean clickDragWindow = false;
   Category hoveringCategory = null;
   Module hoveringModule = null;
   Module bindingModule = null;
   SmoothAnimationTimer moduleSwapAnimation = new SmoothAnimationTimer(0.0F);
   SmoothAnimationTimer bindingAnimation = new SmoothAnimationTimer(0.0F);
   List<Module> categoryModules;
   List<Value> renderValues;
   BooleanValue hoveringBooleanValue;
   FloatValue hoveringFloatValue;
   FloatValue draggingFloatValue;
   ModeValue hoveringModeValue;
   int targetModeValueIndex;
   String bindingModuleName;
   private boolean mouseDown = false;
   private float minCatY = 0.0F;
   private SmoothAnimationTimer moduleAlphaAnimation = new SmoothAnimationTimer(0.0F);
   private TimeHelper moduleAlphaTimer = new TimeHelper();
   private float minValueY = 0.0F;
   private SmoothAnimationTimer valuesAlphaAnimation = new SmoothAnimationTimer(0.0F);
   private TimeHelper valuesAlphaTimer = new TimeHelper();

   public ClickGUI() {
      super(Component.nullToEmpty("Naven"));
   }

   public void onClose() {
      Naven.getInstance().getFileManager().save();
      Naven.getInstance().getEventManager().unregister(this);
      super.onClose();
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent pEvent, boolean pDoubleClick) {
      double mouseX = pEvent.x();
      double mouseY = pEvent.y();
      int mouseButton = pEvent.button();
      if (mouseButton == 0) {
         this.mouseDown = true;
      }

      if (this.bindingModule == null || mouseButton != 3 && mouseButton != 4) {
         if (mouseButton != 2 && this.bindingModule == null) {
            if (this.hoveringModule != null) {
               if (mouseButton == 0) {
                  this.hoveringModule.toggle();
               } else if (mouseButton == 1) {
                  this.selectedModule = this.hoveringModule;
                  this.renderValues = Naven.getInstance().getValueManager().getValuesByHasValue(this.hoveringModule);
                  this.moduleValuesMotionY.target = this.moduleValuesMotionY.value = 0.0F;
               }
            }

            if (mouseButton == 0) {
               if (this.hoveringBack && !this.clickResizeWindow && !this.clickDragWindow) {
                  this.selectedCategory = null;
                  this.selectedModule = null;
                  this.renderValues = null;
               }

               if (this.clickOpenCategoryModules && this.hoveringCategory != null) {
                  this.selectedCategory = this.hoveringCategory;
                  this.categoryMotionY.value = this.categoryMotionY.target = 0.0F;
                  this.moduleSwapAnimation.value = 5.0F;
                  this.moduleSwapAnimation.target = 255.0F;
                  this.clickOpenCategoryModules = false;
               }

               boolean doDragWindow = this.selectedCategory != null
                  ? RenderUtils.isHovering((int)mouseX, (int)mouseY, windowX, windowY, windowX + windowWidth, windowY + 25.0F)
                  : RenderUtils.isHovering((int)mouseX, (int)mouseY, windowX, windowY, windowX + 100.0F, windowY + 40.0F);
               if ((this.selectedCategory == null || !this.hoveringBack) && doDragWindow) {
                  this.SetDragPosition(mouseX, mouseY);
                  this.clickDragWindow = true;
               }

               if (RenderUtils.isHovering(
                  (int)mouseX, (int)mouseY, windowX + windowWidth - 10.0F, windowY + windowHeight - 10.0F, windowX + windowWidth, windowY + windowHeight
               )) {
                  this.SetDragPosition(mouseX, mouseY);
                  this.clickResizeWindow = true;
               }

               if (this.hoveringBooleanValue != null) {
                  this.hoveringBooleanValue.setCurrentValue(!this.hoveringBooleanValue.getCurrentValue());
               }

               if (this.hoveringFloatValue != null) {
                  this.draggingFloatValue = this.hoveringFloatValue;
               }

               if (this.hoveringModeValue != null) {
                  this.hoveringModeValue.setCurrentValue(this.targetModeValueIndex);
                  SmoothAnimationTimer animation = this.valuesAnimation.get(this.hoveringModeValue);
                  animation.value = 0.0F;
                  animation.target = 255.0F;
               }
            }
         } else if (mouseButton == 2 && this.hoveringModule != null) {
            this.bindingModule = this.hoveringModule;
         }

         return true;
      } else {
         this.bindingModule.setKey(-mouseButton);
         this.bindingModule = null;
         return true;
      }
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent pEvent) {
      int pButton = pEvent.button();
      if (pButton == 0) {
         this.mouseDown = false;
      }

      if (this.clickResizeWindow) {
         this.clickResizeWindow = false;
         this.SetDragPosition(-1, -1);
      }

      if (this.clickDragWindow) {
         this.clickDragWindow = false;
         this.SetDragPosition(-1, -1);
      }

      if (this.draggingFloatValue != null) {
         this.draggingFloatValue = null;
      }

      return true;
   }

   @Override
   public boolean keyPressed(KeyEvent pKeyEvent) {
      if (this.bindingModule != null) {
         int pKeyCode = pKeyEvent.key();
         if (pKeyCode == 256) {
            this.bindingModule.setKey(0);
            this.bindingModule = null;
            return true;
         }

         this.bindingModule.setKey(pKeyCode);
         this.bindingModule = null;
      }

      return super.keyPressed(pKeyEvent);
   }

   @Override
   protected void init() {
      Naven.getInstance().getEventManager().register(this);
      this.valuesAnimation.forEach((value, animation) -> {
         if (value.getValueType() == ValueType.MODE) {
            animation.value = 0.0F;
            animation.target = 255.0F;
         }
      });
   }

   @Override
   public boolean mouseScrolled(double pMouseX, double pMouseY, double pHorizontalDelta, double pDelta) {
      if (this.bindingModule == null && RenderUtils.isHoveringBound((int)pMouseX, (int)pMouseY, windowX + 5.0F, windowY + 20.0F, 100.0F, windowHeight - 5.0F)) {
         this.categoryMotionY.target = (float)((double)this.categoryMotionY.target + pDelta * 15.0);
         this.moduleAlphaTimer.reset();
      }

      if (this.renderValues != null
         && this.bindingModule == null
         && RenderUtils.isHoveringBound((int)pMouseX, (int)pMouseY, windowX + 140.0F, windowY + 20.0F, windowWidth - 155.0F, windowHeight - 25.0F)) {
         this.moduleValuesMotionY.target = (float)((double)this.moduleValuesMotionY.target + pDelta * 15.0);
         this.valuesAlphaTimer.reset();
      }

      return true;
   }

   @EventTarget
   public void onRender2D(EventRender2D e) {
      if (mc.screen == this) {
         RenderUtils.drawRoundedRect(
            e.getGuiGraphics(), windowX, windowY, this.widthAnimation.value, this.heightAnimation.value, 5.0F, 1073741824
         );
      }
   }

   @EventTarget
   public void onShader(EventShader e) {
      if ((e.getType() == EventType.BLUR || e.getType() == EventType.SHADOW) && mc.screen == this) {
         RenderUtils.drawRoundedRect(
            e.getStack(), windowX, windowY, this.widthAnimation.value, this.heightAnimation.value, 5.0F, 1073741824
         );
      }
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float pPartialTick) {
      this.hoveringModule = null;
      this.clickReturnModules = this.clickReturnCategories = this.clickOpenCategoryModules = false;
      CustomTextRenderer opensans = Fonts.opensans;
      if (this.selectedCategory == null) {
         this.widthAnimation.target = 100.0F;
         this.heightAnimation.target = 140.0F;
      } else {
         this.widthAnimation.target = windowWidth;
         this.heightAnimation.target = windowHeight;
      }

      this.widthAnimation.update(true);
      this.heightAnimation.update(true);
      RenderUtils.drawRoundedRect(g, windowX, windowY, this.widthAnimation.value, this.heightAnimation.value, 5.0F, Colors.getColor(0, 0, 0, 40));

      for (Category value : Category.values()) {
         SmoothAnimationTimer xAnimation = this.categoryXAnimation.get(value);
         SmoothAnimationTimer yAnimation = this.categoryYAnimation.get(value);
         if (this.selectedCategory == null) {
            yAnimation.target = 255.0F;
         } else {
            yAnimation.target = 0.0F;
         }

         xAnimation.update(true);
         yAnimation.update(true);
         float height = (float)(value.ordinal() * 25) * (yAnimation.value / 255.0F);
         if (yAnimation.target >= 4.0F) {
            opensans.setAlpha(yAnimation.value / 255.0F);
            Fonts.icons.render(g, value.getIcon(), (double)(windowX + 8.0F + xAnimation.value), (double)(windowY + 41.0F + height), Color.WHITE, true, 0.4);
            opensans.render(
               g, value.getDisplayName(), (double)(windowX + 25.0F + xAnimation.value), (double)(windowY + 40.0F + height), Color.WHITE, true, 0.4
            );
            opensans.setAlpha(1.0F);
         }

         boolean hovering = RenderUtils.isHovering(mouseX, mouseY, windowX, windowY + 40.0F + height, windowX + 100.0F, windowY + 40.0F + height + 20.0F);
         if (hovering) {
            xAnimation.target = 5.0F;
         } else {
            xAnimation.target = 0.0F;
         }

         if (yAnimation.value >= 250.0F && hovering) {
            this.hoveringCategory = value;
            this.clickOpenCategoryModules = true;
         }
      }

      this.titleAnimation.update(true);
      this.titleHoverAnimation.update(true);
      if (this.titleAnimation.value > 5.0F) {
         opensans.setAlpha(this.titleAnimation.value / 255.0F);
         opensans.render(
            g, this.titleDisplayName, (double)(windowX + 6.0F + this.titleHoverAnimation.value), (double)(windowY + 3.0F), Color.WHITE, true, 0.4
         );
      }

      opensans.setAlpha((255.0F - this.titleAnimation.value) / 255.0F);
      opensans.render(g, "Naven", (double)(windowX + 50.0F - opensans.getWidth("Naven", 0.75) / 2.0F), (double)(windowY + 5.0F), Color.WHITE, true, 0.75);
      opensans.setAlpha(1.0F);
      if (this.selectedCategory != null) {
         this.titleAnimation.target = 255.0F;
         this.titleDisplayName = "< "
            + this.selectedCategory.getDisplayName()
            + (this.selectedModule != null ? " / " + this.selectedModule.getName() + " - " + this.selectedModule.getDescription() : "");
         this.hoveringBack = RenderUtils.isHovering(
            mouseX,
            mouseY,
            windowX + 8.0F,
            windowY + 5.0F,
            windowX + 5.0F + opensans.getWidth(this.titleDisplayName, 0.4),
            (float)((double)(windowY + 5.0F) + opensans.getHeight(true, 0.4F))
         );
         if (this.hoveringBack && !this.clickResizeWindow && !this.clickDragWindow) {
            this.titleHoverAnimation.target = -2.0F;
         } else {
            this.titleHoverAnimation.target = 0.0F;
         }

         if (this.categoryMotionY.target < -this.finalModuleHeight) {
            this.categoryMotionY.target = -this.finalModuleHeight;
         }

         if (this.categoryMotionY.target > 0.0F) {
            this.categoryMotionY.target = 0.0F;
         }

         this.categoryMotionY.update(true);
      } else {
         this.titleAnimation.target = 4.0F;
      }

      StencilUtils.write(false);
      RenderUtils.drawRoundedRect(g, windowX, windowY, this.widthAnimation.value, this.heightAnimation.value, 5.0F, Integer.MIN_VALUE);
      StencilUtils.erase(true);
      float clipX = windowX;
      float clipY = windowY + 20.0F;
      float clipW = this.widthAnimation.value;
      float clipH = Math.max(0.0F, this.heightAnimation.value - 20.0F);
      g.enableScissor(
         (int)Math.floor((double)clipX),
         (int)Math.floor((double)clipY),
         (int)Math.ceil((double)(clipX + clipW)),
         (int)Math.ceil((double)(clipY + clipH))
      );
      List<Module> inList = this.modules.get(this.selectedCategory);
      if (inList != null) {
         this.categoryModules = inList;
         this.moduleSwapAnimation.target = 255.0F;
      } else {
         this.moduleSwapAnimation.target = 5.0F;
      }

      this.moduleSwapAnimation.update(true);
      if (inList == null && this.moduleSwapAnimation.value < 8.0F) {
         this.categoryModules = null;
      }

      if (this.categoryModules != null) {
         float renderModuleHeight = 0.0F;
         this.minCatY = windowHeight - 25.0F;

         for (Module categoryModule : this.categoryModules) {
            boolean isHovering = RenderUtils.isHoveringBound(mouseX, mouseY, windowX + 5.0F, windowY + 20.0F, 120.0F, windowHeight - 25.0F)
               && RenderUtils.isHoveringBound(mouseX, mouseY, windowX + 5.0F, windowY + 20.0F + renderModuleHeight + this.categoryMotionY.value, 120.0F, 25.0F)
               && this.moduleSwapAnimation.value > 250.0F
               && this.bindingModule == null;
            SmoothAnimationTimer moduleToggleAnimation = this.modulesToggleAnimation.get(categoryModule);
            if (categoryModule.isEnabled()) {
               moduleToggleAnimation.target = this.moduleSwapAnimation.value;
            } else {
               moduleToggleAnimation.target = 6.0F;
            }

            moduleToggleAnimation.update(true);
            int alpha = (int)this.moduleSwapAnimation.value;
            int enabledColor = Colors.getColor(54, 98, 236, (int)moduleToggleAnimation.value);
            int disabledColor = Colors.getColor(25, 25, 25, alpha);
            RenderUtils.drawRoundedRect(
               g, windowX + 5.0F, windowY + 20.0F + renderModuleHeight + this.categoryMotionY.value, 120.0F, 25.0F, 5.0F, disabledColor
            );
            RenderUtils.drawRoundedRect(
               g, windowX + 5.0F, windowY + 20.0F + renderModuleHeight + this.categoryMotionY.value, 120.0F, 25.0F, 5.0F, enabledColor
            );
            SmoothAnimationTimer moduleAnimation = this.modulesAnimation.get(categoryModule);
            if (isHovering) {
               moduleAnimation.target = 150.0F;
               this.hoveringModule = categoryModule;
            } else {
               moduleAnimation.target = 5.0F;
            }

            moduleAnimation.update(true);
            int hoveringColor = Colors.getColor(255, 255, 255, (int)moduleAnimation.value / 3);
            RenderUtils.drawRoundedRect(
               g, windowX + 5.0F, windowY + 20.0F + renderModuleHeight + this.categoryMotionY.value, 120.0F, 25.0F, 5.0F, hoveringColor
            );
            opensans.setAlpha((float)alpha / 255.0F);
            opensans.render(
               g,
               categoryModule.getName(),
               (double)(windowX + 13.0F),
               (double)(windowY + 25.0F + renderModuleHeight + this.categoryMotionY.value),
               Color.WHITE,
               true,
               0.4
            );
            opensans.setAlpha(1.0F);
            renderModuleHeight += 30.0F;
         }

         this.finalModuleHeight = renderModuleHeight + 20.0F - windowHeight;
         this.minCatY -= renderModuleHeight - 5.0F;
         float totalHeight = this.finalModuleHeight + windowHeight;
         if (totalHeight > windowHeight - 25.0F) {
            this.moduleAlphaAnimation.update(true);
            if (this.moduleAlphaTimer.delay(1000.0)) {
               this.moduleAlphaAnimation.target = 0.0F;
            } else {
               this.moduleAlphaAnimation.target = 255.0F;
            }

            float viewable = windowHeight - 25.0F;
            float progress = (float)MathUtils.clamp((double)(-this.categoryMotionY.value / -this.minCatY), 0.0, 1.0);
            float ratio = viewable / totalHeight * viewable;
            float barHeight = Math.max(ratio, 20.0F);
            float position = progress * (viewable - barHeight);
            RenderUtils.drawRoundedRect(
               g,
               windowX + 127.0F,
               windowY + 20.0F + position,
               3.0F,
               barHeight,
               1.5F,
               RenderUtils.reAlpha(3630060, this.moduleAlphaAnimation.value / 255.0F)
            );
         }
      }

      if (this.renderValues != null) {
         boolean isValueInBound = RenderUtils.isHoveringBound(mouseX, mouseY, windowX + 140.0F, windowY + 20.0F, windowWidth - 155.0F, windowHeight - 25.0F);
         float motion = this.moduleValuesMotionY.value;
         if (this.moduleValuesMotionY.target < -this.finalValueHeight) {
            this.moduleValuesMotionY.target = -this.finalValueHeight;
         }

         if (this.moduleValuesMotionY.target > 0.0F) {
            this.moduleValuesMotionY.target = 0.0F;
         }

         this.moduleValuesMotionY.update(true);
         float x = 0.0F;
         float valueHeight = 0.0F;
         this.minValueY = windowHeight - 25.0F;
         this.hoveringBooleanValue = null;

         for (Value value : this.renderValues) {
            if (value.isVisible() && value.getValueType() == ValueType.BOOLEAN) {
               BooleanValue booleanValue = value.getBooleanValue();
               SmoothAnimationTimer animation = this.valuesAnimation.get(booleanValue);
               if (booleanValue.getCurrentValue()) {
                  animation.target = 255.0F;
               } else {
                  animation.target = 0.0F;
               }

               animation.update(true);
               float scale = 0.4F;
               CustomTextRenderer font = Fonts.opensans;
               int heightOffset = 0;
               if (StringUtils.containChinese(value.getName())) {
                  font = Fonts.harmony;
                  scale = 0.325F;
                  heightOffset = 2;
               }

               float currentLength = font.getWidth(value.getName(), (double)scale) + 23.0F;
               if (x + currentLength + 20.0F > windowWidth - 155.0F) {
                  x = 0.0F;
                  valueHeight += 20.0F;
               }

               if (isValueInBound
                  && RenderUtils.isHoveringBound(mouseX, mouseY, windowX + 130.0F + x, windowY + valueHeight + motion + 20.0F, currentLength, 13.0F)) {
                  this.hoveringBooleanValue = booleanValue;
               }

               int enabledColor = Colors.getColor(54, 98, 236, (int)animation.value);
               RenderUtils.drawRoundedRect(
                  g, windowX + 140.0F + x, windowY + valueHeight + motion + 20.0F, 12.0F, 12.0F, 2.0F, Colors.getColor(0, 0, 0, 150)
               );
               RenderUtils.drawRoundedRect(g, windowX + 142.0F + x, windowY + valueHeight + motion + 22.0F, 8.0F, 8.0F, 2.0F, enabledColor);
               font.render(
                  g,
                  value.getName(),
                  (double)(windowX + 155.0F + x),
                  (double)(windowY + valueHeight + motion + 19.0F + (float)heightOffset),
                  Color.WHITE,
                  true,
                  (double)scale
               );
               x += currentLength;
            }
         }

         valueHeight += 10.0F;
         this.hoveringFloatValue = null;

         for (Value valuex : this.renderValues) {
            if (valuex.isVisible() && valuex.getValueType() == ValueType.FLOAT) {
               FloatValue floatValue = valuex.getFloatValue();
               SmoothAnimationTimer animationx = this.valuesAnimation.get(floatValue);
               if (isValueInBound
                  && RenderUtils.isHoveringBound(mouseX, mouseY, windowX + 140.0F, windowY + valueHeight + motion + 39.5F, windowWidth - 155.0F, 10.0F)) {
                  this.hoveringFloatValue = floatValue;
               }

               opensans.render(g, valuex.getName(), (double)(windowX + 140.0F), (double)(windowY + valueHeight + motion + 25.0F), Color.WHITE, true, 0.4);
               String currentValue = (float)Math.round(floatValue.getCurrentValue() * 100.0F) / 100.0F + " / " + floatValue.getMaxValue();
               opensans.render(
                  g,
                  currentValue,
                  (double)(windowX + windowWidth - opensans.getWidth(currentValue, 0.4) - 15.0F),
                  (double)(windowY + valueHeight + motion + 25.0F),
                  Color.WHITE,
                  true,
                  0.4
               );
               float stage = (floatValue.getCurrentValue() - floatValue.getMinValue()) / (floatValue.getMaxValue() - floatValue.getMinValue());
               int enabledColor = Colors.getColor(54, 98, 236, 255);
               RenderUtils.drawRoundedRect(
                  g, windowX + 140.0F, windowY + valueHeight + motion + 42.0F, windowWidth - 155.0F, 5.0F, 3.0F, Colors.getColor(0, 0, 0, 150)
               );
               animationx.target = (windowWidth - 155.0F) * stage;
               animationx.update(true);
               RenderUtils.drawRoundedRect(g, windowX + 140.0F, windowY + valueHeight + motion + 42.0F, animationx.value, 5.0F, 3.0F, enabledColor);
               RenderUtils.drawRoundedRect(
                  g, windowX + 135.0F + animationx.value, windowY + valueHeight + motion + 39.5F, 10.0F, 10.0F, 5.0F, Colors.getColor(255, 255, 255, 255)
               );
               valueHeight += 25.0F;
            }
         }

         this.hoveringModeValue = null;

         for (Value valuexx : this.renderValues) {
            if (valuexx.isVisible() && valuexx.getValueType() == ValueType.MODE) {
               ModeValue modeValue = valuexx.getModeValue();
               SmoothAnimationTimer animationx = this.valuesAnimation.get(modeValue);
               animationx.update(true);
               opensans.render(g, valuexx.getName(), (double)(windowX + 140.0F), (double)(windowY + valueHeight + motion + 25.0F), Color.WHITE, true, 0.4);
               x = 0.0F;
               valueHeight += 15.0F;

               for (int modeIndex = 0; modeIndex < modeValue.getValues().length; modeIndex++) {
                  String mode = modeValue.getValues()[modeIndex];
                  float currentLengthx = opensans.getWidth(mode, 0.4) + 20.0F;
                  if (x + currentLengthx + 20.0F > windowWidth - 155.0F) {
                     x = 0.0F;
                     valueHeight += 20.0F;
                  }

                  if (isValueInBound
                     && RenderUtils.isHoveringBound(mouseX, mouseY, windowX + 140.0F + x, windowY + valueHeight + motion + 25.0F, currentLengthx, 13.0F)) {
                     this.hoveringModeValue = modeValue;
                     this.targetModeValueIndex = modeIndex;
                  }

                  int enabledColor = Colors.getColor(54, 98, 236, modeValue.isCurrentMode(mode) ? (int)animationx.value : 10);
                  RenderUtils.drawRoundedRect(
                     g, windowX + 140.0F + x, windowY + valueHeight + motion + 27.0F, 10.0F, 10.0F, 5.0F, Colors.getColor(0, 0, 0, 150)
                  );
                  RenderUtils.drawRoundedRect(g, windowX + 141.0F + x, windowY + valueHeight + motion + 28.0F, 8.0F, 8.0F, 5.0F, enabledColor);
                  opensans.render(g, mode, (double)(windowX + 152.0F + x), (double)(windowY + valueHeight + motion + 25.0F), Color.WHITE, true, 0.4);
                  x += currentLengthx;
               }

               valueHeight += 20.0F;
            }
         }

         this.finalValueHeight = valueHeight - windowHeight + 25.0F;
         this.minValueY -= valueHeight;
         float valTotalHeight = this.finalValueHeight + windowHeight;
         if (valTotalHeight > windowHeight - 25.0F) {
            this.valuesAlphaAnimation.update(true);
            if (this.valuesAlphaTimer.delay(1000.0)) {
               this.valuesAlphaAnimation.target = 0.0F;
            } else {
               this.valuesAlphaAnimation.target = 255.0F;
            }

            float viewable = windowHeight - 25.0F;
            float progress = (float)MathUtils.clamp((double)(-this.moduleValuesMotionY.value / -this.minValueY), 0.0, 1.0);
            float ratio = viewable / valTotalHeight * viewable;
            float barHeight = Math.max(ratio, 20.0F);
            float position = progress * (viewable - barHeight);
            RenderUtils.drawRoundedRect(
               g,
               windowX + windowWidth - 8.0F,
               windowY + 20.0F + position,
               3.0F,
               barHeight,
               1.5F,
               RenderUtils.reAlpha(3630060, this.valuesAlphaAnimation.value / 255.0F)
            );
         }
      }

      if (this.draggingFloatValue != null) {
         float stage = ((float)mouseX - windowX - 140.0F) / (windowWidth - 160.0F);
         float valuexxx = this.draggingFloatValue.getMinValue() + (this.draggingFloatValue.getMaxValue() - this.draggingFloatValue.getMinValue()) * stage;
         if (valuexxx < this.draggingFloatValue.getMinValue()) {
            valuexxx = this.draggingFloatValue.getMinValue();
         }

         if (valuexxx > this.draggingFloatValue.getMaxValue()) {
            valuexxx = this.draggingFloatValue.getMaxValue();
         }

         valuexxx = (float)Math.round(valuexxx / this.draggingFloatValue.getStep()) * this.draggingFloatValue.getStep();
         this.draggingFloatValue.setCurrentValue(valuexxx);
      }

      if (this.clickDragWindow && this.mouseDown) {
         windowX = windowX + (float)(mouseX - this.dragMousePosition[0]);
         windowY = windowY + (float)(mouseY - this.dragMousePosition[1]);
         this.SetDragPosition(mouseX, mouseY);
      }

      if (this.categoryModules != null && !this.hoveringBack && this.clickResizeWindow && this.mouseDown) {
         windowWidth = windowWidth + (float)(mouseX - this.dragMousePosition[0]);
         windowHeight = windowHeight + (float)(mouseY - this.dragMousePosition[1]);
         if (windowWidth < 500.0F) {
            windowWidth = 500.0F;
         }

         if (windowHeight < 300.0F) {
            windowHeight = 300.0F;
         }

         this.SetDragPosition(mouseX, mouseY);
      }

      if (this.bindingModule != null) {
         this.bindingAnimation.target = 250.0F;
         this.bindingModuleName = this.bindingModule.getName();
      } else {
         this.bindingAnimation.target = 5.0F;
      }

      this.bindingAnimation.update(true);
      if (this.bindingAnimation.value > 6.0F) {
         RenderUtils.fill(
            g,
            windowX,
            windowY,
            windowX + this.widthAnimation.value,
            windowY + this.heightAnimation.value,
            Colors.getColor(0, 0, 0, (int)this.bindingAnimation.value / 2)
         );
         opensans.setAlpha(this.bindingAnimation.value / 255.0F);
         String line1 = "Press a key to bind " + this.bindingModuleName;
         String line2 = "(Press ESC to remove/cancel key bind)";
         opensans.render(
            g,
            line1,
            (double)(windowX + this.widthAnimation.value / 2.0F - opensans.getWidth(line1, 0.6) / 2.0F),
            (double)windowY + ((double)this.heightAnimation.value - opensans.getHeight(true, 0.6)) / 2.0 - 10.0,
            Color.WHITE,
            true,
            0.6
         );
         opensans.render(
            g,
            line2,
            (double)(windowX + this.widthAnimation.value / 2.0F - opensans.getWidth(line2, 0.4) / 2.0F),
            (double)windowY + ((double)this.heightAnimation.value - opensans.getHeight(true, 0.4)) / 2.0 + 15.0,
            Color.WHITE,
            true,
            0.4
         );
         opensans.setAlpha(1.0F);
      }

      if (this.selectedCategory != null) {
         Fonts.icons.setAlpha(0.5F);
         Fonts.icons
            .render(
               g,
               FontIcons.RESIZE,
               (double)(windowX + this.widthAnimation.value - 10.0F),
               (double)(windowY + this.heightAnimation.value - 10.0F),
               Color.WHITE,
               false,
               0.3
            );
         Fonts.icons.setAlpha(1.0F);
      }

      g.disableScissor();
      StencilUtils.dispose();
   }

   public void SetDragPosition(double x, double y) {
      this.SetDragPosition((int)x, (int)y);
   }

   public void SetDragPosition(int x, int y) {
      this.dragMousePosition[0] = x;
      this.dragMousePosition[1] = y;
   }
}
