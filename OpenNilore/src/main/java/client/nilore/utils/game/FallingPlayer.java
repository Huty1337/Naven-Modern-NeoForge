package client.nilore.utils.game;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class FallingPlayer {
    private double x;
    private double y;
    private double z;
    private Vec3 motion;
    private Vec3 eyePos;
    private float yaw;
    private float strafe;
    private float forward;
    private float jumpMovementFactor;
    private final double eyeHeight;

    public FallingPlayer(Player player) {
        this.x = player.getX();
        this.y = player.getY();
        this.z = player.getZ();
        this.motion = player.getDeltaMovement();
        this.yaw = player.getYRot();
        this.strafe = player.xxa;
        this.forward = player.zza;
        this.jumpMovementFactor = player.isSprinting() ? 0.026f : 0.02f;
        this.eyeHeight = player.getEyeHeight();
        this.eyePos = player.getEyePosition();
    }

    private void calculateForTick() {
        float dragX = 0.91f;
        float dragZ = dragX;
        float dragY = 0.98f;
        float acceleration = this.jumpMovementFactor;

        updateVelocity(acceleration, new Vec3(this.strafe, 0, this.forward));
        this.x += this.motion.x;
        this.y += this.motion.y;
        this.z += this.motion.z;

        this.motion = this.motion.add(0, -0.08, 0);
        this.eyePos = new Vec3(this.x, this.y + this.eyeHeight, this.z);
        this.motion = new Vec3(
                this.motion.x * dragX,
                this.motion.y * dragY,
                this.motion.z * dragZ
        );
    }

    private void updateVelocity(float speed, Vec3 input) {
        double lengthSquared = input.lengthSqr();
        if (lengthSquared < 1.0E-7D) {
            return;
        }

        Vec3 normalizedInput = (lengthSquared > 1.0D ? input.normalize() : input).scale((double) speed);
        float f = Mth.sin(this.yaw * ((float) Math.PI / 180F));
        float g = Mth.cos(this.yaw * ((float) Math.PI / 180F));
        double inputX = normalizedInput.x * (double) g - normalizedInput.z * (double) f;
        double inputZ = normalizedInput.z * (double) g + normalizedInput.x * (double) f;

        this.motion = this.motion.add(inputX, 0, inputZ);
    }

    public void calculate(int ticks) {
        for (int i = 0; i < ticks; i++) {
            calculateForTick();
        }
    }

    public double getY() {
        return y;
    }

    public Vec3 getEyePosition() {
        return eyePos.add(0, 0, 0);
    }
}
