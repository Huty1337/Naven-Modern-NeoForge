package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventShader;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.ChestStealer;
import com.heypixel.heypixelmod.obsoverlay.utils.InventoryUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.MathUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.ProjectionUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.Vector2f;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.text.CustomTextRenderer;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.mojang.logging.LogUtils;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.EndCrystalItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4f;
import org.slf4j.Logger;

@ModuleInfo(
   name = "ItemTags",
   description = "Show item tags.",
   category = Category.RENDER
)
public class ItemTags extends Module {
   private static final boolean DEBUG_COORDS = false;
   private static final Logger LOGGER = LogUtils.getLogger();
   private static long debugFrames;

   private final ConcurrentHashMap<ItemEntity, Vector2f> entityPositions = new ConcurrentHashMap<>();
   private final List<TagGroup> tagGroups = new ArrayList<>();
   private final List<Vector4f> blurMatrices = new ArrayList<>();
   private static final int backgroundColor1 = new Color(0, 0, 0, 40).getRGB();
   public FloatValue scale = ValueBuilder.create(this, "Scale")
      .setDefaultFloatValue(0.35F)
      .setFloatStep(0.01F)
      .setMinFloatValue(0.1F)
      .setMaxFloatValue(0.5F)
      .build()
      .getFloatValue();
   BooleanValue allItems = ValueBuilder.create(this, "All Items").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue godItems = ValueBuilder.create(this, "God Items")
      .setDefaultBooleanValue(true)
      .setVisibility(() -> !this.allItems.getCurrentValue())
      .build()
      .getBooleanValue();
   BooleanValue diamond = ValueBuilder.create(this, "Diamond")
      .setDefaultBooleanValue(true)
      .setVisibility(() -> !this.allItems.getCurrentValue())
      .build()
      .getBooleanValue();
   BooleanValue gold = ValueBuilder.create(this, "Gold")
      .setDefaultBooleanValue(true)
      .setVisibility(() -> !this.allItems.getCurrentValue())
      .build()
      .getBooleanValue();
   BooleanValue iron = ValueBuilder.create(this, "Iron")
      .setDefaultBooleanValue(true)
      .setVisibility(() -> !this.allItems.getCurrentValue())
      .build()
      .getBooleanValue();
   BooleanValue enderPearl = ValueBuilder.create(this, "Ender Pearl")
      .setDefaultBooleanValue(true)
      .setVisibility(() -> !this.allItems.getCurrentValue())
      .build()
      .getBooleanValue();
   BooleanValue goldenApple = ValueBuilder.create(this, "Golden Apple")
      .setDefaultBooleanValue(true)
      .setVisibility(() -> !this.allItems.getCurrentValue())
      .build()
      .getBooleanValue();
   BooleanValue usefulItem = ValueBuilder.create(this, "Useful Item")
      .setDefaultBooleanValue(true)
      .setVisibility(() -> !this.allItems.getCurrentValue())
      .build()
      .getBooleanValue();

   private static String stripBrackets(String text) {
      if (text == null) return "";
      String s = text.trim();
      if (s.length() >= 2) {
         char first = s.charAt(0);
         char last = s.charAt(s.length() - 1);
         if ((first == '[' && last == ']') || (first == '【' && last == '】') || (first == '（' && last == '）') || (first == '(' && last == ')')) {
            s = s.substring(1, s.length() - 1).trim();
         }
      }
      return s.replace("[", "").replace("]", "").replace("【", "").replace("】", "");
   }

   private static boolean isEnchantedGoldenApple(ItemStack stack) {
      return stack.is(Items.ENCHANTED_GOLDEN_APPLE);
   }

   private static boolean isValidScreenPos(Vector2f pos) {
      if (pos == null) {
         return false;
      }

      float x = pos.getX();
      float y = pos.getY();
      return !Float.isNaN(x) && !Float.isInfinite(x) && !Float.isNaN(y) && !Float.isInfinite(y) && Math.abs(x) < 1.0E6F && Math.abs(y) < 1.0E6F;
   }

   private boolean isValidItem(ItemStack stack) {
      if (stack == null) {
         return false;
      } else if (stack.isEmpty()) {
         return false;
      } else if (this.allItems.getCurrentValue()) {
         return true;
      } else {
         if (this.godItems.getCurrentValue()) {
            if (InventoryUtils.isKBBall(stack)) {
               return true;
            }

            if (isEnchantedGoldenApple(stack)) {
               return true;
            }

            if (InventoryUtils.isGodAxe(stack)) {
               return true;
            }
         }

         if (this.diamond.getCurrentValue() && stack.getItem() == Items.DIAMOND) {
            return true;
         } else if (this.gold.getCurrentValue() && stack.getItem() == Items.GOLD_INGOT) {
            return true;
         } else if (this.iron.getCurrentValue() && stack.getItem() == Items.IRON_INGOT) {
            return true;
         } else if (this.enderPearl.getCurrentValue() && stack.getItem() == Items.ENDER_PEARL) {
            return true;
         } else if (this.goldenApple.getCurrentValue() && stack.getItem() == Items.GOLDEN_APPLE) {
            return true;
         } else {
            if (this.usefulItem.getCurrentValue()) {
               if (stack.getItem() instanceof BlockItem && stack.getCount() < 8) {
                  return false;
               }

               if ((stack.getItem() instanceof SnowballItem || stack.getItem() instanceof EggItem) && stack.getCount() < 3) {
                  return false;
               }

               if (ChestStealer.isItemUseful(stack)) {
                  return true;
               }
            }

            return false;
         }
      }
   }

   private boolean isGodItem(ItemStack stack) {
      if (InventoryUtils.isKBBall(stack)) {
         return true;
      } else if (isEnchantedGoldenApple(stack)) {
         return true;
      } else {
         return stack.getItem() instanceof EndCrystalItem ? true : InventoryUtils.isGodAxe(stack);
      }
   }

   private void updatePositions(float renderPartialTicks) {
      this.entityPositions.clear();
      this.tagGroups.clear();

      for (Entity entity : mc.level.entitiesForRendering()) {
         if (entity instanceof ItemEntity) {
            ItemEntity itemEntity = (ItemEntity)entity;
            if (this.isValidItem(itemEntity.getItem())) {
               double x = MathUtils.interpolate(renderPartialTicks, entity.xo, entity.getX());
               double y = MathUtils.interpolate(renderPartialTicks, entity.yo, entity.getY()) + (double)entity.getBbHeight() + 0.5;
               double z = MathUtils.interpolate(renderPartialTicks, entity.zo, entity.getZ());
               Vec3 worldPos = new Vec3(x, y, z);
               TagGroup group = null;
               for (TagGroup g : this.tagGroups) {
                  if (g.center.distanceToSqr(worldPos) <= 1.0) {
                     group = g;
                     break;
                  }
               }
               if (group == null) {
                  group = new TagGroup(worldPos);
                  this.tagGroups.add(group);
               } else {
                  group.addCenterSample(worldPos);
               }
               group.addItem(itemEntity.getItem());
            }
         }
      }

      for (TagGroup group : this.tagGroups) {
         Vector2f vector = ProjectionUtils.project(group.center.x, group.center.y, group.center.z, renderPartialTicks);
         if (!isValidScreenPos(vector)) {
            continue;
         }
         vector.setY(vector.getY() - 2.0F);
         group.screenPos = vector;
      }

      if (DEBUG_COORDS && ++debugFrames % 60L == 0L) {
         Vec3 camera = RenderUtils.getCameraPos();
         Camera liveCamera = mc.getEntityRenderDispatcher().camera;
         LOGGER.info(
            "[ItemTags] partialTick={} scale={} groups={} items={} camera=({}, {}, {}) camYaw={} camPitch={} guiScaled={}x{} window={}x{} guiScale={}",
            renderPartialTicks,
            this.scale.getCurrentValue(),
            this.tagGroups.size(),
            this.entityPositions.size(),
            fmt(camera.x), fmt(camera.y), fmt(camera.z),
            fmt((double)liveCamera.getYRot()), fmt((double)liveCamera.getXRot()),
            mc.getWindow().getGuiScaledWidth(),
            mc.getWindow().getGuiScaledHeight(),
            mc.getWindow().getWidth(),
            mc.getWindow().getHeight(),
            mc.getWindow().getGuiScale()
         );

         for (TagGroup group : this.tagGroups) {
            if (group.screenPos != null) {
               LOGGER.info(
                  "[ItemTags] sample world=({}, {}, {}) screen=({}, {})",
                  fmt(group.center.x), fmt(group.center.y), fmt(group.center.z),
                  fmt(group.screenPos.x), fmt(group.screenPos.y)
               );
               break;
            }
         }
      }
   }

   private static String fmt(double value) {
      return String.format("%.2f", value);
   }

   @EventTarget
   public void update(EventRender event) {
      try {
         this.updatePositions(event.getRenderPartialTicks());
      } catch (Exception var3) {
      }
   }

   @EventTarget
   public void onShader(EventShader e) {
      if (e.getType() != EventType.BLUR && e.getType() != EventType.SHADOW && e.getType() != EventType.GLOW) {
         return;
      }
      for (Vector4f blurMatrix : this.blurMatrices) {
         RenderUtils.fill(e.getStack(), blurMatrix.x(), blurMatrix.y(), blurMatrix.z(), blurMatrix.w(), 1073741824);
      }
   }

   @EventTarget
   public void on2DRender(EventRender2D e) {
      try {
         GuiGraphics guiGraphics = e.getGuiGraphics();
         this.blurMatrices.clear();

         CustomTextRenderer harmony = Fonts.harmony;
         double textScale = (double)this.scale.getCurrentValue();
         float lineHeight = (float)harmony.getHeight(true, textScale);

         for (TagGroup group : this.tagGroups) {
            if (group == null || group.screenPos == null) continue;
            Vector2f renderPositions = group.screenPos;
            List<TagLine> lines = group.buildLines();
            if (lines.isEmpty()) continue;

            float maxWidth = 0.0F;
            for (TagLine line : lines) {
               maxWidth = Math.max(maxWidth, harmony.getWidth(line.text, textScale));
            }
            float allWidth = maxWidth + 8.0F;
            float totalHeight = lines.size() * (lineHeight * 0.9F) + 4.0F;
            float boxTop = renderPositions.y - totalHeight;
            float boxLeft = renderPositions.x - allWidth / 2.0F;
            float boxRight = renderPositions.x + allWidth / 2.0F;

            this.blurMatrices.add(new Vector4f(renderPositions.x - allWidth / 2.0F, boxTop, renderPositions.x + allWidth / 2.0F, renderPositions.y));

            guiGraphics.pose().pushMatrix();
            RenderUtils.fill(guiGraphics, boxLeft, boxTop, boxRight, renderPositions.y, backgroundColor1);
            float tY = boxTop + 2.0F;
            for (TagLine line : lines) {
               float lineWidth = harmony.getWidth(line.text, textScale);
               harmony.render(
                  guiGraphics,
                  line.text,
                  (double)(renderPositions.x - lineWidth / 2.0F),
                  (double)tY,
                  line.color,
                  true,
                  textScale
               );
               tY += lineHeight * 0.9F;
            }
            guiGraphics.pose().popMatrix();
         }
      } catch (Exception var9) {
      }
   }

   private static class TagLine {
      private final String text;
      private final Color color;

      private TagLine(String text, Color color) {
         this.text = text;
         this.color = color;
      }
   }

   private class TagGroup {
      private Vec3 center;
      private int centerSamples = 1;
      private Vector2f screenPos;
      private final Map<String, LineData> items = new HashMap<>();
      private int totalCount = 0;

      private TagGroup(Vec3 center) {
         this.center = center;
      }

      private void addCenterSample(Vec3 pos) {
         int next = this.centerSamples + 1;
         this.center = new Vec3(
            (this.center.x * this.centerSamples + pos.x) / (double)next,
            (this.center.y * this.centerSamples + pos.y) / (double)next,
            (this.center.z * this.centerSamples + pos.z) / (double)next
         );
         this.centerSamples = next;
      }

      private void addItem(ItemStack stack) {
         if (stack == null || stack.isEmpty()) return;
         String name = stripBrackets(stack.getDisplayName().getString());
         LineData data = this.items.get(name);
         int c = stack.getCount();
         this.totalCount += c;
         if (data == null) {
            this.items.put(name, new LineData(c, isGodItem(stack)));
         } else {
            data.count += c;
            data.godItem = data.godItem || isGodItem(stack);
         }
      }

      private List<TagLine> buildLines() {
         List<TagLine> out = new ArrayList<>();
         if (this.items.isEmpty()) return out;
         List<Map.Entry<String, LineData>> entries = new ArrayList<>(this.items.entrySet());
         entries.sort(Comparator.<Map.Entry<String, LineData>>comparingInt(e -> -e.getValue().count).thenComparing(Map.Entry::getKey));

         for (Map.Entry<String, LineData> entry : entries) {
            String text = entry.getKey() + " * " + entry.getValue().count;
            Color color = entry.getValue().godItem ? Color.RED : Color.WHITE;
            out.add(new TagLine(text, color));
         }

         return out;
      }
   }

   private static class LineData {
      private int count;
      private boolean godItem;

      private LineData(int count, boolean godItem) {
         this.count = count;
         this.godItem = godItem;
      }
   }
}
