package tech.naven.modules.impl.combat;

import tech.naven.Naven;
import tech.naven.events.api.EventTarget;
import tech.naven.events.api.types.EventType;
import tech.naven.events.impl.EventAttackSlowdown;
import tech.naven.events.impl.EventClick;
import tech.naven.events.impl.EventMoveInput;
import tech.naven.events.impl.EventRender;
import tech.naven.events.impl.EventRender2D;
import tech.naven.events.impl.EventRespawn;
import tech.naven.events.impl.EventRunTicks;
import tech.naven.events.impl.EventShader;
import tech.naven.modules.Category;
import tech.naven.modules.Module;
import tech.naven.modules.ModuleInfo;
import tech.naven.modules.impl.misc.KillSay;
import tech.naven.modules.impl.misc.Teams;
import tech.naven.modules.impl.move.Blink;
import tech.naven.modules.impl.move.Stuck;
import tech.naven.modules.impl.render.HUD;
import tech.naven.modules.impl.render.Render3DUtils;
import tech.naven.utils.BlinkingPlayer;
import tech.naven.utils.ChatUtils;
import tech.naven.utils.FriendManager;
import tech.naven.utils.InventoryUtils;
import tech.naven.utils.NetworkUtils;
import tech.naven.utils.RenderUtils;
import tech.naven.utils.StencilUtils;
import tech.naven.utils.Vector2f;
import tech.naven.utils.renderer.Fonts;
import tech.naven.utils.rotation.RotationManager;
import tech.naven.utils.rotation.RotationUtils;
import tech.naven.values.ValueBuilder;
import tech.naven.values.impl.BooleanValue;
import tech.naven.values.impl.FloatValue;
import tech.naven.values.impl.ModeValue;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.joml.Vector4f;

@ModuleInfo(
   name = "KillAura",
   description = "Automatically attacks entities",
   category = Category.COMBAT
)
public class Aura extends Module {
   private static final float[] targetColorRed = new float[]{0.78431374F, 0.0F, 0.0F, 0.23529412F};
   private static final float[] targetColorGreen = new float[]{0.0F, 0.78431374F, 0.0F, 0.23529412F};
   public static Entity target;
   public static Entity aimingTarget;
   public static List<Entity> targets = new ArrayList<>();
   public static Vector2f rotation;
   BooleanValue targetHud = ValueBuilder.create(this, "Target HUD").setDefaultBooleanValue(true).build().getBooleanValue();
   BooleanValue targetEsp = ValueBuilder.create(this, "Target ESP").setDefaultBooleanValue(true).build().getBooleanValue();
   BooleanValue attackPlayer = ValueBuilder.create(this, "Attack Player").setDefaultBooleanValue(true).build().getBooleanValue();
   BooleanValue attackInvisible = ValueBuilder.create(this, "Attack Invisible").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue attackAnimals = ValueBuilder.create(this, "Attack Animals").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue attackMobs = ValueBuilder.create(this, "Attack Mobs").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue multi = ValueBuilder.create(this, "Multi Attack").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue infSwitch = ValueBuilder.create(this, "Infinity Switch").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue preferBaby = ValueBuilder.create(this, "Prefer Baby").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue moreParticles = ValueBuilder.create(this, "More Particles").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue keepSprint = ValueBuilder.create(this, "Keep Sprint").setDefaultBooleanValue(true).build().getBooleanValue();
   private boolean sprintCancelled;
   FloatValue aimRange = ValueBuilder.create(this, "Aim Range")
      .setDefaultFloatValue(5.0F)
      .setFloatStep(0.1F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(6.0F)
      .build()
      .getFloatValue();
   FloatValue aps = ValueBuilder.create(this, "Attack Per Second")
      .setDefaultFloatValue(10.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(20.0F)
      .build()
      .getFloatValue();
   FloatValue switchSize = ValueBuilder.create(this, "Switch Size")
      .setDefaultFloatValue(1.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(5.0F)
      .setVisibility(() -> !this.infSwitch.getCurrentValue())
      .build()
      .getFloatValue();
   FloatValue switchAttackTimes = ValueBuilder.create(this, "Switch Delay (Attack Times)")
      .setDefaultFloatValue(1.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(10.0F)
      .build()
      .getFloatValue();
   FloatValue fov = ValueBuilder.create(this, "FoV")
      .setDefaultFloatValue(360.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(10.0F)
      .setMaxFloatValue(360.0F)
      .build()
      .getFloatValue();
   FloatValue hurtTime = ValueBuilder.create(this, "Hurt Time")
      .setDefaultFloatValue(10.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(10.0F)
      .build()
      .getFloatValue();
   ModeValue priority = ValueBuilder.create(this, "Priority").setModes("Health", "FoV", "Range").build().getModeValue();
   RotationUtils.Data lastRotationData;
   RotationUtils.Data rotationData;
   int attackTimes = 0;
   float attacks = 0.0F;
   private int index;
   private Vector4f blurMatrix;

   @EventTarget
   public void onShader(EventShader e) {
      if (e.getType() != EventType.BLUR && e.getType() != EventType.BLUR_TARGETHUD && e.getType() != EventType.SHADOW && e.getType() != EventType.GLOW) {
         return;
      }

      if (this.blurMatrix == null || !this.targetHud.getCurrentValue()) {
         return;
      }

      float x = this.blurMatrix.x();
      float y = this.blurMatrix.y();
      float width = this.blurMatrix.z();
      float height = this.blurMatrix.w();
      if (width > 0.0F && height > 0.0F) {
         RenderUtils.drawRoundedRect(e.getStack(), x, y, width, height, 3.0F, 1073741824);
      }
   }

   @EventTarget
   public void onRender(EventRender2D e) {
      this.blurMatrix = null;
      if (target instanceof LivingEntity && this.targetHud.getCurrentValue()) {
         LivingEntity living = (LivingEntity)target;
         GuiGraphics guiGraphics = e.getGuiGraphics();
         float x = (float)mc.getWindow().getGuiScaledWidth() / 2.0F + 10.0F;
         float y = (float)mc.getWindow().getGuiScaledHeight() / 2.0F + 10.0F;
         String targetName = target.getName().getString() + (living.isBaby() ? " (Baby)" : "");
         float width = Math.max(Fonts.harmony.getWidth(targetName, 0.4F) + 10.0F, 60.0F);
         this.blurMatrix = new Vector4f(x, y, width, 30.0F);
         RenderUtils.drawRoundedRect(guiGraphics, x, y, width, 30.0F, 5.0F, HUD.bodyColor);
         float healthRatio = living.getMaxHealth() > 0.0F ? living.getHealth() / living.getMaxHealth() : 0.0F;
         StencilUtils.write(false);
         RenderUtils.drawRoundedRect(guiGraphics, x, y, width, 30.0F, 5.0F, HUD.bodyColor);
         StencilUtils.erase(true);
         RenderUtils.fillBound(
            guiGraphics, x, y, width * Math.max(0.0F, Math.min(1.0F, healthRatio)), 3.0F, HUD.headerColor
         );
         StencilUtils.dispose();
         renderScaledString(guiGraphics, targetName, x + 5.0F, y + 6.0F, 0.35F, Color.WHITE.getRGB());
         renderScaledString(
            guiGraphics,
            "HP: " + Math.round(living.getHealth()) + (living.getAbsorptionAmount() > 0.0F ? "+" + Math.round(living.getAbsorptionAmount()) : ""),
            x + 5.0F,
            y + 17.0F,
            0.35F,
            Color.WHITE.getRGB()
         );
      }
   }

   private static void renderScaledString(GuiGraphics guiGraphics, String text, float x, float y, float scale, int color) {
      if (guiGraphics == null || text == null || text.isEmpty() || scale <= 0.0F) {
         return;
      }

      Fonts.harmony.render(guiGraphics, text, (double)x, (double)y, new Color(color, true), true, (double)scale);
   }

   @EventTarget
   public void onRender(EventRender e) {
      if (this.targetEsp.getCurrentValue()) {
         PoseStack stack = e.getPMatrixStack();
         float partialTicks = e.getRenderPartialTicks();
         stack.pushPose();
         Vec3 cameraPos = RenderUtils.getCameraPos();

         for (Entity entity : targets) {
            if (entity instanceof LivingEntity living) {
               float[] color = target == living ? targetColorRed : targetColorGreen;
               stack.pushPose();
               double motionX = entity.getX() - entity.xo;
               double motionY = entity.getY() - entity.yo;
               double motionZ = entity.getZ() - entity.zo;
               AABB boundingBox = entity.getBoundingBox()
                  .move(-motionX, -motionY, -motionZ)
                  .move((double)partialTicks * motionX, (double)partialTicks * motionY, (double)partialTicks * motionZ)
                  .move(-cameraPos.x, -cameraPos.y, -cameraPos.z);
               Render3DUtils.filledBox(stack, boundingBox, color[0], color[1], color[2], 0.25F);
               Render3DUtils.outlinedBox(stack, boundingBox, color[0], color[1], color[2], 0.85F);
               stack.popPose();
            }
         }

         stack.popPose();
      }
   }

   @Override
   public void onEnable() {
      rotation = null;
      this.index = 0;
      this.sprintCancelled = false;
      target = null;
      aimingTarget = null;
      targets.clear();
   }

   @Override
   public void onDisable() {
      this.sprintCancelled = false;
      target = null;
      aimingTarget = null;
      super.onDisable();
   }

   @EventTarget
   public void onRespawn(EventRespawn e) {
      this.sprintCancelled = false;
      target = null;
      aimingTarget = null;
      this.toggle();
   }

   @EventTarget
   public void onAttackSlowdown(EventAttackSlowdown e) {
      if (!this.keepSprint.getCurrentValue()) {
         e.setCancelled(true);
      }
   }

   public void markSprintReleased() {
      this.sprintCancelled = true;
   }

   public boolean isSprintReleased() {
      return this.sprintCancelled;
   }

   public static boolean isSprintReleaseWindow() {
      return mc.player != null && !mc.player.onGround();
   }

   public static boolean shouldKeepSprintInDelayWindow() {
      return mc.player != null && mc.player.onGround();
   }

   public boolean shouldStopSprint() {
      if (!this.isEnabled() || !this.keepSprint.getCurrentValue() || this.sprintCancelled) {
         return false;
      }

      if (mc.player == null || target == null || mc.player.isUsingItem()) {
         return false;
      }

      Velocity velocity = (Velocity)Naven.getInstance().getModuleManager().getModule(Velocity.class);
      if (velocity.isInDelayWindow() && shouldKeepSprintInDelayWindow()) {
         return false;
      }

      return this.canAttackTarget() && isSprintReleaseWindow();
   }

   private boolean canAttackTarget() {
      return target != null
         && mc.player != null
         && target.getBoundingBox().distanceToSqr(mc.player.position()) <= 12.25
         && mc.player.hasLineOfSight(target);
   }

   @EventTarget
   public void onMoveInput(EventMoveInput e) {
      if (this.keepSprint.getCurrentValue() && this.sprintCancelled && mc.player != null && !mc.player.isSprinting()) {
         e.setJump(false);
      }
   }

   @EventTarget
   public void onMotion(EventRunTicks event) {
      if (event.getType() == EventType.PRE && mc.player != null) {
         if (!isSprintReleaseWindow()
            || mc.player.isInWater()
            || mc.player.isInLava()
            || mc.player.isPassenger()
            || mc.player.isFallFlying()
            || mc.player.getAbilities().flying) {
            this.sprintCancelled = false;
         }
         if (mc.screen instanceof AbstractContainerScreen
            || Naven.getInstance().getModuleManager().getModule(Stuck.class).isEnabled()
            || InventoryUtils.shouldDisableFeatures()) {
            target = null;
            aimingTarget = null;
            this.rotationData = null;
            rotation = null;
            this.lastRotationData = null;
            targets.clear();
            return;
         }

         boolean isSwitch = this.switchSize.getCurrentValue() > 1.0F;
         this.setSuffix(this.multi.getCurrentValue() ? "Multi" : (isSwitch ? "Switch" : "Single"));
         this.updateAttackTargets();
         aimingTarget = this.shouldPreAim();
         this.lastRotationData = this.rotationData;
         this.rotationData = null;
         if (aimingTarget != null) {
            this.rotationData = RotationUtils.getRotationDataToEntity(aimingTarget);
            if (this.rotationData.getRotation() != null) {
               rotation = this.rotationData.getRotation();
            } else {
               rotation = null;
            }
         }

         if (targets.isEmpty()) {
            target = null;
            return;
         }

         if (this.index > targets.size() - 1) {
            this.index = 0;
         }

         if (targets.size() > 1
            && ((float)this.attackTimes >= this.switchAttackTimes.getCurrentValue() || this.rotationData != null && this.rotationData.getDistance() > 3.0)) {
            this.attackTimes = 0;

            for (int i = 0; i < targets.size(); i++) {
               this.index++;
               if (this.index > targets.size() - 1) {
                  this.index = 0;
               }

               Entity nextTarget = targets.get(this.index);
               RotationUtils.Data data = RotationUtils.getRotationDataToEntity(nextTarget);
               if (data.getDistance() < 3.0) {
                  break;
               }
            }
         }

         if (this.index > targets.size() - 1 || !isSwitch) {
            this.index = 0;
         }

         target = targets.get(this.index);
         this.attacks = this.attacks + this.aps.getCurrentValue() / 20.0F;
      }
   }

   @EventTarget
   public void onClick(EventClick e) {
      if (mc.player.getUseItem().isEmpty()
         && mc.screen == null
         && Naven.skipTasks.isEmpty()
         && !NetworkUtils.isServerLag()
         && !Naven.getInstance().getModuleManager().getModule(Blink.class).isEnabled()) {
         while (this.attacks >= 1.0F) {
            this.doAttack();
            this.attacks--;
         }
      }
   }

   public Entity shouldPreAim() {
      Entity target = Aura.target;
      if (target == null) {
         List<Entity> aimTargets = this.getTargets();
         if (!aimTargets.isEmpty()) {
            target = aimTargets.get(0);
         }
      }

      return target;
   }

   public void doAttack() {
      if (!targets.isEmpty()) {
         HitResult hitResult = mc.hitResult;
         if (hitResult.getType() == Type.ENTITY) {
            EntityHitResult result = (EntityHitResult)hitResult;
            if (AntiBots.isBot(result.getEntity())) {
               ChatUtils.addChatMessage("Attacking Bot!");
               return;
            }
         }

         if (this.multi.getCurrentValue()) {
            int attacked = 0;

            for (Entity entity : targets) {
               if (RotationUtils.getDistance(entity, mc.player.getEyePosition(), RotationManager.rotations) < 3.0) {
                  this.attackEntity(entity);
                  if (++attacked >= 2) {
                     break;
                  }
               }
            }
         } else if (hitResult.getType() == Type.ENTITY) {
            EntityHitResult result = (EntityHitResult)hitResult;
            this.attackEntity(result.getEntity());
         }
      }
   }

   public void updateAttackTargets() {
      targets = this.getTargets();
   }

   public boolean isValidTarget(Entity entity) {
      if (entity == mc.player) {
         return false;
      } else if (entity instanceof LivingEntity living) {
         if (living instanceof BlinkingPlayer) {
            return false;
         } else {
            AntiBots module = (AntiBots)Naven.getInstance().getModuleManager().getModule(AntiBots.class);
            if (module == null || !module.isEnabled() || !AntiBots.isBot(entity) && !AntiBots.isBedWarsBot(entity)) {
               if (Teams.isSameTeam(living)) {
                  return false;
               } else if (FriendManager.isFriend(living)) {
                  return false;
               } else if (living.isDeadOrDying() || living.getHealth() <= 0.0F) {
                  return false;
               } else if (entity instanceof ArmorStand) {
                  return false;
               } else if (entity.isInvisible() && !this.attackInvisible.getCurrentValue()) {
                  return false;
               } else if (entity instanceof Player && !this.attackPlayer.getCurrentValue()) {
                  return false;
               } else if (!(entity instanceof Player) || !((double)entity.getBbWidth() < 0.5) && !living.isSleeping()) {
                  if ((entity instanceof Mob || entity instanceof Slime || entity instanceof Bat || entity instanceof AbstractGolem)
                     && !this.attackMobs.getCurrentValue()) {
                     return false;
                  } else if ((entity instanceof Animal || entity instanceof Squid) && !this.attackAnimals.getCurrentValue()) {
                     return false;
                  } else {
                     return entity instanceof Villager && !this.attackAnimals.getCurrentValue() ? false : !(entity instanceof Player) || !entity.isSpectator();
                  }
               } else {
                  return false;
               }
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   public boolean isValidAttack(Entity entity) {
      if (!this.isValidTarget(entity)) {
         return false;
      } else if (entity instanceof LivingEntity && (float)((LivingEntity)entity).hurtTime > this.hurtTime.getCurrentValue()) {
         return false;
      } else {
         Vec3 closestPoint = RotationUtils.getClosestPoint(mc.player.getEyePosition(), entity.getBoundingBox());
         return closestPoint.distanceTo(mc.player.getEyePosition()) > (double)this.aimRange.getCurrentValue()
            ? false
            : RotationUtils.inFoV(entity, this.fov.getCurrentValue() / 2.0F);
      }
   }

   public void attackEntity(Entity entity) {
      this.attackTimes++;
      float currentYaw = mc.player.getYRot();
      float currentPitch = mc.player.getXRot();
      mc.player.setYRot(RotationManager.rotations.x);
      mc.player.setXRot(RotationManager.rotations.y);
      if (entity instanceof Player && !AntiBots.isBot(entity)) {
         KillSay.attackedPlayers.add(entity.getName().getString());
      }

      mc.gameMode.attack(mc.player, entity);
      mc.player.swing(InteractionHand.MAIN_HAND);
      if (this.moreParticles.getCurrentValue()) {
         mc.player.magicCrit(entity);
         mc.player.crit(entity);
      }

      mc.player.setYRot(currentYaw);
      mc.player.setXRot(currentPitch);
   }

   private List<Entity> getTargets() {
      Stream<Entity> stream = StreamSupport.<Entity>stream(mc.level.entitiesForRendering().spliterator(), true)
         .filter(entity -> entity instanceof Entity)
         .filter(this::isValidAttack);
      List<Entity> possibleTargets = stream.collect(Collectors.toList());
      if (this.priority.isCurrentMode("Range")) {
         possibleTargets.sort(Comparator.comparingDouble(o -> (double)o.distanceTo(mc.player)));
      } else if (this.priority.isCurrentMode("FoV")) {
         possibleTargets.sort(
            Comparator.comparingDouble(o -> (double)RotationUtils.getDistanceBetweenAngles(RotationManager.rotations.x, RotationUtils.getRotations(o).x))
         );
      } else if (this.priority.isCurrentMode("Health")) {
         possibleTargets.sort(Comparator.comparingDouble(o -> o instanceof LivingEntity living ? (double)living.getHealth() : 0.0));
      }

      if (this.preferBaby.getCurrentValue() && possibleTargets.stream().anyMatch(entity -> entity instanceof LivingEntity && ((LivingEntity)entity).isBaby())) {
         possibleTargets.removeIf(entity -> !(entity instanceof LivingEntity) || !((LivingEntity)entity).isBaby());
      }

      possibleTargets.sort(Comparator.comparing(o -> o instanceof EndCrystal ? 0 : 1));
      return this.infSwitch.getCurrentValue()
         ? possibleTargets
         : possibleTargets.subList(0, (int)Math.min((float)possibleTargets.size(), this.switchSize.getCurrentValue()));
   }
}
