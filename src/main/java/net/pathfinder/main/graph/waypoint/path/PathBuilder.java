package net.pathfinder.main.graph.waypoint.path;

import io.netty.util.collection.LongObjectHashMap;
import net.minecraft.util.math.BlockPos;
import net.pathfinder.main.Output;
import net.pathfinder.main.graph.RuleHolder;
import net.pathfinder.main.graph.waypoint.WaypointIO;
import net.pathfinder.main.graph.waypoint.data.Waypoint;

import java.util.*;

/**
 * A class responsible for calculating the path using the navigation graph.
 */
public class PathBuilder {

    /**
     * Calculates the path by running Dijkstra's on a graph and connecting start/end segments.
     */
    public static PathNode getPath(BlockPos start, BlockPos end) {
        if (WaypointIO.getData() == null) {
            Output.chat("Couldn't find a path - dimension data is unavailable.");
            return null;
        }

        Waypoint startWaypoint = WaypointIO.getData().getNearest(start, 200);
        Waypoint endWaypoint = WaypointIO.getData().getNearest(end, 200);
        if (startWaypoint == null || endWaypoint == null) {
            Output.chat("Returned path is null. The player or the destination is too far from the graph.");
            return null;
        }

        //Starting segment
        PathNode head = new PathNode(getNearestPoint(start, startWaypoint), false);
        PathNode current = head;

        //Graph segment
        List<DijkstraNode> path = runDijkstra(startWaypoint, endWaypoint);
        for (DijkstraNode node : path) {
            PathNode newNode = new PathNode(node.pos(), node.isTeleport());
            current.next = newNode;
            current = newNode;
        }

        //Ending segment
        PathNode graphEnd = new PathNode(getNearestPoint(end, endWaypoint), false);
        current.next = graphEnd;
        graphEnd.next = new PathNode(end, false);
        return head;
    }

    private static List<DijkstraNode> runDijkstra(Waypoint start, Waypoint end) {
        LongObjectHashMap<DijkstraNode> nodes = new LongObjectHashMap<>();
        WaypointIO.getData().waypoints.values()
                .forEach(waypoint -> nodes.put(waypoint.id(), new DijkstraNode(waypoint)));

        DijkstraNode startNode = nodes.get(start.id());
        startNode.distance = 0;

        Queue<DijkstraNode> open = new PriorityQueue<>();
        LongObjectHashMap<Boolean> closed = new LongObjectHashMap<>();
        open.add(startNode);

        while (!open.isEmpty()) {
            DijkstraNode current = open.poll();

            if (current.id() == end.id()) {
                List<DijkstraNode> path = new ArrayList<>();

                while (current != null) {
                    path.add(current);
                    current = current.previous;
                }
                Collections.reverse(path);
                return path;
            }

            for (long l : current.waypoint.neighbours()) {
                if (closed.containsKey(l)) continue;
                DijkstraNode neighbour = nodes.get(l);

                float newDistance;
                if (current.isTeleport() && neighbour.isTeleport()) newDistance = current.distance;
                else newDistance = current.distance + RuleHolder.getDistance(neighbour.pos(), current.pos());

                if (neighbour.distance > newDistance) {
                    neighbour.distance = newDistance;
                    neighbour.previous = current;
                }
                open.add(neighbour);
            }
            closed.put(current.id(), Boolean.TRUE);
        }
        throw new RuntimeException("Failed to find a path using the waypoint graph. This shouldn't be possible!");
    }

    /**
     * Returns the nearest point of a graph (including connections between points).
     */
    private static BlockPos getNearestPoint(BlockPos pos, Waypoint nearest) {
        int x1 = pos.getX();
        int y1 = pos.getY();
        int z1 = pos.getZ();

        int x2 = nearest.x();
        int y2 = nearest.y();
        int z2 = nearest.z();

        float minDistance = Float.MAX_VALUE;
        BlockPos pos1 = nearest.pos();
        BlockPos pos2 = null;
        BlockPos pos3 = null;

        for (long l : nearest.neighbours()) {
            Waypoint complement = WaypointIO.getData().waypoints.get(l);

            int xVec = complement.x() - x2;
            int yVec = complement.y() - y2;
            int zVec = complement.z() - z2;

            float t = (float) -(xVec * (x2 - x1) + yVec * (y2 - y1) + zVec * (z2 - z1)) / (xVec * xVec + yVec * yVec + zVec * zVec);

            float x = x2 + xVec * t;
            float y = y2 + yVec * t;
            float z = z2 + zVec * t;

            float distance = RuleHolder.getSquaredDistance(x1, y1, z1, x, y, z) + RuleHolder.getSquaredDistance(x2, y2, z2, x, y, z);
            if (minDistance > distance) {
                minDistance = distance;
                pos3 = new BlockPos((int) x, (int) y, (int) z);
                pos2 = complement.pos();
            }
        }
        if (pos2 == null) throw new RuntimeException("Nearest point was not found. This shouldn't be possible.");

        int x3 = pos2.getX();
        int y3 = pos2.getY();
        int z3 = pos2.getZ();

        int x = pos3.getX();
        int y = pos3.getY();
        int z = pos3.getZ();

        if (x2 != x3) {
            if (Math.min(x2, x3) <= x && x <= Math.max(x2, x3)) return pos3;
            else if (x2 < x3) return (x < x2) ? pos1 : pos2;
            else return (x < x3) ? pos2 : pos1;
        }
        if (y2 != y3) {
            if (Math.min(y2, y3) <= y && y <= Math.max(y2, y3)) return pos3;
            else if (y2 < y3) return (y < y2) ? pos1 : pos2;
            else return (y < y3) ? pos2 : pos1;
        }
        if (z2 != z3) {
            if (Math.min(z2, z3) <= z && z <= Math.max(z2, z3)) return pos3;
            else if (z2 < z3) return (z < z2) ? pos1 : pos2;
            else return (z < z3) ? pos2 : pos1;
        }
        //Why is it like this
        return pos2;
    }

    /**
     * Finds the nearest point on a line (pos1; pos2).
     */
    public static float[] getNearestPointInPath(BlockPos playerPos, BlockPos pos1, BlockPos pos2) {
        int x1 = playerPos.getX();
        int y1 = playerPos.getY();
        int z1 = playerPos.getZ();

        int x2 = pos1.getX();
        int y2 = pos1.getY();
        int z2 = pos1.getZ();

        int x3 = pos2.getX();
        int y3 = pos2.getY();
        int z3 = pos2.getZ();

        int xVec = x3 - x2;
        int yVec = y3 - y2;
        int zVec = z3 - z2;

        float t = (float) -(xVec * (x2 - x1) + yVec * (y2 - y1) + zVec * (z2 - z1)) / (xVec * xVec + yVec * yVec + zVec * zVec);

        float x = x2 + xVec * t;
        float y = y2 + yVec * t;
        float z = z2 + zVec * t;

        float[] pos3 = new float[]{x, y, z};

        if (x2 != x3) {
            if (Math.min(x2, x3) <= x && x <= Math.max(x2, x3)) return pos3;
            else if (x2 < x3) return (x < x2) ? toFloatArray(pos1) : toFloatArray(pos2);
            else return (x < x3) ? toFloatArray(pos2) : toFloatArray(pos1);
        }
        if (y2 != y3) {
            if (Math.min(y2, y3) <= y && y <= Math.max(y2, y3)) return pos3;
            else if (y2 < y3) return (y < y2) ? toFloatArray(pos1) : toFloatArray(pos2);
            else return (y < y3) ? toFloatArray(pos2) : toFloatArray(pos1);
        }
        if (z2 != z3) {
            if (Math.min(z2, z3) <= z && z <= Math.max(z2, z3)) return pos3;
            else if (z2 < z3) return (z < z2) ? toFloatArray(pos1) : toFloatArray(pos2);
            else return (z < z3) ? toFloatArray(pos2) : toFloatArray(pos1);
        }
        //What the hell man
        return toFloatArray(pos2);
    }

    private static float[] toFloatArray(BlockPos pos) {
        return new float[]{pos.getX(), pos.getY(), pos.getZ()};
    }
}
