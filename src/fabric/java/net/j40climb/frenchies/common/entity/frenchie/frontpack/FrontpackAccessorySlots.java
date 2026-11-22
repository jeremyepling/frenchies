package net.j40climb.frenchies.common.entity.frenchie.frontpack;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class FrontpackAccessorySlots {
    public static ItemStack findEquipped(LivingEntity entity) {
        return FabricLoader.getInstance().isModLoaded("trinkets_updated") ? TrinketSlots.find(entity) : ItemStack.EMPTY;
    }
}
