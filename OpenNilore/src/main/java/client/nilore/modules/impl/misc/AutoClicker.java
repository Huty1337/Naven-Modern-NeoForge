package client.nilore.modules.impl.misc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.ForgeHooksClient;
import client.nilore.event.impl.SprintEvent;
import client.nilore.modules.Category;
import client.nilore.modules.Module;
import client.nilore.settings.impl.BooleanSetting;
import client.nilore.settings.impl.ModeSetting;
import client.nilore.settings.impl.NumberSetting;
import client.nilore.utils.animation.Timer;
import client.nilore.utils.game.CpsUtil;
import client.nilore.utils.misc.ReflectionUtil;
import client.nilore.utils.misc.UnsafeUtil;
import client.nilore.event.EventTarget;

public class AutoClicker extends Module {
    private final NumberSetting cps = new NumberSetting("CPS", 12, 4, 25, 1);
    private final ModeSetting clickSide = new ModeSetting("Mode", "Left", "Right", "Both").withDefault("Left");
    private final ModeSetting clickMethod = new ModeSetting("Click Mode", "Method", "Key", "Mouse").withDefault("Mouse");
    private final ModeSetting cpsMode = new ModeSetting("CPS Mode", "Normal", "DBC").withDefault("Normal");
    private final BooleanSetting breakBlock = new BooleanSetting("Break Block", true);
    private final Timer leftClickTimer = new Timer();
    private final Timer rightClickTimer = new Timer();

    public AutoClicker() {
        super("AutoClicker", Category.MISC);
    }

    @Override
    public void onEnable() {
        this.leftClickTimer.reset();
        this.rightClickTimer.reset();
    }

    @Override
    public void onDisable() {
    }

    @EventTarget
    public void onSprint(SprintEvent sprintEvent) {
        if (mc.player == null || mc.level == null) {
            return;
        }
        if (this.clickSide.is("Normal")) {
            this.clickSide.setValue("Key");
        }
        if (this.clickSide.is("Left")) {
            this.doLeftClick();
        } else if (this.clickSide.is("Right")) {
            this.doRightClick();
        } else {
            this.doLeftClick();
            this.doRightClick();
        }
    }

    private void doLeftClick() {
        if (mc.player.isUsingItem()
                || !mc.options.keyAttack.isDown()
                || mc.screen != null
                || !this.leftClickTimer.hasPassed(CpsUtil.toDelayMs(this.cpsMode.getValue(), this.cps.getValue().doubleValue()))) {
            return;
        }
        if (this.breakBlock.getValue()
                && mc.hitResult instanceof BlockHitResult blockHitResult
                && blockHitResult.getType() == HitResult.Type.BLOCK) {
            return;
        }
        ReflectionUtil.setMissTime(0);
        switch ((String) this.clickMethod.getValue()) {
            case "Method":
                this.invokeStartAttack();
                break;
            case "Key":
                KeyMapping.click(mc.options.keyAttack.getKey());
                break;
            case "Mouse":
                ForgeHooksClient.onMouseButtonPre(mc.options.keyAttack.getKey().getValue(), 1, 0);
                KeyMapping.click(mc.options.keyAttack.getKey());
                ForgeHooksClient.onMouseButtonPost(mc.options.keyAttack.getKey().getValue(), 1, 0);
                break;
        }
        this.leftClickTimer.reset();
    }

    private void invokeStartUseItem() {
        try {
            Method method = mc.getClass().getDeclaredMethod(ReflectionUtil.getMappedMethodName(mc.getClass(), "startUseItem", "()V"));
            UnsafeUtil.makeAccessible(method);
            method.invoke(mc);
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException throwable) {
            throw new RuntimeException(throwable);
        }
    }

    private void invokeStartAttack() {
        try {
            Method method = mc.getClass().getDeclaredMethod(ReflectionUtil.getMappedMethodName(mc.getClass(), "startAttack", "()V"));
            UnsafeUtil.makeAccessible(method);
            method.invoke(mc);
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException throwable) {
            throw new RuntimeException(throwable);
        }
    }

    private void doRightClick() {
        if (mc.player.isUsingItem()
                || !(mc.hitResult instanceof BlockHitResult)
                || !(mc.player.getMainHandItem().getItem() instanceof BlockItem blockItem)
                || blockItem.getBlock() instanceof DoorBlock
                || !mc.options.keyUse.isDown()
                || mc.screen != null
                || !this.rightClickTimer.hasPassed(CpsUtil.toDelayMs(this.cpsMode.getValue(), this.cps.getValue().doubleValue()))) {
            return;
        }
        switch ((String) this.clickMethod.getValue()) {
            case "Method":
                this.invokeStartUseItem();
                break;
            case "Key":
                KeyMapping.click(mc.options.keyUse.getKey());
                break;
            case "Mouse":
                ForgeHooksClient.onMouseButtonPre(mc.options.keyUse.getKey().getValue(), 1, 0);
                KeyMapping.click(mc.options.keyUse.getKey());
                ForgeHooksClient.onMouseButtonPost(mc.options.keyUse.getKey().getValue(), 1, 0);
                break;
        }
        ReflectionUtil.setRightClickDelay(0);
        this.rightClickTimer.reset();
    }
}
