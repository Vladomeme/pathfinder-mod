package net.pathfinder.main.graph.waypoint.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.pathfinder.main.config.PFConfig;

public class TextButtonWidget extends ButtonWidget {

    final TextRenderer tr = MinecraftClient.getInstance().textRenderer;

    Type type;
    int x;
    int y;
    final int width;
    final int height;
    String text;

    public TextButtonWidget(Type type, int x, int y, int width, int height, String text, PressAction onPress) {
        super(x, y, width, height, Text.of(""), onPress, null);
        this.type = type;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.text = text;
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!visible) return;

        int color = getColor(mouseX, mouseY);
        context.fill(x - 1, y + 1, x, y + height, color); //left
        context.fill(x + width - 1, y + 1, x + width, y + height, color); //right
        context.fill(x - 1, y, x + width, y + 1, color); //top
        context.fill(x - 1, y + height - 1, x + width, y + height, color); //bottom

        context.drawText(tr, text, x + width / 2 - tr.getWidth(text) / 2, y + height / 2 - tr.fontHeight / 2 + 1,
                color, false);
    }

    private int getColor(int mouseX, int mouseY) {
        return isMouseOver(mouseX, mouseY) ? PFConfig.INSTANCE.buttonActiveColor : type.equals(Type.Normal) ?
                PFConfig.INSTANCE.buttonInactiveColor : type.equals(Type.Positive) ?
                PFConfig.INSTANCE.buttonPositiveColor : PFConfig.INSTANCE.buttonNegativeColor;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public enum Type {
        Normal,
        Negative,
        Positive
    }

    @Override
    protected MutableText getNarrationMessage() {
        return Text.literal("");
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        this.x = x;
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        this.y = y;
    }

    @Override
    public void setPosition(int x, int y) {
        super.setPosition(x, y);
        this.setX(x);
        this.setY(y);
    }
}
