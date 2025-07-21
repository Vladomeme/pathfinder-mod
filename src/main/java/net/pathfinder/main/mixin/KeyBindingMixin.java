package net.pathfinder.main.mixin;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.pathfinder.main.graph.render.HudRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyBinding.class)
public abstract class KeyBindingMixin {

	@Inject(method = "setBoundKey", at = @At(value = "HEAD"))
	private void setBoundKey(InputUtil.Key boundKey, CallbackInfo ci) {
		HudRenderer.updateKeybindTexts();
	}
}
