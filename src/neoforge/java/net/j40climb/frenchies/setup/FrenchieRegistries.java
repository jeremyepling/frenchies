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
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

class FrenchieRegistries {
    static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(Frenchies.LEGACY_NAMESPACE, name);
    }
    static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, Frenchies.LEGACY_NAMESPACE);
    static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Frenchies.LEGACY_NAMESPACE);

    static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, Frenchies.LEGACY_NAMESPACE);

    static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Frenchies.LEGACY_NAMESPACE);

    static final DeferredRegister<TicketType> TICKET_TYPES =
            DeferredRegister.create(Registries.TICKET_TYPE, Frenchies.LEGACY_NAMESPACE);

    static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Frenchies.LEGACY_NAMESPACE);

    static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Frenchies.LEGACY_NAMESPACE);
    public static final Holder<MobEffect> WELL_RESTED = MOB_EFFECTS.register("well_rested", WellRestedEffect::new);

    public static final Supplier<AttachmentType<FrontpackWarning>> FRENCHIE_WARNING =
            ATTACHMENT_TYPES.register("frenchie_warning", () -> AttachmentType.builder(() -> FrontpackWarning.NONE)
                    .sync(FrontpackWarning.STREAM_CODEC).build());

    public static final Supplier<AttachmentType<FrontpackData>> FRENCH_FRONTPACK_DATA =
            ATTACHMENT_TYPES.register("french_frontpack_data", () ->
                    AttachmentType.builder(() -> FrontpackData.DEFAULT)
                            .serialize(FrontpackData.CODEC.fieldOf("french_frontpack_data"))
                            .sync(FrontpackData.STREAM_CODEC)
                            .copyOnDeath()
                            .build());

    public static void bind(IEventBus bus) {
        ITEMS.register(bus);
        SOUNDS.register(bus);
        RECIPE_SERIALIZERS.register(bus);
        ENTITY_TYPES.register(bus);
        MOB_EFFECTS.register(bus);
        TICKET_TYPES.register(bus);
        ATTACHMENT_TYPES.register(bus);
    }
}
