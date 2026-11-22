package net.j40climb.frenchies.setup;

import net.j40climb.frenchies.Frenchies;
import net.j40climb.frenchies.common.entity.frenchie.frontpack.FrontpackData;
import net.j40climb.frenchies.common.entity.frenchie.frontpack.FrontpackWarning;
import net.j40climb.frenchies.common.entity.frenchie.rest.WellRestedEffect;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.core.Registry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import java.util.function.Supplier;

class FrenchieRegistries {
    static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(Frenchies.LEGACY_NAMESPACE, name);
    }

    record Entries<T>(Registry<T> registry) {
        <U extends T> Supplier<U> register(String name, Supplier<U> factory) {
            U entry = Registry.register(registry, id(name), factory.get());
            return () -> entry;
        }
    }

    static final Entries<Item> ITEMS = new Entries<>(BuiltInRegistries.ITEM);
    static final Entries<EntityType<?>> ENTITY_TYPES = new Entries<>(BuiltInRegistries.ENTITY_TYPE);
    static final Entries<RecipeSerializer<?>> RECIPE_SERIALIZERS = new Entries<>(BuiltInRegistries.RECIPE_SERIALIZER);
    static final Entries<TicketType> TICKET_TYPES = new Entries<>(BuiltInRegistries.TICKET_TYPE);
    static final Entries<SoundEvent> SOUNDS = new Entries<>(BuiltInRegistries.SOUND_EVENT);

    public static final Holder<MobEffect> WELL_RESTED = Registry.registerForHolder(
            BuiltInRegistries.MOB_EFFECT, id("well_rested"), new WellRestedEffect());

    public static final Supplier<AttachmentType<FrontpackWarning>> FRENCHIE_WARNING = attachment(
            AttachmentRegistry.<FrontpackWarning>builder().initializer(() -> FrontpackWarning.NONE)
                    .syncWith(FrontpackWarning.STREAM_CODEC, AttachmentSyncPredicate.all())
                    .buildAndRegister(id("frenchie_warning")));
    public static final Supplier<AttachmentType<FrontpackData>> FRENCH_FRONTPACK_DATA = attachment(
            AttachmentRegistry.<FrontpackData>builder().initializer(() -> FrontpackData.DEFAULT)
                    .persistent(FrontpackData.CODEC).copyOnDeath()
                    .syncWith(FrontpackData.STREAM_CODEC, AttachmentSyncPredicate.all())
                    .buildAndRegister(id("french_frontpack_data")));

    private static <T> Supplier<AttachmentType<T>> attachment(AttachmentType<T> type) {
        return () -> type;
    }
}
