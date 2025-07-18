package net.pathfinder.main.graph;

import me.x150.renderer.render.Renderer3d;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Vec3d;
import net.pathfinder.main.Output;

import java.util.ArrayList;
import java.util.List;

import static net.pathfinder.main.config.PFConfig.cfg;

//todo merge graph renderers
/**
 * Debug renderer used for displaying Base and A* graphs.
 */
public class GraphRenderer {

    public static boolean enabled = true;

    private static final Vec3d BOX = new Vec3d(1d, 1d, 1d);

    public static final List<Pair<Vec3d, Vec3d>> lines = new ArrayList<>();

    public static void render(MatrixStack matrices) {
        renderLines(matrices);
        renderPositions(matrices);
    }

    public static void renderLines(MatrixStack matrices) {
        for (Pair<Vec3d, Vec3d> line : lines)
            Renderer3d.renderLine(matrices, cfg.lineColour, line.getLeft(), line.getRight());
    }

    public static void renderPositions(MatrixStack matrices) {
        if (RuleHolder.start != null)
            Renderer3d.renderEdged(matrices, cfg.startFillColour, cfg.startColour, RuleHolder.start3d, BOX);
        if (RuleHolder.target != null)
            Renderer3d.renderEdged(matrices, cfg.selectedFillColour, cfg.selectedColour, RuleHolder.target3d, BOX);
    }

    @SuppressWarnings("SameReturnValue")
    public static int toggleRender() {
        enabled = !enabled;
        Output.actionBar(enabled ? "Graph rendering enabled." : "Graph rendering disabled.", Output.Color.GOLD);
        return 1;
    }
}
