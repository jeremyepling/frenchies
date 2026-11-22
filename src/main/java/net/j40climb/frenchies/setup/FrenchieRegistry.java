package net.j40climb.frenchies.setup;

import net.j40climb.frenchies.Frenchies;
import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.j40climb.frenchies.common.entity.frenchie.PlayBubbleEntity;
import net.j40climb.frenchies.common.entity.frenchie.frontpack.FrontpackElytraRecipe;
import net.j40climb.frenchies.common.item.FrenchieWhistleItem;
import net.j40climb.frenchies.common.item.SqueakyToyItem;
import net.j40climb.frenchies.common.item.BubbleToyItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class FrenchieRegistry extends FrenchieRegistries {
    public static final ResourceKey<EntityType<?>> FRENCHIE_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Frenchies.LEGACY_NAMESPACE, "frenchie"));

    public static final Supplier<EntityType<FrenchieEntity>> FRENCHIE = ENTITY_TYPES.register("frenchie",
            () -> EntityType.Builder.of(FrenchieEntity::new, MobCategory.CREATURE)
                    .sized(0.9f, 0.9f).build(FRENCHIE_KEY));

    public static final Supplier<Item> FRENCHIE_SPAWN_EGG = item("frenchie_spawn_egg", properties ->
            new SpawnEggItem(properties.spawnEgg(FRENCHIE.get())));

    public static final Supplier<FrenchieWhistleItem> FRENCHIE_WHISTLE =
            item("frenchie_whistle",
                    properties -> new FrenchieWhistleItem(properties.stacksTo(1).component(DataComponents.LORE, new ItemLore(List.of(
                            Component.translatable("item.florafauna.frenchie_whistle.use"),
                            Component.translatable("item.florafauna.frenchie_whistle.select"))))));
    public static final Supplier<SqueakyToyItem> SQUEAKY_TOY = item("squeaky_toy", SqueakyToyItem::new);
    public static final Supplier<BubbleToyItem> BUBBLE_TOY = item("bubble_toy", BubbleToyItem::new);

    public static final Supplier<EntityType<PlayBubbleEntity>> PLAY_BUBBLE = ENTITY_TYPES.register("play_bubble",
            () -> EntityType.Builder.of(PlayBubbleEntity::new, MobCategory.MISC)
                    .sized(0.20F, 0.20F).noSave().noSummon().noLootTable().clientTrackingRange(4).updateInterval(2)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Frenchies.LEGACY_NAMESPACE, "play_bubble"))));

    private static final ItemLore FRONT_PACK_LORE = new ItemLore(List.of(
            Component.translatable("item.florafauna.front_pack.equip"),
            Component.translatable("item.florafauna.front_pack.dye")));

    public static final Supplier<Item> FRONT_PACK = item("front_pack",
            properties -> new Item(properties.stacksTo(1).equippable(EquipmentSlot.CHEST).component(DataComponents.LORE, FRONT_PACK_LORE)));

    public static final Supplier<Item> WINGED_FRONT_PACK = item("winged_front_pack",
            properties -> new Item(properties.durability(432).rarity(Rarity.EPIC).repairable(Items.PHANTOM_MEMBRANE)
                    .component(DataComponents.GLIDER, Unit.INSTANCE)
                    .component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.CHEST)
                            .setEquipSound(SoundEvents.ARMOR_EQUIP_ELYTRA).setAsset(EquipmentAssets.ELYTRA)
                            .setDamageOnHurt(false).build())
                    .component(DataComponents.LORE, FRONT_PACK_LORE)));

    public static final Supplier<RecipeSerializer<FrontpackElytraRecipe>> FRONT_PACK_ELYTRA_RECIPE =
            RECIPE_SERIALIZERS.register("front_pack_elytra", () -> FrontpackElytraRecipe.SERIALIZER);

    public static final Supplier<TicketType> COURIER_STOP = TICKET_TYPES.register("courier_stop",
            () -> new TicketType(120, TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION | TicketType.FLAG_PERSIST));

    public static final Supplier<SoundEvent> TOY_SQUEAK = SOUNDS.register("toy_squeak",
            () -> SoundEvent.createVariableRangeEvent(id("toy_squeak")));

    private static <T extends Item> Supplier<T> item(String name, Function<Item.Properties, T> factory) {
        var key = ResourceKey.create(Registries.ITEM, id(name));
        return ITEMS.register(name, () -> factory.apply(new Item.Properties().setId(key)));
    }

    public static void creativeItems(ResourceKey<CreativeModeTab> tab, Consumer<Item> add) {
        if (tab == CreativeModeTabs.SPAWN_EGGS) add.accept(FRENCHIE_SPAWN_EGG.get());
        if (tab == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            add.accept(FRENCHIE_WHISTLE.get());
            add.accept(SQUEAKY_TOY.get());
            add.accept(BUBBLE_TOY.get());
            add.accept(FRONT_PACK.get());
            add.accept(WINGED_FRONT_PACK.get());
        }
    }

    public static void bootstrap() {
        // Initialize all content before the loader binds its registries.
    }
}
