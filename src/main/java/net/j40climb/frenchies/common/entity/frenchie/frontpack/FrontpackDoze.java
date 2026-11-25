package net.j40climb.frenchies.common.entity.frenchie.frontpack;

import net.j40climb.frenchies.common.entity.frenchie.CompanionSettings;
import net.j40climb.frenchies.common.entity.frenchie.reaction.FrenchieAwareness;
import net.j40climb.frenchies.common.entity.frenchie.rest.CompanionRest;
import net.minecraft.server.level.ServerPlayer;
import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

public final class FrontpackDoze {
    private static final int SETTLE_TICKS = 20;

    private FrontpackDoze() {}

    public static float amount(LivingEntity wearer, float partialTick) {
        var pack = FrontpackAttachments.packOrNull(wearer);
        if (pack == null || !pack.hasCarriedFrenchie() || pack.dozeAt() < 0) return 0F;
        return Math.clamp((wearer.level().getGameTime() - pack.dozeAt() + partialTick) / SETTLE_TICKS, 0F, 1F);
    }

    public static void tick(ServerPlayer player) {
        var pack = FrontpackAttachments.packOrNull(player);
        if (pack == null || !pack.hasCarriedFrenchie()) return;
        // Sample a new opportunity once per second. A dog already settling can wake on any tick.
        if (pack.dozeAt() < 0 && player.tickCount % 20 != 0) return;
        long now = player.level().getGameTime();
        var warning = FrontpackAttachments.warningOrNull(player);
        boolean recentlyWarned = warning != null && now - warning.start() < FrenchieAwareness.COOLDOWN_TICKS;
        if (recentlyWarned || !canRest(player) || FrontpackEquipment.wornBy(player).isEmpty()) {
            if (pack.dozeAt() >= 0) FrontpackAttachments.setPack(player, pack.withDozeAt(-1L));
            return;
        }
        if (pack.dozeAt() < 0) {
            FrontpackAttachments.setPack(player, pack.withDozeAt(now + 100 + player.getRandom().nextInt(61)));
            return;
        }
        // A quiet opportunity per wearer, independent of how often the dog wakes and resettles.
        int seed = player.getUUID().hashCode();
        int snoreInterval = 600 + Math.floorMod(seed, 301);
        if (now - pack.dozeAt() >= SETTLE_TICKS && Math.floorMod(now + (long) seed, snoreInterval) == 0) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    FrenchieRegistry.FRENCHIE_SNORE.get(), SoundSource.NEUTRAL, 0.18F, 0.78F + player.getRandom().nextFloat() * 0.12F);
        }
    }

    private static boolean canRest(ServerPlayer player) {
        var level = player.level();
        if (CompanionRest.ownerIsBusy(player) || player.isFallFlying() || player.getAbilities().flying
                || player.isOnFire() || player.hurtTime > 0) return false;
        if (level.dimensionType().hasFixedTime() || !level.dimensionType().hasSkyLight() || level.isThundering()) return false;
        long timeOfDay = Math.floorMod(level.getDefaultClockTime(), 24000L);
        return timeOfDay >= 13000 && timeOfDay < 23000 && CompanionSettings.ambientCompanionship();
    }
}
