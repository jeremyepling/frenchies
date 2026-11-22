package net.j40climb.frenchies.common.entity.frenchie.frontpack;

import com.mojang.serialization.MapCodec;
import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.NormalCraftingRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class FrontpackElytraRecipe extends NormalCraftingRecipe {
    private static final FrontpackElytraRecipe INSTANCE = new FrontpackElytraRecipe();
    public static final RecipeSerializer<FrontpackElytraRecipe> SERIALIZER =
            new RecipeSerializer<>(MapCodec.unit(INSTANCE), StreamCodec.unit(INSTANCE));
    private static final Set<DataComponentType<?>> PACK_APPEARANCE =
            Set.of(DataComponents.DYED_COLOR, DataComponents.ITEM_NAME, DataComponents.ITEM_MODEL);
    private static final List<DataComponentType<?>> FLIGHT_COMPONENTS = List.of(
            DataComponents.GLIDER, DataComponents.EQUIPPABLE, DataComponents.MAX_DAMAGE, DataComponents.DAMAGE,
            DataComponents.REPAIRABLE);

    private FrontpackElytraRecipe() {
        super(new CommonInfo(true), new CraftingBookInfo(CraftingBookCategory.EQUIPMENT, ""));
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return ingredients(input) != null;
    }

    private record Ingredients(ItemStack pack, ItemStack wings, List<Component> lore) {}

    private static @Nullable Ingredients ingredients(CraftingInput input) {
        if (input.ingredientCount() != 2) return null;
        ItemStack pack = ItemStack.EMPTY;
        ItemStack wings = ItemStack.EMPTY;
        for (var stack : input.items()) {
            if (stack.is(FrenchieRegistry.FRONT_PACK.get()) && !stack.has(DataComponents.GLIDER)) pack = stack;
            if (stack.is(Items.ELYTRA) && stack.has(DataComponents.GLIDER)) wings = stack;
        }
        if (pack.isEmpty() || wings.isEmpty()) return null;
        var lines = new ArrayList<>(pack.getOrDefault(DataComponents.LORE, ItemLore.EMPTY).lines());
        for (var line : wings.getOrDefault(DataComponents.LORE, ItemLore.EMPTY).lines()) {
            if (!lines.contains(line)) lines.add(line);
        }
        return lines.size() <= ItemLore.MAX_LINES ? new Ingredients(pack, wings, List.copyOf(lines)) : null;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        var ingredients = ingredients(input);
        if (ingredients == null) return ItemStack.EMPTY;
        ItemStack pack = ingredients.pack();
        ItemStack wings = ingredients.wings();
        ItemStack result = new ItemStack(FrenchieRegistry.WINGED_FRONT_PACK.get());
        result.applyComponents(wings.getComponentsPatch().forget(PACK_APPEARANCE::contains));
        result.applyComponents(pack.getComponentsPatch());
        result.set(DataComponents.MAX_STACK_SIZE, 1);
        for (var type : FLIGHT_COMPONENTS) result.copyFrom(type, wings);
        if (!ingredients.lore().isEmpty()) result.set(DataComponents.LORE, new ItemLore(ingredients.lore()));
        mergeAttributes(result, pack, wings);

        var enchantments = new ItemEnchantments.Mutable(pack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY));
        wings.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).entrySet().forEach(entry -> enchantments.upgrade(entry.getKey(), entry.getIntValue()));
        result.set(DataComponents.ENCHANTMENTS, enchantments.toImmutable());
        result.set(DataComponents.REPAIR_COST, Math.max(pack.getOrDefault(DataComponents.REPAIR_COST, 0),
                wings.getOrDefault(DataComponents.REPAIR_COST, 0)));

        var wingData = wings.get(DataComponents.CUSTOM_DATA);
        var packData = pack.get(DataComponents.CUSTOM_DATA);
        if (wingData != null && packData != null) {
            result.set(DataComponents.CUSTOM_DATA, CustomData.of(wingData.copyTag().merge(packData.copyTag())));
        }
        return result;
    }

    private static void mergeAttributes(ItemStack result, ItemStack pack, ItemStack wings) {
        var packEntries = pack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).modifiers();
        var wingEntries = wings.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).modifiers();
        var merged = new ArrayList<>(packEntries);
        for (var wing : wingEntries) {
            if (packEntries.stream().noneMatch(entry -> entry.matches(wing.attribute(), wing.modifier().id()))) {
                merged.add(wing);
            }
        }
        if (!merged.isEmpty()) result.set(DataComponents.ATTRIBUTE_MODIFIERS, new ItemAttributeModifiers(List.copyOf(merged)));
    }

    @Override
    protected PlacementInfo createPlacementInfo() {
        return PlacementInfo.create(List.of(Ingredient.of(FrenchieRegistry.FRONT_PACK.get()), Ingredient.of(Items.ELYTRA)));
    }

    @Override
    public List<RecipeDisplay> display() {
        return List.of(new ShapelessCraftingRecipeDisplay(
                List.of(new SlotDisplay.ItemSlotDisplay(FrenchieRegistry.FRONT_PACK.get()), new SlotDisplay.ItemSlotDisplay(Items.ELYTRA)),
                new SlotDisplay.ItemSlotDisplay(FrenchieRegistry.WINGED_FRONT_PACK.get()), new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)));
    }

    @Override
    public RecipeSerializer<FrontpackElytraRecipe> getSerializer() {
        return SERIALIZER;
    }
}
