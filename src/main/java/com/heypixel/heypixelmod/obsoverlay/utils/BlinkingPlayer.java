package com.heypixel.heypixelmod.obsoverlay.utils;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerSkin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BlinkingPlayer extends RemotePlayer {
   private final AbstractClientPlayer player;

   public BlinkingPlayer(AbstractClientPlayer player) {
      super(Minecraft.getInstance().level, new GameProfile(UUID.randomUUID(), "Real Position"));
      this.player = player;
      this.copyPosition(player);
      this.noPhysics = true;
      this.yRotO = this.getYRot();
      this.xRotO = this.getXRot();
      this.yHeadRot = player.yHeadRot;
      this.yBodyRot = player.yBodyRot;
      this.yHeadRotO = this.yHeadRot;
      this.yBodyRotO = this.yBodyRot;
      Byte playerModel = (Byte)player.getEntityData().get(Player.DATA_PLAYER_MODE_CUSTOMISATION);
      this.entityData.set(Player.DATA_PLAYER_MODE_CUSTOMISATION, playerModel);
   }

   public boolean isSkinLoaded() {
      return true;
   }

   @NotNull
   public ResourceLocation getSkinTextureLocation() {
      return this.player.getSkin().body().texturePath();
   }

   public boolean isCapeLoaded() {
      return this.player.getSkin().cape() != null;
   }

   @Nullable
   public ResourceLocation getCloakTextureLocation() {
      ClientAsset.Texture cape = this.player.getSkin().cape();
      return cape != null ? cape.texturePath() : null;
   }

   @Override
   public PlayerSkin getSkin() {
      return this.player.getSkin();
   }
}
