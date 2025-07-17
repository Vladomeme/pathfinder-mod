package net.pathfinder.main;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A helper class used for shortening output related code elsewhere in the mod.
 */
public class Output {

    private static final ChatHud chat = MinecraftClient.getInstance().inGameHud.getChatHud();
    private static final Logger logger = LoggerFactory.getLogger("pathfinder");

    public static void chat(String message) {
        chat.addMessage(Text.of(message));
    }

    @SuppressWarnings("unused")
    public static void chat(Text message) {
        chat.addMessage(message);
    }

    public static void chat(String message, Color color) {
        chat.addMessage(Text.literal(message).setStyle(Style.EMPTY.withColor(color.formatting)));
    }

    @SuppressWarnings("unused")
    public static void actionBar(String message) {
        if (MinecraftClient.getInstance().player != null)
            MinecraftClient.getInstance().player.sendMessage(Text.of(message), true);
    }

    public static void actionBar(String message, Color color) {
        if (MinecraftClient.getInstance().player != null)
            MinecraftClient.getInstance().player.sendMessage(Text.literal(message).setStyle(Style.EMPTY.withColor(color.formatting)), true);
    }

    public static void log(String message) {
        logger.info(message);
    }

    public static void logWarn(String message) {
        logger.warn(message);
    }

    public static void logError(String message) {
        logger.error(message);
    }

    public enum Color {
        RED(Formatting.RED),
        GOLD(Formatting.GOLD),
        @SuppressWarnings("unused") AQUA(Formatting.AQUA),
        @SuppressWarnings("unused") BLUE(Formatting.BLUE),
        @SuppressWarnings("unused") GRAY(Formatting.BLUE),
        GREEN(Formatting.GREEN);

        public final Formatting formatting;

        Color(Formatting formatting) {
            this.formatting = formatting;
        }
    }
}
