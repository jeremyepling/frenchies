package net.j40climb.frenchies.common.entity.frenchie.frontpack;

import net.j40climb.frenchies.Frenchies;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

final class FrontpackEquipment {
    static final TagKey<Item> PACKS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Frenchies.LEGACY_NAMESPACE, "front_packs"));

    static ItemStack wornBy(LivingEntity wearer) {
        var chest = wearer.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.is(PACKS)) return chest;
        return FrontpackAccessorySlots.findEquipped(wearer);
    }
}
