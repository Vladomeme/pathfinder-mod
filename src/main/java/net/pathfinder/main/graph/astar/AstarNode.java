package net.pathfinder.main.graph.astar;

import net.minecraft.util.math.BlockPos;

/**
 * A node type used in A*.
 */
public class AstarNode implements Comparable<AstarNode> {

    final BlockPos currentPos;
    BlockPos previousPos;
    double currentScore;
    double estimatedScore;

    public AstarNode(BlockPos currentPos) {
        this(currentPos, null, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    public AstarNode(BlockPos currentPos, BlockPos previousPos, double currentScore, double estimatedScore) {
        this.currentPos = currentPos;
        this.previousPos = previousPos;
        this.currentScore = currentScore;
        this.estimatedScore = estimatedScore;
    }

    @Override
    public int compareTo(AstarNode o) {
        return Double.compare(this.estimatedScore, o.estimatedScore);
    }
}
