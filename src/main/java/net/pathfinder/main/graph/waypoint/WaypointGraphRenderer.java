package net.pathfinder.main.graph.waypoint;

import io.netty.util.collection.LongObjectHashMap;
import me.x150.renderer.render.Renderer3d;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.pathfinder.main.Output;
import net.pathfinder.main.PathfinderMod;
import net.pathfinder.main.graph.RuleHolder;
import net.pathfinder.main.graph.waypoint.data.LocationData;
import net.pathfinder.main.graph.waypoint.data.Waypoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.pathfinder.main.config.PFConfig.cfg;

//todo different line colors for connections between teleporters
//todo one-way travel indicators
//todo move away from using the Renderer library
//todo do precise changes after editing instead of recomputing all elements
/**
 * Displays all the visual parts during graph editing.
 */
public class WaypointGraphRenderer {

    private static final TextRenderer tr = MinecraftClient.getInstance().textRenderer;

    public static final AtomicBoolean updateLocked = new AtomicBoolean(false);

    public static List<Pair<Vec3d, Vec3d>> lines;
    public static List<Pair<Vec3d, Vec3d>> linesActive;
    public static List<Vec3d> teleports;
    public static List<Vec3d> teleportsActive;
    private static ChunkPos lastPos;
    public static boolean renderThroughWalls = false;

    private static final Text ENABLED_TEXT = Text.literal("Waypoint Graph Editor").setStyle(Style.EMPTY.withColor(Formatting.GREEN));
    private static final MutableText POSITION_TEXT = Text.literal("Start: ");

    private static final Text CONTROLS_TEXT = Text.of("Hold shift to show controls.");
    private static final Text SELECT_TEXT = Text.of("Left click to select a waypoint.");
    private static final Text REMOVE_MODE_TEXT = Text.of("Left click to delete a waypoint.");
    private static final Text CLEAR_SELECTION_TEXT = Text.of("Press Alt to clear the clear selection.");
    private static final Text REMOVE_TEXT = Text.of("Press Del to remove the selected waypoint.");
    private static final Text NEW_PATH_TEXT = Text.of("Right click a block to build a path from selected/nearest waypoint.");
    private static final Text CONNECTION_TEXT = Text.of("Left click a second waypoint to build a path/remove direct connection.");
    private static final Text STRAIGHT_CONNECTION_TEXT = Text.of("Left click a second waypoint to toggle their connection.");
    private static final Text SCREEN_TEXT = Text.of("Left click the selected waypoint again for location editing.");

    private static final Vec3d BOX_ONE = new Vec3d(1d, 1d, 1d);
    private static final Vec3d BOX_HALF = new Vec3d(0.5d, 0.5d, 0.5d);

    public static void render(MatrixStack matrices) {
        if (renderThroughWalls) Renderer3d.renderThroughWalls();
        renderLines(matrices);
        renderTeleports(matrices);
        renderPoints(matrices);
    }

    public static void renderDisplay(DrawContext context) {
        if (!GraphEditor.active) return;

        List<Text> texts = new ArrayList<>();
        texts.add(ENABLED_TEXT);
        texts.add(Text.literal("Mode: ").setStyle(Style.EMPTY.withColor(Formatting.GOLD))
                        .append(Text.literal(GraphEditor.mode.name()).setStyle(Style.EMPTY.withColor(GraphEditor.mode.color))));

        MutableText position = POSITION_TEXT.copy();
        Waypoint w = GraphEditor.selected;
        if (w == null) position.append(Text.of("Unset"));
        else position.append(Text.literal(w.x() + " " + w.y() + " " + w.z()).setStyle(Style.EMPTY.withColor(Formatting.GREEN)));
        texts.add(position);

        texts.add(Text.empty());

        if (!Screen.hasShiftDown()) texts.add(CONTROLS_TEXT);
        else {
            //todo update texts only when keybind is changed
            texts.add(Text.of((char) KeyBindingHelper.getBoundKeyOf(PathfinderMod.modeToggleKey).getCode() + " to change selection mode."));
            texts.add(Text.of((char) KeyBindingHelper.getBoundKeyOf(PathfinderMod.appendKey).getCode() + " to append current selection."));
            texts.add(Text.of((char) KeyBindingHelper.getBoundKeyOf(PathfinderMod.clearKey).getCode() + " to clear current selection."));
            if (GraphEditor.mode == EditMode.DELETE) {
                texts.add(REMOVE_MODE_TEXT);
            }
            else {
                texts.add(SELECT_TEXT);
                if (GraphEditor.mode == EditMode.STRAIGHT) texts.add(STRAIGHT_CONNECTION_TEXT);
                else texts.add(CONNECTION_TEXT);
                texts.add(SCREEN_TEXT);
            }
            texts.add(CLEAR_SELECTION_TEXT);
            texts.add(REMOVE_TEXT);
            texts.add(NEW_PATH_TEXT);
        }

        float scale = cfg.textScale;
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.scale(scale, scale, scale);
        int y = 0;
        for (Text text : texts) {
            context.drawText(tr, text, 10, 35 + y, 16777215, true);
            y += 12;
        }
        matrices.pop();
    }

    private static void renderLines(MatrixStack matrices) {
        if (linesActive == null) return; //HOW the hell does that even happen

        if (renderThroughWalls) Renderer3d.renderThroughWalls();

        for (Pair<Vec3d, Vec3d> line : linesActive)
            Renderer3d.renderLine(matrices, cfg.lineColour, line.getLeft(), line.getRight());

        LongObjectHashMap<Waypoint> oldWaypoints = GraphEditor.waypointsState;
        LongObjectHashMap<Waypoint> newWaypoints = GraphEditor.currentSelection;
        if (!newWaypoints.isEmpty()) {
            LongObjectHashMap<Waypoint> closed = new LongObjectHashMap<>(newWaypoints.size());
            for (Waypoint waypoint : newWaypoints.values()) {
                if (closed.containsKey(waypoint.id())) continue;

                closed.put(waypoint.id(), waypoint);
                Vec3d vec3d1 = waypoint.pos().toCenterPos();

                for (long id : waypoint.neighbours()) {
                    if (!closed.containsKey(id)) {
                        Waypoint neighbour = newWaypoints.get(id);
                        if (neighbour == null) neighbour = oldWaypoints.get(id);

                        Renderer3d.renderLine(matrices, cfg.newLineColour, vec3d1, neighbour.pos().toCenterPos());
                    }
                }
            }
        }
    }

    public static void renderTeleports(MatrixStack matrices) {
        if (teleportsActive == null) return;

        if (renderThroughWalls) Renderer3d.renderThroughWalls();
        for (Vec3d pos : teleportsActive) {
            Renderer3d.renderEdged(matrices, cfg.teleportFillColour, cfg.teleportColour, pos, BOX_HALF);
        }
        if (renderThroughWalls) Renderer3d.stopRenderThroughWalls();
    }

    //todo simplify?
    private static void renderPoints(MatrixStack matrices) {
        Renderer3d.renderThroughWalls();
        Waypoint targeted = TargetHolder.targeted;
        Waypoint selected = GraphEditor.selected;
        if (targeted != null) {
            if (selected != null) {
                if (targeted != selected) {
                    Renderer3d.renderEdged(matrices, cfg.startFillColour, cfg.startColour, targeted.vec3d(), BOX_ONE);
                    Renderer3d.renderEdged(matrices, cfg.selectedFillColour, cfg.selectedColour, selected.vec3d(), BOX_ONE);
                }
                else Renderer3d.renderEdged(matrices, cfg.selectedTargetFillColour, cfg.selectedTargetColour, targeted.vec3d(), BOX_ONE);
            }
            else Renderer3d.renderEdged(matrices, cfg.startFillColour, cfg.startColour, targeted.vec3d(), BOX_ONE);
        }
        else if (selected != null)
            Renderer3d.renderEdged(matrices, cfg.selectedFillColour, cfg.selectedColour, selected.vec3d(), BOX_ONE);
        Renderer3d.stopRenderThroughWalls();
    }

    public static void updateRender() {
        Future<?> future = PathfinderMod.executor.submit(() -> {
            //Output.chat("Render update started.");
            lines = new ArrayList<>();
            assert MinecraftClient.getInstance().player != null;
            lastPos = MinecraftClient.getInstance().player.getChunkPos();

            LongObjectHashMap<Waypoint> waypoints = GraphEditor.waypointsState;
            LongObjectHashMap<Boolean> closed = new LongObjectHashMap<>(waypoints.size());

            for (Waypoint waypoint : waypoints.values()) {
                closed.put(waypoint.id(), Boolean.TRUE);
                Vec3d vec3d1 = waypoint.pos().toCenterPos();

                for (long id : waypoint.neighbours()) {
                    Waypoint neighbour = waypoints.get(id);
                    if (neighbour == null) neighbour = GraphEditor.currentSelection.get(id);
                    if (neighbour != null && !closed.containsKey(id)) lines.add(new Pair<>(vec3d1, neighbour.pos().toCenterPos()));
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
            //Output.chat("Render update ended.");
            assert MinecraftClient.getInstance().player != null;
            rangeUpdate(MinecraftClient.getInstance().player.getBlockPos());
        }
        catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public static void updatePosition() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) {
            ChunkPos currentPos = player.getChunkPos();
            if (currentPos != lastPos) {
                //Output.chat("Position update started.");
                lastPos = currentPos;
                rangeUpdate(player.getBlockPos());
                //Output.chat("Position update ended.");
            }
        }
    }

    private static void rangeUpdate(BlockPos playerPos) {
        if (updateLocked.get()) return;
        updateLocked.set(true);
        //Output.chat("Range update locked.");
        PathfinderMod.executor.submit(() -> {
            //Output.chat("Range update started.");
            List<Pair<Vec3d, Vec3d>> newLines = new ArrayList<>();
            for (Pair<Vec3d, Vec3d> line : lines) {
                if (RuleHolder.isInRange(playerPos, line.getLeft(), cfg.renderRange)
                        || RuleHolder.isInRange(playerPos, line.getRight(), cfg.renderRange))
                    newLines.add(line);
            }
            linesActive = newLines;

            List<Vec3d> newTeleports = new ArrayList<>();
            for (Vec3d teleport : teleports) {
                if (RuleHolder.isInRange(playerPos, teleport, cfg.renderRange / 2))
                    newTeleports.add(teleport);
            }
            teleportsActive = newTeleports;

            updateLocked.set(false);
            //Output.chat("Range update unlocked and ended.");
        });
    }

    @SuppressWarnings("SameReturnValue")
    public static int toggleDepthTest() {
        renderThroughWalls = !renderThroughWalls;
        Output.actionBar(renderThroughWalls ? "Depth test enabled." : "Depth test disabled.", Output.Color.GOLD);
        return 1;
    }

    public static void clear() {
        lines = null;
        linesActive = null;
        teleports = null;
        teleportsActive = null;
        lastPos = null;
    }
}
