package net.pathfinder.main.graph.waypoint;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.pathfinder.main.Output;
import net.pathfinder.main.graph.RuleHolder;
import net.pathfinder.main.graph.waypoint.data.DimensionData;
import net.pathfinder.main.graph.waypoint.data.Waypoint;
import net.pathfinder.main.graph.waypoint.data.WorldConfig;
import net.pathfinder.main.mixin.PlayerListHudAccessor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//todo remove special symbols from file names
//todo server address aliases
//todo resource pack data loading
/**
 * A class responsible for loading all the data for a current dimensions.
 */
public class WaypointIO {

    private static final Path PATH = Path.of(FabricLoader.getInstance().getConfigDir() + "/pathfinder");
    private static final MinecraftClient client = MinecraftClient.getInstance();

    static String world;
    static String dimension;
    static WorldConfig config;
    static DimensionData data;
    static Matcher tabMatcher;

    /**
     * Adds a dimension to the system, creates first waypoint & enables the editing mode.
     */
    @SuppressWarnings({"ResultOfMethodCallIgnored, SameReturnValue", "OptionalGetWithoutIsPresent"}) //for .mkdirs()
    public static int initializeDimension() {
        ClientPlayerEntity player = Objects.requireNonNull(client.player);
        BlockPos pos = player.getBlockPos();
        if (data != null) {
            Waypoint nearest = (GraphEditor.active) ? GraphEditor.getNearest(pos) : WaypointIO.getData().getNearest(pos, 100);
            Output.chat("Dimension is already initialized. Waypoint available at " +
                    Objects.requireNonNullElseGet(nearest, () -> WaypointIO.getData().waypoints.values().stream().findAny().get()).coordinates());
            return 1;
        }
        if (!RuleHolder.isValidPosition(player.clientWorld, pos)) {
            Output.chat("Invalid position for initializing.");
            return 1;
        }

        Path worldPath = Path.of(PATH + "/" + world);
        if (!Files.exists(worldPath)) worldPath.toFile().mkdirs();

        updateWorldConfig(Path.of(worldPath + "/config.json"));

        updateDimensionName(false);
        Path dimensionPath = Path.of(worldPath + "/" + dimension);
        updateDimensionData(dimensionPath);

        data.add(new Waypoint(pos));

        Output.chat("Dimension `" + dimension + "` initialized! " +
                "An origin waypoint have been placed at your current position.");
        GraphEditor.toggleEditing();
        return 1;
    }

    /**
     * Updates data after changing current dimension.
     */
    public static void read() {
        clear();
        updateWorldName();

        Path worldPath = Path.of(PATH + "/" + world);
        if (Files.exists(worldPath)) {
            updateWorldConfig(Path.of(worldPath + "/config.json"));

            updateDimensionName(false);
            Path dimensionPath = Path.of(worldPath + "/" + dimension);
            if (Files.exists(dimensionPath)) updateDimensionData(dimensionPath);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private static void updateWorldName() {
        if (client.isInSingleplayer()) world = client.getServer().getSaveProperties().getLevelName();
        else world = client.getCurrentServerEntry().address;
    }

    private static void updateWorldConfig(Path path) {
        config = WorldConfig.read(path);
        config.onRead(path);

        if (config.useTabDetection)
            tabMatcher = Pattern.compile(config.tabPattern).matcher("");
        else tabMatcher = null;
    }

    @SuppressWarnings("ConstantConditions") //world can't be null
    private static void updateDimensionName(boolean force) {
        if (config.useTabDetection || force) {
            PlayerListHudAccessor tab = ((PlayerListHudAccessor) client.inGameHud.getPlayerListHud());
            Text tabText;

            if (config.useTabFooter) tabText = tab.getFooter();
            else tabText = tab.getHeader();

            if (tabText != null) {
                if (tabMatcher.reset(tabText.getString()).find()) {
                    WaypointIO.dimension = tabMatcher.group();
                    return;
                }
            }
        }
        if (config.useDimensionInfo || force) {
            Identifier id = client.world.getRegistryKey().getValue();
            WaypointIO.dimension = id.toString();
            return;
        }
        Output.logWarn("Couldn't retrieve a valid dimension name!");
        WaypointIO.dimension = "unknown";
    }

    private static void updateDimensionData(Path path) {
        data = DimensionData.read(path);
        data.onUpdate(path);
    }

    public static boolean notInitialized() {
        boolean b = data == null;
        if (b) Output.chat("Can't edit waypoint graph until dimension is initialized. " +
                "Run `/pathfinder waypoint init` to begin.", Output.Color.RED);
        return b;
    }

    public static String getDimension() {
        if (dimension == null) updateDimensionName(true);
        return dimension;
    }

    public static DimensionData getData() {
        return data;
    }

    public static void clear() {
        world = null;
        dimension = null;
        config = null;
        data = null;
        tabMatcher = null;
    }
}
