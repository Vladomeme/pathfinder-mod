package net.pathfinder.main.graph;

import net.minecraft.util.math.BlockPos;

/**
 * A node type used in converting the base world data into base view.
 */
public record CandidateNode(BlockPos pos, float cost) {

}
