package net.pathfinder.main.graph.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Vec3d;
import net.pathfinder.main.Output;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.List;

public class RenderUtils {

    static BufferBuilder buffer;

    public static boolean renderThroughWalls = false;

    public static void setupRender(VertexFormat.DrawMode mode) {
        buffer = RenderSystem.renderThreadTesselator().begin(mode, VertexFormats.POSITION_COLOR);

        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.depthFunc(renderThroughWalls ? GL11.GL_ALWAYS : GL11.GL_LEQUAL);
    }

    public static void endRender() {
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        buffer = null;
    }

    public static void drawLines(Matrix4f matrices, List<Pair<Vec3d, Vec3d>> lines, float[] colour) {
        if (lines == null || lines.isEmpty()) return;

        setupRender(VertexFormat.DrawMode.DEBUG_LINES);
        for (Pair<Vec3d, Vec3d> line : lines) drawLine(matrices, line.getLeft(), line.getRight(), colour);
        endRender();
    }

    public static void drawLine(Matrix4f matrices, Vec3d p1, Vec3d p2, float[] colour) {
        buffer.vertex(matrices, (float) p1.x, (float) p1.y, (float) p1.z).color(colour[0], colour[1], colour[2], colour[3]);
        buffer.vertex(matrices, (float) p2.x, (float) p2.y, (float) p2.z).color(colour[0], colour[1], colour[2], colour[3]);
    }

    public static void drawBoxes(Matrix4f matrices, List<Vec3d> positions, float size, float[] colour, float[] fillColour) {
        if (positions == null || positions.isEmpty()) return;

        Vec3d[] positions2 = new Vec3d[positions.size()];
        for (int i = 0; i < positions.size(); i++) positions2[i] = positions.get(i).add(size);

        setupRender(VertexFormat.DrawMode.DEBUG_LINES);
        for (int i = 0; i < positions.size(); i++) drawBoxEdges(matrices, positions.get(i), positions2[i], colour);
        endRender();

        setupRender(VertexFormat.DrawMode.QUADS);
        for (int i = 0; i < positions.size(); i++) drawBoxFill(matrices, positions.get(i), positions2[i], fillColour);
        endRender();
    }

    public static void drawBox(Matrix4f matrices, Vec3d pos, float size, float[] colour, float[] fillColour) {
        Vec3d pos2 = pos.add(size);
        setupRender(VertexFormat.DrawMode.DEBUG_LINES);
        drawBoxEdges(matrices, pos, pos2, colour);
        endRender();

        setupRender(VertexFormat.DrawMode.QUADS);
        drawBoxFill(matrices, pos, pos2, fillColour);
        endRender();
    }

    public static void drawBoxEdges(Matrix4f matrices, Vec3d p1, Vec3d p2, float[] colour) {
        float x1 = (float) p1.x;
        float y1 = (float) p1.y;
        float z1 = (float) p1.z;
        float x2 = (float) p2.x;
        float y2 = (float) p2.y;
        float z2 = (float) p2.z;

        buffer.vertex(matrices, x1, y1, z1).color(colour[0], colour[1], colour[2], colour[3]);
        buffer.vertex(matrices, x1, y1, z2).color(colour[0], colour[1], colour[2], colour[3]);

        buffer.vertex(matrices, x1, y1, z2).color(colour[0], colour[1], colour[2], colour[3]);
        buffer.vertex(matrices, x2, y1, z2).color(colour[0], colour[1], colour[2], colour[3]);

        buffer.vertex(matrices, x2, y1, z2).color(colour[0], colour[1], colour[2], colour[3]);
        buffer.vertex(matrices, x2, y1, z1).color(colour[0], colour[1], colour[2], colour[3]);

        buffer.vertex(matrices, x2, y1, z1).color(colour[0], colour[1], colour[2], colour[3]);
        buffer.vertex(matrices, x1, y1, z1).color(colour[0], colour[1], colour[2], colour[3]);

        buffer.vertex(matrices, x1, y2, z1).color(colour[0], colour[1], colour[2], colour[3]);
        buffer.vertex(matrices, x1, y2, z2).color(colour[0], colour[1], colour[2], colour[3]);

        buffer.vertex(matrices, x1, y2, z2).color(colour[0], colour[1], colour[2], colour[3]);
        buffer.vertex(matrices, x2, y2, z2).color(colour[0], colour[1], colour[2], colour[3]);

        buffer.vertex(matrices, x2, y2, z2).color(colour[0], colour[1], colour[2], colour[3]);
        buffer.vertex(matrices, x2, y2, z1).color(colour[0], colour[1], colour[2], colour[3]);

        buffer.vertex(matrices, x2, y2, z1).color(colour[0], colour[1], colour[2], colour[3]);
        buffer.vertex(matrices, x1, y2, z1).color(colour[0], colour[1], colour[2], colour[3]);

        buffer.vertex(matrices, x1, y1, z1).color(colour[0], colour[1], colour[2], colour[3]);
        buffer.vertex(matrices, x1, y2, z1).color(colour[0], colour[1], colour[2], colour[3]);

        buffer.vertex(matrices, x2, y1, z1).color(colour[0], colour[1], colour[2], colour[3]);
        buffer.vertex(matrices, x2, y2, z1).color(colour[0], colour[1], colour[2], colour[3]);

        buffer.vertex(matrices, x2, y1, z2).color(colour[0], colour[1], colour[2], colour[3]);
        buffer.vertex(matrices, x2, y2, z2).color(colour[0], colour[1], colour[2], colour[3]);

        buffer.vertex(matrices, x1, y1, z2).color(colour[0], colour[1], colour[2], colour[3]);
        buffer.vertex(matrices, x1, y2, z2).color(colour[0], colour[1], colour[2], colour[3]);
    }

    public static void drawBoxFill(Matrix4f matrices, Vec3d p1, Vec3d p2, float[] colour) {
        float x1 = (float) p1.x;
        float y1 = (float) p1.y;
        float z1 = (float) p1.z;
        float x2 = (float) p2.x;
        float y2 = (float) p2.y;
        float z2 = (float) p2.z;
        
        buffer.vertex(matrices, x1, y2, z1).color(colour[0], colour[1], colour[2], colour[3]);
        buffer.vertex(matrices, x1, y2, z2).color(colour[0], colour[1], colour[2], colour[3]);
        buffer.vertex(matrices, x2, y2, z2).color(colour[0], colour[1], colour[2], colour[3]);
        buffer.vertex(matrices, x2, y2, z1).color(colour[0], colour[1], colour[2], colour[3]);

        buffer.vertex(matrices, x1, y1, z2).color(colour[0], colour[1], colour[2], colour[3]);
        buffer.vertex(matrices, x2, y1, z2).color(colour[0], colour[1], colour[2], colour[3]);
        buffer.vertex(matrices, x2, y2, z2).color(colour[0], colour[1], colour[2], colour[3]);
        buffer.vertex(matrices, x1, y2, z2).color(colour[0], colour[1], colour[2], colour[3]);

        buffer.vertex(matrices, x2, y2, z2).color(colour[0], colour[1], colour[2], colour[3]);
        buffer.vertex(matrices, x2, y1, z2).color(colour[0], colour[1], colour[2], colour[3]);
        buffer.vertex(matrices, x2, y1, z1).color(colour[0], colour[1], colour[2], colour[3]);
        buffer.vertex(matrices, x2, y2, z1).color(colour[0], colour[1], colour[2], colour[3]);

        buffer.vertex(matrices, x2, y2, z1).color(colour[0], colour[1], colour[2], colour[3]);
        buffer.vertex(matrices, x2, y1, z1).color(colour[0], colour[1], colour[2], colour[3]);
        buffer.vertex(matrices, x1, y1, z1).color(colour[0], colour[1], colour[2], colour[3]);
        buffer.vertex(matrices, x1, y2, z1).color(colour[0], colour[1], colour[2], colour[3]);

        buffer.vertex(matrices, x1, y2, z1).color(colour[0], colour[1], colour[2], colour[3]);
        buffer.vertex(matrices, x1, y1, z1).color(colour[0], colour[1], colour[2], colour[3]);
        buffer.vertex(matrices, x1, y1, z2).color(colour[0], colour[1], colour[2], colour[3]);
        buffer.vertex(matrices, x1, y2, z2).color(colour[0], colour[1], colour[2], colour[3]);

        buffer.vertex(matrices, x1, y1, z1).color(colour[0], colour[1], colour[2], colour[3]);
        buffer.vertex(matrices, x2, y1, z1).color(colour[0], colour[1], colour[2], colour[3]);
        buffer.vertex(matrices, x2, y1, z2).color(colour[0], colour[1], colour[2], colour[3]);
        buffer.vertex(matrices, x1, y1, z2).color(colour[0], colour[1], colour[2], colour[3]);
    }

    @SuppressWarnings("SameReturnValue")
    public static int toggleDepthTest() {
        renderThroughWalls = !renderThroughWalls;
        Output.actionBar(renderThroughWalls ? "Depth test enabled." : "Depth test disabled.", Output.Color.GOLD);
        return 1;
    }
}
