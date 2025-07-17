package net.pathfinder.main.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.pathfinder.main.PathfinderMod;
import net.pathfinder.main.graph.RuleHolder;
import net.pathfinder.main.graph.astar.AstarBuilder;
import net.pathfinder.main.graph.waypoint.GraphEditor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin used for getting left & right click inputs.=
 */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

	@Shadow
	public ClientPlayerEntity player;

	@Inject(method = "doAttack", at = @At(value = "HEAD"), cancellable = true)
	private void doAttack(CallbackInfoReturnable<Boolean> cir) {
		if (GraphEditor.active) {
			GraphEditor.onLeftClick();
			cir.setReturnValue(false);
		}
		else if (PathfinderMod.activationKey.isPressed()) {
			BlockHitResult hitResult = (BlockHitResult) player.raycast(RuleHolder.maxDistance, 0, false);

			if (hitResult.getType().equals(HitResult.Type.BLOCK)) {
                RuleHolder.start = getPosition(hitResult);
				RuleHolder.start3d = Vec3d.of(RuleHolder.start);

				if (RuleHolder.target != null) AstarBuilder.findOnUpdate();
			}
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "doItemUse", at = @At(value = "HEAD"), cancellable = true)
	private void doItemUse(CallbackInfo ci) {
		if (PathfinderMod.activationKey.isPressed() || GraphEditor.active) {
			BlockHitResult hitResult = (BlockHitResult) player.raycast(RuleHolder.maxDistance, 0, false);

			if (hitResult.getType().equals(HitResult.Type.BLOCK)) {
				BlockPos pos = getPosition(hitResult);
				if (GraphEditor.active) GraphEditor.onRightClick(pos);
				else {
					RuleHolder.target = getPosition(hitResult);
					RuleHolder.target3d = Vec3d.of(RuleHolder.target);

					if (RuleHolder.start != null) AstarBuilder.findOnUpdate();
				}
			}
			ci.cancel();
        }
	}

	@Unique
	private BlockPos getPosition(BlockHitResult hitResult) {
		BlockPos pos = hitResult.getBlockPos();
		return pos.add(hitResult.getSide().getVector());
	}
}
