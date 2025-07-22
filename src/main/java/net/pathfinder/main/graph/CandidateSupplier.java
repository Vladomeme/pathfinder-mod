package net.pathfinder.main.graph;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

import static net.pathfinder.main.graph.PositionUtils.*;
import static net.pathfinder.main.config.PFConfig.cfg;

/**
 * Class responsible for finding new world positions using movement rules.
 */
public class CandidateSupplier {

    private final ClientWorld world;
    private BlockPos pos;
    private List<CandidateNode> nodes;

    public CandidateSupplier(ClientWorld world) {
        this.world = world;
    }

    public List<CandidateNode> getCandidates(BlockPos pos) {
        if (outOfRangeTrue(pos)) return List.of();

        this.pos = pos;
        this.nodes = new ArrayList<>();

        moveStraight0(-1, 0);
        moveStraight0(0, -1);
        moveStraight0(1, 0);
        moveStraight0(0, 1);

        moveDiagonal0(-1, -1);
        moveDiagonal0(-1, 1);
        moveDiagonal0(1, -1);
        moveDiagonal0(1, 1);

        moveStraight1(-1, 0);
        moveStraight1(0, -1);
        moveStraight1(1, 0);
        moveStraight1(0, 1);

        moveDiagonal1(-1, -1);
        moveDiagonal1(-1, 1);
        moveDiagonal1(1, -1);
        moveDiagonal1(1, 1);

        moveStraight2(-1, 0);
        moveStraight2(0, -1);
        moveStraight2(1, 0);
        moveStraight2(0, 1);

        moveDiagonal2(-1, -1);
        moveDiagonal2(-1, 1);
        moveDiagonal2(1, -1);
        moveDiagonal2(1, 1);

        moveDown();
        moveUp();

        return nodes;
    }

    private void moveStraight0(int xVec, int zVec) {
        BlockPos newPos = pos.mutableCopy().add(xVec, -1, zVec);

        boolean b1 = isValidPosition(world, newPos);
        boolean b2 = isPassable(world, newPos, 0, 2, 0);

        if (b1 && b2) addNode(newPos, cfg.diagonalCost, Movement.DOWN);
    }

    private void moveDiagonal0(int xVec, int zVec) {
        BlockPos newPos = pos.mutableCopy().add(xVec, -1, zVec);

        int xVector = pos.getX() - newPos.getX();
        int zVector = pos.getZ() - newPos.getZ();

        boolean b1 = isValidPosition(world, newPos);
        boolean b2 = isPassable(world, newPos, 0, 2, 0);
        boolean b3 = isPassable(world, newPos, xVector, 1, 0);
        boolean b4 = isPassable(world, newPos, xVector, 2, 0);
        boolean b5 = isPassable(world, newPos, 0, 1, zVector);
        boolean b6 = isPassable(world, newPos, 0, 2, zVector);

        if (b1 && b2 && ((b3 && b4) || (b5 && b6))) addNode(newPos, cfg.cubeDiagonalCost, Movement.DOWN);
    }

    private void moveStraight1(int xVec, int zVec) {
        BlockPos newPos = pos.mutableCopy().add(xVec, 0, zVec);

        boolean b1 = isValidPosition(world, newPos);

        if (b1) addNode(newPos, cfg.straightCost, Movement.LEVEL);
    }

    private void moveDiagonal1(int xVec, int zVec) {
        BlockPos newPos = pos.mutableCopy().add(xVec, 0, zVec);

        int xVector = pos.getX() - newPos.getX();
        int zVector = pos.getZ() - newPos.getZ();

        boolean b1 = isValidPosition(world, newPos);
        boolean b2 = isPassable(world, newPos, xVector, 0, 0);
        boolean b3 = isPassable(world, newPos, xVector, 1, 0);
        boolean b4 = isPassable(world, newPos, 0, 0, zVector);
        boolean b5 = isPassable(world, newPos, 0, 1, zVector);

        if (b1 && ((b2 && b3) || (b4 && b5))) addNode(newPos, cfg.diagonalCost, Movement.LEVEL);
    }

    private void moveStraight2(int xVec, int zVec) {
        BlockPos newPos = pos.mutableCopy().add(xVec, 1, zVec);

        boolean b1 = isValidPosition(world, newPos) && notFence(world, newPos, 0, -1, 0);
        boolean b2 = isSolid(world, pos, 0, -1, 0);
        boolean b3 = isPassable(world, pos, 0, 2, 0);

        if (b1 && b2 && b3) addNode(newPos, cfg.diagonalCost, Movement.UP);
    }

    private void moveDiagonal2(int xVec, int zVec) {
        BlockPos newPos = pos.mutableCopy().add(xVec, 1, zVec);

        int xVector = pos.getX() - newPos.getX();
        int zVector = pos.getZ() - newPos.getZ();

        boolean b1 = isValidPosition(world, newPos) && notFence(world, newPos, 0, -1, 0);
        boolean b2 = isSolid(world, pos, 0, -1, 0);
        boolean b3 = isPassable(world, newPos, xVector, 0, 0);
        boolean b4 = isPassable(world, newPos, xVector, 1, 0);
        boolean b5 = isPassable(world, newPos, 0, 0, zVector);
        boolean b6 = isPassable(world, newPos, 0, 1, zVector);
        boolean b7 = isPassable(world, pos, 0, 2, 0);

        if (b1 && b2 && ((b3 && b4) || (b5 && b6)) && b7) addNode(newPos, cfg.cubeDiagonalCost, Movement.UP);
    }

    private void moveDown() {
        BlockPos newPos = pos.mutableCopy().add(0, -1, 0);

        boolean b1 = isClimbable(world, newPos);
        boolean b2 = isPassable(world, pos);

        if (b1 && b2) addNode(newPos, cfg.verticalCost, Movement.DOWN);
        else {
            boolean b3 = isStandable(world, newPos, 0, -1, 0);
            boolean b4 = isPassable(world, newPos) && isSafe(world, newPos);

            if (b3 && b4) addNode(newPos, cfg.verticalCost, Movement.DOWN);
        }
    }

    private void moveUp() {
        BlockPos newPos = pos.mutableCopy().add(0, 1, 0);

        boolean b1 = isClimbable(world, newPos);
        boolean b2 = isPassable(world, newPos, 0, 1, 0);

        if (b1 && b2) addNode(newPos, cfg.verticalCost, Movement.UP);
        else {
            boolean b3 = isClimbable(world, pos) && isStandable(world, pos);
            boolean b4 = isPassable(world, newPos);
            boolean b5 = isPassable(world, newPos, 0, 1, 0);

            if (b3 && b4 && b5) addNode(newPos, cfg.verticalCost, Movement.UP);
        }
    }

    private void addNode(BlockPos newPos, float cost, Movement move) {
        nodes.add(new CandidateNode(newPos, computeCost(newPos, cost, move)));
    }

    private float computeCost(BlockPos pos, float cost, Movement move) {
        BlockState state1 = world.getBlockState(pos.mutableCopy().add(0, -1, 0));
        BlockState state2 = world.getBlockState(pos);
        BlockState state3 = world.getBlockState(pos.mutableCopy().add(0, 1, 0));
        
        if (move != Movement.LEVEL) cost += cfg.yChangeCost;
        if (state1.getBlock().equals(Blocks.WATER) || state2.getBlock().equals(Blocks.WATER)) cost *= cfg.waterMulti;
        if (state2.getBlock().equals(Blocks.COBWEB) || state3.getBlock().equals(Blocks.COBWEB)) cost += cfg.cobwebMulti;
        if (move != Movement.LEVEL && state1.isIn(BlockTags.STAIRS)) cost = cfg.stairsCost;

        return cost;
    }

    private enum Movement {
        DOWN,
        LEVEL,
        UP
    }
}
