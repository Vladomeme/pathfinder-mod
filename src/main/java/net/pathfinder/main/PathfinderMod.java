package net.pathfinder.main;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
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
import net.pathfinder.main.graph.RuleHolder;
import net.pathfinder.main.graph.astar.AstarBuilder;
import net.pathfinder.main.graph.base.BaseBuilder;
import net.pathfinder.main.graph.GraphRenderer;
import net.pathfinder.main.graph.waypoint.TargetHolder;
import net.pathfinder.main.graph.waypoint.GraphEditor;
import net.pathfinder.main.graph.waypoint.WaypointGraphRenderer;
import net.pathfinder.main.graph.waypoint.WaypointIO;
import net.pathfinder.main.graph.waypoint.data.DimensionData;
import net.pathfinder.main.graph.waypoint.path.PathManager;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

//todo cleanup assertions
//todo de-static everything

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
                .then(literal("toggle_render")
                        .executes(context -> GraphRenderer.toggleRender()))
                .then(literal("base")
                        .executes(BaseBuilder.INSTANCE::compute)
                        .then(literal("clear")
                                .executes(context -> BaseBuilder.INSTANCE.clear())))
                .then(literal("astar")
                        .then(argument("x", IntegerArgumentType.integer())
                                .then(argument("y", IntegerArgumentType.integer())
                                        .then(argument("z", IntegerArgumentType.integer())
                                                .executes(AstarBuilder::findRoute))))
                        .then(literal("toggle_smoothing")
                                .executes(context -> AstarBuilder.toggleSmoothing()))
                        .then(literal("toggle_optimization")
                                .executes(context -> AstarBuilder.toggleOptimization()))
                        .then(literal("clear")
                                .executes(context -> AstarBuilder.clear()))
                        .then(literal("unlock")
                                .executes(context -> AstarBuilder.unblock())))
                .then(literal("clear")
                        .executes(context -> clear()))
                .then(literal("waypoint")
                        .then(literal("unlock_render")
                                .executes(context -> WaypointGraphRenderer.unblock()))
                        .then(literal("toggle_editing")
                                .executes(context -> GraphEditor.toggleEditing()))
                        .then(literal("toggle_depth_test")
                                .executes(context -> WaypointGraphRenderer.toggleDepthTest()))
                        .then(literal("discard")
                                .executes(context -> GraphEditor.discard()))
                        .then(literal("save")
                                .executes(context -> GraphEditor.save()))
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
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            if (GraphEditor.active) WaypointGraphRenderer.render(context.matrixStack());
            else if (GraphRenderer.enabled) GraphRenderer.render(context.matrixStack());
        });

        HudLayerRegistrationCallback.EVENT.register(drawer -> drawer.attachLayerBefore(
                IdentifiedLayer.CHAT, Identifier.of("pathfinder", "display"),
                (context, tick) ->  WaypointGraphRenderer.renderDisplay(context)));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> WaypointIO.read());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (GraphEditor.active) {
                GraphEditor.tick();
                tickKeybinds();
                WaypointGraphRenderer.updatePosition();
            }
            if (PathManager.isActive()) PathManager.tick();
        });
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

        if (InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), 261))
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
        BaseBuilder.INSTANCE.clear();
        AstarBuilder.clear();
        RuleHolder.clear();
        GraphEditor.clear();
        PathManager.clear();
        return 1;
    }

    @SuppressWarnings("SameReturnValue")
    private int debug() {
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
            Output.chat("Active waypoints: " + GraphEditor.getWaypointsState().size());
            Output.chat("Active locations: " + GraphEditor.getLocationsState().size());
            Output.chat("Selection: " + GraphEditor.getCurrentSelection().size());
        }
        Output.chat("Renderer locked: " + WaypointGraphRenderer.updateLocked.get());
        if (WaypointGraphRenderer.lines != null)
            Output.chat("Lines total: " + WaypointGraphRenderer.lines.size());
        if (WaypointGraphRenderer.linesActive != null)
            Output.chat("Lines active: " + WaypointGraphRenderer.linesActive.size());
        if (WaypointGraphRenderer.teleports != null)
            Output.chat("Teleports total: " + WaypointGraphRenderer.teleports.size());
        if (WaypointGraphRenderer.teleportsActive != null)
            Output.chat("Teleports active: " + WaypointGraphRenderer.teleportsActive.size());
        return 1;
    }
}
