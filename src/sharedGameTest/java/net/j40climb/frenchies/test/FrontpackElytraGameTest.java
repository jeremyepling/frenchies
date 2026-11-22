package net.j40climb.frenchies.test;

import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;

import java.util.List;

final class FrontpackElytraGameTest {
    static void craftAndRepair(GameTestHelper h) {
        var player = GameTestPlayers.connect(h);
        try {
            player.setGameMode(GameType.SURVIVAL);
            player.giveExperienceLevels(30);
            var access = ContainerLevelAccess.create(h.getLevel(), player.blockPosition());
            var menu = new CraftingMenu(1, player.getInventory(), access);
            var grid = menu.getInputGridSlots();
            var pack = new ItemStack(FrenchieRegistry.FRONT_PACK.get());
            var name = Component.literal("Pip's flight carrier");
            var dye = new DyedItemColor(0x56A2C7);
            pack.set(DataComponents.CUSTOM_NAME, name);
            pack.set(DataComponents.DYED_COLOR, dye);
            var enchants = h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var unbreaking = enchants.getOrThrow(Enchantments.UNBREAKING);
            var mending = enchants.getOrThrow(Enchantments.MENDING);
            var vanishing = enchants.getOrThrow(Enchantments.VANISHING_CURSE);
            pack.set(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            pack.enchant(vanishing, 1);
            pack.enchant(unbreaking, 2);
            var elytra = new ItemStack(Items.ELYTRA);
            elytra.setDamageValue(150);
            elytra.enchant(mending, 1);
            elytra.enchant(unbreaking, 3);
            int durability = elytra.getMaxDamage();
            var packLore = Component.literal("For Pip");
            var elytraLore = Component.literal("End expedition");
            pack.set(DataComponents.LORE, new ItemLore(List.of(packLore)));
            elytra.set(DataComponents.LORE, new ItemLore(List.of(packLore, elytraLore)));
            var armor = new AttributeModifier(Identifier.withDefaultNamespace("test_harness"), 2, AttributeModifier.Operation.ADD_VALUE);
            var speed = new AttributeModifier(Identifier.withDefaultNamespace("test_flight"), 0.02, AttributeModifier.Operation.ADD_VALUE);
            var packAttributes = ItemAttributeModifiers.builder().add(Attributes.ARMOR, armor, EquipmentSlotGroup.CHEST).build();
            pack.set(DataComponents.ATTRIBUTE_MODIFIERS, packAttributes);
            elytra.set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.builder()
                    .add(Attributes.ARMOR, new AttributeModifier(armor.id(), 1, armor.operation()), EquipmentSlotGroup.ANY)
                    .add(Attributes.MOVEMENT_SPEED, speed, EquipmentSlotGroup.CHEST).build());
            var expectedAttributes = packAttributes.withModifierAdded(Attributes.MOVEMENT_SPEED, speed, EquipmentSlotGroup.CHEST);

            grid.get(2).set(elytra);
            grid.get(4).set(pack);
            var wings = menu.getResultSlot().safeTake(1, 1, player);
            h.assertTrue(wings.is(FrenchieRegistry.WINGED_FRONT_PACK.get()) && wings.getCount() == 1 && wings.has(DataComponents.GLIDER)
                            && grid.stream().noneMatch(slot -> slot.hasItem()) && !menu.getResultSlot().hasItem(),
                    "Taking the winged pack must consume exactly one pack and one elytra");
            var merged = wings.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            h.assertTrue(name.equals(wings.get(DataComponents.CUSTOM_NAME)) && dye.equals(wings.get(DataComponents.DYED_COLOR))
                            && wings.getDamageValue() == 150 && wings.getMaxDamage() == durability
                            && merged.size() == 3 && merged.getLevel(unbreaking) == 3 && merged.getLevel(mending) == 1 && merged.getLevel(vanishing) == 1,
                    "The winged pack must keep the name, dye, elytra wear and higher level of each shared enchantment");
            h.assertTrue(new ItemLore(List.of(packLore, elytraLore)).equals(wings.get(DataComponents.LORE))
                            && expectedAttributes.equals(wings.get(DataComponents.ATTRIBUTE_MODIFIERS)),
                    "Distinct lore and modifiers from both items must survive; a shared modifier keeps the pack's slot and value");
            h.assertTrue(mending.value().canEnchant(wings) && unbreaking.value().canEnchant(wings),
                    "The upgraded pack must accept the same Mending and Unbreaking books as an elytra");
            grid.get(0).set(wings);
            grid.get(1).set(new ItemStack(Items.ELYTRA));
            h.assertTrue(!menu.getResultSlot().hasItem() && grid.get(0).hasItem() && grid.get(1).hasItem(),
                    "A winged pack cannot consume another elytra");
            grid.get(1).set(new ItemStack(Items.RED_DYE));
            var expected = wings.copy();
            expected.set(DataComponents.DYED_COLOR, DyedItemColor.applyDyes(dye, List.of(DyeColor.RED)));
            var redyed = menu.getResultSlot().safeTake(1, 1, player);
            h.assertTrue(ItemStack.matches(expected, redyed) && grid.stream().noneMatch(slot -> slot.hasItem()),
                    "Re-dyeing must consume one dye and keep the winged carrier's other components");

            var anvil = new AnvilMenu(2, player.getInventory(), access);
            anvil.getSlot(0).set(redyed);
            // The anvil screen submits the existing display name when its input slot changes.
            anvil.setItemName(redyed.getHoverName().getString());
            anvil.getSlot(1).set(new ItemStack(Items.PHANTOM_MEMBRANE));
            var repaired = anvil.getSlot(2).safeTake(1, 1, player);
            h.assertTrue(repaired.is(FrenchieRegistry.WINGED_FRONT_PACK.get()) && repaired.getCount() == 1 && repaired.has(DataComponents.GLIDER)
                            && repaired.getDamageValue() == 150 - durability / 4 && merged.equals(repaired.get(DataComponents.ENCHANTMENTS))
                            && name.equals(repaired.get(DataComponents.CUSTOM_NAME)) && expected.get(DataComponents.DYED_COLOR).equals(repaired.get(DataComponents.DYED_COLOR))
                            && !anvil.getSlot(0).hasItem() && !anvil.getSlot(1).hasItem() && !anvil.getSlot(2).hasItem(),
                    "One phantom membrane must repair one quarter of elytra durability and preserve the dyed, enchanted carrier: "
                            + repaired + ", damage=" + repaired.getDamageValue() + ", name=" + repaired.getHoverName().getString()
                            + ", remaining material=" + anvil.getSlot(1).getItem());
            var washed = repaired.copy();
            washed.remove(DataComponents.DYED_COLOR);
            var cauldron = new BlockPos(1, 1, 1);
            h.setBlock(cauldron, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 2));
            player.setItemInHand(InteractionHand.MAIN_HAND, repaired);
            h.useBlock(cauldron, player);
            h.assertTrue(ItemStack.matches(washed, player.getMainHandItem()) && h.getBlockState(cauldron).getValue(LayeredCauldronBlock.LEVEL) == 1,
                    "Washing should remove just the dye and use one level of water");
            h.succeed();
        } finally {
            h.getLevel().getServer().getPlayerList().remove(player);
        }
    }
}
