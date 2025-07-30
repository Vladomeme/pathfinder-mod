package net.pathfinder.main.graph.waypoint;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.pathfinder.main.Output;
import net.pathfinder.main.graph.PositionUtils;
import net.pathfinder.main.graph.waypoint.data.DimensionData;
import net.pathfinder.main.graph.waypoint.data.Waypoint;
import net.pathfinder.main.graph.waypoint.data.WorldConfig;
import net.pathfinder.main.mixin.PlayerListHudAccessor;

import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//todo resource pack data loading
/**
 * A class responsible for loading all the data for a current dimensions.
 */
public class WaypointIO {

    private static final Path PATH = Path.of(FabricLoader.getInstance().getConfigDir() + "/pathfinder");
    private static final Path INDEX_PATH = Path.of(PATH + "/" + "index.json");
    private static final MinecraftClient client = MinecraftClient.getInstance();

    static HashMap<String, List<String>> worldIndex = new HashMap<>();
    static String world;
    static String dimension;
    static WorldConfig config;
    static DimensionData data;
    static Matcher tabMatcher;

    /**
     * Adds a dimension to the system, creates first waypoint & enables the editing mode.
     */
    @SuppressWarnings({"ResultOfMethodCallIgnored, SameReturnValue", "OptionalGetWithoutIsPresent", "ConstantConditions"})
    public static int initializeDimension() {
        ClientPlayerEntity player = Objects.requireNonNull(client.player);
        BlockPos pos = player.getBlockPos();
        if (data != null) {
            Waypoint nearest = (GraphEditor.active) ? GraphEditor.getNearest(pos) : getData().getNearest(pos, 100);
            Output.chat("Dimension is already initialized. Waypoint available at " +
                    Objects.requireNonNullElseGet(nearest, () -> getData().waypoints.values().stream().findAny().get()).coordinates());
            return 1;
        }
        if (!PositionUtils.isValidPosition(player.clientWorld, pos)) {
            Output.chat("Invalid position for initializing.");
            return 1;
        }

        Path worldPath = Path.of(PATH + "/" + world);
        if (!Files.exists(worldPath)) {
            worldPath.toFile().mkdirs();

            List<String> newList = new ArrayList<>();
            if (client.isInSingleplayer()) newList.add(client.getServer().getSaveProperties().getLevelName());
            else newList.add(client.getCurrentServerEntry().address);

            worldIndex.put(world, newList);
        }

        updateWorldConfig(Path.of(worldPath + "/config.json"));

        updateDimensionName(false);
        Path dimensionPath = Path.of(worldPath + "/" + dimension);
        updateDimensionData(dimensionPath);

        data.add(new Waypoint(pos));
        GraphEditor.toggleEditing();
        GraphEditor.save(true);

        Output.chat("Dimension `" + dimension + "` initialized! " +
                "An origin waypoint have been placed at your current position.");
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

    public static void readIndex() {
        if (Files.exists(INDEX_PATH)) {
            try {
                worldIndex = new Gson().fromJson(Files.readString(INDEX_PATH, StandardCharsets.UTF_8),
                        new TypeToken<HashMap<String, List<String>>>(){}.getType());
            }
            catch (Exception e) {
                Output.logError("Failed to read Pathfinder world index.");
                throw new RuntimeException(e);
            }
        }
        else {
            worldIndex = new HashMap<>();
            writeIndex();
        }
    }

    public static void writeIndex() {
        if (worldIndex == null) return;

        Gson gson = new Gson();
        try (JsonWriter writer = gson.newJsonWriter(new FileWriter(INDEX_PATH.toFile(), StandardCharsets.UTF_8))) {
            writer.setIndent("    ");
            gson.toJson(gson.toJsonTree(worldIndex, new TypeToken<HashMap<String, List<String>>>(){}.getType()), writer);
        }
        catch (Exception e) {
            Output.logError("Failed to save Pathfinder world index.");
            Output.logError(e.getMessage());
        }
    }

    @SuppressWarnings("ConstantConditions")
    private static void updateWorldName() {
        if (client.isInSingleplayer()) world = client.getServer().getSaveProperties().getLevelName();
        else world = client.getCurrentServerEntry().address;

        String worldClean = world.replaceAll("[^a-zA-Z0-9]", "_");
        Path worldPath = Path.of(PATH + "/" + worldClean);
        if (Files.exists(worldPath)) {
            world = worldClean;
            return;
        }

        for (Map.Entry<String, List<String>> entry : worldIndex.entrySet()) {
            if (entry.getValue().contains(world)) {
                world = entry.getKey();
                return;
            }
        }
        world = worldClean;
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
        if (force || config.useTabDetection) {
            PlayerListHudAccessor tab = ((PlayerListHudAccessor) client.inGameHud.getPlayerListHud());
            Text tabText;

            if (config != null && config.useTabFooter) tabText = tab.getFooter();
            else tabText = tab.getHeader();

            if (tabText != null) {
                if (tabMatcher.reset(tabText.getString()).find()) {
                    dimension = tabMatcher.group().replaceAll("[^a-zA-Z0-9]", "_");
                    return;
                }
            }
        }
        if (force || config.useDimensionInfo) {
            Identifier id = client.world.getRegistryKey().getValue();
            dimension = id.toString().replaceAll("[^a-zA-Z0-9]", "_");
            return;
        }
        Output.logWarn("Couldn't retrieve a valid dimension name!");
        dimension = "unknown";
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

    public static String getWorld() {
        return world;
    }

    public static void clear() {
        world = null;
        dimension = null;
        config = null;
        data = null;
        tabMatcher = null;
    }
}
