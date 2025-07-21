package net.pathfinder.main.graph;

import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.pathfinder.main.graph.astar.AstarBuilder;

import java.util.ArrayList;
import java.util.List;

public class DebugManager {

    public static BlockPos start;
    public static BlockPos target;
    public static Vec3d start3d;
    public static Vec3d target3d;

    public static List<Pair<Vec3d, Vec3d>> lines = new ArrayList<>();

    public static void updateStart(BlockPos pos) {
        start = pos;
        start3d = Vec3d.of(pos);
        if (target != null) AstarBuilder.findOnUpdate();
    }

    public static void updateTarget(BlockPos pos) {
        target = pos;
        target3d = Vec3d.of(pos);
        if (start != null) AstarBuilder.findOnUpdate();
    }

    public static boolean shouldRender() {
        return !lines.isEmpty() || start != null || target != null;
    }

    public static void clear() {
        start = null;
        target = null;
        start3d = null;
        target3d = null;
        lines = new ArrayList<>();
    }
}