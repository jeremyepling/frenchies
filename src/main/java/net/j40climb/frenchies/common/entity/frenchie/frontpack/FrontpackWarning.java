package net.j40climb.frenchies.common.entity.frenchie.frontpack;

import io.netty.buffer.ByteBuf;
import net.j40climb.frenchies.common.entity.frenchie.FrenchieAwareness;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

public record FrontpackWarning(long start, float bearing) {
    public static final FrontpackWarning NONE = new FrontpackWarning(-FrenchieAwareness.COOLDOWN_TICKS, 0);
    public static final StreamCodec<ByteBuf, FrontpackWarning> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, FrontpackWarning::start, ByteBufCodecs.FLOAT, FrontpackWarning::bearing, FrontpackWarning::new);

    public boolean visible(long now) {
        return FrenchieAwareness.flashing(now - start);
    }

    public static void onTick(ServerPlayer player) {
        var pack = FrontpackAttachments.packOrNull(player);
        var previous = FrontpackAttachments.warningOrNull(player);
        if (pack == null || !pack.hasCarriedFrenchie() || player.isSleeping() || FrontpackEquipment.wornBy(player).isEmpty()) {
            if (previous != null) FrontpackAttachments.clearWarning(player);
            return;
        }
        long now = player.level().getGameTime();
        if (previous != null) {
            if (now - previous.start == FrenchieAwareness.SECOND_BARK_TICK) FrenchieAwareness.bark(player, 1.18F);
            if (now - previous.start < FrenchieAwareness.COOLDOWN_TICKS) return;
        }
        if (player.tickCount % 8 != 0) return;
        var threat = FrenchieAwareness.findCreeper(player.level(), player);
        if (threat == null) return;
        float bearing = (float)(Mth.atan2(threat.getZ() - player.getZ(), threat.getX() - player.getX()) * 180 / Math.PI - 90);
        FrontpackAttachments.setWarning(player, new FrontpackWarning(now, bearing));
        FrenchieAwareness.bark(player, 1.4F);
    }
}
