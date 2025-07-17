package net.pathfinder.main.config;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.apache.commons.io.IOUtils;

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/**
 * Main config which determines the behavior of the mod in general. Uses YetAnotherConfigLib for the edit screen.
 */
//todo move cost parameters to config
//todo add a direction change cost modifier
public class PFConfig {

    public int renderRange = 50;
    public float textScale = 0.75f;
    public int maxPathDistance = 50;
    public int gapSearchStartingRange = 20;
    public transient int maxPathDistanceSquared = 2500;
    public boolean appendOnSave = false;

    public int buttonInactiveColor = -2236963;
    public int buttonNegativeColor = -1823700;
    public int buttonPositiveColor = -6226016;
    public int buttonActiveColor = -9737764;
    public int backgroundColor = 866822826;
    public int borderColor = -1;
    public int textColor = -1;

//    public int overlayColour = 1358954495;
//    public transient float[] overlay4F = getComponents(overlayColour);
//
//    public int targetColour = 1342218495;
//    public transient float[] target4F = getComponents(targetColour);
//
//    public int selectionColour = 1342242640;
//    public transient float[] selection4F = getComponents(selectionColour);
//
//    public int selectionTargetColour = 1342208060;
//    public transient float[] selectionTarget4F = getComponents(selectionTargetColour);
//
//    public int rangeColour = -50116;
//    public transient float[] range4F = getComponents(rangeColour);
//
//    public int highlightRange = 64;
//    public transient int squaredRange = 4096;

    private static final File FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "pathfinder.json");

    public static final PFConfig INSTANCE = read();

    public static PFConfig read() {
        PFConfig config;
        if (!FILE.exists())
            config = new PFConfig().write();
        else {
            Reader reader = null;
            try {
                config = new Gson().fromJson(reader = new FileReader(FILE, StandardCharsets.UTF_8), PFConfig.class);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
            finally {
                IOUtils.closeQuietly(reader);
            }
        }
        config.onUpdate();
        return config;
    }
    
    private void onUpdate() {
        maxPathDistanceSquared = maxPathDistance * maxPathDistance;
    }

    public PFConfig write() {
        Gson gson = new Gson();
        JsonWriter writer = null;
        try {
            writer = gson.newJsonWriter(new FileWriter(FILE, StandardCharsets.UTF_8));
            writer.setIndent("    ");
            gson.toJson(gson.toJsonTree(this, PFConfig.class), writer);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        finally {
            IOUtils.closeQuietly(writer);
        }
        onUpdate();
        return this;
    }

    public Screen create(Screen parent) {
        return YetAnotherConfigLib.createBuilder()
                .save(this::write)
                .title(Text.literal("GSE Display"))

                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("General"))

                        .option(Option.<Integer>createBuilder()
                                .name(Text.literal("Graph render range"))
                                .binding(64, () -> renderRange, newVal -> renderRange = newVal)
                                .controller(IntegerFieldControllerBuilder::create).build())

                        .option(Option.<Float>createBuilder()
                                .name(Text.literal("HUD text scale"))
                                .binding(0.75f, () -> textScale, newVal -> textScale = newVal)
                                .controller(FloatFieldControllerBuilder::create).build())

                        .option(Option.<Integer>createBuilder()
                                .name(Text.literal("Max path distance"))
                                .binding(50, () -> maxPathDistance, newVal -> maxPathDistance = newVal)
                                .controller(IntegerFieldControllerBuilder::create).build())

                        .option(Option.<Color>createBuilder()
                                .name(Text.literal("Inactive button color"))
                                .binding(new Color(-2236963, true),
                                        () -> new Color(buttonInactiveColor, true), newVal -> buttonInactiveColor = newVal.getRGB())
                                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())

                        .option(Option.<Color>createBuilder()
                                .name(Text.literal("Negative button color"))
                                .binding(new Color(-1823700, true),
                                        () -> new Color(buttonNegativeColor, true), newVal -> buttonNegativeColor = newVal.getRGB())
                                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())

                        .option(Option.<Color>createBuilder()
                                .name(Text.literal("Positive button color"))
                                .binding(new Color(-6226016, true),
                                        () -> new Color(buttonPositiveColor, true), newVal -> buttonPositiveColor = newVal.getRGB())
                                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())

                        .option(Option.<Color>createBuilder()
                                .name(Text.literal("Active button color"))
                                .binding(new Color(-9737764, true),
                                        () -> new Color(buttonActiveColor, true), newVal -> buttonActiveColor = newVal.getRGB())
                                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())

                        .option(Option.<Color>createBuilder()
                                .name(Text.literal("Menu background color"))
                                .binding(new Color(866822826, true),
                                        () -> new Color(backgroundColor, true), newVal -> backgroundColor = newVal.getRGB())
                                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())

                        .option(Option.<Color>createBuilder()
                                .name(Text.literal("Menu border color"))
                                .binding(new Color(-1, true),
                                        () -> new Color(borderColor, true), newVal -> borderColor = newVal.getRGB())
                                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())

                        .option(Option.<Color>createBuilder()
                                .name(Text.literal("Text color"))
                                .binding(new Color(-1, true),
                                        () -> new Color(textColor, true), newVal -> textColor = newVal.getRGB())
                                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())
                        .build())
                .build()
                .generateScreen(parent);
//                        .option(Option.<Boolean>createBuilder()
//                                .name(Text.literal("Enabled"))
//                                .binding(true, () -> enabled, newVal -> enabled = newVal)
//                                .controller(TickBoxControllerBuilder::create).build())
//
//                        .option(Option.<Boolean>createBuilder()
//                                .name(Text.literal("Display range"))
//                                .binding(false, () -> showRange, newVal -> showRange = newVal)
//                                .controller(TickBoxControllerBuilder::create).build())
//
//                        .option(Option.<Boolean>createBuilder()
//                                .name(Text.literal("Display icons"))
//                                .binding(true, () -> showIcons, newVal -> showIcons = newVal)
//                                .controller(TickBoxControllerBuilder::create).build())
//
//                        .option(Option.<Color>createBuilder()
//                                .name(Text.literal("Overlay colour"))
//                                .binding(new Color(1358954495, true),
//                                        () -> new Color(overlayColour, true), newVal -> overlayColour = newVal.getRGB())
//                                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())
//
//                        .option(Option.<Color>createBuilder()
//                                .name(Text.literal("Target colour"))
//                                .binding(new Color(1342218495, true),
//                                        () -> new Color(targetColour, true), newVal -> targetColour = newVal.getRGB())
//                                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())
//
//                        .option(Option.<Color>createBuilder()
//                                .name(Text.literal("Selection colour"))
//                                .binding(new Color(1342242640, true),
//                                        () -> new Color(selectionColour, true), newVal -> selectionColour = newVal.getRGB())
//                                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())
//
//                        .option(Option.<Color>createBuilder()
//                                .name(Text.literal("Selection & Target colour"))
//                                .binding(new Color(1342208060, true),
//                                        () -> new Color(selectionTargetColour, true), newVal -> selectionTargetColour = newVal.getRGB())
//                                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())
//
//                        .option(Option.<Color>createBuilder()
//                                .name(Text.literal("Spawner range colour"))
//                                .binding(new Color(-50116, true),
//                                        () -> new Color(rangeColour, true), newVal -> rangeColour = newVal.getRGB())
//                                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())
//
//                        .option(Option.<Integer>createBuilder()
//                                .name(Text.literal("Highlight Range"))
//                                .description(OptionDescription.of(Text.of("Max distance for highlighting spawners.")))
//                                .binding(30, () -> highlightRange, newVal -> highlightRange = newVal)
//                                .controller(IntegerFieldControllerBuilder::create).build())
//                        .build())
    }

//    private float[] getComponents(int colour) {
//        return new float[]{(float) (colour >> 16 & 255) / 255, (float) (colour >> 8 & 255) / 255,
//                (float) (colour & 255) / 255, (float) (colour >>> 24) / 255};
//    }
}
