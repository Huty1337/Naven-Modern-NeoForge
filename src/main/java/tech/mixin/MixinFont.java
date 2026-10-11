package tech.mixin;

import tech.naven.utils.TextReplacer;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin({Font.class})
public class MixinFont {
   @ModifyVariable(
      method = {"width(Ljava/lang/String;)I"},
      at = @At("HEAD"),
      argsOnly = true
   )
   private String naven$protectWidthString(String text) {
      return TextReplacer.replace(text);
   }

   @ModifyVariable(
      method = {"width(Lnet/minecraft/network/chat/FormattedText;)I"},
      at = @At("HEAD"),
      argsOnly = true
   )
   private FormattedText naven$protectWidthText(FormattedText text) {
      return TextReplacer.rewriteText(text);
   }

   @ModifyVariable(
      method = {"width(Lnet/minecraft/util/FormattedCharSequence;)I"},
      at = @At("HEAD"),
      argsOnly = true
   )
   private FormattedCharSequence naven$protectWidthSequence(FormattedCharSequence text) {
      return TextReplacer.rewriteSequence(text);
   }
}
