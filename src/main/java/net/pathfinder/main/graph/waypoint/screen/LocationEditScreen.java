package net.pathfinder.main.graph.waypoint.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.pathfinder.main.Output;
import net.pathfinder.main.graph.waypoint.GraphEditor;
import net.pathfinder.main.graph.waypoint.WaypointGraphRenderer;
import net.pathfinder.main.graph.waypoint.data.LocationData;

import java.util.Arrays;

import static net.pathfinder.main.config.PFConfig.cfg;

/**
 * A screen for viewing and editing Waypoint location data.
 */
public class LocationEditScreen extends Screen {

    private static final MinecraftClient client = MinecraftClient.getInstance();
    private final TextRenderer tr = client.textRenderer;

    private final long id;
    private final LocationData info;

    private TextButtonWidget teleportToggle;
    private TextFieldWidget nameField;
    private TextFieldWidget areaField;

    private int lastKey = 0;
    final int centerX;
    final int centerY;

    public LocationEditScreen(long id) {
        super(Text.of(""));

        this.id = id;
        this.info = GraphEditor.getLocationsState().get(id);
        this.centerX = client.getWindow().getScaledWidth() / 2;
        this.centerY = client.getWindow().getScaledHeight() / 2;
    }

    @Override
    protected void init() {
        addElements();
        super.init();
    }

    private void addElements() {
        Style style = Style.EMPTY.withItalic(true).withColor(-10197916);

        addDrawableChild(teleportToggle = new TextButtonWidget(TextButtonWidget.Type.Normal, centerX - 80, centerY - 52,
                50, 11, "false", button -> toggleTeleport()));

        addDrawableChild(nameField = new TextFieldWidget(tr, centerX - 80, centerY - 36, 160, 12, Text.of("")));
        nameField.setPlaceholder(Text.of("name").getWithStyle(style).get(0));
        nameField.setMaxLength(9999);

        addDrawableChild(areaField = new TextFieldWidget(tr, centerX - 80, centerY - 20, 160, 12, Text.of("")));
        areaField.setPlaceholder(Text.of("area").getWithStyle(style).get(0));
        areaField.setChangedListener(s -> verifyArea());
        areaField.setMaxLength(9999);

        //Cancel button
        addDrawableChild(new TextButtonWidget(TextButtonWidget.Type.Negative,
                centerX - 60, centerY, 36, 11, "Cancel", button -> close()));

        //Save button
        addDrawableChild(new TextButtonWidget(TextButtonWidget.Type.Normal,
                centerX + 24, centerY, 36, 11, "Save", button -> save()));

        fillFields();
    }

    private void fillFields() {
        if (info != null) {
            teleportToggle.setText(String.valueOf(info.isTeleport()));
            teleportToggle.setType(info.isTeleport() ? TextButtonWidget.Type.Positive : TextButtonWidget.Type.Negative);
            if (info.name() != null) nameField.setText(info.name());
            int[] area = info.area();
            if (area != null) {
                areaField.setText(area[0] + ", " + area[1] + ", " + area[2] + ", " + area[3] + ", " + area[4] + ", " + area[5]);
            }
        }
        else areaField.setText("0, 0, 0, 0, 0, 0");
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (client.player == null) return;
        renderBackground(context, mouseX, mouseY, delta);

        RenderSystem.enableBlend();
        MatrixStack matrices = context.getMatrices();

        matrices.push();
        matrices.translate(0, 0, 1);
        context.fill(centerX - 132, centerY - 86, centerX + 132, centerY + 15, 0, cfg.backgroundColour);
        context.drawBorder(centerX - 133, centerY - 87, 266, 102, cfg.borderColour);
        context.drawCenteredTextWithShadow(tr, "Editing location info...",
                centerX, centerY - 79, cfg.textColour);

        context.drawText(tr, Text.of("Teleport"), centerX - 85 - tr.getWidth("Teleport"), centerY - 50,
                cfg.textColour, false);
        context.drawText(tr, Text.of("Name"), centerX - 85 - tr.getWidth("Name"), centerY - 34,
                cfg.textColour, false);
        context.drawText(tr, Text.of("Area"), centerX - 85 - tr.getWidth("Area"), centerY - 18,
                cfg.textColour, false);

        for (Element element : children())
            ((Drawable) element).render(context, mouseX, mouseY, delta);
        matrices.pop();
        RenderSystem.disableBlend();
    }

    @SuppressWarnings("DataFlowIssue")
    private int[] verifyArea() {
        String s = areaField.getText();
        try {
            int[] pos = Arrays.stream(s.replace(" ", "").split(",")).mapToInt(Integer::parseInt).toArray();
            if (pos.length == 6) {
                areaField.setEditableColor(Formatting.WHITE.getColorValue());
                return pos;
            }
        }
        catch (Exception ignored) {}

        areaField.setEditableColor(Formatting.RED.getColorValue());
        return null;
    }

    private void toggleTeleport() {
        if (teleportToggle.text.equals("false")) {
            teleportToggle.setText("true");
            teleportToggle.setType(TextButtonWidget.Type.Positive);
        }
        else {
            teleportToggle.setText("false");
            teleportToggle.setType(TextButtonWidget.Type.Negative);
        }
    }

    private void save() {
        int[] area = verifyArea();
        if (area == null) return;

        LocationData info = (this.info != null ? this.info : LocationData.create(id));
        info.setTeleport(teleportToggle.text.equals("true"))
                .setName(nameField.getText())
                .setArea(area);

        if (info.matchesDefault()) {
            if (this.info == null)
                Output.chat("Ignoring location info because all fields contain default values.", Output.Color.RED);
            else {
                GraphEditor.getLocationsState().remove(id);
                Output.chat("Removed location info because all fields now contain default values.");
                WaypointGraphRenderer.updateRender();
            }
        }
        else {
            if (this.info == null) GraphEditor.getLocationsState().put(id, info);
            Output.chat("Updated location info for waypoint " + id + ".");
            WaypointGraphRenderer.updateRender();
        }
        close();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        lastKey = keyCode;

        if (!super.keyPressed(keyCode, scanCode, modifiers)
                && client.options.inventoryKey.matchesKey(keyCode, scanCode)) close();
        return true;
    }

    @Override
    public void close() {
        if (lastKey != 69) {
            if (client.player != null) client.player.closeScreen();
            super.close();
        }
    }
}
