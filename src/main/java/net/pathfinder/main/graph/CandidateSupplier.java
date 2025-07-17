package net.pathfinder.main.graph;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static net.pathfinder.main.graph.RuleHolder.*;

/**
 * Class responsible for finding new world positions using movement rules.
 */
public class CandidateSupplier {

    //todo wall proximity cost
    private static GraphType type = GraphType.Base;
    private static final float SQRT2 = (float) Math.sqrt(2d);
    private static final float SQRT3 = (float) Math.sqrt(3d);
    private static final float SQRT2y = (float) Math.sqrt(2d) + 0.1f;
    private static final float SQRT3y = (float) Math.sqrt(3d) + 0.1f;

    public static List<CandidateNode> getCandidates(ClientWorld world, BlockPos pos, GraphType type) {
        CandidateSupplier.type = type;

        List<CandidateNode> nodes = new ArrayList<>();

        moveStraight0(world, pos, -1, 0).ifPresent(nodes::add);
        moveStraight0(world, pos, 0, -1).ifPresent(nodes::add);
        moveStraight0(world, pos, 1, 0).ifPresent(nodes::add);
        moveStraight0(world, pos, 0, 1).ifPresent(nodes::add);

        moveDiagonal0(world, pos, -1, -1).ifPresent(nodes::add);
        moveDiagonal0(world, pos, -1, 1).ifPresent(nodes::add);
        moveDiagonal0(world, pos, 1, -1).ifPresent(nodes::add);
        moveDiagonal0(world, pos, 1, 1).ifPresent(nodes::add);

        moveStraight1(world, pos, -1, 0).ifPresent(nodes::add);
        moveStraight1(world, pos, 0, -1).ifPresent(nodes::add);
        moveStraight1(world, pos, 1, 0).ifPresent(nodes::add);
        moveStraight1(world, pos, 0, 1).ifPresent(nodes::add);

        moveDiagonal1(world, pos, -1, -1).ifPresent(nodes::add);
        moveDiagonal1(world, pos, -1, 1).ifPresent(nodes::add);
        moveDiagonal1(world, pos, 1, -1).ifPresent(nodes::add);
        moveDiagonal1(world, pos, 1, 1).ifPresent(nodes::add);

        moveStraight2(world, pos, -1, 0).ifPresent(nodes::add);
        moveStraight2(world, pos, 0, -1).ifPresent(nodes::add);
        moveStraight2(world, pos, 1, 0).ifPresent(nodes::add);
        moveStraight2(world, pos, 0, 1).ifPresent(nodes::add);

        moveDiagonal2(world, pos, -1, -1).ifPresent(nodes::add);
        moveDiagonal2(world, pos, -1, 1).ifPresent(nodes::add);
        moveDiagonal2(world, pos, 1, -1).ifPresent(nodes::add);
        moveDiagonal2(world, pos, 1, 1).ifPresent(nodes::add);

        moveDown(world, pos).ifPresent(nodes::add);
        moveUp(world, pos).ifPresent(nodes::add);

        return nodes;
    }

    public static Optional<CandidateNode> moveStraight0(ClientWorld world, BlockPos oldPos, int xVec, int zVec) {
        BlockPos newPos = oldPos.mutableCopy().add(xVec, -1, zVec);
        if (outOfRangeTrue(newPos)) return Optional.empty();

        boolean b1 = isValidPosition(world, newPos);
        boolean b2 = isPassable(world, newPos, 0, 2, 0);

        if (b1 && b2) return createNode(world, newPos, SQRT2y, Movement.DOWN);
        return Optional.empty();
    }

    public static Optional<CandidateNode> moveDiagonal0(ClientWorld world, BlockPos oldPos, int xVec, int zVec) {
        BlockPos newPos = oldPos.mutableCopy().add(xVec, -1, zVec);
        if (outOfRangeTrue(newPos)) return Optional.empty();

        int xVector = oldPos.getX() - newPos.getX();
        int zVector = oldPos.getZ() - newPos.getZ();

        boolean b1 = isValidPosition(world, newPos);
        boolean b2 = isPassable(world, newPos, 0, 2, 0);
        boolean b3 = isPassable(world, newPos, xVector, 1, 0);
        boolean b4 = isPassable(world, newPos, xVector, 2, 0);
        boolean b5 = isPassable(world, newPos, 0, 1, zVector);
        boolean b6 = isPassable(world, newPos, 0, 2, zVector);

        if (b1 && b2 && ((b3 && b4) || (b5 && b6))) return createNode(world, newPos, SQRT3y, Movement.DOWN);
        return Optional.empty();
    }

    public static Optional<CandidateNode> moveStraight1(ClientWorld world, BlockPos oldPos, int xVec, int zVec) {
        BlockPos newPos = oldPos.mutableCopy().add(xVec, 0, zVec);
        if (outOfRangeTrue(newPos)) return Optional.empty();

        boolean b1 = isValidPosition(world, newPos);

        if (b1) return createNode(world, newPos, 1.0f, Movement.LEVEL);
        return Optional.empty();
    }

    public static Optional<CandidateNode> moveDiagonal1(ClientWorld world, BlockPos oldPos, int xVec, int zVec) {
        BlockPos newPos = oldPos.mutableCopy().add(xVec, 0, zVec);
        if (outOfRangeTrue(newPos)) return Optional.empty();

        int xVector = oldPos.getX() - newPos.getX();
        int zVector = oldPos.getZ() - newPos.getZ();

        boolean b1 = isValidPosition(world, newPos);
        boolean b2 = isPassable(world, newPos, xVector, 0, 0);
        boolean b3 = isPassable(world, newPos, xVector, 1, 0);
        boolean b4 = isPassable(world, newPos, 0, 0, zVector);
        boolean b5 = isPassable(world, newPos, 0, 1, zVector);

        if (b1 && ((b2 && b3) || (b4 && b5))) return createNode(world, newPos, SQRT2, Movement.LEVEL);
        return Optional.empty();
    }

    public static Optional<CandidateNode> moveStraight2(ClientWorld world, BlockPos oldPos, int xVec, int zVec) {
        BlockPos newPos = oldPos.mutableCopy().add(xVec, 1, zVec);
        if (outOfRangeTrue(newPos)) return Optional.empty();

        boolean b1 = isValidPosition(world, newPos) && notFence(world, newPos, 0, -1, 0);
        boolean b2 = isSolid(world, oldPos, 0, -1, 0);
        boolean b3 = isPassable(world, oldPos, 0, 2, 0);

        if (b1 && b2 && b3) return createNode(world, newPos, SQRT2y, Movement.UP);
        return Optional.empty();
    }

    public static Optional<CandidateNode> moveDiagonal2(ClientWorld world, BlockPos oldPos, int xVec, int zVec) {
        BlockPos newPos = oldPos.mutableCopy().add(xVec, 1, zVec);
        if (outOfRangeTrue(newPos)) return Optional.empty();

        int xVector = oldPos.getX() - newPos.getX();
        int zVector = oldPos.getZ() - newPos.getZ();

        boolean b1 = isValidPosition(world, newPos) && notFence(world, newPos, 0, -1, 0);
        boolean b2 = isSolid(world, oldPos, 0, -1, 0);
        boolean b3 = isPassable(world, newPos, xVector, 0, 0);
        boolean b4 = isPassable(world, newPos, xVector, 1, 0);
        boolean b5 = isPassable(world, newPos, 0, 0, zVector);
        boolean b6 = isPassable(world, newPos, 0, 1, zVector);
        boolean b7 = isPassable(world, oldPos, 0, 2, 0);

        if (b1 && b2 && ((b3 && b4) || (b5 && b6)) && b7) return createNode(world, newPos, SQRT3y, Movement.UP);
        return Optional.empty();
    }

    public static Optional<CandidateNode> moveDown(ClientWorld world, BlockPos oldPos) {
        BlockPos newPos = oldPos.mutableCopy().add(0, -1, 0);
        if (outOfRangeTrue(newPos)) return Optional.empty();

        boolean b1 = isClimbable(world, newPos);
        boolean b2 = isPassable(world, oldPos);

        if (b1 && b2) return createNode(world, newPos, 2.0f, Movement.LEVEL);
        else {
            boolean b3 = isStandable(world, newPos, 0, -1, 0);
            boolean b4 = isPassable(world, newPos) && isSafe(world, newPos);

            if (b3 && b4) return createNode(world, newPos, 2.0f, Movement.LEVEL);
        }
        return Optional.empty();
    }

    public static Optional<CandidateNode> moveUp(ClientWorld world, BlockPos oldPos) {
        BlockPos newPos = oldPos.mutableCopy().add(0, 1, 0);
        if (outOfRangeTrue(newPos)) return Optional.empty();

        boolean b1 = isClimbable(world, newPos);
        boolean b2 = isPassable(world, newPos, 0, 1, 0);

        if (b1 && b2) return createNode(world, newPos, 2.0f, Movement.LEVEL);
        else {
            boolean b3 = isClimbable(world, oldPos) && isStandable(world, oldPos);
            boolean b4 = isPassable(world, newPos);
            boolean b5 = isPassable(world, newPos, 0, 1, 0);

            if (b3 && b4 && b5) return createNode(world, newPos, 2.0f, Movement.LEVEL);
        }
        return Optional.empty();
    }

    private static Optional<CandidateNode> createNode(ClientWorld world, BlockPos newPos, float cost, Movement move) {
        return Optional.of(new CandidateNode(newPos, computeCost(world, newPos, cost, move)));
    }

    static float computeCost(ClientWorld world, BlockPos pos, float cost, Movement move) {
        BlockState state1 = world.getBlockState(pos.mutableCopy().add(0, -1, 0));
        BlockState state2 = world.getBlockState(pos);
        BlockState state3 = world.getBlockState(pos.mutableCopy().add(0, 1, 0));

        if (state1.getBlock().equals(Blocks.WATER) || state2.getBlock().equals(Blocks.WATER)) cost *= 2f;
        if (state2.getBlock().equals(Blocks.COBWEB) || state3.getBlock().equals(Blocks.COBWEB)) cost += 10f;
        if (move != Movement.LEVEL && state1.isIn(BlockTags.STAIRS)) cost = 1.0f;

        return cost;
    }

    enum Movement {
        DOWN,
        LEVEL,
        UP
    }
}
