package net.j40climb.frenchies.setup;

import net.j40climb.frenchies.Frenchies;
import net.j40climb.frenchies.common.entity.frenchie.delivery.CourierTrips;
import net.j40climb.frenchies.common.entity.frenchie.frontpack.FrontpackEvents;
import net.j40climb.frenchies.common.entity.frenchie.frontpack.FrontpackDoze;
import net.j40climb.frenchies.common.entity.frenchie.frontpack.FrontpackWarning;
import net.j40climb.frenchies.common.entity.frenchie.recall.CompanionRoster;
import net.j40climb.frenchies.common.entity.frenchie.recall.FrenchieCommands;
import net.j40climb.frenchies.common.entity.frenchie.recall.LegacyCompanionIndex;
import net.j40climb.frenchies.common.entity.frenchie.rest.BedtimeBuddyGoal;
import net.j40climb.frenchies.common.entity.frenchie.weather.SnowballPlayEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = Frenchies.MOD_ID)
public final class NeoForgeGameplayEvents {
    @SubscribeEvent
    public static void start(ServerStartedEvent event) {
        LegacyCompanionIndex.start(event.getServer());
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        CourierTrips.tick(event.getServer());
        CompanionRoster.tick(event.getServer());
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            FrontpackEvents.releaseWhenUnequipped(player);
            FrontpackWarning.onTick(player);
            FrontpackDoze.tick(player);
        }
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level) CompanionRoster.leave(event.getEntity(), level);
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!event.isEndConquered() && event.getEntity() instanceof ServerPlayer player) CompanionRoster.respawn(player);
    }

    @SubscribeEvent
    public static void onWake(PlayerWakeUpEvent event) {
        BedtimeBuddyGoal.onWake(event.getEntity(), event.wakeImmediately(), event.updateLevel());
    }

    @SubscribeEvent
    public static void interact(PlayerInteractEvent.EntityInteract event) {
        var result = FrontpackEvents.interact(event.getEntity(), event.getTarget(), event.getHand());
        if (result != InteractionResult.PASS) {
            event.setCancellationResult(result);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void useBed(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var result = FrenchieCommands.useBed(player, event.getHand(), event.getPos());
        if (result != InteractionResult.PASS) {
            event.setCancellationResult(result);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void impact(ProjectileImpactEvent event) {
        SnowballPlayEvents.onImpact(event.getProjectile(), event.getRayTraceResult());
    }
}
