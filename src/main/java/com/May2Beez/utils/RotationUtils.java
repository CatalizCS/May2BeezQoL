package com.May2Beez.utils;

import com.May2Beez.May2BeezQoL;
import com.May2Beez.events.PlayerMoveEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class RotationUtils {

    private static final Minecraft mc = Minecraft.getMinecraft();
    public static Rotation startRot;
    public static Rotation neededChange;
    public static Rotation endRot;
    private static long startTime;
    private static long endTime;

    private static float serverPitch;
    private static float serverYaw;
    public static float currentFakeYaw;
    public static float currentFakePitch;
    public static boolean done = true;
    private enum RotationType {
        NORMAL,
        SERVER
    }

    private static RotationType rotationType;

    public static class Rotation {
        public float pitch;
        public float yaw;

        public Rotation(float pitch, float yaw) {
            this.pitch = pitch;
            this.yaw = yaw;
        }

        public float getValue() {
            return Math.abs(this.yaw) + Math.abs(this.pitch);
        }

        @Override
        public String toString() {
            return "pitch=" + pitch +
                    ", yaw=" + yaw;
        }
    }

    public static double wrapAngleTo180(double angle) {
        return angle - Math.floor(angle / 360 + 0.5) * 360;
    }

    public static float wrapAngleTo180(float angle) {
        return (float) (angle - Math.floor(angle / 360 + 0.5) * 360);
    }

    public static float fovToVec3(Vec3 vec) {
        double x = vec.xCoord - mc.thePlayer.posX;
        double z = vec.zCoord - mc.thePlayer.posZ;
        double yaw = Math.atan2(x, z) * 57.2957795;
        return (float) (yaw * -1.0);
    }

    public static Rotation getRotation(Vec3 vec3) {
        double diffX = vec3.xCoord - mc.thePlayer.posX;
        double diffY = vec3.yCoord - mc.thePlayer.posY - mc.thePlayer.getEyeHeight();
        double diffZ = vec3.zCoord - mc.thePlayer.posZ;
        double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float pitch = (float) -Math.atan2(dist, diffY);
        float yaw = (float) Math.atan2(diffZ, diffX);
        pitch = (float) wrapAngleTo180((pitch * 180F / Math.PI + 90) * -1);
        yaw = (float) wrapAngleTo180((yaw * 180 / Math.PI) - 90);

        return new Rotation(pitch, yaw);
    }

    public static Rotation getRotation(Vec3 from, Vec3 to) {
        double diffX = from.xCoord - to.xCoord;
        double diffY = from.yCoord - to.yCoord;
        double diffZ = from.zCoord - to.zCoord;
        double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float pitch = (float) -Math.atan2(dist, diffY);
        float yaw = (float) Math.atan2(diffZ, diffX);
        pitch = (float) wrapAngleTo180((pitch * 180F / Math.PI + 90) * -1);
        yaw = (float) wrapAngleTo180((yaw * 180 / Math.PI) - 90);

        return new Rotation(pitch, yaw);
    }

    public static Rotation getRotation(BlockPos block) {
        return getRotation(new Vec3(block.getX() + 0.5, block.getY() + 0.5, block.getZ() + 0.5));
    }

    public static Rotation getRotation(Entity entity) {
        return getRotation(new Vec3(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ));
    }

    public static boolean IsDiffLowerThan(float diff) {
        return IsDiffLowerThan(diff, diff);
    }

    public static boolean IsDiffLowerThan(float pitch, float yaw) {
        if (neededChange == null)
            return false;
        return Math.abs(neededChange.pitch) < pitch && Math.abs(neededChange.yaw) < yaw;
    }

    public static Rotation getNeededChange(Rotation startRot, Rotation endRot) {
        float yawDiff = wrapAngleTo180(endRot.yaw) - wrapAngleTo180(startRot.yaw);

        if (yawDiff <= -180) {
            yawDiff += 360;
        } else if (yawDiff > 180) {
            yawDiff -= 360;
        }

        return new Rotation(endRot.pitch - startRot.pitch, yawDiff);
    }

    public static Rotation getNeededChange(Rotation endRot) {
        return getNeededChange(new Rotation(mc.thePlayer.rotationPitch, mc.thePlayer.rotationYaw), endRot);
    }

    private static float interpolate(float start, float end) {
        return (end - start) * easeOutCubic((float) (System.currentTimeMillis() - startTime) / (endTime - startTime)) + start;
    }

    public static float easeOutCubic(double number) {
        return (float) Math.max(0, Math.min(1, 1 - Math.pow(1 - number, 3)));
    }

    public static void smoothLook(Rotation rotation, long time) {
        rotationType = RotationType.NORMAL;
        done = false;
        startRot = new Rotation(mc.thePlayer.rotationPitch, mc.thePlayer.rotationYaw);

        neededChange = getNeededChange(startRot, rotation);

        endRot = new Rotation(startRot.pitch + neededChange.pitch, startRot.yaw + neededChange.yaw);

        startTime = System.currentTimeMillis();
        endTime = System.currentTimeMillis() + time;
    }

    public static void serverSmoothLook(Rotation rotation, long time) {
        rotationType = RotationType.SERVER;
        done = false;

        if (currentFakePitch == 0) currentFakePitch = mc.thePlayer.rotationPitch;
        if (currentFakeYaw == 0) currentFakeYaw = mc.thePlayer.rotationYaw;

        startRot = new Rotation(currentFakePitch, currentFakeYaw);

        neededChange = getNeededChange(startRot, rotation);

        endRot = new Rotation(startRot.pitch + neededChange.pitch, startRot.yaw + neededChange.yaw);

        startTime = System.currentTimeMillis();
        endTime = System.currentTimeMillis() + time;
    }

    public static void look(Rotation rotation) {
        mc.thePlayer.rotationPitch = rotation.pitch;
        mc.thePlayer.rotationYaw = rotation.yaw;
    }

    public static void reset() {
        done = true;
        startRot = null;
        neededChange = null;
        endRot = null;
        startTime = 0;
        endTime = 0;
        currentFakeYaw = 0;
        currentFakePitch = 0;
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (rotationType != RotationType.NORMAL) return;
        if (System.currentTimeMillis() <= endTime) {
            mc.thePlayer.rotationPitch = interpolate(startRot.pitch, endRot.pitch);
            mc.thePlayer.rotationYaw = interpolate(startRot.yaw, endRot.yaw);
        } else {
            if (!done) {
                mc.thePlayer.rotationYaw = endRot.yaw;
                mc.thePlayer.rotationPitch = endRot.pitch;

                reset();
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onUpdatePre(PlayerMoveEvent.Pre pre) {
        serverPitch = mc.thePlayer.rotationPitch;
        serverYaw = mc.thePlayer.rotationYaw;

        if (rotationType != RotationType.SERVER) return;
        if (System.currentTimeMillis() <= endTime) {
            mc.thePlayer.rotationYaw = interpolate(startRot.yaw, endRot.yaw);
            mc.thePlayer.rotationPitch = interpolate(startRot.pitch, endRot.pitch);

            currentFakeYaw = mc.thePlayer.rotationYaw;
            currentFakePitch = mc.thePlayer.rotationPitch;
        } else {
            if (!done) {
                mc.thePlayer.rotationYaw = endRot.yaw;
                mc.thePlayer.rotationPitch = endRot.pitch;

                currentFakeYaw = mc.thePlayer.rotationYaw;
                currentFakePitch = mc.thePlayer.rotationPitch;

                if (System.currentTimeMillis() >= endTime + May2BeezQoL.config.holdRotationMS) {
                    reset();
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onUpdatePost(PlayerMoveEvent.Post post) {
        mc.thePlayer.rotationPitch = serverPitch;
        mc.thePlayer.rotationYaw = serverYaw;
    }
}