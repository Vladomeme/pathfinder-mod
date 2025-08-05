package net.pathfinder.main.graph.waypoint.path;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.block.entity.BeaconBlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.pathfinder.main.Output;
import net.pathfinder.main.graph.PositionUtils;
import net.pathfinder.main.graph.astar.AstarBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static net.pathfinder.main.config.PFConfig.cfg;

public class PathManager {

    private static final MinecraftClient client = MinecraftClient.getInstance();

    static PathNode head;
    static PathNode nearest;
    static PathNode last;
    static final List<Swirl> swirls = new ArrayList<>();
    static boolean pathReached = false;
    static ChunkPos lastPos;

    static int ticks = 0;

    static final Identifier BEAM_TEXTURE = Identifier.ofVanilla("textures/entity/beacon_beam.png");

    public static void tick() {
        ticks++;

        positionUpdate(Objects.requireNonNull(client.player));
        if (ticks % 20 == 0) displayPath();
        if (!swirls.isEmpty()) displaySwirls();
        //displayNodes();
    }

    public static int compute(CommandContext<FabricClientCommandSource> context) {
        int x = IntegerArgumentType.getInteger(context, "x");
        int y = IntegerArgumentType.getInteger(context, "y");
        int z = IntegerArgumentType.getInteger(context, "z");
        return compute(x, y, z);
    }

    @SuppressWarnings("SameReturnValue")
    public static int compute(int x, int y, int z) {
        ClientPlayerEntity player = client.player;
        assert player != null;
        head = PathBuilder.getPath(player.getBlockPos(), new BlockPos(x, y, z));
        if (head == null) return 1;

        pathReached = false;
        nearest = head;
        last = head.getLast();
        positionUpdate(player);
        if (head != null) displayPath();
        Output.chat("Destination set: " + x + " " + y + " " + z + ".");
        assert client.world != null;
        client.world.playSound(player.getX(), player.getY(), player.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE,
                SoundCategory.PLAYERS, 1, 1, false);
        return 1;
    }

    private static void recompute() {
        ClientPlayerEntity player = client.player;
        assert player != null;
        assert client.world != null;

        Output.actionBar("Rebuilding path!");
        client.world.playSound(player.getX(), player.getY(), player.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE,
                SoundCategory.PLAYERS, 1, 1, false);

        head = PathBuilder.getPath(player.getBlockPos(), last);
        if (head == null) return;

        pathReached = false;
        nearest = head;
        last = head.getLast();
        positionUpdate(player);
        if (head != null) displayPath();
    }

    private static void positionUpdate(ClientPlayerEntity player) {
        ChunkPos currentPos = player.getChunkPos();
        if (currentPos != lastPos) {
            lastPos = currentPos;
            applySmoothing();
        }

        int minDistance = Integer.MAX_VALUE;

        PathNode current = head;
        while (current != null) {
            int distance = PositionUtils.getSquaredDistance(current);
            if (minDistance > distance) {
                minDistance = distance;
                nearest = current;
            }
            current = current.next;
        }
        if (PositionUtils.getSquaredDistance(last) < cfg.destinationRangeSquared) {
            Output.chat("Destination reached!", Output.Color.GREEN);
            Objects.requireNonNull(client.world).playSound(player.getX(), player.getY(), player.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK,
                    SoundCategory.PLAYERS, 1, 1, false);
            clear();
            return;
        }
        if (minDistance < 25) pathReached = true;
        else if (minDistance > cfg.recomputingDistanceSquared && pathReached) recompute();
        updateSwirls();
    }

    private static void applySmoothing() {
        if (nearest.next == null || nearest.next.next == null) return;

        int distance = Math.max((client.options.getClampedViewDistance() - 2) * 16, 32);
        distance = distance * distance;

        PathNode start = nearest;
        PathNode end = nearest.next.next;

        while (end.next != null
                && PositionUtils.getSquaredDistance(start) < distance
                && PositionUtils.getSquaredDistance(end) < distance) {
            if (AstarBuilder.isLinkValid(client.world, start, end)) start.next = end;
            else start = start.next;
            end = end.next;
        }
    }

    private static void updateSwirls() {
        swirls.clear();
        double travelDistance = cfg.pathDisplayLength;

        PathNode current = nearest;
        travelDistance -= PositionUtils.getDistance(current);

        while (travelDistance > 0 && current.next != null) {
            if (current.isTeleport && current.next.isTeleport)
                swirls.add(new Swirl(current, current.oneWay));
            travelDistance -= PositionUtils.getDistance(current, current.next);
            current = current.next;
        }
        if (current.next == null) swirls.add(new Swirl(current, false));
    }

    @SuppressWarnings("unused")
    private static void displayNodes() {
        ClientWorld world = client.world;
        assert world != null;
        PathNode current = head;
        while (current != null) {
            world.addParticle(ParticleTypes.HEART,
                    current.getX() + 0.5 + (Math.random() - Math.random()) * 0.25,
                    current.getY() + 1.0 + (Math.random() - Math.random()) * 0.25,
                    current.getZ() + 0.5 + (Math.random() - Math.random()) * 0.25, 0, 0, 0);
            current = current.next;
        }
    }

    private static void displayPath() {
        assert client.player != null;
        float pathLength = cfg.pathDisplayLength;
        float leftover = 0;

        //Player to nearest point
        float segmentLength, localDistance;
        float[] nearestPoint;
        if (!pathReached) {
            nearestPoint = PathBuilder.getVisiblePointInPath(client.world, client.player.getBlockPos(), nearest);
            segmentLength = PositionUtils.getDistance(nearestPoint[0], nearestPoint[1], nearestPoint[2]);
            if (segmentLength > 0.1f) {
                localDistance = 0;
                while (pathLength > 0 && localDistance < segmentLength) {
                    addPathParticles(localDistance / segmentLength, client.player.getBlockPos(), nearestPoint);
                    localDistance += cfg.pathParticleStep;
                    pathLength -= cfg.pathParticleStep;
                }
                leftover = localDistance - segmentLength;
            }
            nearest = PathBuilder.getNearestPointInPath(new Vec3i((int) nearestPoint[0], (int) nearestPoint[1], (int) nearestPoint[2]), head);
        }
        else nearestPoint = PathBuilder.getNearestPointOnLine(client.player.getBlockPos(), nearest, nearest.next);

        //Nearest point to next node
        segmentLength = PositionUtils.getDistance(nearestPoint, nearest.next);
        if (segmentLength > 0.1f) {
            localDistance = leftover;
            while (pathLength > 0 && localDistance < segmentLength) {
                addPathParticles(localDistance / segmentLength, nearestPoint, nearest.next);
                localDistance += cfg.pathParticleStep;
                pathLength -= cfg.pathParticleStep;
            }
            leftover = localDistance - segmentLength;
        }

        //Node to node, skipping teleportation segments
        PathNode current = nearest.next;
        if (current == null || current.next == null) return;
        while (pathLength > 0) {
            segmentLength = PositionUtils.getDistance(current, current.next);
            if (segmentLength > 0.1f) {
                localDistance = leftover;
                while (pathLength > 0 && localDistance < segmentLength) {
                    addPathParticles(localDistance / segmentLength, current, current.next);
                    localDistance += cfg.pathParticleStep;
                    pathLength -= cfg.pathParticleStep;
                }
                leftover = localDistance - segmentLength;
            }
            current = current.next;
            if (current.next == null) break;
            if (current.isTeleport && current.next.isTeleport) {
                current = current.next;
                if (current.next == null) break;
            }
        }
    }

    private static void addPathParticles(float fraction, float[] arr, Vec3i pos) {
        addPathParticles(fraction, arr[0], arr[1], arr[2], pos.getX(), pos.getY(), pos.getZ());
    }

    private static void addPathParticles(float fraction, Vec3i pos, float[] arr) {
        addPathParticles(fraction, pos.getX(), pos.getY(), pos.getZ(), arr[0], arr[1], arr[2]);
    }

    private static void addPathParticles(float fraction, Vec3i start, Vec3i end) {
        addPathParticles(fraction, start.getX(), start.getY(), start.getZ(), end.getX(), end.getY(), end.getZ());
    }

    private static void addPathParticles(float fraction, float x1, float y1, float z1, float x2, float y2, float z2) {
        double add = 0.5 + (Math.random() - Math.random()) * 0.2;
        double x = x1 + (x2 - x1) * fraction + add;
        double y = y1 + (y2 - y1) * fraction + add;
        double z = z1 + (z2 - z1) * fraction + add;

        Objects.requireNonNull(client.world).addImportantParticle(ParticleTypes.END_ROD, x, y, z, 0, 0, 0);
    }

    //x(t) = R * cos(t), y(t) = a * t, z(t) = R * sin(t).
    private static void displaySwirls() {
        double time = ticks % 40;
        double rad = time / 20 * Math.PI;
        double random = (Math.random() - Math.random()) * 0.2;

        double swirlX1 = Math.cos(rad) + random + 0.5;
        double swirlZ1 = Math.sin(rad) + random + 0.5;
        double swirlX2 = -Math.cos(rad) + random + 0.5;
        double swirlZ2 = -Math.sin(rad) + random + 0.5;
        double swirlY = time / 13.3f + random;

        ClientWorld world = client.world;
        assert world != null;

        for (Swirl swirl : swirls) {
            double x = swirl.pos.getX();
            double z = swirl.pos.getZ();
            double y = swirl.pos.getY() + swirlY;

            world.addParticle(ParticleTypes.END_ROD, x + swirlX1, y, z + swirlZ1, 0, 0, 0);
            world.addParticle(ParticleTypes.END_ROD, x + swirlX2, y, z + swirlZ2, 0, 0, 0);

            SimpleParticleType particle = swirl.oneWay ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.WITCH;
            world.addParticle(particle, x + swirlX1, y, z + swirlZ1, 0, 0, 0);
            world.addParticle(particle, x + swirlX2, y, z + swirlZ2, 0, 0, 0);
        }
    }

    @SuppressWarnings("ConstantConditions")
    public static void renderBeacon(WorldRenderContext context) {
        ClientWorld world = client.world;
        Vec3d pos = client.gameRenderer.getCamera().getPos();
        if (world == null) return;

        float distance = PositionUtils.getDistance(last);
        if (distance < 30) return;
        double beaconX = distance < 150 ? (double) last.getX() - pos.getX() : ((last.getX() - pos.getX()) / distance) * 150;
        double beaconZ = distance < 150 ? (double) last.getZ() - pos.getZ() : ((last.getZ() - pos.getZ()) / distance) * 150;

        MatrixStack matrices = context.matrixStack();
        matrices.push();
        matrices.translate(beaconX, -(pos.getY() + 64), beaconZ);
        BeaconBlockEntityRenderer.renderBeam(context.matrixStack(), context.consumers(), BEAM_TEXTURE, 0, 1,
                world.getTime(), 0, 1024, cfg.beaconColor, 0.3f, 0.3f
        );
        matrices.pop();
    }

    public static boolean isActive() {
        return head != null;
    }

    public static void clear() {
        head = null;
    }

    private record Swirl(Vec3i pos, boolean oneWay) {

    }
}
