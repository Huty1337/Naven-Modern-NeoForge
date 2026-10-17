package tech.naven.utils.rotation;

import tech.naven.Naven;
import tech.naven.events.api.EventTarget;
import tech.naven.events.api.types.EventType;
import tech.naven.events.impl.EventAttackYaw;
import tech.naven.events.impl.EventFallFlying;
import tech.naven.events.impl.EventJump;
import tech.naven.events.impl.EventMotion;
import tech.naven.events.impl.EventMoveInput;
import tech.naven.events.impl.EventPositionItem;
import tech.naven.events.impl.EventRayTrace;
import tech.naven.events.impl.EventRespawn;
import tech.naven.events.impl.EventRotationAnimation;
import tech.naven.events.impl.EventRunTicks;
import tech.naven.events.impl.EventStrafe;
import tech.naven.events.impl.EventUseItemRayTrace;
import tech.naven.modules.impl.combat.AimAssist;
import tech.naven.modules.impl.combat.AttackCrystal;
import tech.naven.modules.impl.combat.Aura;
import tech.naven.modules.impl.move.AutoMLG;
import tech.naven.modules.impl.move.LongJump;
import tech.naven.modules.impl.move.Scaffold;
import tech.naven.utils.MoveUtils;
import tech.naven.utils.Vector2f;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RotationManager {
   private static final Logger log = LogManager.getLogger(RotationManager.class);
   private static final Minecraft mc = Minecraft.getInstance();
   public static Vector2f rotations;
   public static Vector2f lastRotations;
   public static Vector2f animationRotation;
   public static Vector2f lastAnimationRotation;
   public static boolean active = false;

   public static void setRotations(Vector2f rotations) {
      RotationManager.rotations = rotations;
   }

   @EventTarget
   public void onRespawn(EventRespawn e) {
      lastRotations = null;
      rotations = null;
   }

   @EventTarget(4)
   public void updateGlobalYaw(EventRunTicks e) {
      if (e.getType() == EventType.PRE && mc.player != null) {
         Aura aura = (Aura)Naven.getInstance().getModuleManager().getModule(Aura.class);
         Scaffold scaffold = (Scaffold)Naven.getInstance().getModuleManager().getModule(Scaffold.class);
         AttackCrystal attackCrystal = (AttackCrystal)Naven.getInstance().getModuleManager().getModule(AttackCrystal.class);
         AutoMLG autoMLG = (AutoMLG)Naven.getInstance().getModuleManager().getModule(AutoMLG.class);
         AimAssist aimAssist = (AimAssist)Naven.getInstance().getModuleManager().getModule(AimAssist.class);
         LongJump longJump = (LongJump)Naven.getInstance().getModuleManager().getModule(LongJump.class);
         tech.naven.modules.impl.combat.AutoThrow autoThrow = (tech.naven.modules.impl.combat.AutoThrow)Naven
            .getInstance()
            .getModuleManager()
            .getModule(tech.naven.modules.impl.combat.AutoThrow.class);
         tech.naven.modules.impl.misc.AutoHelper autoHelper = (tech.naven.modules.impl.misc.AutoHelper)Naven
            .getInstance()
            .getModuleManager()
            .getModule(tech.naven.modules.impl.misc.AutoHelper.class);
         active = true;
         if (autoMLG.isEnabled() && autoMLG.rotation) {
            setRotations(new Vector2f(mc.player.getYRot(), 90.0F));
         } else if (longJump.isEnabled() && LongJump.rotation != null) {
            setRotations(LongJump.rotation.toVec2f());
         } else if (attackCrystal.isEnabled() && AttackCrystal.rotations != null) {
            setRotations(new Vector2f(AttackCrystal.rotations.x, AttackCrystal.rotations.y));
         } else if (scaffold.isEnabled() && scaffold.rots != null) {
            setRotations(new Vector2f(scaffold.rots.x, scaffold.rots.y));
         } else if (autoHelper.isEnabled() && autoHelper.rotation != null) {
            setRotations(new Vector2f(autoHelper.rotation.x, autoHelper.rotation.y));
         } else if (autoThrow.isEnabled() && autoThrow.rotation != null) {
            setRotations(new Vector2f(autoThrow.rotation.getX(), autoThrow.rotation.getY()));
         } else if (aura.isEnabled() && Aura.target != null && Aura.rotation != null) {
            setRotations(new Vector2f(Aura.rotation.x, Aura.rotation.y));
         } else if (!aimAssist.isEnabled() || !aimAssist.working) {
            active = false;
         } else if (aimAssist.slientaim) {
            setRotations(new Vector2f(aimAssist.targetRotation.x, aimAssist.targetRotation.y));
         }
      }
   }

   @EventTarget
   public void onAnimation(EventRotationAnimation e) {
      if (animationRotation != null && lastAnimationRotation != null) {
         e.setYaw(animationRotation.x);
         e.setLastYaw(lastAnimationRotation.x);
         e.setPitch(animationRotation.y);
         e.setLastPitch(lastAnimationRotation.y);
      }
   }

   @EventTarget(4)
   public void onPre(EventMotion e) {
      if (e.getType() == EventType.PRE) {
         if (rotations == null || lastRotations == null) {
            rotations = lastRotations = new Vector2f(mc.player.getYRot(), mc.player.getXRot());
         }

         lastAnimationRotation = animationRotation;
         float yaw = rotations.x;
         float pitch = rotations.y;
         if (!Float.isNaN(yaw) && !Float.isNaN(pitch) && active) {
            e.setYaw(yaw);
            e.setPitch(pitch);
         }

         Scaffold scaffold = (Scaffold)Naven.getInstance().getModuleManager().getModule(Scaffold.class);
         if (scaffold.isEnabled() && scaffold.mode.isCurrentMode("Normal") && scaffold.snap.getCurrentValue() && scaffold.hideSnap.getCurrentValue()) {
            animationRotation = scaffold.correctRotation;
         } else {
            animationRotation = new Vector2f(e.getYaw(), e.getPitch());
         }

         lastRotations = new Vector2f(e.getYaw(), e.getPitch());
      }
   }

   @EventTarget
   public void onMove(EventMoveInput event) {
      if (active && rotations != null) {
         float yaw = rotations.x;
         MoveUtils.fixMovement(event, yaw);
      }
   }

   @EventTarget
   public void onMove(EventRayTrace event) {
      if (rotations != null && event.entity == mc.player && active) {
         event.setYaw(rotations.x);
         event.setPitch(rotations.y);
      }
   }

   @EventTarget
   public void onItemRayTrace(EventUseItemRayTrace event) {
      if (rotations != null && active) {
         event.setYaw(rotations.x);
         event.setPitch(rotations.y);
      }
   }

   @EventTarget
   public void onStrafe(EventStrafe event) {
      if (active && rotations != null) {
         event.setYaw(rotations.x);
      }
   }

   @EventTarget
   public void onJump(EventJump event) {
      if (active && rotations != null) {
         event.setYaw(rotations.x);
      }
   }

   @EventTarget(0)
   public void onPositionItem(EventPositionItem e) {
      if (!active || rotations == null) {
         return;
      }

      if (e.getPacket() instanceof PosRot packet) {
         e.setPacket(
            new PosRot(packet.getX(0.0), packet.getY(0.0), packet.getZ(0.0), rotations.getX(), rotations.getY(), packet.isOnGround(), packet.horizontalCollision())
         );
      } else if (e.getPacket() instanceof ServerboundUseItemPacket useItem) {
         e.setPacket(new ServerboundUseItemPacket(useItem.getHand(), useItem.getSequence(), rotations.getX(), rotations.getY()));
      }
   }

   @EventTarget
   public void onFallFlying(EventFallFlying e) {
      if (rotations != null) {
         e.setPitch(rotations.y);
      }
   }

   @EventTarget
   public void onAttack(EventAttackYaw e) {
      if (rotations != null) {
         e.setYaw(rotations.x);
      }
   }
}
