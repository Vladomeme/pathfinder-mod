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
import static net.pathfinder.main.config.PFConfig.cfg;

/**
 * Used for constructing a Base graph, which shows all locations reachable from a current one.
 */
public class BaseBuilder {

    public static final BaseBuilder INSTANCE = new BaseBuilder();

    final Set<BaseLink> links = new HashSet<>();
    final Stack<BlockPos> open = new Stack<>();
    final Map<BlockPos, Boolean> closed = new HashMap<>();

    @SuppressWarnings("SameReturnValue")
    public int compute(CommandContext<FabricClientCommandSource> context) {
        ClientPlayerEntity player = context.getSource().getPlayer();
        CandidateSupplier supplier = new CandidateSupplier(player.clientWorld);
        BlockPos start = player.getBlockPos();
        DebugManager.start = start;
        DebugManager.start3d = Vec3d.of(start);

        open.add(start);

        while (!open.isEmpty()) {
            BlockPos current = open.pop();
            closed.put(current, true);
            List<BlockPos> newNodes = supplier.getCandidates(current)
                    .stream().map(CandidateNode::pos).filter(pos -> !closed.containsKey(pos)).toList();
            open.addAll(newNodes);
            newNodes.forEach(pos -> links.add(new BaseLink(current, pos)));
            if (closed.size() >= cfg.baseGraphMaxNodes) break;
        }
        Output.chat("Found " + closed.size() + " nodes with " + links.size() + " links.");
        DebugManager.lines.addAll(links.stream().map(link -> new Pair<>(link.left.toCenterPos(), link.right.toCenterPos())).toList());

        links.clear();
        closed.clear();
        open.clear();
        return 1;
    }
}
