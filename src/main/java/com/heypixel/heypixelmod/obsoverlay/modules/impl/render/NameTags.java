package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMouseClick;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventShader;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.ClientFriend;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.Teams;
import com.heypixel.heypixelmod.obsoverlay.utils.BlinkingPlayer;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.EntityWatcher;
import com.heypixel.heypixelmod.obsoverlay.utils.FriendManager;
import com.heypixel.heypixelmod.obsoverlay.utils.InventoryUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.MathUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.ProjectionUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.SharedESPData;
import com.heypixel.heypixelmod.obsoverlay.utils.StencilUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.Vector2f;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.text.CustomTextRenderer;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationUtils;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4f;

@ModuleInfo(
        name = "NameTags",
        category = Category.RENDER,
        description = "Renders name tags"
)
public class NameTags extends Module {
    public BooleanValue mcf = ValueBuilder.create(this, "Middle Click Friend").setDefaultBooleanValue(true).build().getBooleanValue();
    public BooleanValue showCompassPosition = ValueBuilder.create(this, "Compass Position").setDefaultBooleanValue(true).build().getBooleanValue();
    public BooleanValue compassOnly = ValueBuilder.create(this, "Compass Only")
            .setDefaultBooleanValue(true)
            .setVisibility(() -> this.showCompassPosition.getCurrentValue())
            .build()
            .getBooleanValue();
    public BooleanValue noPlayerOnly = ValueBuilder.create(this, "No Player Only")
            .setDefaultBooleanValue(true)
            .setVisibility(() -> this.showCompassPosition.getCurrentValue())
            .build()
            .getBooleanValue();
    public BooleanValue shared = ValueBuilder.create(this, "Shared ESP").setDefaultBooleanValue(true).build().getBooleanValue();
    public BooleanValue showPlayerEquipment = ValueBuilder.create(this, "Show Player Equipment").setDefaultBooleanValue(false).build().getBooleanValue();
    public FloatValue scale = ValueBuilder.create(this, "Scale")
            .setDefaultFloatValue(0.3F)
            .setFloatStep(0.01F)
            .setMinFloatValue(0.1F)
            .setMaxFloatValue(0.5F)
            .build()
            .getFloatValue();
    private static final int color1 = new Color(0, 0, 0, 25).getRGB();
    private static final int color2 = new Color(0, 0, 0, 55).getRGB();
    private static final int navenColor1 = new Color(0, 0, 0, 40).getRGB();
    private static final int navenColor2 = new Color(0, 0, 0, 80).getRGB();
    private final Map<Entity, Vector2f> entityPositions = new ConcurrentHashMap<>();
    private final List<NameTags.NameTagData> sharedPositions = new CopyOnWriteArrayList<>();
    List<Vector4f> blurMatrices = new ArrayList<>();
    private BlockPos spawnPosition;
    private Vector2f compassPosition;
    private final Map<Player, Integer> aimTicks = new ConcurrentHashMap<>();
    private Player aimingPlayer;

    private boolean hasPlayer() {
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity != mc.player && !(entity instanceof BlinkingPlayer) && entity instanceof Player) {
                return true;
            }
        }

        return false;
    }

    private BlockPos getSpawnPosition(ClientLevel p_117922_) {
        return p_117922_.dimensionType().natural() ? p_117922_.getRespawnData().pos() : null;
    }

    @EventTarget
    public void onMotion(EventMotion e) {
        if (e.getType() == EventType.PRE) {
            if (!this.mcf.getCurrentValue()) {
                this.aimingPlayer = null;
            } else {
                for (Player player : mc.level.players()) {
                    if (!(player instanceof BlinkingPlayer) && player != mc.player) {
                        if (isAiming(player, mc.player.getYRot(), mc.player.getXRot())) {
                            if (this.aimTicks.containsKey(player)) {
                                this.aimTicks.put(player, this.aimTicks.get(player) + 1);
                            } else {
                                this.aimTicks.put(player, 1);
                            }

                            if (this.aimTicks.get(player) >= 10) {
                                this.aimingPlayer = player;
                                break;
                            }
                        } else if (this.aimTicks.containsKey(player) && this.aimTicks.get(player) > 0) {
                            this.aimTicks.put(player, this.aimTicks.get(player) - 1);
                        } else {
                            this.aimTicks.put(player, 0);
                        }
                    }
                }

                if (this.aimingPlayer != null && this.aimTicks.containsKey(this.aimingPlayer) && this.aimTicks.get(this.aimingPlayer) <= 0) {
                    this.aimingPlayer = null;
                }
            }

            this.spawnPosition = null;
            if (!InventoryUtils.hasItem(Items.COMPASS) && this.compassOnly.getCurrentValue()) {
                return;
            }

            if (this.hasPlayer() && this.noPlayerOnly.getCurrentValue()) {
                return;
            }

            this.spawnPosition = this.getSpawnPosition(mc.level);
        }
    }

    public static boolean isAiming(Entity targetEntity, float yaw, float pitch) {
        Vec3 playerEye = new Vec3(mc.player.getX(), mc.player.getY() + (double)mc.player.getEyeHeight(), mc.player.getZ());
        HitResult intercept = RotationUtils.getIntercept(targetEntity.getBoundingBox(), new Vector2f(yaw, pitch), playerEye, 150.0);
        if (intercept == null) {
            return false;
        } else {
            return intercept.getType() != Type.ENTITY ? false : intercept.getLocation().distanceTo(playerEye) < 150.0;
        }
    }

    private static void addIfNotEmpty(List<ItemStack> items, ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            items.add(stack);
        }
    }

    private static boolean isFinite(float value) {
        return !Float.isNaN(value) && !Float.isInfinite(value);
    }

    private static boolean isValidScreenPos(Vector2f pos) {
        if (pos == null) {
            return false;
        }

        float x = pos.getX();
        float y = pos.getY();
        if (!isFinite(x) || !isFinite(y)) {
            return false;
        }

        return Math.abs(x) < 1.0E6F && Math.abs(y) < 1.0E6F;
    }

    private static ItemStack safeCopy(ItemStack stack) {
        return stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
    }

    private static void resetGuiRenderState() {
    }

    private void renderEquipment(GuiGraphics guiGraphics, Player player, float centerX, float tagTopY) {
        List<ItemStack> items = new ArrayList<>();
        addIfNotEmpty(items, safeCopy(player.getMainHandItem()));
        addIfNotEmpty(items, safeCopy(player.getItemBySlot(EquipmentSlot.HEAD)));
        addIfNotEmpty(items, safeCopy(player.getItemBySlot(EquipmentSlot.CHEST)));
        addIfNotEmpty(items, safeCopy(player.getItemBySlot(EquipmentSlot.LEGS)));
        addIfNotEmpty(items, safeCopy(player.getItemBySlot(EquipmentSlot.FEET)));
        addIfNotEmpty(items, safeCopy(player.getOffhandItem()));

        if (items.isEmpty()) {
            return;
        }

        float itemScale = 1.0F;
        float itemSize = 16.0F;
        float spacing = 2.0F;
        float rowWidth = items.size() * itemSize + (items.size() - 1) * spacing;

        float startX = centerX - rowWidth / 2.0F;
        float y = tagTopY - itemSize - 3.0F * itemScale;
        if (!isFinite(startX) || !isFinite(y) || Math.abs(startX) > 1.0E6F || Math.abs(y) > 1.0E6F) {
            return;
        }

        float x = startX;
        for (ItemStack stack : items) {
            if (stack.isEmpty()) {
                x += itemSize + spacing;
                continue;
            }

            float drawXF = x;
            float drawYF = y;
            if (!isFinite(drawXF) || !isFinite(drawYF)) {
                x += itemSize + spacing;
                continue;
            }
            int drawX = Mth.floor(drawXF);
            int drawY = Mth.floor(drawYF);

            try {
                guiGraphics.renderItem(stack, drawX, drawY);
                guiGraphics.renderItemDecorations(mc.font, stack, drawX, drawY);
            } catch (Throwable ignored) {
                resetGuiRenderState();
            } finally {
                resetGuiRenderState();
            }

            x += itemSize + spacing;
        }

        resetGuiRenderState();
    }

    @EventTarget
    public void onShader(EventShader e) {
        if (e.getType() != EventType.BLUR && e.getType() != EventType.SHADOW && e.getType() != EventType.GLOW) {
            return;
        }
        float screenW = (float)mc.getWindow().getGuiScaledWidth();
        float screenH = (float)mc.getWindow().getGuiScaledHeight();

        for (Vector4f blurMatrix : this.blurMatrices) {
            float x1 = blurMatrix.x();
            float y1 = blurMatrix.y();
            float x2 = blurMatrix.z();
            float y2 = blurMatrix.w();

            if (!isFinite(x1) || !isFinite(y1) || !isFinite(x2) || !isFinite(y2)) {
                continue;
            }

            x1 = Mth.clamp(x1, 0.0F, screenW);
            y1 = Mth.clamp(y1, 0.0F, screenH);
            x2 = Mth.clamp(x2, 0.0F, screenW);
            y2 = Mth.clamp(y2, 0.0F, screenH);

            if (Math.abs(x2 - x1) < 0.5F || Math.abs(y2 - y1) < 0.5F) {
                continue;
            }

            RenderUtils.fill(e.getStack(), x1, y1, x2, y2, 1073741824);
        }
    }

    @EventTarget
    public void update(EventRender e) {
        try {
            this.updatePositions(e.getRenderPartialTicks());
            this.compassPosition = null;
            if (this.spawnPosition != null) {
                Vector2f projected = ProjectionUtils.project(
                        (double)this.spawnPosition.getX() + 0.5,
                        (double)this.spawnPosition.getY() + 1.75,
                        (double)this.spawnPosition.getZ() + 0.5,
                        e.getRenderPartialTicks()
                );
                if (isValidScreenPos(projected)) {
                    this.compassPosition = projected;
                }
            }
        } catch (Exception var3) {
        }
    }

    @EventTarget
    public void onMouseKey(EventMouseClick e) {
        if (e.getKey() == 2 && !e.isState() && this.mcf.getCurrentValue() && this.aimingPlayer != null) {
            if (FriendManager.isFriend(this.aimingPlayer)) {
                        ChatUtils.addChatMessage("Removed " + this.aimingPlayer.getName().getString() + " from friends!");
                FriendManager.removeFriend(this.aimingPlayer);
            } else {
                ChatUtils.addChatMessage("Added " + this.aimingPlayer.getName().getString() + " as friends!");
                FriendManager.addFriend(this.aimingPlayer);
            }
        }
    }

    @EventTarget
    public void onRender(EventRender2D e) {
        if (this.isModernHudStyle()) {
            this.onRenderModern(e);
        } else {
            this.onRenderNaven(e);
        }
    }

    private boolean isModernHudStyle() {
        return true;
    }

    private void onRenderModern(EventRender2D e) {
        GuiGraphics guiGraphics = e.getGuiGraphics();
        this.blurMatrices.clear();
        if (this.compassPosition != null) {
            Vector2f position = this.compassPosition;
            float scale = Math.max(
                    80.0F
                            - Mth.sqrt(
                            (float)mc.player
                                    .distanceToSqr(
                                            (double)this.spawnPosition.getX() + 0.5, (double)this.spawnPosition.getY() + 1.75, (double)this.spawnPosition.getZ() + 0.5
                                    )
                    ),
                    0.0F
            )
                    * this.scale.getCurrentValue()
                    / 80.0F;
            String text = "Compass";
            float width = Fonts.harmony.getWidth(text, (double)scale);
            double height = Fonts.harmony.getHeight(true, (double)scale);
            float topY = position.y - 3.0F;
            float bottomY = (float)((double)position.y + height) + 1.0F;
            this.blurMatrices
                    .add(new Vector4f(position.x - width / 2.0F - 2.0F, topY, position.x + width / 2.0F + 2.0F, bottomY));
            StencilUtils.write(false);
            try {
                RenderUtils.fill(
                        guiGraphics, position.x - width / 2.0F - 2.0F, topY, position.x + width / 2.0F + 2.0F, bottomY, -1
                );
                StencilUtils.erase(true);
                RenderUtils.fill(
                        guiGraphics, position.x - width / 2.0F - 2.0F, topY, position.x + width / 2.0F + 2.0F, bottomY, color1
                );
            } finally {
                resetGuiRenderState();
                StencilUtils.dispose();
            }
            Fonts.harmony.setAlpha(0.8F);
            Fonts.harmony.render(guiGraphics, text, (double)(position.x - width / 2.0F), (double)(position.y - 1.0F), Color.WHITE, true, (double)scale);
        }

        for (Entry<Entity, Vector2f> entry : this.entityPositions.entrySet()) {
            if (entry.getKey() != mc.player && entry.getKey() instanceof Player) {
                Player living = (Player)entry.getKey();
                guiGraphics.pose().pushMatrix();
                float hp = living.getHealth();
                if (hp > 20.0F) {
                    living.setHealth(20.0F);
                }

                Vector2f position = entry.getValue();
                String name = living.getName().getString();
                String displayName = name.replace(mc.player.getName().getString(), "§dHidden§7");
                ClientFriend clientFriend = (ClientFriend)Naven.getInstance().getModuleManager().getModule(ClientFriend.class);
                boolean clientFriendEnabled = clientFriend != null && clientFriend.isEnabled();

                float scale = this.scale.getCurrentValue();
                CustomTextRenderer font = Fonts.harmony;
                double fontScale = (double) scale;

                double distance = mc.player.distanceTo(living);
                String distanceText = "[" + String.format(Locale.ROOT, "%.1f", distance) + "m] ";
                Color distanceColor = new Color(160, 160, 160);

                boolean isTeam = Teams.isSameTeam(living);
                boolean isFriend = FriendManager.isFriend(living);
                boolean isAiming = this.aimingPlayer == living;
                boolean hasTopLabel = isAiming;
                boolean showFriendLabel = isAiming && isFriend;
                boolean showAimingLabel = isAiming && !isFriend;

                String prefixText = (isTeam ? "§aTeam§f " : "") + displayName;
                String hpText = Math.round(hp) + (living.getAbsorptionAmount() > 0.0F ? "+" + Math.round(living.getAbsorptionAmount()) : "") + "HP";
                String hpTextWithSpace = (prefixText.isEmpty() ? "" : " ") + hpText;

                float distanceWidth = font.getWidth(distanceText, fontScale);
                float prefixWidth = font.getWidth(prefixText, fontScale);
                float hpWidth = font.getWidth(hpTextWithSpace, fontScale);
                float mainWidth = distanceWidth + prefixWidth + hpWidth;
                float labelWidth = 0.0F;
                if (hasTopLabel) {
                    labelWidth = font.getWidth(showFriendLabel ? "Friend" : "Aiming", fontScale);
                }
                float bgWidth = Math.max(mainWidth, labelWidth);
                float delta = 1.0F - living.getHealth() / living.getMaxHealth();
                float lineH = (float) font.getHeight(true, fontScale);

                float bgX1 = position.x - bgWidth / 2.0F - 2.0F;
                float bgX2 = position.x + bgWidth / 2.0F + 2.0F;

                float bgTopY = position.y - 3.0F;
                float bgBottomY = position.y + lineH + 1.0F;
                float mainTextY = position.y - 1.0F;
                float labelTextY = mainTextY;
                if (hasTopLabel) {
                    bgTopY -= lineH + 2.0F;
                    labelTextY = mainTextY - lineH - 2.0F;
                }
                this.blurMatrices
                        .add(new Vector4f(bgX1, bgTopY, bgX2, bgBottomY));
                RenderUtils.fill(
                        guiGraphics,
                        bgX1,
                        bgTopY,
                        bgX2,
                        bgBottomY,
                        color1
                );
                RenderUtils.fill(
                        guiGraphics,
                        bgX1,
                        bgTopY,
                        bgX2 - (bgWidth + 4.0F) * delta,
                        bgBottomY,
                        color2
                );
                float mainStartX = position.x - mainWidth / 2.0F;

                font.setAlpha(0.8F);
                if (hasTopLabel && labelWidth > 0.0F) {
                    float labelStartX = position.x - labelWidth / 2.0F;
                    if (showFriendLabel) {
                        font.render(guiGraphics, "Friend", (double)labelStartX, (double)labelTextY, new Color(85, 255, 85), true, fontScale);
                    } else if (showAimingLabel) {
                        font.render(guiGraphics, "Aiming", (double)labelStartX, (double)labelTextY, new Color(255, 85, 85), true, fontScale);
                    }
                }

                font.render(guiGraphics, distanceText, (double)mainStartX, (double)mainTextY, distanceColor, true, fontScale);
                font.render(guiGraphics, prefixText, (double)(mainStartX + distanceWidth), (double)mainTextY, Color.WHITE, true, fontScale);

                Color hpColor = getHpGradientColor(hp, living.getMaxHealth());
                font.render(guiGraphics, hpTextWithSpace, (double)(mainStartX + distanceWidth + prefixWidth), (double)mainTextY, hpColor, true, fontScale);
                font.setAlpha(1.0F);
                if (this.showPlayerEquipment.getCurrentValue()) {
                    float equipmentTopY = position.y - 2.0F;
                    if (hasTopLabel) {
                        equipmentTopY -= lineH + 2.0F;
                    }
                    this.renderEquipment(guiGraphics, living, position.x, equipmentTopY);
                }
                guiGraphics.pose().popMatrix();
            }
        }

        if (this.shared.getCurrentValue()) {
            for (NameTags.NameTagData data : this.sharedPositions) {
                guiGraphics.pose().pushMatrix();
                Vector2f positionx = data.getRender();
                float scale = this.scale.getCurrentValue();
                CustomTextRenderer font = Fonts.harmony;
                double fontScale = (double) scale;

                double distance = mc.player.position().distanceTo(data.getPosition());
                String distanceText = "[" + String.format(Locale.ROOT, "%.1f", distance) + "m] ";
                Color distanceColor = new Color(160, 160, 160);

                String prefixText = data.getDisplayName() + " §aShared§f";
                String hpText = Math.round(data.getHealth())
                        + (data.getAbsorption() > 0.0 ? "+" + Math.round(data.getAbsorption()) : "")
                        + "HP";
                String hpTextWithSpace = " " + hpText;

                float distanceWidth = font.getWidth(distanceText, fontScale);
                float prefixWidth = font.getWidth(prefixText, fontScale);
                float hpWidth = font.getWidth(hpTextWithSpace, fontScale);
                float width = distanceWidth + prefixWidth + hpWidth;

                double delta = 1.0 - data.getHealth() / data.getMaxHealth();
                float lineH = (float) font.getHeight(true, fontScale);
                float bgX1 = positionx.x - width / 2.0F - 2.0F;
                float bgX2 = positionx.x + width / 2.0F + 2.0F;
                float bgTopY = positionx.y - 3.0F;
                float bgBottomY = positionx.y + lineH + 1.0F;
                this.blurMatrices
                        .add(
                                new Vector4f(bgX1, bgTopY, bgX2, bgBottomY)
                        );
                RenderUtils.fill(
                        guiGraphics,
                        bgX1,
                        bgTopY,
                        bgX2,
                        bgBottomY,
                        color1
                );
                RenderUtils.fill(
                        guiGraphics,
                        bgX1,
                        bgTopY,
                        (float)((double)bgX2 - (double)(width + 4.0F) * delta),
                        bgBottomY,
                        color2
                );
                float startX = positionx.x - width / 2.0F;
                float textY = positionx.y - 1.0F;

                font.setAlpha(0.8F);
                font.render(guiGraphics, distanceText, (double)startX, (double)textY, distanceColor, true, fontScale);
                font.render(guiGraphics, prefixText, (double)(startX + distanceWidth), (double)textY, Color.WHITE, true, fontScale);

                Color hpColor = getHpGradientColor((float) data.getHealth(), (float) data.getMaxHealth());
                font.render(guiGraphics, hpTextWithSpace, (double)(startX + distanceWidth + prefixWidth), (double)textY, hpColor, true, fontScale);
                font.setAlpha(1.0F);
                guiGraphics.pose().popMatrix();
            }
        }
    }

    private void onRenderNaven(EventRender2D e) {
        GuiGraphics guiGraphics = e.getGuiGraphics();
        this.blurMatrices.clear();
        if (this.compassPosition != null) {
            Vector2f position = this.compassPosition;
            float scale = Math.max(
                    80.0F
                            - Mth.sqrt(
                            (float)mc.player
                                    .distanceToSqr(
                                            (double)this.spawnPosition.getX() + 0.5, (double)this.spawnPosition.getY() + 1.75, (double)this.spawnPosition.getZ() + 0.5
                                    )
                    ),
                    0.0F
            )
                    * this.scale.getCurrentValue()
                    / 80.0F;
            String text = "Compass";
            float width = Fonts.harmony.getWidth(text, (double)scale);
            double height = Fonts.harmony.getHeight(true, (double)scale);
            float topY = position.y - 2.0F;
            float bottomY = (float)((double)position.y + height);
            this.blurMatrices
                    .add(new Vector4f(position.x - width / 2.0F - 2.0F, topY, position.x + width / 2.0F + 2.0F, bottomY));
            StencilUtils.write(false);
            try {
                RenderUtils.fill(
                        guiGraphics, position.x - width / 2.0F - 2.0F, topY, position.x + width / 2.0F + 2.0F, bottomY, -1
                );
                StencilUtils.erase(true);
                RenderUtils.fill(
                        guiGraphics, position.x - width / 2.0F - 2.0F, topY, position.x + width / 2.0F + 2.0F, bottomY, navenColor1
                );
            } finally {
                resetGuiRenderState();
                StencilUtils.dispose();
            }
            Fonts.harmony.setAlpha(0.8F);
            Fonts.harmony.render(guiGraphics, text, (double)(position.x - width / 2.0F), (double)(position.y - 1.0F), Color.WHITE, true, (double)scale);
        }

        for (Entry<Entity, Vector2f> entry : this.entityPositions.entrySet()) {
            if (entry.getKey() != mc.player && entry.getKey() instanceof Player) {
                Player living = (Player)entry.getKey();
                guiGraphics.pose().pushMatrix();
                float hp = living.getHealth();
                if (hp > 20.0F) {
                    living.setHealth(20.0F);
                }

                Vector2f position = entry.getValue();
                String name = living.getName().getString();
                String displayName = name.replace(mc.player.getName().getString(), "§dHidden§7");
                ClientFriend clientFriend = (ClientFriend)Naven.getInstance().getModuleManager().getModule(ClientFriend.class);
                boolean clientFriendEnabled = clientFriend != null && clientFriend.isEnabled();

                String text = "";
                if (Teams.isSameTeam(living)) {
                    text = text + "§aTeam§f | ";
                }

                if (FriendManager.isFriend(living)) {
                    text = text + "§aFriend§f | ";
                }

                if (this.aimingPlayer == living) {
                    text = text + "§cAiming§f | ";
                }

                text = text + displayName;
                text = text + "§f | §c" + Math.round(hp) + (living.getAbsorptionAmount() > 0.0F ? "+" + Math.round(living.getAbsorptionAmount()) : "") + "HP";

                float scale = this.scale.getCurrentValue();
                float width = Fonts.harmony.getWidth(text, (double)scale);
                float delta = 1.0F - living.getHealth() / living.getMaxHealth();
                double height = Fonts.harmony.getHeight(true, (double)scale);
                this.blurMatrices
                        .add(new Vector4f(position.x - width / 2.0F - 2.0F, position.y - 2.0F, position.x + width / 2.0F + 2.0F, (float)((double)position.y + height)));
                RenderUtils.fill(
                        guiGraphics,
                        position.x - width / 2.0F - 2.0F,
                        position.y - 2.0F,
                        position.x + width / 2.0F + 2.0F,
                        (float)((double)position.y + height),
                        navenColor1
                );
                RenderUtils.fill(
                        guiGraphics,
                        position.x - width / 2.0F - 2.0F,
                        position.y - 2.0F,
                        position.x + width / 2.0F + 2.0F - (width + 4.0F) * delta,
                        (float)((double)position.y + height),
                        navenColor2
                );
                Fonts.harmony.setAlpha(0.8F);
                Fonts.harmony.render(guiGraphics, text, (double)(position.x - width / 2.0F), (double)(position.y - 1.0F), Color.WHITE, true, (double)scale);
                Fonts.harmony.setAlpha(1.0F);
                if (this.showPlayerEquipment.getCurrentValue()) {
                    this.renderEquipment(guiGraphics, living, position.x, position.y - 2.0F);
                }
                guiGraphics.pose().popMatrix();
            }
        }

        if (this.shared.getCurrentValue()) {
            for (NameTags.NameTagData data : this.sharedPositions) {
                guiGraphics.pose().pushMatrix();
                Vector2f positionx = data.getRender();
                String textx = "§aShared§f | " + data.getDisplayName();
                textx = textx
                        + "§f | §c"
                        + Math.round(data.getHealth())
                        + (data.getAbsorption() > 0.0 ? "+" + Math.round(data.getAbsorption()) : "")
                        + "HP";
                float scale = this.scale.getCurrentValue();
                float width = Fonts.harmony.getWidth(textx, (double)scale);
                double max = data.getMaxHealth() <= 0.0 ? 20.0 : data.getMaxHealth();
                double delta = 1.0 - data.getHealth() / max;
                double height = Fonts.harmony.getHeight(true, (double)scale);
                this.blurMatrices
                        .add(
                                new Vector4f(positionx.x - width / 2.0F - 2.0F, positionx.y - 2.0F, positionx.x + width / 2.0F + 2.0F, (float)((double)positionx.y + height))
                        );
                RenderUtils.fill(
                        guiGraphics,
                        positionx.x - width / 2.0F - 2.0F,
                        positionx.y - 2.0F,
                        positionx.x + width / 2.0F + 2.0F,
                        (float)((double)positionx.y + height),
                        navenColor1
                );
                RenderUtils.fill(
                        guiGraphics,
                        positionx.x - width / 2.0F - 2.0F,
                        positionx.y - 2.0F,
                        (float)((double)(positionx.x + width / 2.0F + 2.0F) - (double)(width + 4.0F) * delta),
                        (float)((double)positionx.y + height),
                        navenColor2
                );
                Fonts.harmony.setAlpha(0.8F);
                Fonts.harmony.render(guiGraphics, textx, (double)(positionx.x - width / 2.0F), (double)(positionx.y - 1.0F), Color.WHITE, true, (double)scale);
                Fonts.harmony.setAlpha(1.0F);
                guiGraphics.pose().popMatrix();
            }
        }
    }

    private static Color getHpGradientColor(float health, float maxHealth) {
        float max = maxHealth <= 0.0F ? 20.0F : maxHealth;
        float t = health / max;
        t = Mth.clamp(t, 0.0F, 1.0F);
        int r;
        int g;
        if (t >= 0.5F) {
            float u = (t - 0.5F) / 0.5F;
            r = (int)(255.0F * (1.0F - u));
            g = 255;
        } else {
            float u = t / 0.5F;
            r = 255;
            g = (int)(255.0F * u);
        }
        return new Color(Mth.clamp(r, 0, 255), Mth.clamp(g, 0, 255), 0);
    }

    private void updatePositions(float renderPartialTicks) {
        this.entityPositions.clear();
        this.sharedPositions.clear();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof Player && !entity.getName().getString().startsWith("CIT-")) {
                double x = MathUtils.interpolate(renderPartialTicks, entity.xo, entity.getX());
                double y = MathUtils.interpolate(renderPartialTicks, entity.yo, entity.getY()) + (double)entity.getBbHeight() + 0.5;
                double z = MathUtils.interpolate(renderPartialTicks, entity.zo, entity.getZ());
                Vector2f vector = ProjectionUtils.project(x, y, z, renderPartialTicks);
                if (!isValidScreenPos(vector)) {
                    continue;
                }
                vector.setY(vector.getY() - 2.0F);
                this.entityPositions.put(entity, vector);
            }
        }

        if (this.shared.getCurrentValue()) {
            Map<String, SharedESPData> dataMap = EntityWatcher.getSharedESPData();

            for (SharedESPData value : dataMap.values()) {
                double x = value.getPosX();
                double y = value.getPosY() + (double)mc.player.getBbHeight() + 0.5;
                double z = value.getPosZ();
                Vector2f vector = ProjectionUtils.project(x, y, z, renderPartialTicks);
                if (!isValidScreenPos(vector)) {
                    continue;
                }
                vector.setY(vector.getY() - 2.0F);
                String displayName = value.getDisplayName();
                this.sharedPositions
                        .add(new NameTags.NameTagData(displayName, value.getHealth(), value.getMaxHealth(), value.getAbsorption(), new Vec3(x, y, z), vector));
            }
        }
    }

    private static class NameTagData {
        private final String displayName;
        private final double health;
        private final double maxHealth;
        private final double absorption;
        private final Vec3 position;
        private final Vector2f render;

        public String getDisplayName() {
            return this.displayName;
        }

        public double getHealth() {
            return this.health;
        }

        public double getMaxHealth() {
            return this.maxHealth;
        }

        public double getAbsorption() {
            return this.absorption;
        }

        public Vec3 getPosition() {
            return this.position;
        }

        public Vector2f getRender() {
            return this.render;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (!(o instanceof NameTags.NameTagData other)) {
                return false;
            } else if (!other.canEqual(this)) {
                return false;
            } else if (Double.compare(this.getHealth(), other.getHealth()) != 0) {
                return false;
            } else if (Double.compare(this.getMaxHealth(), other.getMaxHealth()) != 0) {
                return false;
            } else if (Double.compare(this.getAbsorption(), other.getAbsorption()) != 0) {
                return false;
            } else {
                Object this$displayName = this.getDisplayName();
                Object other$displayName = other.getDisplayName();
                if (this$displayName == null ? other$displayName == null : this$displayName.equals(other$displayName)) {
                    Object this$position = this.getPosition();
                    Object other$position = other.getPosition();
                    if (this$position == null ? other$position == null : this$position.equals(other$position)) {
                        Object this$render = this.getRender();
                        Object other$render = other.getRender();
                        return this$render == null ? other$render == null : this$render.equals(other$render);
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        }

        protected boolean canEqual(Object other) {
            return other instanceof NameTags.NameTagData;
        }

        @Override
        public int hashCode() {
            int PRIME = 59;
            int result = 1;
            long $health = Double.doubleToLongBits(this.getHealth());
            result = result * 59 + (int)($health >>> 32 ^ $health);
            long $maxHealth = Double.doubleToLongBits(this.getMaxHealth());
            result = result * 59 + (int)($maxHealth >>> 32 ^ $maxHealth);
            long $absorption = Double.doubleToLongBits(this.getAbsorption());
            result = result * 59 + (int)($absorption >>> 32 ^ $absorption);
            Object $displayName = this.getDisplayName();
            result = result * 59 + ($displayName == null ? 43 : $displayName.hashCode());
            Object $position = this.getPosition();
            result = result * 59 + ($position == null ? 43 : $position.hashCode());
            Object $render = this.getRender();
            return result * 59 + ($render == null ? 43 : $render.hashCode());
        }

        @Override
        public String toString() {
            return "NameTags.NameTagData(displayName="
                    + this.getDisplayName()
                    + ", health="
                    + this.getHealth()
                    + ", maxHealth="
                    + this.getMaxHealth()
                    + ", absorption="
                    + this.getAbsorption()
                    + ", position="
                    + this.getPosition()
                    + ", render="
                    + this.getRender()
                    + ")";
        }

        public NameTagData(String displayName, double health, double maxHealth, double absorption, Vec3 position, Vector2f render) {
            this.displayName = displayName;
            this.health = health;
            this.maxHealth = maxHealth;
            this.absorption = absorption;
            this.position = position;
            this.render = render;
        }
    }
}
