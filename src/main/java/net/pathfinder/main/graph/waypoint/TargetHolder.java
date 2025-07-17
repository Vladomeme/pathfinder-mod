package net.pathfinder.main.graph.waypoint;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.pathfinder.main.graph.RuleHolder;
import net.pathfinder.main.graph.waypoint.data.Waypoint;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Calculates the targeted waypoint based on an angle between it, player's head and player's current camera direction.
 */
//todo max range config option
//todo line of sight requirement config option
public class TargetHolder {

    public static Waypoint targeted = null;

    //todo move max angle check to end
    public static void updateTargeted() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;

        BlockPos pos = player.getBlockPos();
        Vec3d eyePos = player.getEyePos();
        float pitch = player.getPitch();
        float yaw = player.getYaw();
        Optional<Waypoint> waypoint = Stream.concat(GraphEditor.waypointsState.values().stream(), GraphEditor.currentSelection.values().stream())
                .filter(w -> Math.abs(getAngleRadians(eyePos, w, pitch, yaw)) <= Math.toRadians(15))
                .filter(w -> RuleHolder.isInRange(pos, w.pos().toCenterPos(), 50))
                .min((w1, w2) -> {
                    double angle1 = getAngleRadians(eyePos, w1, pitch, yaw);
                    double angle2 = getAngleRadians(eyePos, w2, pitch, yaw);
                    if (Math.abs(MathHelper.wrapDegrees(angle1 - angle2)) < 0.1) {
                        double distance1 = eyePos.squaredDistanceTo(w1.x() + 0.5, w1.y() + 1, w1.z() + 0.5);
                        double distance2 = eyePos.squaredDistanceTo(w2.x() + 0.5, w2.y() + 1, w2.z() + 0.5);
                        if (Math.min(distance1, distance2) < RuleHolder.getSquaredDistance(w1, w2) * 4) {
                            return Double.compare(distance1, distance2);
                        }
                    }
                    return Double.compare(Math.abs(angle1), Math.abs(angle2));
                });
        targeted = waypoint.orElse(null);
    }

    //todo extract lookVec to a local in updateTargeted
    private static double getAngleRadians(Vec3d pos, Waypoint waypoint, float pitch, float yaw) {
        Vec3d blockVec = new Vec3d(waypoint.x() + 0.5 - pos.x, waypoint.y() + 1.0 - pos.y, waypoint.z() + 0.5 - pos.z).normalize();
        Vec3d lookVec = Vec3d.fromPolar(pitch, yaw).normalize();
        return Math.acos(lookVec.dotProduct(blockVec));
    }

}
