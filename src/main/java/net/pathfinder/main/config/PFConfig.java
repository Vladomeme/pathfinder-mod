package net.pathfinder.main.config;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/**
 * Main config which determines the behavior of the mod in general. Uses YetAnotherConfigLib for the edit screen.
 */
public class PFConfig {

    private static final File FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "pathfinder.json");
    public static final PFConfig cfg = read();

    //### GENERAL
    public int maxOffGraphDistance = 200;
    public int recomputingDistance = 20;
    public int destinationRange = 4;

    public transient int destinationRangeSquared = destinationRange * destinationRange;
    public transient int recomputingDistanceSquared = recomputingDistance * recomputingDistance;
    //### EDITING
    //Behaviour
    public boolean appendOnSave = false;
    public boolean useAstarSmoothing = true;
    public boolean useAstarOptimizing = true;
    public boolean saveUncompressedData = false;
    public int baseGraphMaxNodes = 10000;
    //Ranges
    public int maxPathDistance = 80;
    public int gapSearchStartingRange = 20;
    public int nearestSearchRange = 50;
    public int targetMaxDistance = 30;
    public int targetMaxAngle = 15;
    public boolean targetLoSCheck = true;

    public transient int maxPathDistanceSquared = maxPathDistance * maxPathDistance;
    public transient double targetMaxAngleRad = Math.toRadians(targetMaxAngle);
    //Costs
    public float straightCost = 1.0f;
    public float verticalCost = 2.0f;
    public float diagonalCost = (float) Math.sqrt(2d);
    public float cubeDiagonalCost = (float) Math.sqrt(3d);
    public float yChangeCost = 0.1f;
    public float stairsCost = 1.0f;
    public float waterMulti = 2.0f;
    public float cobwebMulti = 10.0f;
    //### RENDERING
    public int renderRange = 64;
    public float textScale = 0.75f;
    public int pathDisplayLength = 50;
    public float pathParticleStep = 0.333f;
    //Graph colours
    public int lineColourRaw = -16711681;
    public int newLineColourRaw = -16711936;
    public int startColourRaw = -1;
    public int selectedColourRaw = -16711936;
    public int selectedTargetColourRaw = -16744320;
    public int teleportColourRaw = -65281;

    public transient float[] lineColour4f = getComponents(lineColourRaw);
    public transient float[] newLineColour4f = getComponents(newLineColourRaw);
    public transient float[] startColour4f = getComponents(startColourRaw);
    public transient float[] startFillColour4f = getComponents(withAlpha(startColourRaw));
    public transient float[] selectedColour4f = getComponents(selectedColourRaw);
    public transient float[] selectedFillColour4f = getComponents(withAlpha(selectedColourRaw));
    public transient float[] selectedTargetColour4f = getComponents(selectedTargetColourRaw);
    public transient float[] selectedTargetFillColour4f = getComponents(withAlpha(selectedTargetColourRaw));
    public transient float[] teleportColour4f = getComponents(teleportColourRaw);
    public transient float[] teleportFillColour4f = getComponents(withAlpha(teleportColourRaw));
    //Screen colours
    public int buttonInactiveColour = -2236963;
    public int buttonNegativeColour = -1823700;
    public int buttonPositiveColour = -6226016;
    public int buttonActiveColour = -9737764;
    public int backgroundColour = 866822826;
    public int borderColour = -1;
    public int textColour = -1;

    public static PFConfig read() {
        PFConfig config;
        if (!FILE.exists()) config = new PFConfig().write();
        else {
            try (Reader reader = new FileReader(FILE, StandardCharsets.UTF_8)) {
                config = new Gson().fromJson(reader, PFConfig.class);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        config.onUpdate();
        return config;
    }
    
    private void onUpdate() {
        destinationRangeSquared = destinationRange * destinationRange;
        maxPathDistanceSquared = maxPathDistance * maxPathDistance;
        recomputingDistanceSquared = recomputingDistance * recomputingDistance;

        targetMaxAngleRad = Math.toRadians(targetMaxAngle);

        lineColour4f = getComponents(lineColourRaw);
        newLineColour4f = getComponents(newLineColourRaw);
        startColour4f = getComponents(startColourRaw);
        startFillColour4f = getComponents(withAlpha(startColourRaw));
        selectedColour4f = getComponents(selectedColourRaw);
        selectedFillColour4f = getComponents(withAlpha(selectedColourRaw));
        selectedTargetColour4f = getComponents(selectedTargetColourRaw);
        selectedTargetFillColour4f = getComponents(withAlpha(selectedTargetColourRaw));
        teleportColour4f = getComponents(teleportColourRaw);
        teleportFillColour4f = getComponents(withAlpha(teleportColourRaw));
    }

    private int withAlpha(int colour) {
        return (colour & 0xffffff) | (50 << 24);
    }

    private float[] getComponents(int colour) {
        return new float[]{(float) (colour >> 16 & 255) / 255, (float) (colour >> 8 & 255) / 255,
                (float) (colour & 255) / 255, (float) (colour >>> 24) / 255};
    }

    public PFConfig write() {
        Gson gson = new Gson();
        try (JsonWriter writer = gson.newJsonWriter(new FileWriter(FILE, StandardCharsets.UTF_8))) {
            writer.setIndent("    ");
            gson.toJson(gson.toJsonTree(this, PFConfig.class), writer);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        onUpdate();
        return this;
    }

    public Screen create(Screen parent) {
        return YetAnotherConfigLib.createBuilder()
                .save(this::write)
                .title(Text.literal("Pathfinder Global Settings"))

                .category(ConfigCategory.createBuilder()
                        .name(Text.of("General"))

                        .option(Option.<Integer>createBuilder()
                                .name(Text.literal("Max off-graph path distance"))
                                .binding(200, () -> maxOffGraphDistance, newVal -> maxOffGraphDistance = newVal)
                                .controller(IntegerFieldControllerBuilder::create).build())

                        .option(Option.<Integer>createBuilder()
                                .name(Text.literal("Path recomputing distance"))
                                .binding(20, () -> recomputingDistance, newVal -> recomputingDistance = newVal)
                                .controller(IntegerFieldControllerBuilder::create).build())

                        .option(Option.<Integer>createBuilder()
                                .name(Text.literal("Destination reach range"))
                                .binding(4, () -> destinationRange, newVal -> destinationRange = newVal)
                                .controller(IntegerFieldControllerBuilder::create).build())
                        .build())

                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("Editing"))

                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Behaviour"))

                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Auto-append on save"))
                                        .binding(false, () -> appendOnSave, newVal -> appendOnSave = newVal)
                                        .controller(TickBoxControllerBuilder::create).build())

                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Use A* path smoothing"))
                                        .binding(true, () -> useAstarSmoothing, newVal -> useAstarSmoothing = newVal)
                                        .controller(TickBoxControllerBuilder::create).build())

                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Use A* path optimizing"))
                                        .binding(true, () -> useAstarOptimizing, newVal -> useAstarOptimizing = newVal)
                                        .controller(TickBoxControllerBuilder::create).build())

                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Save uncompressed data"))
                                        .binding(false, () -> saveUncompressedData, newVal -> saveUncompressedData = newVal)
                                        .controller(TickBoxControllerBuilder::create).build())

                                .option(Option.<Integer>createBuilder()
                                        .name(Text.literal("Max base graph node count"))
                                        .binding(10000, () -> baseGraphMaxNodes, newVal -> baseGraphMaxNodes = newVal)
                                        .controller(IntegerFieldControllerBuilder::create).build())
                                .build())

                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Ranges"))

                                .option(Option.<Integer>createBuilder()
                                        .name(Text.literal("Max base-pathfinding distance"))
                                        .binding(80, () -> maxPathDistance, newVal -> maxPathDistance = newVal)
                                        .controller(IntegerFieldControllerBuilder::create).build())

                                .option(Option.<Integer>createBuilder()
                                        .name(Text.literal("Graph gap search starting range"))
                                        .binding(20, () -> gapSearchStartingRange, newVal -> gapSearchStartingRange = newVal)
                                        .controller(IntegerFieldControllerBuilder::create).build())

                                .option(Option.<Integer>createBuilder()
                                        .name(Text.literal("Nearest waypoint search range"))
                                        .binding(50, () -> nearestSearchRange, newVal -> nearestSearchRange = newVal)
                                        .controller(IntegerFieldControllerBuilder::create).build())

                                .option(Option.<Integer>createBuilder()
                                        .name(Text.literal("Max target distance"))
                                        .binding(30, () -> targetMaxDistance, newVal -> targetMaxDistance = newVal)
                                        .controller(IntegerFieldControllerBuilder::create).build())

                                .option(Option.<Integer>createBuilder()
                                        .name(Text.literal("Max target angle"))
                                        .binding(15, () -> targetMaxAngle, newVal -> targetMaxAngle = newVal)
                                        .controller(IntegerFieldControllerBuilder::create).build())

                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Require line of sight for targeting"))
                                        .binding(true, () -> targetLoSCheck, newVal -> targetLoSCheck = newVal)
                                        .controller(TickBoxControllerBuilder::create).build())
                                .build())

                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Costs"))

                                .option(Option.<Float>createBuilder()
                                        .name(Text.literal("Horizontal movement cost"))
                                        .binding(1.0f, () -> straightCost, newVal -> straightCost = newVal)
                                        .controller(FloatFieldControllerBuilder::create).build())

                                .option(Option.<Float>createBuilder()
                                        .name(Text.literal("Vertical movement cost"))
                                        .binding(2.0f, () -> verticalCost, newVal -> verticalCost = newVal)
                                        .controller(FloatFieldControllerBuilder::create).build())

                                .option(Option.<Float>createBuilder()
                                        .name(Text.literal("Diagonal movement cost"))
                                        .binding((float) Math.sqrt(2d), () -> diagonalCost, newVal -> diagonalCost = newVal)
                                        .controller(FloatFieldControllerBuilder::create).build())

                                .option(Option.<Float>createBuilder()
                                        .name(Text.literal("Cube diagonal movement cost"))
                                        .binding((float) Math.sqrt(3d), () -> cubeDiagonalCost, newVal -> cubeDiagonalCost = newVal)
                                        .controller(FloatFieldControllerBuilder::create).build())

                                .option(Option.<Float>createBuilder()
                                        .name(Text.literal("Y-level change cost"))
                                        .binding(0.1f, () -> yChangeCost, newVal -> yChangeCost = newVal)
                                        .controller(FloatFieldControllerBuilder::create).build())

                                .option(Option.<Float>createBuilder()
                                        .name(Text.literal("Stairs movement cost"))
                                        .binding(1.0f, () -> stairsCost, newVal -> stairsCost = newVal)
                                        .controller(FloatFieldControllerBuilder::create).build())

                                .option(Option.<Float>createBuilder()
                                        .name(Text.literal("Water movement cost multiplier"))
                                        .binding(2.0f, () -> waterMulti, newVal -> waterMulti = newVal)
                                        .controller(FloatFieldControllerBuilder::create).build())

                                .option(Option.<Float>createBuilder()
                                        .name(Text.literal("Cobweb movement cost multiplier"))
                                        .binding(10.0f, () -> cobwebMulti, newVal -> cobwebMulti = newVal)
                                        .controller(FloatFieldControllerBuilder::create).build())
                                .build())
                        .build())

                .category(ConfigCategory.createBuilder()
                        .name(Text.of("Rendering"))

                        .option(Option.<Integer>createBuilder()
                                .name(Text.literal("Graph render range"))
                                .binding(64, () -> renderRange, newVal -> renderRange = newVal)
                                .controller(IntegerFieldControllerBuilder::create).build())

                        .option(Option.<Float>createBuilder()
                                .name(Text.literal("HUD text scale"))
                                .binding(0.75f, () -> textScale, newVal -> textScale = newVal)
                                .controller(FloatFieldControllerBuilder::create).build())

                        .option(Option.<Integer>createBuilder()
                                .name(Text.literal("Path display length"))
                                .binding(50, () -> pathDisplayLength, newVal -> pathDisplayLength = newVal)
                                .controller(IntegerFieldControllerBuilder::create).build())

                        .option(Option.<Float>createBuilder()
                                .name(Text.literal("Path particle step"))
                                .binding(0.333f, () -> pathParticleStep, newVal -> pathParticleStep = newVal)
                                .controller(FloatFieldControllerBuilder::create).build())

                        .group(OptionGroup.createBuilder()
                                .name(Text.of("Graph colours"))

                                .option(Option.<Color>createBuilder()
                                        .name(Text.literal("Graph line"))
                                        .binding(new Color(-16711681, true),
                                                () -> new Color(lineColourRaw, true), newVal -> lineColourRaw = newVal.getRGB())
                                        .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())

                                .option(Option.<Color>createBuilder()
                                        .name(Text.literal("New graph line"))
                                        .binding(new Color(-16711936, true),
                                                () -> new Color(newLineColourRaw, true), newVal -> newLineColourRaw = newVal.getRGB())
                                        .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())

                                .option(Option.<Color>createBuilder()
                                        .name(Text.literal("Targeted waypoint"))
                                        .binding(new Color(-1, true),
                                                () -> new Color(startColourRaw, true), newVal -> startColourRaw = newVal.getRGB())
                                        .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())

                                .option(Option.<Color>createBuilder()
                                        .name(Text.literal("Selected waypoint"))
                                        .binding(new Color(-16711936, true),
                                                () -> new Color(selectedColourRaw, true), newVal -> selectedColourRaw = newVal.getRGB())
                                        .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())

                                .option(Option.<Color>createBuilder()
                                        .name(Text.literal("Selected & targeted waypoint"))
                                        .binding(new Color(-16744320, true),
                                                () -> new Color(selectedTargetColourRaw, true), newVal -> selectedTargetColourRaw = newVal.getRGB())
                                        .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())

                                .option(Option.<Color>createBuilder()
                                        .name(Text.literal("Teleport marker"))
                                        .binding(new Color(-65281, true),
                                                () -> new Color(teleportColourRaw, true), newVal -> teleportColourRaw = newVal.getRGB())
                                        .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())

                                .build())

                        .group(OptionGroup.createBuilder()
                                .name(Text.of("Screen colours"))

                                .option(Option.<Color>createBuilder()
                                        .name(Text.literal("Button: inactive"))
                                        .binding(new Color(-2236963, true),
                                                () -> new Color(buttonInactiveColour, true), newVal -> buttonInactiveColour = newVal.getRGB())
                                        .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())

                                .option(Option.<Color>createBuilder()
                                        .name(Text.literal("Button: negative"))
                                        .binding(new Color(-1823700, true),
                                                () -> new Color(buttonNegativeColour, true), newVal -> buttonNegativeColour = newVal.getRGB())
                                        .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())

                                .option(Option.<Color>createBuilder()
                                        .name(Text.literal("Button: positive"))
                                        .binding(new Color(-6226016, true),
                                                () -> new Color(buttonPositiveColour, true), newVal -> buttonPositiveColour = newVal.getRGB())
                                        .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())

                                .option(Option.<Color>createBuilder()
                                        .name(Text.literal("Button: active"))
                                        .binding(new Color(-9737764, true),
                                                () -> new Color(buttonActiveColour, true), newVal -> buttonActiveColour = newVal.getRGB())
                                        .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())

                                .option(Option.<Color>createBuilder()
                                        .name(Text.literal("Menu background"))
                                        .binding(new Color(866822826, true),
                                                () -> new Color(backgroundColour, true), newVal -> backgroundColour = newVal.getRGB())
                                        .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())

                                .option(Option.<Color>createBuilder()
                                        .name(Text.literal("Menu border"))
                                        .binding(new Color(-1, true),
                                                () -> new Color(borderColour, true), newVal -> borderColour = newVal.getRGB())
                                        .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())

                                .option(Option.<Color>createBuilder()
                                        .name(Text.literal("Text"))
                                        .binding(new Color(-1, true),
                                                () -> new Color(textColour, true), newVal -> textColour = newVal.getRGB())
                                        .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())
                                .build())
                        .build())
                .build()
                .generateScreen(parent);
    }
}
