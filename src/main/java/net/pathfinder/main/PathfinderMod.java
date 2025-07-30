package net.pathfinder.main;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import net.pathfinder.main.datagen.BlockTagProvider;
import net.pathfinder.main.graph.astar.AstarBuilder;
import net.pathfinder.main.graph.base.BaseBuilder;
import net.pathfinder.main.graph.DebugManager;
import net.pathfinder.main.graph.render.GraphRenderer;
import net.pathfinder.main.graph.render.HudRenderer;
import net.pathfinder.main.graph.render.RenderUtils;
import net.pathfinder.main.graph.waypoint.TargetHolder;
import net.pathfinder.main.graph.waypoint.GraphEditor;
import net.pathfinder.main.graph.waypoint.WaypointIO;
import net.pathfinder.main.graph.waypoint.data.DimensionData;
import net.pathfinder.main.graph.waypoint.path.PathManager;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static net.pathfinder.main.config.PFConfig.cfg;

//todo cleanup assertions
//todo de-static everything
//todo choose a better mod name
//todo translations
/**
 * Mod entrypoint class, containing various methods for initializing different hooks, as well as utility methods.
 */
public class PathfinderMod implements ClientModInitializer {

    /**
     * Used for running some expensive pathfinding & related calculations to prevent game from freezing during them.
     */
    public static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static KeyBinding activationKey;
    public static KeyBinding modeToggleKey;
    public static KeyBinding appendKey;
    public static KeyBinding clearKey;

    @Override
    public void onInitializeClient() {
        registerCommands();
        BlockTagProvider.registerOnClient();
        registerEvents();
        registerKeybinds();
    }

    private void registerCommands() {
        LiteralArgumentBuilder<FabricClientCommandSource> root = literal("pathfinder")
                .then(literal("debug")
                        .executes(context -> debug()))
                .then(literal("base")
                        .executes(BaseBuilder.INSTANCE::compute))
                .then(literal("astar")
                        .then(argument("x", IntegerArgumentType.integer())
                                .then(argument("y", IntegerArgumentType.integer())
                                        .then(argument("z", IntegerArgumentType.integer())
                                                .executes(AstarBuilder::findRoute)))))
                .then(literal("clear")
                        .executes(context -> clear()))
                .then(literal("toggle_editing")
                        .executes(context -> GraphEditor.toggleEditing()))
                .then(literal("waypoint")
                        .then(literal("toggle_depth_test")
                                .executes(context -> RenderUtils.toggleDepthTest()))
                        .then(literal("discard")
                                .executes(context -> GraphEditor.discard()))
                        .then(literal("save")
                                .executes(context -> GraphEditor.save(false)))
                        .then(literal("init")
                                .executes(context -> WaypointIO.initializeDimension())))
                .then(literal("route")
                        .then(argument("x", IntegerArgumentType.integer())
                                .then(argument("y", IntegerArgumentType.integer())
                                        .then(argument("z", IntegerArgumentType.integer())
                                                .executes(PathManager::compute)))))
                .then(literal("clear_route")
                        .executes(context -> PathManager.clear()));
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registry) ->
                dispatcher.register(root)
        );
    }

    private void registerEvents() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> WaypointIO.read());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> WaypointIO.readIndex());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> WaypointIO.writeIndex());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                if (GraphEditor.active) {
                    GraphEditor.tick();
                    tickKeybinds();
                    GraphRenderer.updatePosition(client.player);
                }
                if (PathManager.isActive()) PathManager.tick();
            }
        });

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (GraphEditor.active) GraphRenderer.render(context);
            else if (DebugManager.shouldRender()) GraphRenderer.renderDebug(Objects.requireNonNull(context.matrixStack()));
            if (PathManager.isActive() && cfg.renderBeacon) PathManager.renderBeacon(context);
        });

        HudLayerRegistrationCallback.EVENT.register(drawer -> drawer.attachLayerBefore(
                IdentifiedLayer.CHAT, Identifier.of("pathfinder", "display"),
                (context, tick) ->  {
                    if (GraphEditor.active) HudRenderer.renderDisplay(context);
                }));
    }

    @SuppressWarnings("NoTranslation")
    private void registerKeybinds() {
        activationKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("Selector activation", GLFW.GLFW_KEY_F, "Pathfinder"));
        modeToggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("Toggle selection mode", GLFW.GLFW_KEY_T, "Pathfinder"));
        appendKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("Append current path", GLFW.GLFW_KEY_B, "Pathfinder"));
        clearKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("Clear current path", GLFW.GLFW_KEY_N, "Pathfinder"));
    }

    private void tickKeybinds() {
        if (Screen.hasAltDown()) GraphEditor.selected = null; //Deselect a waypoint
        else TargetHolder.updateTargeted();

        if (InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), 261)) //Delete key
            GraphEditor.removeSelected(); //Remove selected waypoint

        if (modeToggleKey.wasPressed()) {
            modeToggleKey.reset();
            GraphEditor.toggleMode();
        }
        if (appendKey.wasPressed()) {
            appendKey.reset();
            GraphEditor.appendSelection();
        }
        if (clearKey.wasPressed()) {
            clearKey.reset();
            GraphEditor.clearSelection();
        }
    }

    @SuppressWarnings("SameReturnValue")
    private int clear() {
        DebugManager.clear();
        GraphEditor.clear();
        PathManager.clear();
        return 1;
    }

    @SuppressWarnings("SameReturnValue")
    private int debug() {
        MinecraftClient client = MinecraftClient.getInstance();
        Output.chat("World: " + (client.isInSingleplayer() ?
                Objects.requireNonNull(client.getServer()).getSaveProperties().getLevelName() :
                Objects.requireNonNull(client.getCurrentServerEntry()).address) + " | " + WaypointIO.getWorld());
        Output.chat("Dimension: " + WaypointIO.getDimension());
        Output.chat("Initialized: " + (WaypointIO.getData() != null));
        DimensionData data = WaypointIO.getData();
        if (data != null) {
            Output.chat("Current ID: " + WaypointIO.getData().idTracker);
            Output.chat("Stored waypoints: " + WaypointIO.getData().waypoints.size());
            Output.chat("Stored locations: " + WaypointIO.getData().locations.size());
        }
        Output.chat("Builder active: " + GraphEditor.active);
        if (GraphEditor.active) {
            Output.chat("Active waypoints: " + GraphEditor.waypointsState.size());
            Output.chat("Active locations: " + GraphEditor.locationsState.size());
            Output.chat("Selection: " + GraphEditor.currentSelection.size());
        }
        Output.chat("Renderer locked: " + GraphRenderer.updateLocked.get());
        if (GraphRenderer.lines != null)
            Output.chat("Lines total: " + GraphRenderer.lines.size());
        if (GraphRenderer.linesActive != null)
            Output.chat("Lines active: " + GraphRenderer.linesActive.size());
        if (GraphRenderer.teleports != null)
            Output.chat("Teleports total: " + GraphRenderer.teleports.size());
        if (GraphRenderer.teleportsActive != null)
            Output.chat("Teleports active: " + GraphRenderer.teleportsActive.size());
        return 1;
    }
}
