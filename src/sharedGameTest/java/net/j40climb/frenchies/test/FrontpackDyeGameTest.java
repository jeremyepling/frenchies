package net.j40climb.frenchies.test;

import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;

import java.util.List;

final class FrontpackDyeGameTest {
    static void craftAndWash(GameTestHelper h) {
        var player = GameTestPlayers.connect(h);
        try {
            player.setGameMode(GameType.SURVIVAL);
            var pos = new BlockPos(1, 1, 1);
            var menu = new CraftingMenu(1, player.getInventory(), ContainerLevelAccess.create(h.getLevel(), h.absolutePos(pos)));
            var grid = menu.getInputGridSlots();
            grid.get(0).set(new ItemStack(Items.STRING));
            grid.get(2).set(new ItemStack(Items.STRING));
            for (int i = 3; i <= 5; i++) grid.get(i).set(new ItemStack(Items.LEATHER));
            var pack = takePack(h, menu, player);
            h.assertTrue(grid.stream().noneMatch(slot -> slot.hasItem()) && !pack.has(DataComponents.DYED_COLOR),
                    "Base recipe should consume both straps and three leather and produce an undyed pack");
            var name = Component.literal("Pip's pack");
            pack.set(DataComponents.CUSTOM_NAME, name);

            for (var color : DyeColor.values()) {
                var dye = BuiltInRegistries.ITEM.get(Identifier.withDefaultNamespace(color.getName() + "_dye")).orElseThrow();
                grid.get(0).set(pack);
                grid.get(1).set(new ItemStack(dye, 2));
                pack = takePack(h, menu, player);
                h.assertTrue(grid.get(0).getItem().isEmpty() && grid.get(1).getItem().getCount() == 1
                                && grid.get(1).getItem().is(dye) && !menu.getResultSlot().hasItem(),
                        "Dyeing with " + color + " must consume one pack and one dye exactly once");
                grid.get(1).set(ItemStack.EMPTY);
                h.assertTrue(name.equals(pack.get(DataComponents.CUSTOM_NAME))
                                && color.getTextureDiffuseColor() == DyedItemColor.getOrDefault(pack, 0),
                        "Unexpected color or name after dyeing with " + color);
                player.setItemInHand(InteractionHand.MAIN_HAND, pack);
                h.setBlock(pos, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 2));
                h.useBlock(pos, player);
                pack = player.getMainHandItem();
                h.assertTrue(pack.is(FrenchieRegistry.FRONT_PACK.get()) && pack.getCount() == 1
                                && name.equals(pack.get(DataComponents.CUSTOM_NAME)) && !pack.has(DataComponents.DYED_COLOR),
                        "Washing should return the same named pack without its dye");
                h.assertTrue(h.getBlockState(pos).getValue(LayeredCauldronBlock.LEVEL) == 1,
                        "Washing a dyed pack must spend one water level");
                h.useBlock(pos, player);
                h.assertTrue(h.getBlockState(pos).is(Blocks.WATER_CAULDRON)
                                && h.getBlockState(pos).getValue(LayeredCauldronBlock.LEVEL) == 1,
                        "Washing an undyed pack must not spend more water");
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            }

            grid.get(0).set(pack);
            grid.get(1).set(new ItemStack(Items.RED_DYE));
            grid.get(2).set(new ItemStack(Items.BLUE_DYE));
            var mixed = takePack(h, menu, player);
            var expected = DyedItemColor.applyDyes((DyedItemColor) null, List.of(DyeColor.RED, DyeColor.BLUE));
            h.assertTrue(expected.equals(mixed.get(DataComponents.DYED_COLOR)) && name.equals(mixed.get(DataComponents.CUSTOM_NAME))
                            && grid.stream().noneMatch(slot -> slot.hasItem()),
                    "Mixing two dyes should preserve the name and consume one of each ingredient");
            grid.get(0).set(mixed);
            grid.get(1).set(new ItemStack(Items.WHITE_DYE));
            var overdyed = takePack(h, menu, player);
            h.assertTrue(DyedItemColor.applyDyes(expected, List.of(DyeColor.WHITE)).equals(overdyed.get(DataComponents.DYED_COLOR))
                            && name.equals(overdyed.get(DataComponents.CUSTOM_NAME)) && grid.stream().noneMatch(slot -> slot.hasItem()),
                    "Re-dyeing should blend with the existing pack color and preserve the name");
            h.succeed();
        } finally {
            h.getLevel().getServer().getPlayerList().remove(player);
        }
    }

    private static ItemStack takePack(GameTestHelper h, CraftingMenu menu, ServerPlayer player) {
        var result = menu.getResultSlot().safeTake(1, 1, player);
        h.assertTrue(result.is(FrenchieRegistry.FRONT_PACK.get()) && result.getCount() == 1,
                "Taking the crafting result should give the player exactly one front pack");
        return result;
    }
}
