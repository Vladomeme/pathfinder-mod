package net.pathfinder.main.graph.waypoint;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.pathfinder.main.graph.RuleHolder;
import net.pathfinder.main.graph.waypoint.data.Waypoint;

import java.util.Optional;
import java.util.stream.Stream;

import static net.pathfinder.main.config.PFConfig.cfg;

/**
 * Calculates the targeted waypoint based on an angle between it, player's head and player's current camera direction.
 */
public class TargetHolder {

    public static Waypoint targeted = null;

    public static void updateTargeted() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;

        BlockPos pos = player.getBlockPos();
        Vec3d eyePos = player.getEyePos();
        Vec3d lookVec = Vec3d.fromPolar(player.getPitch(), player.getYaw()).normalize();

        Optional<Waypoint> waypoint = Stream.concat(GraphEditor.waypointsState.values().stream(), GraphEditor.currentSelection.values().stream())
                .filter(w -> Math.abs(getAngleRadians(eyePos, w, lookVec)) <= cfg.targetMaxAngleRad)
                .filter(w -> RuleHolder.isInRange(pos, w.pos().toCenterPos(), cfg.targetMaxDistance))
                .filter(w -> !cfg.targetLoSCheck ||
                        player.raycast(RuleHolder.getDistance(w.pos()), 0, false).getType() == HitResult.Type.MISS)
                .min((w1, w2) -> {
                    double angle1 = getAngleRadians(eyePos, w1, lookVec);
                    double angle2 = getAngleRadians(eyePos, w2, lookVec);
                    if (Math.abs(MathHelper.wrapDegrees(angle1 - angle2)) < 0.1) {
                        double distance1 = eyePos.squaredDistanceTo(w1.x() + 0.5, w1.y() + 0.5, w1.z() + 0.5);
                        double distance2 = eyePos.squaredDistanceTo(w2.x() + 0.5, w2.y() + 0.5, w2.z() + 0.5);
                        if (Math.min(distance1, distance2) < RuleHolder.getSquaredDistance(w1, w2) * 4) {
                            return Double.compare(distance1, distance2);
                        }
                    }
                    return Double.compare(Math.abs(angle1), Math.abs(angle2));
                });
        targeted = waypoint.orElse(null);
    }

    private static double getAngleRadians(Vec3d pos, Waypoint w, Vec3d lookVec) {
        Vec3d blockVec = new Vec3d(w.x() + 0.5 - pos.x, w.y() + 0.5 - pos.y, w.z() + 0.5 - pos.z).normalize();
        return Math.acos(lookVec.dotProduct(blockVec));
    }
}
