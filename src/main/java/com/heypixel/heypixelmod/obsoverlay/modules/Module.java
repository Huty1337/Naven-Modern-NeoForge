package com.heypixel.heypixelmod.obsoverlay.modules;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.ClickGUIModule;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.HUD;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.Notification;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.NotificationLevel;
import com.heypixel.heypixelmod.obsoverlay.utils.SmoothAnimationTimer;
import com.heypixel.heypixelmod.obsoverlay.values.HasValue;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;

public class Module extends HasValue {
   protected static final Minecraft mc = Minecraft.getInstance();
   public static boolean update = true;
   private final SmoothAnimationTimer animation = new SmoothAnimationTimer(100.0F);
   private String name;
   private String prettyName;
   private String description;
   private String suffix;
   private Category category;
   private boolean enabled;
   private int minPermission = 0;
   private int key;

   public Module(String name, String description, Category category) {
      this.name = name;
      this.description = description;
      this.category = category;
      super.setName(name);
      this.setPrettyName();
   }

   public void setSuffix(String suffix) {
      if (suffix == null) {
         this.suffix = null;
         update = true;
      } else if (!suffix.equals(this.suffix)) {
         this.suffix = suffix;
         update = true;
      }
   }

   private void setPrettyName() {
      StringBuilder builder = new StringBuilder();
      char[] chars = this.name.toCharArray();

      for (int i = 0; i < chars.length - 1; i++) {
         if (Character.isLowerCase(chars[i]) && Character.isUpperCase(chars[i + 1])) {
            builder.append(chars[i]).append(" ");
         } else {
            builder.append(chars[i]);
         }
      }

      builder.append(chars[chars.length - 1]);
      this.prettyName = builder.toString();
   }

   protected void initModule() {
      if (this.getClass().isAnnotationPresent(ModuleInfo.class)) {
         ModuleInfo moduleInfo = this.getClass().getAnnotation(ModuleInfo.class);
         this.name = moduleInfo.name();
         this.description = moduleInfo.description();
         this.category = moduleInfo.category();
         super.setName(this.name);
         this.setPrettyName();
         Naven.getInstance().getHasValueManager().registerHasValue(this);
      }
   }

   public void onEnable() {
   }

   public void onDisable() {
   }

   public void setEnabled(boolean enabled) {
      try {
         Naven naven = Naven.getInstance();
         if (enabled) {
            this.enabled = true;
            naven.getEventManager().register(this);
            this.onEnable();
            if (!(this instanceof ClickGUIModule)) {
               HUD module = (HUD)Naven.getInstance().getModuleManager().getModule(HUD.class);
               if (module.moduleToggleSound.getCurrentValue()) {
                  mc.player.playSound(SoundEvents.WOODEN_BUTTON_CLICK_ON, 0.5F, 1.3F);
               }

               Notification notification = new Notification(NotificationLevel.SUCCESS, this.name + " Enabled!", 3000L);
               naven.getNotificationManager().addNotification(notification);
            }
         } else {
            this.enabled = false;
            naven.getEventManager().unregister(this);
            this.onDisable();
            if (!(this instanceof ClickGUIModule)) {
               HUD module = (HUD)Naven.getInstance().getModuleManager().getModule(HUD.class);
               if (module.moduleToggleSound.getCurrentValue()) {
                  mc.player.playSound(SoundEvents.WOODEN_BUTTON_CLICK_OFF, 0.5F, 0.8F);
               }

               Notification notification = new Notification(NotificationLevel.ERROR, this.name + " Disabled!", 3000L);
               naven.getNotificationManager().addNotification(notification);
            }
         }
      } catch (Exception var5) {
      }
   }

   public void toggle() {
      this.setEnabled(!this.enabled);
   }

   public SmoothAnimationTimer getAnimation() {
      return this.animation;
   }

   @Override
   public String getName() {
      return this.name;
   }

   public String getPrettyName() {
      return this.prettyName;
   }

   public String getDescription() {
      return this.description;
   }

   public String getSuffix() {
      return this.suffix;
   }

   public Category getCategory() {
      return this.category;
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public int getMinPermission() {
      return this.minPermission;
   }

   public int getKey() {
      return this.key;
   }

   public Module() {
   }

   public void setMinPermission(int minPermission) {
      this.minPermission = minPermission;
   }

   public void setKey(int key) {
      this.key = key;
   }
}
