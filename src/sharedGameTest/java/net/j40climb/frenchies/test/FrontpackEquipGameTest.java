package net.j40climb.frenchies.test;

import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.GameType;

final class FrontpackEquipGameTest {
    static void equipFromHand(GameTestHelper h) {
        var p = GameTestPlayers.connect(h);
        try {
            p.setGameMode(GameType.SURVIVAL);
            var pack = new ItemStack(FrenchieRegistry.FRONT_PACK.get());
            pack.set(DataComponents.CUSTOM_NAME, Component.literal("Pip's blue carrier"));
            pack.set(DataComponents.DYED_COLOR, new DyedItemColor(0x3366AA));
            var chestplate = new ItemStack(Items.IRON_CHESTPLATE);
            chestplate.set(DataComponents.CUSTOM_NAME, Component.literal("Iron harness"));
            chestplate.setDamageValue(7);
            for (var hand : InteractionHand.values()) {
                for (boolean crouching : new boolean[]{false, true}) {
                    p.getInventory().clearContent();
                    p.setShiftKeyDown(crouching);
                    p.setItemSlot(EquipmentSlot.CHEST, chestplate.copy());
                    p.setItemInHand(hand, pack.copy());

                    var result = p.gameMode.useItem(p, p.level(), p.getItemInHand(hand), hand);
                    h.assertTrue(result.consumesAction() && ItemStack.matches(p.getItemBySlot(EquipmentSlot.CHEST), pack)
                                    && ItemStack.matches(p.getItemInHand(hand), chestplate)
                                    && p.getInventory().countItem(pack.getItem()) == 1 && p.getInventory().countItem(Items.IRON_CHESTPLATE) == 1,
                            "Right-click with " + hand + ", crouching=" + crouching
                                    + " must wear the named dyed pack and return exactly the original chestplate to the same hand");
                }
            }
            h.succeed();
        } finally {
            h.getLevel().getServer().getPlayerList().remove(p);
        }
    }
}
