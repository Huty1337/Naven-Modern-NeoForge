package tech.mixin;

import tech.naven.modules.impl.render.NameProtect;
import tech.naven.utils.TextReplacer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin({GuiGraphics.class})
public class MixinGuiGraphics {
   @ModifyVariable(
      method = {"drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V"},
      at = @At("HEAD"),
      argsOnly = true
   )
   private String naven$protectDrawString(String text) {
      return TextReplacer.replace(text);
   }

   @ModifyVariable(
      method = {"drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V"},
      at = @At("HEAD"),
      argsOnly = true
   )
   private Component naven$protectDrawComponent(Component text) {
      return NameProtect.rewriteComponent(text);
   }

   @ModifyVariable(
      method = {"drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V"},
      at = @At("HEAD"),
      argsOnly = true
   )
   private FormattedCharSequence naven$protectDrawSequence(FormattedCharSequence text) {
      return TextReplacer.rewriteSequence(text);
   }
}
