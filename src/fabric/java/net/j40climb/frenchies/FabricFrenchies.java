package net.j40climb.frenchies;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.j40climb.frenchies.common.entity.frenchie.CompanionSettings;
import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.j40climb.frenchies.common.entity.frenchie.delivery.DeliveryDialog;
import net.j40climb.frenchies.common.entity.frenchie.delivery.DeliveryPopupPayload;
import net.j40climb.frenchies.common.entity.frenchie.frontpack.networking.PutDownFrenchiePayload;
import net.j40climb.frenchies.common.entity.frenchie.recall.FrenchieCommands;
import net.j40climb.frenchies.setup.FabricGameplayEvents;
import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.minecraft.world.item.CreativeModeTabs;

public final class FabricFrenchies implements ModInitializer {
    @Override
    public void onInitialize() {
        CompanionSettings.load(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir());
        FrenchieRegistry.bootstrap();
        FabricDefaultAttributeRegistry.register(FrenchieRegistry.FRENCHIE.get(), FrenchieEntity.createAttributes());
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(
                output -> FrenchieRegistry.creativeItems(CreativeModeTabs.SPAWN_EGGS, output::accept));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(
                output -> FrenchieRegistry.creativeItems(CreativeModeTabs.TOOLS_AND_UTILITIES, output::accept));
        CommandRegistrationCallback.EVENT.register((dispatcher, registries, selection) -> {
            DeliveryDialog.register(dispatcher);
            FrenchieCommands.register(dispatcher);
        });
        PayloadTypeRegistry.serverboundPlay().register(PutDownFrenchiePayload.TYPE, PutDownFrenchiePayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(DeliveryPopupPayload.TYPE, DeliveryPopupPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(PutDownFrenchiePayload.TYPE,
                (payload, context) -> PutDownFrenchiePayload.handle(context.player()));
        FabricGameplayEvents.register();
    }
}
