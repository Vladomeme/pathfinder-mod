package net.pathfinder.main.graph.render;

import io.netty.util.collection.LongObjectHashMap;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.pathfinder.main.PathfinderMod;
import net.pathfinder.main.graph.DebugManager;
import net.pathfinder.main.graph.PositionUtils;
import net.pathfinder.main.graph.waypoint.GraphEditor;
import net.pathfinder.main.graph.waypoint.TargetHolder;
import net.pathfinder.main.graph.waypoint.WaypointIO;
import net.pathfinder.main.graph.waypoint.data.LocationData;
import net.pathfinder.main.graph.waypoint.data.Waypoint;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.pathfinder.main.config.PFConfig.cfg;
import static net.pathfinder.main.graph.render.RenderUtils.*;

//todo do precise changes after editing instead of recomputing all elements
public class GraphRenderer {

    public static final AtomicBoolean updateLocked = new AtomicBoolean(false);

    public static List<Pair<Vec3d, Vec3d>> lines;
    public static List<Pair<Vec3d, Vec3d>> linesActive;
    public static List<Pair<Vec3d, Vec3d>> teleportLines;
    public static List<Pair<Vec3d, Vec3d>> teleportLinesActive;
    public static List<Vec3d> teleports;
    public static List<Vec3d> teleportsActive;
    private static ChunkPos lastPos;

    public static void render(WorldRenderContext context) {
        MatrixStack matrices = Objects.requireNonNull(context.matrixStack());
        Vec3d camera = MinecraftClient.getInstance().gameRenderer.getCamera().getPos();

        matrices.push();
        matrices.translate(-camera.getX(), -camera.getY(), -camera.getZ());
        Matrix4f matrix4f = matrices.peek().getPositionMatrix();
        renderLines(matrix4f);
        renderTeleports(matrix4f);
        renderPoints(matrix4f);
        matrices.pop();
    }

    private static void renderLines(Matrix4f matrices) {
        drawLines(matrices, linesActive, cfg.lineColour4f);
        drawLines(matrices, teleportLinesActive, cfg.teleportColour4f);

        LongObjectHashMap<Waypoint> oldWaypoints = GraphEditor.waypointsState;
        LongObjectHashMap<Waypoint> newWaypoints = GraphEditor.currentSelection;

        if (newWaypoints.isEmpty()) return;

        setupRender(VertexFormat.DrawMode.DEBUG_LINES);
        LongObjectHashMap<Waypoint> closed = new LongObjectHashMap<>(newWaypoints.size());
        for (Waypoint waypoint : newWaypoints.values()) {
            if (closed.containsKey(waypoint.id())) continue;

            closed.put(waypoint.id(), waypoint);
            Vec3d vec3d1 = waypoint.pos().toCenterPos();

            for (long id : waypoint.neighbours()) {
                if (closed.containsKey(id)) continue;

                Waypoint neighbour = Objects.requireNonNullElseGet(newWaypoints.get(id), () -> oldWaypoints.get(id));
                drawLine(matrices, vec3d1, neighbour.pos().toCenterPos(), cfg.newLineColour4f);
            }
        }
        endRender();
    }

    public static void renderTeleports(Matrix4f matrices) {
        drawBoxes(matrices, teleportsActive, 0.5f, cfg.teleportColour4f, cfg.teleportFillColour4f);
    }

    private static void renderPoints(Matrix4f matrices) {
        Waypoint targeted = TargetHolder.targeted;
        Waypoint selected = GraphEditor.selected;

        if (targeted != null) {
            if (targeted != selected)
                drawBox(matrices, targeted.vec3d(), 1, cfg.startColour4f, cfg.startFillColour4f);
            else
                drawBox(matrices, targeted.vec3d(), 1, cfg.selectedTargetColour4f, cfg.selectedTargetFillColour4f);
        }
        if (selected != null)
            drawBox(matrices, selected.vec3d(), 1, cfg.selectedColour4f, cfg.selectedFillColour4f);
    }

    public static void updateElements() {
        Future<?> future = PathfinderMod.executor.submit(() -> {
            lines = new ArrayList<>();
            teleportLines = new ArrayList<>();
            lastPos = Objects.requireNonNull(MinecraftClient.getInstance().player).getChunkPos();

            LongObjectHashMap<Waypoint> waypoints = GraphEditor.waypointsState;
            LongObjectHashMap<Boolean> closed = new LongObjectHashMap<>(waypoints.size());

            for (Waypoint waypoint : waypoints.values()) {
                if (closed.containsKey(waypoint.id())) continue;
                closed.put(waypoint.id(), Boolean.TRUE);
                Vec3d vec3d1 = waypoint.pos().toCenterPos();
                LocationData data1 = WaypointIO.getData().locations.get(waypoint.id());
                if (data1 == null || !data1.isTeleport()) {
                    for (long id : waypoint.neighbours()) {
                        if (!closed.containsKey(id)) {
                            Waypoint neighbour = waypoints.get(id);
                            if (neighbour == null) neighbour = GraphEditor.currentSelection.get(id);
                            if (neighbour != null) lines.add(new Pair<>(vec3d1, neighbour.pos().toCenterPos()));
                        }
                    }
                }
                else {
                    for (long id : waypoint.neighbours()) {
                        if (!closed.containsKey(id)) {
                            Waypoint neighbour = waypoints.get(id);
                            if (neighbour == null) neighbour = GraphEditor.currentSelection.get(id);
                            if (neighbour != null) {
                                LocationData data2 = WaypointIO.getData().locations.get(neighbour.id());
                                if (data2 != null && data2.isTeleport())
                                    teleportLines.add(new Pair<>(vec3d1, neighbour.pos().toCenterPos()));
                                else
                                    lines.add(new Pair<>(vec3d1, neighbour.pos().toCenterPos()));
                            }
                        }
                    }
                }
            }
            teleports = new ArrayList<>();
            for (LocationData info : GraphEditor.locationsState.values()) {
                Waypoint teleport = Objects.requireNonNullElseGet(waypoints.get(info.id()), () -> GraphEditor.currentSelection.get(info.id()));
                if (info.isTeleport()) teleports.add(teleport.teleportPos());
            }
        });
        try {
            future.get();
            rangeUpdate(Objects.requireNonNull(MinecraftClient.getInstance().player).getBlockPos());
        }
        catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public static void updatePosition(ClientPlayerEntity player) {
        ChunkPos currentPos = player.getChunkPos();

        if (currentPos != lastPos) {
            lastPos = currentPos;
            rangeUpdate(player.getBlockPos());
        }
    }

    private static void rangeUpdate(BlockPos playerPos) {
        if (updateLocked.get()) return;
        updateLocked.set(true);
        PathfinderMod.executor.submit(() -> {
            try {
                List<Pair<Vec3d, Vec3d>> newLines = new ArrayList<>();
                for (Pair<Vec3d, Vec3d> line : lines) {
                    if (PositionUtils.isInRange(playerPos, line.getLeft()) || PositionUtils.isInRange(playerPos, line.getRight()))
                        newLines.add(line);
                }
                linesActive = newLines;

                List<Pair<Vec3d, Vec3d>> newTeleportLines = new ArrayList<>();
                for (Pair<Vec3d, Vec3d> line : teleportLines) {
                    if (PositionUtils.isInRange(playerPos, line.getLeft()) || PositionUtils.isInRange(playerPos, line.getRight()))
                        newTeleportLines.add(line);
                }
                teleportLinesActive = newTeleportLines;

                List<Vec3d> newTeleports = new ArrayList<>();
                for (Vec3d teleport : teleports) {
                    if (PositionUtils.isInRange(playerPos, teleport, cfg.renderRange))
                        newTeleports.add(teleport);
                }
                teleportsActive = newTeleports;
            }
            finally {
                updateLocked.set(false);
            }
        });
    }

    public static void renderDebug(MatrixStack matrices) {
        Vec3d camera = MinecraftClient.getInstance().gameRenderer.getCamera().getPos();

        matrices.push();
        matrices.translate(-camera.getX(), -camera.getY(), -camera.getZ());
        Matrix4f matrix4f = matrices.peek().getPositionMatrix();
        if (!DebugManager.updatingLines) drawLines(matrix4f, DebugManager.lines, cfg.lineColour4f);
        if (DebugManager.start != null) drawBox(matrix4f, DebugManager.start3d, 1, cfg.startColour4f, cfg.startFillColour4f);
        if (DebugManager.target != null) drawBox(matrix4f, DebugManager.target3d, 1, cfg.selectedColour4f, cfg.selectedFillColour4f);
        matrices.pop();
    }

    public static void clear() {
        lines = null;
        linesActive = null;
        teleports = null;
        teleportsActive = null;
        lastPos = null;
    }
}
