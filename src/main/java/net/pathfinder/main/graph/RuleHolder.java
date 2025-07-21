package net.pathfinder.main.graph;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.pathfinder.main.graph.waypoint.GraphEditor;
import net.pathfinder.main.graph.waypoint.data.Waypoint;

import java.util.Objects;

import static net.pathfinder.main.datagen.BlockTagProvider.*;
import static net.pathfinder.main.config.PFConfig.cfg;

//todo compress tags for performance
/**
 * Contains "rule" methods used for determining valid path positions and distance between them.
 */
public class RuleHolder {

    public static boolean isPassable(ClientWorld world, BlockPos pos, int xVec, int yVec, int zVec) {
        BlockState state = world.getBlockState(pos.mutableCopy().add(xVec, yVec, zVec));
        return !state.getBlock().collidable || isIn(PASSABLE, state);
    }

    public static boolean isPassable(ClientWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return !state.getBlock().collidable || isIn(PASSABLE, state);
    }

    public static boolean isStandable(ClientWorld world, BlockPos pos, int xVec, int yVec, int zVec) {
        BlockState state = world.getBlockState(pos.mutableCopy().add(xVec, yVec, zVec));
        return (state.getBlock().collidable && !isIn(CARPETS, state) && !state.getBlock().equals(Blocks.LIGHT))
                || state.isIn(BlockTags.CLIMBABLE) || state.getBlock().equals(Blocks.WATER);
    }

    public static boolean isStandable(ClientWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return (state.getBlock().collidable && !isIn(CARPETS, state) && !state.getBlock().equals(Blocks.LIGHT))
                || state.isIn(BlockTags.CLIMBABLE) || state.getBlock().equals(Blocks.WATER);
    }

    public static boolean notFence(ClientWorld world, BlockPos pos, int xVec, int yVec, int zVec) {
        BlockState state = world.getBlockState(pos.mutableCopy().add(xVec, yVec, zVec));
        return !state.isIn(BlockTags.FENCES) && !state.isIn(BlockTags.WALLS);
    }

    public static boolean isSafe(ClientWorld world, BlockPos pos) {
        return !isIn(DANGEROUS, world.getBlockState(pos));
    }

    public static boolean isSolid(ClientWorld world, BlockPos pos, int xVec, int yVec, int zVec) {
        return world.getBlockState(pos.mutableCopy().add(xVec, yVec, zVec)).getBlock().collidable;
    }

    public static boolean isClimbable(ClientWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.isIn(BlockTags.CLIMBABLE) || state.getBlock().equals(Blocks.WATER);
    }

    public static boolean isValidPosition(ClientWorld world, BlockPos pos) {
        boolean b1 = RuleHolder.isStandable(world, pos, 0, -1, 0);
        boolean b2 = RuleHolder.isPassable(world, pos) && isSafe(world, pos);
        boolean b3 = RuleHolder.isPassable(world, pos, 0, 1, 0);

        return b1 && b2 && b3;
    }

    public static boolean outOfRangeTrue(BlockPos pos) {
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

    public static float getDistance(float[] pos1, BlockPos pos2) {
        return getDistance(pos1[0], pos1[1], pos1[2], pos2.getX(), pos2.getY(), pos2.getZ());
    }

    public static float getDistance(BlockPos pos1, BlockPos pos2) {
        return getDistance(pos1.getX(), pos1.getY(), pos1.getZ(), pos2.getX(), pos2.getY(), pos2.getZ());
    }

    public static float getDistance(BlockPos pos) {
        ClientPlayerEntity player = Objects.requireNonNull(MinecraftClient.getInstance().player);
        return getDistance((float) player.getX(), (float) player.getY(), (float) player.getZ(), pos.getX(), pos.getY(), pos.getZ());
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

    public static int getSquaredDistance(BlockPos pos1, BlockPos pos2) {
        return getSquaredDistance(pos1.getX(), pos1.getY(), pos1.getZ(), pos2.getX(), pos2.getY(), pos2.getZ());
    }

    public static int getSquaredDistance(BlockPos pos) {
        ClientPlayerEntity player = Objects.requireNonNull(MinecraftClient.getInstance().player);
        return getSquaredDistance(player.getBlockX(), player.getBlockY(), player.getBlockZ(), pos.getX(), pos.getY(), pos.getZ());
    }

    public static int getSquaredDistance(Waypoint w1, Waypoint w2) {
        return getSquaredDistance(w1.x(), w1.y(), w1.z(), w2.x(), w2.y(), w2.z());
    }

    public static int getSquaredDistance(Waypoint waypoint, BlockPos pos) {
        return getSquaredDistance(waypoint.x(), waypoint.y(), waypoint.z(), pos.getX(), pos.getY(), pos.getZ());
    }

    public static boolean isInRange(int x1, int y1, int z1, int x2, int y2, int z2, int range) {
        return x2 >  x1 - range && x2 < x1 + range && y2 > y1 - range && y2 < y1 + range && z2 > z1 - range && z2 < z1 + range;
    }

    public static boolean isInRange(BlockPos p1, Vec3d p2) {
        return isInRange(p1.getX(), p1.getY(), p1.getZ(), (int) p2.x, (int) p2.y, (int) p2.z, cfg.renderRange);
    }

    public static boolean isInRange(BlockPos p1, Vec3d p2, int range) {
        return isInRange(p1.getX(), p1.getY(), p1.getZ(), (int) p2.x, (int) p2.y, (int) p2.z, range);
    }

    public static boolean isInRange(BlockPos p1, BlockPos p2, int range) {
        return isInRange(p1.getX(), p1.getY(), p1.getZ(), p2.getX(), p2.getY(), p2.getZ(), range);
    }

    public static boolean isInRange(Waypoint p1, Waypoint p2, int range) {
        return isInRange(p1.x(), p1.y(), p1.z(), p2.x(), p2.y(), p2.z(), range);
    }
}
