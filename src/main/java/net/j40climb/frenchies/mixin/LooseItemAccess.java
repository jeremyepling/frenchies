package net.j40climb.frenchies.mixin;

import java.util.UUID;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.item.ItemEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Vanilla keeps these pickup restrictions even while the referenced player is offline. */
@Mixin(ItemEntity.class)
public interface LooseItemAccess {
    @Accessor("thrower")
    @Nullable EntityReference<Entity> frenchies$thrower();

    @Accessor("target")
    @Nullable UUID frenchies$pickupTarget();
}
