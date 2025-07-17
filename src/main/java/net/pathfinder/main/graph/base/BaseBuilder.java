package net.pathfinder.main.graph.base;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.pathfinder.main.Output;
import net.pathfinder.main.graph.*;

import java.util.*;

/**
 * Used for constructing a Base graph, which shows all locations reachable from a current one.
 */
public class BaseBuilder {

    public static final BaseBuilder INSTANCE = new BaseBuilder();

    int nodeCount = 1;
    final Set<BaseLink> links = new HashSet<>();
    final Stack<BlockPos> open = new Stack<>();
    final Map<BlockPos, Boolean> closed = new HashMap<>();

    @SuppressWarnings("SameReturnValue")
    public int compute(CommandContext<FabricClientCommandSource> context) {
        ClientPlayerEntity player = context.getSource().getPlayer();
        BlockPos start = player.getBlockPos();
        RuleHolder.start = start;
        RuleHolder.start3d = Vec3d.of(start);

        open.add(start);

        while (!open.isEmpty()) {
            BlockPos current = open.pop();
            closed.put(current, true);
            List<BlockPos> newNodes = CandidateSupplier.getCandidates(player.clientWorld, current, GraphType.Base)
                    .stream().map(CandidateNode::pos).filter(pos -> !closed.containsKey(pos)).toList();
            nodeCount += newNodes.size();
            open.addAll(newNodes);
            newNodes.forEach(pos -> links.add(new BaseLink(current, pos)));
        }
        Output.chat("Found " + nodeCount + " nodes with " + links.size() + " links.");
        GraphRenderer.lines.addAll(links.stream().map(link -> new Pair<>(link.left.toCenterPos(), link.right.toCenterPos())).toList());
        finish();
        return 1;
    }

    @SuppressWarnings("SameReturnValue")
    public void finish() {
        links.clear();
        closed.clear();
        open.clear();
        nodeCount = 1;
    }

    public int clear() {
        GraphRenderer.lines.clear();
        finish();
        return 1;
    }
}
