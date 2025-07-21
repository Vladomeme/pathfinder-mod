package net.pathfinder.main.graph.render;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.pathfinder.main.PathfinderMod;
import net.pathfinder.main.graph.waypoint.EditMode;
import net.pathfinder.main.graph.waypoint.GraphEditor;
import net.pathfinder.main.graph.waypoint.data.Waypoint;

import java.util.ArrayList;
import java.util.List;

import static net.pathfinder.main.config.PFConfig.cfg;

public class HudRenderer {

    static final Style STYLE_GOLD = Style.EMPTY.withColor(Formatting.GOLD);
    static final Style STYLE_GREEN = Style.EMPTY.withColor(Formatting.GREEN);

    private static final Text ENABLED_TEXT = Text.literal("Waypoint Graph Editor").setStyle(STYLE_GREEN);

    private static Text MODE_TOGGLE_TEXT = Text.of((char) KeyBindingHelper.getBoundKeyOf(PathfinderMod.modeToggleKey).getCode() + " to change selection mode.");
    private static Text APPEND_TEXT = Text.of((char) KeyBindingHelper.getBoundKeyOf(PathfinderMod.appendKey).getCode() + " to append current selection.");
    private static Text CLEAR_TEXT = Text.of((char) KeyBindingHelper.getBoundKeyOf(PathfinderMod.clearKey).getCode() + " to clear current selection.");

    private static final Text CONTROLS_TEXT = Text.of("Hold shift to show controls.");
    private static final Text SELECT_TEXT = Text.of("Left click to select a waypoint.");
    private static final Text REMOVE_MODE_TEXT = Text.of("Left click to delete a waypoint.");
    private static final Text CLEAR_SELECTION_TEXT = Text.of("Press Alt to clear the clear selection.");
    private static final Text REMOVE_TEXT = Text.of("Press Del to remove the selected waypoint.");
    private static final Text NEW_PATH_TEXT = Text.of("Right click a block to build a path from selected/nearest waypoint.");
    private static final Text CONNECTION_TEXT = Text.of("Left click a second waypoint to build a path/remove direct connection.");
    private static final Text STRAIGHT_CONNECTION_TEXT = Text.of("Left click a second waypoint to toggle their connection.");
    private static final Text SCREEN_TEXT = Text.of("Left click the selected waypoint again for location editing.");

    public static void renderDisplay(DrawContext context) {
        List<Text> texts = new ArrayList<>();
        texts.add(ENABLED_TEXT);
        texts.add(Text.literal("Mode: ").setStyle(STYLE_GOLD)
                .append(Text.literal(GraphEditor.mode.name()).setStyle(Style.EMPTY.withColor(GraphEditor.mode.color))));

        Waypoint w = GraphEditor.selected;
        if (w == null) texts.add(Text.of("Start: unset"));
        else texts.add(Text.literal("Start: ").append(Text.literal(w.coordinates()).setStyle(STYLE_GREEN)));

        texts.add(Text.empty());

        if (!Screen.hasShiftDown()) texts.add(CONTROLS_TEXT);
        else {
            texts.add(MODE_TOGGLE_TEXT);
            texts.add(APPEND_TEXT);
            texts.add(CLEAR_TEXT);
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
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        for (Text text : texts) {
            context.drawText(tr, text, 10, 35 + y, 16777215, true);
            y += 12;
        }
        matrices.pop();
    }

    public static void updateKeybindTexts() {
        MODE_TOGGLE_TEXT = Text.of((char) KeyBindingHelper.getBoundKeyOf(PathfinderMod.modeToggleKey).getCode() + " to change selection mode.");
        APPEND_TEXT = Text.of((char) KeyBindingHelper.getBoundKeyOf(PathfinderMod.appendKey).getCode() + " to append current selection.");
        CLEAR_TEXT = Text.of((char) KeyBindingHelper.getBoundKeyOf(PathfinderMod.clearKey).getCode() + " to clear current selection.");
    }
}
