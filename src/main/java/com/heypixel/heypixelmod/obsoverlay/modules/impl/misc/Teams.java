package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import java.util.Objects;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

@ModuleInfo(
   name = "Teams",
   description = "Prevent attack teammates",
   category = Category.MISC
)
public class Teams extends Module {
   public static Teams instance;
   public ModeValue mode = ValueBuilder.create(this, "Mode").setDefaultModeIndex(0).setModes("Scoreboard", "Color").build().getModeValue();

   public Teams() {
      instance = this;
   }

   public static boolean isSameTeam(Entity player) {
      if (!Naven.getInstance().getModuleManager().getModule(Teams.class).isEnabled()) {
         return false;
      } else if (player instanceof Player) {
         if (instance.mode.isCurrentMode("Color")) {
            Integer c1 = player.getTeamColor();
            Integer c2 = mc.player.getTeamColor();
            return c1.equals(c2);
         } else {
            String playerTeam = getTeam(player);
            String targetTeam = getTeam(mc.player);
            return Objects.equals(playerTeam, targetTeam);
         }
      } else {
         return false;
      }
   }

   public static String getTeam(Entity entity) {
      PlayerInfo playerInfo = mc.getConnection().getPlayerInfo(entity.getUUID());
      if (playerInfo == null) {
         return null;
      } else {
         return playerInfo.getTeam() != null ? playerInfo.getTeam().getName() : null;
      }
   }
}
