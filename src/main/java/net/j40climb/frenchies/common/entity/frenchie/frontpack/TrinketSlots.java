package net.j40climb.frenchies.common.entity.frenchie.frontpack;

import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.TrinketsApi;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

final class TrinketSlots {
    static ItemStack find(LivingEntity entity) {
        var slots = TrinketsApi.getAttachment(entity);
        if (slots == null) return ItemStack.EMPTY;
        // Ignore cosmetic slots, which cannot apply equipment effects.
        return slots.findFirst(FrontpackEquipment.PACKS, true)
                .map(TrinketSlotAccess::get).orElse(ItemStack.EMPTY);
    }
}
