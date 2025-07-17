package net.pathfinder.main.graph.waypoint.path;

import net.minecraft.util.math.BlockPos;

/**
 * A node type used to store current path.
 */
public class PathNode {

    BlockPos pos;
    PathNode next;
    boolean isTeleport;

    public PathNode(BlockPos pos, boolean isTeleport) {
        this.pos = pos;
        this.isTeleport = isTeleport;
    }

    public int x() {
        return pos.getX();
    }

    public int y() {
        return pos.getY();
    }

    public int z() {
        return pos.getZ();
    }

    public PathNode getLast() {
        PathNode current = this;
        while (current.next != null) current = current.next;
        return current;
    }
}
