package tech.mixin;

import tech.naven.Naven;
import tech.naven.events.api.types.EventType;
import tech.naven.events.impl.EventAttack;
import tech.naven.events.impl.EventDestroyBlock;
import tech.naven.events.impl.EventPositionItem;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({MultiPlayerGameMode.class})
public class MixinMultiPlayerGameMode {
   @Redirect(
      method = {"startPrediction"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V"
      )
   )
   public void onSendPacket(ClientPacketListener instance, Packet<?> pPacket) {
      if (pPacket instanceof ServerboundUseItemPacket) {
         EventPositionItem event = new EventPositionItem(pPacket);
         Naven.getInstance().getEventManager().call(event);
         if (event.isCancelled()) {
            return;
         }

         instance.send(event.getPacket());
      } else {
         instance.send(pPacket);
      }
   }

   @Inject(
      method = {"startDestroyBlock"},
      at = {@At("HEAD")}
   )
   public void onStartDestroyBlock(BlockPos pLoc, Direction pFace, CallbackInfoReturnable<Boolean> cir) {
      Naven.getInstance().getEventManager().call(new EventDestroyBlock(pLoc, pFace));
   }

   @Inject(
      method = {"attack(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void onAttackPre(Player pPlayer, Entity pTarget, CallbackInfo ci) {
      EventAttack event = new EventAttack(pTarget, EventType.PRE);
      Naven.getInstance().getEventManager().call(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"attack(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;)V"},
      at = {@At("RETURN")}
   )
   public void onAttackPost(Player pPlayer, Entity pTarget, CallbackInfo ci) {
      Naven.getInstance().getEventManager().call(new EventAttack(pTarget, EventType.POST));
   }
}
