package net.pathfinder.main.graph.waypoint.path;

import net.minecraft.util.math.BlockPos;
import net.pathfinder.main.graph.waypoint.WaypointIO;
import net.pathfinder.main.graph.waypoint.data.LocationData;
import net.pathfinder.main.graph.waypoint.data.Waypoint;
import org.jetbrains.annotations.NotNull;

/**
 * A node type used in path building.
 */
public class DijkstraNode implements Comparable<DijkstraNode> {

    final Waypoint waypoint;
    DijkstraNode previous;
    float distance;

    public DijkstraNode(Waypoint waypoint) {
        this.waypoint = waypoint;
        this.distance = Float.MAX_VALUE;
    }

    public long id() {
        return waypoint.id();
    }

    public BlockPos pos() {
        return waypoint.pos();
    }

    public boolean isTeleport() {
        LocationData info = WaypointIO.getData().locations.get(waypoint.id());
        if (info == null) return false;
        else return info.isTeleport();
    }

    @Override
    public int compareTo(@NotNull DijkstraNode o) {
        return Float.compare(distance, o.distance);
    }
}
