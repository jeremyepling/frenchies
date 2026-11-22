package net.j40climb.frenchies.setup;

import net.j40climb.frenchies.Frenchies;
import net.j40climb.frenchies.common.entity.frenchie.CompanionRoster;
import net.j40climb.frenchies.common.entity.frenchie.CourierTrips;
import net.j40climb.frenchies.common.entity.frenchie.DiggingAssistantEvents;
import net.j40climb.frenchies.common.entity.frenchie.FrenchieAwareness;
import net.j40climb.frenchies.common.entity.frenchie.FrenchieCommands;
import net.j40climb.frenchies.common.entity.frenchie.LegacyCompanionIndex;
import net.j40climb.frenchies.common.entity.frenchie.SnowballPlayEvents;
import net.j40climb.frenchies.common.entity.frenchie.frontpack.FrontpackEvents;
import net.j40climb.frenchies.common.entity.frenchie.frontpack.FrontpackWarning;
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
//? if <26.3 {
import net.neoforged.neoforge.event.level.BlockEvent.BreakEvent;
//?} else {
/*import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
*///?}
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
        FrenchieAwareness.onWake(event.getEntity(), event.wakeImmediately(), event.updateLevel());
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
    //? if <26.3 {
    public static void blockBreak(BreakEvent event) {
    //?} else {
    /*public static void blockBreak(BreakBlockEvent event) {
    *///?}
        if (!event.isCanceled() && event.getLevel() instanceof ServerLevel level) {
            DiggingAssistantEvents.ownerDigs(level, event.getPlayer(), event.getPos(), event.getState());
        }
    }

    @SubscribeEvent
    public static void impact(ProjectileImpactEvent event) {
        SnowballPlayEvents.onImpact(event.getProjectile(), event.getRayTraceResult());
    }
}
