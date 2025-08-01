package net.pathfinder.main.graph.astar;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.pathfinder.main.Output;
import net.pathfinder.main.PathfinderMod;
import net.pathfinder.main.graph.CandidateSupplier;
import net.pathfinder.main.graph.DebugManager;
import net.pathfinder.main.graph.PositionUtils;
import net.pathfinder.main.graph.waypoint.GraphEditor;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static net.pathfinder.main.config.PFConfig.cfg;

/**
 * Contains the A* algorithm and methods related to it.
 */
public class AstarBuilder {

    private static boolean inProcess = false;

    /**
     * Calculates and displays the path when either position is moved outside of waypoint graph editing mode.
     */
    public static void findOnUpdate() {
        if (inProcess) return;
        inProcess = true;

        PathfinderMod.executor.submit(() -> {
            ClientPlayerEntity player = Objects.requireNonNull(MinecraftClient.getInstance().player);
            Optional<List<BlockPos>> path = runAstar(player.clientWorld, DebugManager.start, DebugManager.target);

            if (path.isPresent()) processResults(player.clientWorld, path.get());
            else Output.chat("Path couldn't be found.");

            inProcess = false;
        });
    }

    /**
     * Calculates and returns the path, used by waypoint graph builder.
     */
    public static List<BlockPos> findAndReturn(BlockPos start, BlockPos end) {
        if (inProcess) return List.of();
        inProcess = true;

        ClientPlayerEntity player = Objects.requireNonNull(MinecraftClient.getInstance().player);

        Future<List<BlockPos>> future = PathfinderMod.executor.submit(() -> {
            DebugManager.start = start;
            Optional<List<BlockPos>> path = runAstar(player.clientWorld, start, end);
            DebugManager.start = null;

            if (path.isPresent()) return processResults(player.clientWorld, path.get());
            else {
                Output.chat("Path couldn't be found.");
                return List.of();
            }
        });

        try {
            return future.get();
        }
        catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        finally {
            inProcess = false;
        }
    }

    /**
     * Calculates and displays the path, only called manually via a command.
     */
    @SuppressWarnings("SameReturnValue")
    public static int findRoute(CommandContext<FabricClientCommandSource> context) {
        if (inProcess) return 1;
        inProcess = true;

        PathfinderMod.executor.submit(() -> {
            ClientPlayerEntity player = context.getSource().getPlayer();

            BlockPos start = player.getBlockPos();
            int x = IntegerArgumentType.getInteger(context, "x");
            int y = IntegerArgumentType.getInteger(context, "y");
            int z = IntegerArgumentType.getInteger(context, "z");
            BlockPos target = new BlockPos(x, y, z);

            DebugManager.start = start;
            Optional<List<BlockPos>> path = runAstar(player.clientWorld, start, target);
            DebugManager.start = null;

            if (path.isPresent()) processResults(player.clientWorld, path.get());
            else Output.chat("Path couldn't be found.");

            inProcess = false;
        });
        return 1;
    }

    /**
     * A* implementation. Returns an Optional with a path, if found, or an empty optional otherwise.
     */
    public static Optional<List<BlockPos>> runAstar(ClientWorld world, BlockPos start, BlockPos target) {
        CandidateSupplier supplier = new CandidateSupplier(world);
        Queue<AstarNode> open = new PriorityQueue<>();
        Map<BlockPos, AstarNode> nodes = new HashMap<>();

        AstarNode first = new AstarNode(start, null, 0d, PositionUtils.getDistance(start, target));
        open.add(first);
        nodes.put(start, first);

        while (!open.isEmpty()) {
            AstarNode next = open.poll();

            if (next.currentPos.equals(target)) {
                List<BlockPos> route = new ArrayList<>();
                AstarNode current = next;

                while (current != null) {
                    route.add(0, current.currentPos);
                    current = nodes.get(current.previousPos);
                }
                return Optional.of(route);
            }

            supplier.getCandidates(next.currentPos).forEach(candidate -> {
                AstarNode nextNode = nodes.getOrDefault(candidate.pos(), new AstarNode(candidate.pos()));
                nodes.put(candidate.pos(), nextNode);

                double newScore = next.currentScore + candidate.cost();
                if (newScore < nextNode.currentScore) {
                    nextNode.previousPos = next.currentPos;
                    nextNode.currentScore = newScore;
                    nextNode.estimatedScore = newScore + PositionUtils.getDistance(candidate.pos(), target);
                    open.add(nextNode);
                }
            });
        }
        return Optional.empty();
    }

    /**
     * Processes found paths according to config, displays them if waypoint graph editor is inactive.
     */
    private static List<BlockPos> processResults(ClientWorld world, List<BlockPos> path) {
        if (cfg.useAstarSmoothing) applySmoothing(world, path);
        if (cfg.useAstarOptimizing) optimizePath(path);
        if (!GraphEditor.active) setRenderPath(path);
        Output.chat("Found path with " + path.size() + " nodes.");
        return path;
    }

    /**
     * Applies smoothing to a path, making segments on flat surfaces more direct by removing excess nodes.
     */
    private static void applySmoothing(ClientWorld world, List<BlockPos> path) {
        if (path.size() < 3) return;

        int start = 0;
        int end = 2;

        while (end != path.size()) {
            BlockPos pos1 = path.get(start);
            BlockPos pos2 = path.get(end);

            if (isLinkValid(world, pos1, pos2)) path.remove(end - 1);
            else {
                start++;
                end++;
            }
        }
    }

    /**
     * Used in path smoothing to check if smoothed path segments are traversable.
     */
    public static boolean isLinkValid(ClientWorld world, Vec3i pos1, Vec3i pos2) {
        if (pos1.getY() != pos2.getY()) return false;

        float steps = PositionUtils.getDistance(pos1, pos2) * 5;
        float xStep = (pos2.getX() - pos1.getX()) / steps;
        float zStep = (pos2.getZ() - pos1.getZ()) / steps;

        int targetX = pos2.getX();
        int targetZ = pos2.getZ();

        float pointerX = pos1.getX() + 0.5f;
        float pointerZ = pos1.getZ() + 0.5f;
        int pointerY = pos1.getY();

        List<BlockPos> positions = new ArrayList<>();
        int currentX = pos1.getX();
        int currentZ = pos1.getZ();

        for (int i = 0; i < steps; i++) {
            pointerX += xStep;
            pointerZ += zStep;
            int pointerIntX = (int) (pointerX < 0 ? pointerX - 1 : pointerX);
            int pointerIntZ = (int) (pointerZ < 0 ? pointerZ - 1 : pointerZ);

            if (!(pointerIntX == currentX && pointerIntZ == currentZ)) {
                if (pointerIntX == targetX && pointerIntZ == targetZ) break;
                positions.add(new BlockPos(pointerIntX, pointerY, pointerIntZ));
                currentX = pointerIntX;
                currentZ = pointerIntZ;
            }
        }
        if (world.getBlockState(new BlockPos(pos1.getX(), pos1.getY() - 1, pos1.getZ())).getBlock().equals(Blocks.WATER)) {
            for (BlockPos pos : positions) {
                if (!PositionUtils.isValidSwimPosition(world, pos)) return false;
            }
        }
        else {
            for (BlockPos pos : positions) {
                if (!PositionUtils.isValidWalkPosition(world, pos)) return false;
            }
        }
        return true;
    }

    /**
     * Optimizes a path by combining consecutive segments with the same vector.
     */
    private static void optimizePath(List<BlockPos> path) {
        if (path.size() < 3) return;

        int pointer = 0;

        int[] startVec = new int[3];
        int[] nextVec = new int[3];

        while (pointer + 2 != path.size()) {
            BlockPos pos1 = path.get(pointer);
            BlockPos pos2 = path.get(pointer + 1);
            BlockPos pos3 = path.get(pointer + 2);

            startVec[0] = pos1.getX() - pos2.getX();
            startVec[1] = pos1.getY() - pos2.getY();
            startVec[2] = pos1.getZ() - pos2.getZ();

            nextVec[0] = pos2.getX() - pos3.getX();
            nextVec[1] = pos2.getY() - pos3.getY();
            nextVec[2] = pos2.getZ() - pos3.getZ();

            while (startVec[0] == nextVec[0] && startVec[1] == nextVec[1] && startVec[2] == nextVec[2]) {
                path.remove(pointer + 1);
                pos2 = pos3;
                if (pointer + 2 == path.size()) return;
                pos3 = path.get(pointer + 2);

                nextVec[0] = pos2.getX() - pos3.getX();
                nextVec[1] = pos2.getY() - pos3.getY();
                nextVec[2] = pos2.getZ() - pos3.getZ();
            }
            pointer++;
        }
    }

    private static void setRenderPath(List<BlockPos> path) {
        DebugManager.updatingLines = true;
        DebugManager.lines.clear();
        for (int i = 0; i < path.size() - 1; i++)
            DebugManager.lines.add(new Pair<>(path.get(i).toCenterPos(), path.get(i + 1).toCenterPos()));
        DebugManager.updatingLines = false;
    }
}
