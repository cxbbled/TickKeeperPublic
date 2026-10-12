package com.example.tickcountstorer.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class ExampleClientMixin {
	@Shadow
	private int tickCount;

	private long lastPrintTime = 0;

	@Inject(method = "tick(Z)V", at = @At("TAIL"))
	private void tickCountStorer$afterHudTick(boolean pause, CallbackInfo ci) {
		if (Minecraft.getInstance().level == null) {
			return;
		}

		long currentTime = System.currentTimeMillis();

		if (currentTime - lastPrintTime >= 500) {
			// System.out.println("HUD tickCount = " + tickCount);
			lastPrintTime = currentTime;
		}
	}
}