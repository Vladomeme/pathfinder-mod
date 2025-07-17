package net.pathfinder.main.graph.waypoint.path;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.pathfinder.main.Output;
import net.pathfinder.main.graph.RuleHolder;

import java.util.ArrayList;
import java.util.List;

public class PathManager {

    private static final MinecraftClient client = MinecraftClient.getInstance();

    static PathNode head;
    static PathNode nearest;
    static List<BlockPos> swirls = new ArrayList<>();
    static boolean pathReached = false;

    static int ticks = 0;

    public static void tick() {
        ticks++;

        positionUpdate();
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
        positionUpdate();
        displayPath();
        Output.chat("Destination set: " + x + " " + y + " " + z + ".");
        return 1;
    }

    private static void recompute() {
        ClientPlayerEntity player = client.player;
        assert player != null;

        Output.logWarn("Recomputing path!");

        head = PathBuilder.getPath(player.getBlockPos(), head.getLast().pos);
        if (head == null) return;

        pathReached = false;
        nearest = head;
        positionUpdate();
        displayPath();
    }

    //todo save last in a field
    private static void positionUpdate() {
        int minDistance = Integer.MAX_VALUE;

        PathNode current = head;
        while (current != null) {
            int distance = RuleHolder.getSquaredDistance(current.pos);
            if (minDistance > distance) {
                minDistance = distance;
                nearest = current;
            }
            current = current.next;
        }
        if (RuleHolder.getSquaredDistance(nearest.getLast().pos) < 16) {
            Output.chat("Destination reached!", Output.Color.GREEN);
            clear();
            return;
        }
        if (minDistance < 25) pathReached = true;
        else if (minDistance > 400 && pathReached) recompute();
        updateSwirls();
    }

    private static void updateSwirls() {
        swirls.clear();
        double travelDistance = 30;

        PathNode current = nearest;
        travelDistance -= RuleHolder.getDistance(current.pos);

        while (travelDistance > 0 && current.next != null) {
            if (current.isTeleport && current.next.isTeleport)
                swirls.add(current.pos);
            travelDistance -= RuleHolder.getDistance(current.pos, current.next.pos);
            current = current.next;
        }
        if (current.next == null) swirls.add(current.pos);
    }

    private static void displayNodes() {
        ClientWorld world = client.world;
        assert world != null;
        PathNode current = head;
        while (current != null) {
            world.addParticle(ParticleTypes.HEART,
                    current.x() + 0.5 + (Math.random() - Math.random()) * 0.25,
                    current.y() + 1.0 + (Math.random() - Math.random()) * 0.25,
                    current.z() + 0.5 + (Math.random() - Math.random()) * 0.25, 0, 0, 0);
            current = current.next;
        }
    }

    //todo improve visuals
    private static void displayPath() {
        assert client.player != null;

        int i = 0;

        int pathLength = 50 * 3;

        //Last node shortcut
        if (nearest.next == null) {
            float distance = RuleHolder.getDistance(nearest.pos);
            float localDistance = 0;
            while (i++ < pathLength && localDistance < distance) {
                addPathParticles(localDistance / distance, client.player.getBlockPos(), nearest.pos);
                localDistance += 0.33f;
            }
            return;
        }

        //Player to nearest point
        float distance = RuleHolder.getDistance(nearest.pos);
        float[] trueNearest = PathBuilder.getNearestPointInPath(client.player.getBlockPos(), nearest.pos, nearest.next.pos);
        float localDistance = 0;
        while (i++ < pathLength && localDistance < distance) {
            addPathParticles(localDistance / distance, client.player.getBlockPos(), trueNearest);
            localDistance += 0.33f;
        }

        //Nearest point to next node
        localDistance = 0;
        distance = RuleHolder.getDistance(trueNearest, nearest.next.pos);
        while (i++ < pathLength && localDistance < distance) {
            addPathParticles(localDistance / distance, trueNearest, nearest.next.pos);
            localDistance += 0.33f;
        }

        //Node to node, skipping teleportation segments
        PathNode current = nearest;
        if (current.isTeleport) {
            if (current.next.isTeleport) {
                current = current.next;
                if (current.next == null) return;
            }
        }
        distance = RuleHolder.getDistance(current.pos, current.next.pos);
        localDistance = 0;
        while (i++ < pathLength) {
            if (localDistance > distance) {
                current = current.next;
                if (current.next == null) break;
                if (current.isTeleport) {
                    if (current.next.isTeleport) {
                        current = current.next;
                        if (current.next == null) return;
                    }
                }
                distance = RuleHolder.getDistance(current.pos, current.next.pos);
                localDistance = 0;
            }
            addPathParticles(localDistance / distance, current.pos, current.next.pos);
            localDistance += 0.33f;
        }
    }

    private static void addPathParticles(float fraction, float[] arr, BlockPos pos) {
        addPathParticles(fraction, arr[0], arr[1], arr[2], pos.getX(), pos.getY(), pos.getZ());
    }

    private static void addPathParticles(float fraction, BlockPos pos, float[] arr) {
        addPathParticles(fraction, pos.getX(), pos.getY(), pos.getZ(), arr[0], arr[1], arr[2]);
    }

    private static void addPathParticles(float fraction, BlockPos start, BlockPos end) {
        addPathParticles(fraction, start.getX(), start.getY(), start.getZ(), end.getX(), end.getY(), end.getZ());
    }

    private static void addPathParticles(float fraction, float x1, float y1, float z1, float x2, float y2, float z2) {
        double x = x1 + (x2 - x1) * fraction + 0.5;
        double y = y1 + (y2 - y1) * fraction + 0.5;
        double z = z1 + (z2 - z1) * fraction + 0.5;

        ClientWorld world = client.world;
        assert world != null;
        world.addImportantParticle(ParticleTypes.END_ROD, x, y, z, 0, 0, 0);
    }

    //x(t) = R * cos(t), y(t) = a * t, z(t) = R * sin(t).
    private static void displaySwirls() {
        double t = client.getRenderTime();
        double random = (Math.random() - Math.random()) * 0.2;

        double swirlX1 = Math.cos(t) + random + 0.5;
        double swirlZ1 = Math.sin(t) + random + 0.5;
        double swirlX2 = -Math.cos(t) + random + 0.5;
        double swirlZ2 = -Math.sin(t) + random + 0.5;
        double swirlY = (t % 40) / 13.3f + random;

        ClientWorld world = client.world;
        assert world != null;

        for (BlockPos pos : swirls) {
            double x = pos.getX();
            double z = pos.getZ();
            double y = pos.getY() + swirlY;

            world.addParticle(ParticleTypes.END_ROD, x + swirlX1, y, z + swirlZ1, 0, 0, 0);
            world.addParticle(ParticleTypes.END_ROD, x + swirlX2, y, z + swirlZ2, 0, 0, 0);
        }
    }

    public static boolean isActive() {
        return head != null;
    }

    public static int clear() {
        head = null;
        return 1;
    }
}
