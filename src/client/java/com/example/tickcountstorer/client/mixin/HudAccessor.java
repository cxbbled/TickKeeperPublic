
package com.example.tickcountstorer.client.mixin;

import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Hud.class)
public interface HudAccessor {
    @Accessor("tickCount")
    int tickCountStorer$getTickCount();

    @Accessor("tickCount")
    void tickCountStorer$setTickCount(int value);
}