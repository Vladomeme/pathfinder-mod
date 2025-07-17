package net.pathfinder.main.graph.base;

import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Updates data after changing current dimension.
 */
public class BaseLink implements Comparable<BaseLink> {

    public final BlockPos left;
    public final BlockPos right;

    public BaseLink(BlockPos pos1, BlockPos pos2) {
        if (pos1.compareTo(pos2) < 0) {
            this.left = pos1;
            this.right = pos2;
        }
        else {
            this.left = pos2;
            this.right = pos1;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof BaseLink link) return (left.equals(link.left) && right.equals(link.right));
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, right);
    }

    @Override
    public int compareTo(@NotNull BaseLink o) {
        int result = left.compareTo(o.left);
        if (result != 0) return result;
        return right.compareTo(o.right);
    }
}
