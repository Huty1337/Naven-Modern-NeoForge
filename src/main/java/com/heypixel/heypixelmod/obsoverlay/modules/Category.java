package com.heypixel.heypixelmod.obsoverlay.modules;

import com.heypixel.heypixelmod.obsoverlay.utils.FontIcons;

public enum Category {
   COMBAT("Combat", FontIcons.SWORD),
   MOVEMENT("Movement", FontIcons.RUNNING),
   RENDER("Render", FontIcons.EYE),
   MISC("Misc", FontIcons.OTHER);

   private final String displayName;
   private final String icon;

   private Category(final String displayName, final String icon) {
      this.displayName = displayName;
      this.icon = icon;
   }

   public String getDisplayName() {
      return this.displayName;
   }

   public String getIcon() {
      return this.icon;
   }
}
