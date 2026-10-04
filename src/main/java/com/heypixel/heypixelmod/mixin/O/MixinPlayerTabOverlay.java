package com.heypixel.heypixelmod.mixin.O;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRenderTabOverlay;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({PlayerTabOverlay.class})
public abstract class MixinPlayerTabOverlay {
   @Shadow
   public abstract Component getNameForDisplay(PlayerInfo var1);

   @Redirect(
      method = {"render"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/Font;split(Lnet/minecraft/network/chat/FormattedText;I)Ljava/util/List;",
         ordinal = 0
      )
   )
   public List<FormattedCharSequence> hookHeader(Font instance, FormattedText pText, int pMaxWidth) {
      Component component = (Component)pText;
      EventRenderTabOverlay event = new EventRenderTabOverlay(EventType.HEADER, component);
      Naven.getInstance().getEventManager().call(event);
      return instance.split(event.getComponent(), pMaxWidth);
   }

   @Redirect(
      method = {"render"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/Font;split(Lnet/minecraft/network/chat/FormattedText;I)Ljava/util/List;",
         ordinal = 1
      )
   )
   public List<FormattedCharSequence> hookFooter(Font instance, FormattedText pText, int pMaxWidth) {
      Component component = (Component)pText;
      EventRenderTabOverlay event = new EventRenderTabOverlay(EventType.FOOTER, component);
      Naven.getInstance().getEventManager().call(event);
      return instance.split(event.getComponent(), pMaxWidth);
   }

   @Redirect(
      method = {"render"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/components/PlayerTabOverlay;getNameForDisplay(Lnet/minecraft/client/multiplayer/PlayerInfo;)Lnet/minecraft/network/chat/Component;"
      )
   )
   public Component hookName(PlayerTabOverlay instance, PlayerInfo pPlayerInfo) {
      Component nameForDisplay = this.getNameForDisplay(pPlayerInfo);
      EventRenderTabOverlay event = new EventRenderTabOverlay(EventType.NAME, nameForDisplay);
      Naven.getInstance().getEventManager().call(event);
      return event.getComponent();
   }
}
