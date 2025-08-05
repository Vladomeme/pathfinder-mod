package net.pathfinder.main.graph.waypoint.path;

import net.minecraft.util.math.Vec3i;

/**
 * A node type used to store current path.
 */
public class PathNode extends Vec3i {

    PathNode next;
    final boolean isTeleport;
    final boolean oneWay;

    public PathNode(Vec3i pos, boolean isTeleport, boolean oneWay) {
        super(pos.getX(), pos.getY(), pos.getZ());
        this.isTeleport = isTeleport;
        this.oneWay = oneWay;
    }

    public PathNode getLast() {
        PathNode current = this;
        while (current.next != null) current = current.next;
        return current;
    }
}
