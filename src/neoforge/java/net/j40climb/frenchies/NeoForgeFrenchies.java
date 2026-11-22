package net.j40climb.frenchies;

import net.j40climb.frenchies.common.entity.frenchie.DeliveryDialog;
import net.j40climb.frenchies.common.entity.frenchie.DeliveryPopupPayload;
import net.j40climb.frenchies.common.entity.frenchie.FrenchieCommands;
import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.j40climb.frenchies.common.entity.frenchie.frontpack.networking.PutDownFrenchiePayload;
import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.j40climb.frenchies.test.CompanionTests;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.Arrays;

@Mod(Frenchies.MOD_ID)
public final class NeoForgeFrenchies {
    public NeoForgeFrenchies(IEventBus bus) {
        FrenchieRegistry.bootstrap();
        FrenchieRegistry.bind(bus);
        bus.addListener(NeoForgeFrenchies::networking);
        bus.addListener(NeoForgeFrenchies::attributes);
        bus.addListener(NeoForgeFrenchies::creativeTabs);
        NeoForge.EVENT_BUS.addListener(NeoForgeFrenchies::commands);
        if (Arrays.asList(System.getProperty("neoforge.enabledGameTestNamespaces", "").split(",")).contains(Frenchies.MOD_ID)) {
            bus.addListener((RegisterGameTestsEvent event) ->
                    new CompanionTests(event::registerEnvironment, event::registerTest).register());
        }
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(FrenchieRegistry.FRENCHIE.get(), FrenchieEntity.createAttributes().build());
    }

    private static void creativeTabs(BuildCreativeModeTabContentsEvent event) {
        FrenchieRegistry.creativeItems(event.getTabKey(), event::accept);
    }

    private static void commands(RegisterCommandsEvent event) {
        DeliveryDialog.register(event.getDispatcher());
        FrenchieCommands.register(event.getDispatcher());
    }

    private static void networking(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(PutDownFrenchiePayload.TYPE, PutDownFrenchiePayload.STREAM_CODEC,
                (payload, context) -> PutDownFrenchiePayload.handle((ServerPlayer) context.player()));
        registrar.versioned("2").playToClient(DeliveryPopupPayload.TYPE, DeliveryPopupPayload.STREAM_CODEC);
    }
}
