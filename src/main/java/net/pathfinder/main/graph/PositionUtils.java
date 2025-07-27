package net.pathfinder.main.graph;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.pathfinder.main.graph.waypoint.GraphEditor;
import net.pathfinder.main.graph.waypoint.data.Waypoint;

import java.util.Objects;

import static net.pathfinder.main.datagen.BlockTagProvider.*;
import static net.pathfinder.main.config.PFConfig.cfg;

//todo compress tags for performance
/**
 * Contains "rule" methods used for determining valid path positions and distance between them.
 */
public class PositionUtils {

    public static boolean isPassable(ClientWorld world, BlockPos pos, int xVec, int yVec, int zVec) {
        BlockState state = world.getBlockState(add(pos, xVec, yVec, zVec));
        return !state.getBlock().collidable || isIn(PASSABLE, state);
    }

    public static boolean isPassable(ClientWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return !state.getBlock().collidable || isIn(PASSABLE, state);
    }

    public static boolean isStandable(ClientWorld world, BlockPos pos, int xVec, int yVec, int zVec) {
        BlockState state = world.getBlockState(add(pos, xVec, yVec, zVec));
        return (state.getBlock().collidable && !isIn(CARPETS, state) && !state.getBlock().equals(Blocks.LIGHT))
                || state.isIn(BlockTags.CLIMBABLE) || state.getBlock().equals(Blocks.WATER);
    }

    public static boolean isStandable(ClientWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return (state.getBlock().collidable && !isIn(CARPETS, state) && !state.getBlock().equals(Blocks.LIGHT))
                || state.isIn(BlockTags.CLIMBABLE) || state.getBlock().equals(Blocks.WATER);
    }

    public static boolean isStandableSolid(ClientWorld world, BlockPos pos, int xVec, int yVec, int zVec) {
        BlockState state = world.getBlockState(add(pos, xVec, yVec, zVec));
        return (state.getBlock().collidable && !isIn(CARPETS, state) && !state.getBlock().equals(Blocks.LIGHT));
    }

    public static boolean notFence(ClientWorld world, BlockPos pos, int xVec, int yVec, int zVec) {
        BlockState state = world.getBlockState(add(pos, xVec, yVec, zVec));
        return !state.isIn(BlockTags.FENCES) && !state.isIn(BlockTags.WALLS);
    }

    public static boolean isSafe(ClientWorld world, BlockPos pos) {
        return !isIn(DANGEROUS, world.getBlockState(pos));
    }

    public static boolean isSolid(ClientWorld world, BlockPos pos, int xVec, int yVec, int zVec) {
        return world.getBlockState(add(pos, xVec, yVec, zVec)).getBlock().collidable;
    }

    public static boolean isClimbable(ClientWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.isIn(BlockTags.CLIMBABLE) || state.getBlock().equals(Blocks.WATER);
    }

    public static boolean isValidPosition(ClientWorld world, BlockPos pos) {
        boolean b1 = isStandable(world, pos, 0, -1, 0);
        boolean b2 = isPassable(world, pos) && isSafe(world, pos);
        boolean b3 = isPassable(world, pos, 0, 1, 0);

        return b1 && b2 && b3;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isValidWalkPosition(ClientWorld world, BlockPos pos) {
        boolean b1 = isStandableSolid(world, pos, 0, -1, 0);
        boolean b2 = isPassable(world, pos) && isSafe(world, pos);
        boolean b3 = isPassable(world, pos, 0, 1, 0);

        return b1 && b2 && b3;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isValidSwimPosition(ClientWorld world, BlockPos pos) {
        boolean b1 = world.getBlockState(addY(pos, -1)).getBlock().equals(Blocks.WATER);
        boolean b2 = isPassable(world, pos) && isSafe(world, pos);

        return b1 && b2;
    }

    public static BlockPos add(BlockPos pos, int x, int y, int z) {
        return new BlockPos(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
    }

    public static BlockPos addXY(BlockPos pos, int x, int y) {
        return new BlockPos(pos.getX() + x, pos.getY() + y, pos.getZ());
    }

    public static BlockPos addZY(BlockPos pos, int z, int y) {
        return new BlockPos(pos.getX(), pos.getY() + y, pos.getZ() + z);
    }

    public static BlockPos addXZ(BlockPos pos, int x, int z) {
        return new BlockPos(pos.getX() + x, pos.getY(), pos.getZ() + z);
    }

    public static BlockPos addX(BlockPos pos, int x) {
        return new BlockPos(pos.getX() + x, pos.getY(), pos.getZ());
    }

    public static BlockPos addY(BlockPos pos, int y) {
        return new BlockPos(pos.getX(), pos.getY() + y, pos.getZ());
    }

    public static BlockPos addZ(BlockPos pos, int z) {
        return new BlockPos(pos.getX(), pos.getY(), pos.getZ() + z);
    }

    public static boolean outOfRangeTrue(Vec3i pos) {
        if (GraphEditor.active && GraphEditor.selected != null) {
            Waypoint origin = GraphEditor.selected;
            return cfg.maxPathDistanceSquared < getSquaredDistance(origin.x(), origin.y(), origin.z(), pos.getX(), pos.getY(), pos.getZ());
        }
        BlockPos start = DebugManager.start;
        return cfg.maxPathDistanceSquared < getSquaredDistance(start.getX(), start.getY(), start.getZ(), pos.getX(), pos.getY(), pos.getZ());
    }

    public static float getDistance(float x1, float y1, float z1, float x2, float y2, float z2) {
        float x = x1 - x2;
        float y = y1 - y2;
        float z = z1 - z2;
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    public static float getDistance(float[] pos1, Vec3i pos2) {
        return getDistance(pos1[0], pos1[1], pos1[2], pos2.getX(), pos2.getY(), pos2.getZ());
    }

    public static float getDistance(Vec3i pos1, Vec3i pos2) {
        return getDistance(pos1.getX(), pos1.getY(), pos1.getZ(), pos2.getX(), pos2.getY(), pos2.getZ());
    }

    public static float getDistance(Vec3i pos) {
        ClientPlayerEntity player = Objects.requireNonNull(MinecraftClient.getInstance().player);
        return getDistance((float) player.getX(), (float) player.getY(), (float) player.getZ(), pos.getX(), pos.getY(), pos.getZ());
    }

    public static float getDistance(float x, float y, float z) {
        ClientPlayerEntity player = Objects.requireNonNull(MinecraftClient.getInstance().player);
        return getDistance((float) player.getX(), (float) player.getY(), (float) player.getZ(), x, y, z);
    }

    public static int getSquaredDistance(int x1, int y1, int z1, int x2, int y2, int z2) {
        int x = x1 - x2;
        int y = y1 - y2;
        int z = z1 - z2;
        return x * x + y * y + z * z;
    }

    public static float getSquaredDistance(float x1, float y1, float z1, float x2, float y2, float z2) {
        float x = x1 - x2;
        float y = y1 - y2;
        float z = z1 - z2;
        return x * x + y * y + z * z;
    }

    public static int getSquaredDistance(Vec3i pos1, Vec3i pos2) {
        return getSquaredDistance(pos1.getX(), pos1.getY(), pos1.getZ(), pos2.getX(), pos2.getY(), pos2.getZ());
    }

    public static int getSquaredDistance(Vec3i pos) {
        ClientPlayerEntity player = Objects.requireNonNull(MinecraftClient.getInstance().player);
        return getSquaredDistance(player.getBlockX(), player.getBlockY(), player.getBlockZ(), pos.getX(), pos.getY(), pos.getZ());
    }

    public static int getSquaredDistance(Waypoint w1, Waypoint w2) {
        return getSquaredDistance(w1.x(), w1.y(), w1.z(), w2.x(), w2.y(), w2.z());
    }

    public static int getSquaredDistance(Waypoint waypoint, Vec3i pos) {
        return getSquaredDistance(waypoint.x(), waypoint.y(), waypoint.z(), pos.getX(), pos.getY(), pos.getZ());
    }

    public static boolean isInRange(int x1, int y1, int z1, int x2, int y2, int z2, int range) {
        return x2 >  x1 - range && x2 < x1 + range && y2 > y1 - range && y2 < y1 + range && z2 > z1 - range && z2 < z1 + range;
    }

    public static boolean isInRange(Vec3i p1, Vec3d p2) {
        return isInRange(p1.getX(), p1.getY(), p1.getZ(), (int) p2.x, (int) p2.y, (int) p2.z, cfg.renderRange);
    }

    public static boolean isInRange(Vec3i p1, Vec3d p2, int range) {
        return isInRange(p1.getX(), p1.getY(), p1.getZ(), (int) p2.x, (int) p2.y, (int) p2.z, range);
    }

    public static boolean isInRange(Vec3i p1, Vec3i p2, int range) {
        return isInRange(p1.getX(), p1.getY(), p1.getZ(), p2.getX(), p2.getY(), p2.getZ(), range);
    }

    public static boolean isInRange(Waypoint p1, Waypoint p2, int range) {
        return isInRange(p1.x(), p1.y(), p1.z(), p2.x(), p2.y(), p2.z(), range);
    }
}
