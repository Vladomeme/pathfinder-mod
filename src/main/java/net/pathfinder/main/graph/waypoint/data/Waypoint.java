package net.pathfinder.main.graph.waypoint.data;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.pathfinder.main.Output;
import net.pathfinder.main.graph.waypoint.WaypointIO;

/**
 * Represents a waypoint - the main element of a waypoint graph.
 */
public class Waypoint {

    private final long id;
    private final int x;
    private final int y;
    private final int z;
    private final LongArrayList neighbours;

    public Waypoint(BlockPos pos) {
        this(pos.getX(), pos.getY(), pos.getZ());
    }

    public Waypoint(int x, int y, int z) {
        this.id = nextID();
        this.x = x;
        this.y = y;
        this.z = z;
        this.neighbours = new LongArrayList();
    }

    private Waypoint(Waypoint waypoint) {
        this.id = waypoint.id;
        this.x = waypoint.x;
        this.y = waypoint.y;
        this.z = waypoint.z;
        this.neighbours = waypoint.neighbours.clone();
    }

    public long id() {
        return id;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int z() {
        return z;
    }

    public LongArrayList neighbours() {
        return neighbours;
    }

    public LocationData location() {
        return WaypointIO.getData().locations.get(id);
    }

    //todo cache created objects using a separate class
    public BlockPos pos() {
        return new BlockPos(x, y, z);
    }

    public Vec3d teleportPos() {
        return new Vec3d(x + 0.25, y + 1.25, z + 0.25);
    }

    public Vec3d vec3d() {
        return new Vec3d(x, y, z);
    }

    public void addNeighbour(long id) {
        if (neighbours.contains(id))
            Output.logWarn("Attempted to add an already existing neighbour id! TARGET: " + this.id + ", ID: " + id);
        else neighbours.add(id);
    }

    public void removeNeighbour(long id) {
        neighbours.rem(id);
    }

    public String coordinates() {
        return x + " " + y + " " + z;
    }

    public Waypoint copy() {
        return new Waypoint(this);
    }

    private static long nextID() {
        return WaypointIO.getData().idTracker++;
    }
}
