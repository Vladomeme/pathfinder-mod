package net.pathfinder.main.graph.waypoint.data;

import net.pathfinder.main.graph.waypoint.GraphEditor;

import java.util.Arrays;

//todo remove id field
//todo traversal requirements
/**
 * Contains optional data for a waypoint.
 */
public class LocationData {

    final long id;

    private boolean isTeleport;
    private boolean oneWay;
    private String name;
    private int[] area;

    private LocationData(long id) {
        this.id = id;
    }

    private LocationData(long id, boolean isTeleport, boolean oneWay, String name, int[] area) {
        this.id = id;
        this.isTeleport = isTeleport;
        this.oneWay = oneWay;
        this.name = name;
        this.area = area;
    }

    public LocationData setTeleport(boolean isTeleport) {
        this.isTeleport = isTeleport;
        return this;
    }

    public LocationData setOneWay(boolean oneWay) {
        this.oneWay = oneWay;
        return this;
    }

    public LocationData setName(String name) {
        this.name = name;
        return this;
    }

    public LocationData setArea(int[] area) {
        this.area = area;
        return this;
    }

    public long id() {
        return id;
    }

    public boolean isTeleport() {
        return isTeleport;
    }

    public boolean isOneWay() {
        return oneWay;
    }

    public String name() {
        return name;
    }

    public int[] area() {
        return area;
    }

    public LocationData copy() {
        return new LocationData(this.id, this.isTeleport, this.oneWay, this.name, Arrays.copyOf(this.area, this.area.length));
    }

    public void replaceWithID(long id) {
        GraphEditor.locationsState.remove(this.id());
        GraphEditor.locationsState.put(id, new LocationData(id, this.isTeleport, this.oneWay, this.name, Arrays.copyOf(this.area, this.area.length)));
    }

    public boolean matchesDefault() {
        return (!isTeleport && !oneWay && (name == null || name.isEmpty())
                && (area == null || (area[0] == 0 && area[1] == 0 && area[2] == 0 && area[3] == 0 && area[4] == 0 && area[5] == 0)));
    }

    public static LocationData create(long waypointID) {
        return new LocationData(waypointID);
    }
}
