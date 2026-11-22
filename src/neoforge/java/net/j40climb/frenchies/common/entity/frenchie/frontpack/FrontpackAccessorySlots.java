package net.j40climb.frenchies.common.entity.frenchie.frontpack;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

public final class FrontpackAccessorySlots {
    private FrontpackAccessorySlots() {}

    public static ItemStack findEquipped(LivingEntity entity) {
        if (ModList.get().isLoaded("curios")) {
            var pack = CuriosSlots.find(entity);
            if (!pack.isEmpty()) return pack;
        }
        if (ModList.get().isLoaded("trinkets_updated")) {
            var pack = TrinketSlots.find(entity);
            if (!pack.isEmpty()) return pack;
        }
        //? if <26.3 {
        if (entity instanceof net.minecraft.world.entity.Avatar avatar && ModList.get().isLoaded("baubles")) {
            return BaublesFrontpack.findEquipped(avatar);
        }
        //?}
        return ItemStack.EMPTY;
    }

    private static final class CuriosSlots {
        private static ItemStack find(LivingEntity entity) {
            var slots = CuriosApi.getCuriosInventoryOrNull(entity);
            if (slots == null) return ItemStack.EMPTY;
            // The predicate overload sees equips and removals immediately, including in the same tick.
            return slots.findFirstCurio(stack -> stack.is(FrontpackEquipment.PACKS))
                    .map(SlotResult::stack).orElse(ItemStack.EMPTY);
        }
    }
}
