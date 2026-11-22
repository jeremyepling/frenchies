package net.j40climb.frenchies.setup;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.j40climb.frenchies.Frenchies;
import net.j40climb.frenchies.common.entity.frenchie.assistance.DiggingAssistantEvents;
import net.j40climb.frenchies.common.entity.frenchie.delivery.CourierTrips;
import net.j40climb.frenchies.common.entity.frenchie.frontpack.FrontpackEvents;
import net.j40climb.frenchies.common.entity.frenchie.frontpack.FrontpackWarning;
import net.j40climb.frenchies.common.entity.frenchie.play.TreeStickSearch;
import net.j40climb.frenchies.common.entity.frenchie.recall.CompanionRoster;
import net.j40climb.frenchies.common.entity.frenchie.recall.FrenchieCommands;
import net.j40climb.frenchies.common.entity.frenchie.recall.LegacyCompanionIndex;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;

public final class FabricGameplayEvents {
    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(LegacyCompanionIndex::start);
        ServerEntityEvents.ENTITY_UNLOAD.register(CompanionRoster::leave);
        // The roster must see the copied front pack before deciding which dogs to recall.
        var afterAttachments = Identifier.fromNamespaceAndPath(Frenchies.MOD_ID, "after_attachment_copy");
        ServerPlayerEvents.AFTER_RESPAWN.addPhaseOrdering(Event.DEFAULT_PHASE, afterAttachments);
        ServerPlayerEvents.AFTER_RESPAWN.register(afterAttachments, (oldPlayer, newPlayer, alive) -> {
            if (!alive) CompanionRoster.respawn(newPlayer);
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (var player : server.getPlayerList().getPlayers()) {
                FrontpackEvents.releaseWhenUnequipped(player);
                FrontpackWarning.onTick(player);
            }
            CourierTrips.tick(server);
            CompanionRoster.tick(server);
        });
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            if (level instanceof ServerLevel serverLevel) {
                DiggingAssistantEvents.ownerDigs(serverLevel, player, pos, state);
                TreeStickSearch.ownerChops(serverLevel, player, pos, state);
            }
        });
        UseBlockCallback.EVENT.register((player, level, hand, hit) ->
                !player.isSpectator() && player instanceof ServerPlayer owner
                        ? FrenchieCommands.useBed(owner, hand, hit.getBlockPos()) : InteractionResult.PASS);
        UseEntityCallback.EVENT.register((player, level, hand, target, hit) ->
                player.isSpectator() ? InteractionResult.PASS : FrontpackEvents.interact(player, target, hand));
    }
}
