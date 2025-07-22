package net.pathfinder.main.graph.waypoint;

import io.netty.util.collection.LongObjectHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.pathfinder.main.Output;
import net.pathfinder.main.PathfinderMod;
import net.pathfinder.main.graph.PositionUtils;
import net.pathfinder.main.graph.astar.AstarBuilder;
import net.pathfinder.main.graph.render.GraphRenderer;
import net.pathfinder.main.graph.waypoint.data.LocationData;
import net.pathfinder.main.graph.waypoint.data.Waypoint;
import net.pathfinder.main.graph.waypoint.screen.LocationEditScreen;

import java.util.*;

import static net.pathfinder.main.graph.waypoint.EditMode.*;
import static net.pathfinder.main.config.PFConfig.cfg;

//todo undo button
/**
 * A class containing all the controls and operations for editing mode.
 */
public class GraphEditor {

    private static final MinecraftClient client = MinecraftClient.getInstance();

    public static boolean active = false;
    private static int clickCooldown = 0;
    static boolean hasChanges = false;
    public static EditMode mode = NORMAL;

    public static LongObjectHashMap<Waypoint> waypointsState;
    public static LongObjectHashMap<LocationData> locationsState;
    public static LongObjectHashMap<Waypoint> currentSelection;

    public static Waypoint selected;

    public static void tick() {
        if (clickCooldown != 0) clickCooldown--;
    }

    public static void onLeftClick() {
        if (WaypointIO.notInitialized() || clickCooldown != 0) return;
        clickCooldown = 5;

        Waypoint targeted = TargetHolder.targeted;
        if (targeted == null) return;

        if (mode == DELETE) remove(targeted);
        else if (selected == null) selected = targeted; //Select
        else if (selected == targeted) openEditScreen(selected); //Open location screen
        else if (selected.neighbours().contains(targeted.id())) disconnect(selected, targeted); //Disconnect
        else {
            if (mode == STRAIGHT) connectStraight(selected, targeted); //Straight connection for teleportation
            else connect(selected, targeted); //Connect
        }
    }

    //todo path preview?
    public static void onRightClick(BlockPos newPos) {
        if (WaypointIO.notInitialized() || clickCooldown != 0) return;
        clickCooldown = 5;

        if (selected == null) {
            if (!waypointsState.isEmpty() || !currentSelection.isEmpty()) {
                Waypoint nearest = getNearest(newPos);
                if (nearest == null) Output.chat("Couldn't find a valid nearby waypoint", Output.Color.RED);
                else connectNearest(nearest, newPos); //Append to nearest
            }
        }
        else {
            Waypoint end = processNewPath(selected, AstarBuilder.findAndReturn(selected.pos(), newPos));
            if (mode == CONTINUOUS) selected = end; //Reassign current starting point
        }
    }

    /**
     * Returns the nearest waypoint (ignoring obstacles).
     */
    public static Waypoint getNearest(BlockPos target) {
        if (WaypointIO.notInitialized()) throw new RuntimeException("Requested nearest waypoint in an empty dimension.");

        Waypoint nearest = null;
        int minDistance = Integer.MAX_VALUE;

        for (Waypoint waypoint : waypointsState.values()) {
            BlockPos pos = waypoint.pos();
            if (PositionUtils.isInRange(target, pos, cfg.nearestSearchRange)) {
                int distance = PositionUtils.getSquaredDistance(target, pos);
                if (distance < minDistance) {
                    nearest = waypoint;
                    minDistance = distance;
                }
            }
        }
        for (Waypoint waypoint : currentSelection.values()) {
            BlockPos pos = waypoint.pos();
            if (PositionUtils.isInRange(target, pos, cfg.nearestSearchRange)) {
                int distance = PositionUtils.getSquaredDistance(target, pos);
                if (distance < minDistance) {
                    nearest = waypoint;
                    minDistance = distance;
                }
            }
        }
        return nearest;
    }

    private static void openEditScreen(Waypoint waypoint) {
        client.setScreen(new LocationEditScreen(waypoint.id()));
    }

    /**
     * Connects two waypoints by building an A*-based path between them.
     */
    private static void connect(Waypoint waypoint1, Waypoint waypoint2) {
        List<BlockPos> path = AstarBuilder.findAndReturn(waypoint1.pos(), waypoint2.pos());

        Waypoint last = waypoint1;
        if (path.size() > 2) {
            for (int i = 1; i < path.size() - 1; i++) {
                Waypoint current = new Waypoint(path.get(i));
                last.addNeighbour(current.id());
                current.addNeighbour(last.id());
                currentSelection.put(current.id(), current);
                last = current;
            }
        }
        last.addNeighbour(waypoint2.id());
        waypoint2.addNeighbour(last.id());
        hasChanges = true;
        GraphRenderer.updateElements();

        Output.chat("Path created: " + waypoint1.id() + " - " + waypoint2.id() + ".");
    }

    /**
     * Extends graph to a position by building an A*-based path to a true-nearest waypoint.
     */
    private static void connectNearest(Waypoint waypoint, BlockPos pos) {
        Waypoint original = waypoint;
        List<BlockPos> path = AstarBuilder.findAndReturn(pos, waypoint.pos());

        int minDistance = PositionUtils.getSquaredDistance(waypoint, pos);
        for (BlockPos node : path) {
            Waypoint nearest = getNearest(node);
            int distance = PositionUtils.getSquaredDistance(nearest, node);

            if (distance < minDistance) {
                minDistance = distance;
                waypoint = nearest;
            }
        }
        if (waypoint != original) path = AstarBuilder.findAndReturn(pos, waypoint.pos());

        Waypoint last = waypoint;
        if (path.size() > 1) {
            for (int i = 1; i < path.size() - 1; i++) {
                Waypoint current = new Waypoint(path.get(i));
                last.addNeighbour(current.id());
                current.addNeighbour(last.id());
                currentSelection.put(current.id(), current);
                last = current;
            }
        }
        Waypoint end = new Waypoint(pos);
        last.addNeighbour(end.id());
        end.addNeighbour(last.id());
        currentSelection.put(end.id(), end);
        hasChanges = true;
        GraphRenderer.updateElements();

        Output.chat("Connected to nearest point: " + waypoint.coordinates());
    }

    /**
     * Connect waypoints by marking them as each other's neighbours (used in STRAIGHT mode).
     */
    private static void connectStraight(Waypoint waypoint1, Waypoint waypoint2) {
        waypoint1.addNeighbour(waypoint2.id());
        waypoint2.addNeighbour(waypoint1.id());
        hasChanges = true;
        GraphRenderer.updateElements();

        Output.chat("Straight path created: " + waypoint1.id() + " - " + waypoint2.id() + ".");
    }

    /**
     * Removes a direct connection between waypoints.
     */
    private static void disconnect(Waypoint waypoint1, Waypoint waypoint2) {
        waypoint1.removeNeighbour(waypoint2.id());
        waypoint2.removeNeighbour(waypoint1.id());
        hasChanges = true;
        GraphRenderer.updateElements();

        Output.chat("Path removed: " + waypoint1.id() + " - " + waypoint2.id() + ".");
    }

    public static void removeSelected() {
        if (selected != null) remove(selected);
    }

    private static void remove(Waypoint waypoint) {
        if (waypointsState.size() == 1) {
            Output.chat("Cannot remove the only available waypoint. " +
                    "If you want to re-initialize the graph, delete the file for this dimension.", Output.Color.RED);
            return;
        }
        long id = waypoint.id();
        LongArrayList neighbours = waypoint.neighbours();
        for (long l : neighbours) {
            Waypoint w = waypointsState.get(l);
            if (w != null)
                w.removeNeighbour(id);
            else {
                w = currentSelection.get(l);
                if (w == null) Output.logWarn("Couldn't find a neighbour waypoint in any state.");
                else w.removeNeighbour(id);
            }
        }
        waypointsState.remove(id);
        currentSelection.remove(id);
        locationsState.remove(id);
        hasChanges = true;
        selected = null;
        TargetHolder.targeted = null;
        GraphRenderer.updateElements();

        Output.chat("Waypoint " + waypoint.coordinates() + " removed.");
    }

    /**
     * Converts a path found using A* into a new graph segment.
     */
    private static Waypoint processNewPath(Waypoint start, List<BlockPos> path) {
        if (path.size() < 2) return start;

        Waypoint last = start;
        for (int i = 1; i < path.size() - 1; i++) {
            Waypoint current = new Waypoint(path.get(i));
            last.addNeighbour(current.id());
            current.addNeighbour(last.id());
            currentSelection.put(current.id(), current);
            last = current;
        }
        Waypoint end = new Waypoint(path.get(path.size() - 1));
        last.addNeighbour(end.id());
        end.addNeighbour(last.id());
        currentSelection.put(end.id(), end);
        hasChanges = true;
        return end;
    }

    //todo add save action clickEvent
    public static void appendSelection() {
        if (WaypointIO.notInitialized()) return;

        waypointsState.putAll(currentSelection);
        currentSelection.clear();
        hasChanges = false;
        GraphRenderer.updateElements();

        Output.chat("Selection appended. Don't forget to save it.");
    }

    public static void clearSelection() {
        if (WaypointIO.notInitialized()) return;
        currentSelection.clear();
        hasChanges = false;

        Output.chat("Selection cleared.");
    }

    @SuppressWarnings("SameReturnValue")
    public static int save() {
        if (WaypointIO.notInitialized()) return 1;
        if (!active) {
            Output.chat("Graph editor isn't active.");
            return 1;
        }
        if (hasChanges) {
            if (cfg.appendOnSave) {
                Output.chat("Auto-appended unfinished changes.");
                appendSelection();
            }
            else {
                Output.chat("There are unfinished graph changes. Append selection or use `discard` command to drop them.", Output.Color.RED);
                return 1;
            }
        }

        cleanupGraph();
        if (!checkGraphContinuity()) return 1;

        WaypointIO.data.waypoints = new LongObjectHashMap<>();
        waypointsState.forEach((key, value) -> WaypointIO.data.waypoints.put(key, value.copy()));
        WaypointIO.data.locations = new LongObjectHashMap<>();
        locationsState.forEach((key, value) -> WaypointIO.data.locations.put(key, value.copy()));
        WaypointIO.data.write();
        GraphRenderer.updateElements();
        hasChanges = false;

        Output.chat("Changes saved.");
        return 1;
    }

    /**
     * Removes graph defects before saving.
     */
    private static void cleanupGraph() {
        Map<BlockPos, Waypoint> posMap = new HashMap<>();
        int dupeCount = 0;
        for (Waypoint waypoint : waypointsState.values()) {
            LongArrayList neighbours = waypoint.neighbours();
            //remove waypoints with no neighbours
            if (neighbours.isEmpty()) {
                waypointsState.remove(waypoint.id());
                continue;
            }
            //remove nonexistent neighbours
            neighbours.removeIf(l -> !waypointsState.containsKey(l));

            //make sure the connection is stored on both waypoints
            for (long l : neighbours) {
                Waypoint w = waypointsState.get(l);
                if (!w.neighbours().contains(waypoint.id())) w.neighbours().add(waypoint.id());
            }

            BlockPos pos = waypoint.pos();
            Waypoint original = posMap.get(pos);

            if (original != null) {
                //merge waypoint neighbours
                for (long l : neighbours) {
                    if (!original.neighbours().contains(l)) original.neighbours().add(l);
                }
                //merge location info
                if (waypoint.location() != null) {
                    if (original.location() == null) waypoint.location().replaceWithID(original.id());
                    else Output.chat("Found location info on conflicting waypoints " + original.coordinates()
                            + ". Some data might be lost now.", Output.Color.RED);
                }
                //remove the duplicate
                remove(waypoint);
                dupeCount++;
            }
            else posMap.put(waypoint.pos(), waypoint);
        }
        int locationCount = 0;
        //remove locations if their associated waypoints no longer exist
        for (long id : locationsState.keySet()) {
            if (!waypointsState.containsKey(id)) {
                locationsState.remove(id);
                locationCount++;
            }
        }
        if (dupeCount != 0 || locationCount != 0)
            Output.logWarn("Removed " + dupeCount + " duplicate waypoints, " + locationCount + " unused location entries during cleanup.");
    }

    /**
     * Makes sure all existing waypoints are connected.
     */
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private static boolean checkGraphContinuity() {
        Waypoint start = waypointsState.values().stream().findFirst().get();
        LongObjectHashMap<Waypoint> graph = getFullGraph(start);

        if (graph.size() == waypointsState.size()) {
            Output.log("Graph is valid.");
            return true;
        }
        else {
            for (Waypoint waypoint : waypointsState.values()) {
                if (!graph.containsKey(waypoint.id())) {
                    Output.chat("Graph can't be saved: one or more disconnected segments found. Preparing suggestions...", Output.Color.RED);
                    PathfinderMod.executor.submit(() -> findShortestGap(waypoint, start));
                    break;
                }
            }
            return false;
        }
    }

    /**
     * Returns all waypoints belonging to the same graph as argument waypoint.
     */
    private static LongObjectHashMap<Waypoint> getFullGraph(Waypoint start) {
        Stack<Waypoint> open = new Stack<>();
        LongObjectHashMap<Waypoint> graph = new LongObjectHashMap<>();
        open.push(start);
        while (!open.empty()) {
            Waypoint current = open.pop();
            graph.put(current.id(), current);

            for (long id : current.neighbours()) {
                if (graph.containsKey(id)) continue;
                open.push(waypointsState.get(id));
            }
        }
        return graph;
    }

    /**
     * Finds the shortest gap between two disconnected graph segments.
     */
    private static void findShortestGap(Waypoint graph1waypoint, Waypoint graph2waypoint) {
        LongObjectHashMap<Waypoint> graph1 = getFullGraph(graph1waypoint);
        LongObjectHashMap<Waypoint> graph2 = getFullGraph(graph2waypoint);
        LongObjectHashMap<Boolean> closed = new LongObjectHashMap<>();

        Set<Waypoint> graph1options = new HashSet<>();
        Set<Waypoint> graph2options = new HashSet<>();
        int range = cfg.gapSearchStartingRange;

        while (graph1options.isEmpty() || graph2options.isEmpty()) {
            for (Waypoint w1 : graph1.values()) {
                boolean shouldAdd = false;
                for (Waypoint w2 : graph2.values()) {
                    if (closed.containsKey(w2.id())) continue;
                    if (PositionUtils.isInRange(w1, w2, range)) {
                        shouldAdd = true;
                        graph2options.add(w2);
                        closed.put(w2.id(), Boolean.TRUE);
                    }
                }
                if (shouldAdd) graph1options.add(w1);
            }
            range += 10;
        }
        Waypoint gapWaypoint1 = graph1waypoint;
        Waypoint gapWaypoint2 = graph2waypoint;
        int minDistance = Integer.MAX_VALUE;

        for (Waypoint w1 : graph1options) {
            for (Waypoint w2 : graph2options) {
                int distance = PositionUtils.getSquaredDistance(w1, w2);
                if (distance < minDistance) {
                    gapWaypoint1 = w1;
                    gapWaypoint2 = w2;
                    minDistance = distance;
                }
            }
        }
        //todo teleport clickEvent
        Output.chat("Shortest gap between two graph segments is: ["
                + gapWaypoint1.coordinates() + "; " + gapWaypoint2.coordinates() + "] (" + minDistance + " blocks).");
    }

    @SuppressWarnings("SameReturnValue")
    public static int discard() {
        if (WaypointIO.notInitialized()) return 1;

        hasChanges = false;
        selected = null;
        currentSelection.clear();
        waypointsState = new LongObjectHashMap<>();
        WaypointIO.data.waypoints.forEach((key, value) -> waypointsState.put(key, value.copy()));
        locationsState = new LongObjectHashMap<>();
        WaypointIO.data.locations.forEach((key, value) -> locationsState.put(key, value.copy()));
        GraphRenderer.updateElements();

        Output.chat("Discarded all unsaved changes.");
        return 1;
    }

    @SuppressWarnings("SameReturnValue")
    public static int toggleEditing() {
        if (!active && WaypointIO.notInitialized()) return 1;
        if (active && hasChanges) {
            Output.chat("There are unappended graph changes. Append them or use `discard` command to drop them.", Output.Color.RED);
            return 1;
        }
        if (!active) {
            waypointsState = new LongObjectHashMap<>();
            WaypointIO.data.waypoints.forEach((key, value) -> waypointsState.put(key, value.copy()));
            locationsState = new LongObjectHashMap<>();
            WaypointIO.data.locations.forEach((key, value) -> locationsState.put(key, value.copy()));
            currentSelection = new LongObjectHashMap<>();
            GraphRenderer.updateElements();
            active = true;
        }
        else {
            save();
            clear();
        }
        Output.actionBar(active ? "Graph editing enabled." : "Graph editing disabled.", Output.Color.GOLD);
        return 1;
    }

    public static void toggleMode() {
        if (!currentSelection.isEmpty()) {
            Output.chat("Can't change mode: current selection isn't empty.");
            return;
        }
        mode = mode.next();
        Output.actionBar("Using " + mode.name + " mode.", Output.Color.GOLD);
    }

    public static void clear() {
        active = false;
        hasChanges = false;
        TargetHolder.targeted = null;
        waypointsState = null;
        locationsState = null;
        currentSelection = null;
        selected = null;
        GraphRenderer.clear();
    }
}
