package net.j40climb.frenchies.test;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.Vec3;
import java.util.function.Predicate;

/** Count matching effects actually sent to a nearby player. */
final class GameTestEffects extends ChannelOutboundHandlerAdapter implements AutoCloseable {
    private final ChannelPipeline pipeline;
    private final Predicate<Object> matches;
    private int count;

    static GameTestEffects sounds(ServerPlayer player, SoundEvent sound, Vec3 position, double radiusSquared) {
        return observe(player, message -> message instanceof ClientboundSoundPacket packet
                && packet.getSound().value() == sound
                && position.distanceToSqr(packet.getX(), packet.getY(), packet.getZ()) < radiusSquared);
    }

    static GameTestEffects particles(ServerPlayer player, ParticleType<?> type, Vec3 position, double radiusSquared) {
        return observe(player, message -> message instanceof ClientboundLevelParticlesPacket packet
                //? if <26.3 {
                && packet.getParticle().getType() == type && packet.getCount() > 0
                && position.distanceToSqr(packet.getX(), packet.getY(), packet.getZ()) < radiusSquared);
                //?} else {
                /*&& packet.particle().getType() == type && packet.count() > 0
                && position.distanceToSqr(packet.x(), packet.y(), packet.z()) < radiusSquared);
                *///?}
    }

    private static GameTestEffects observe(ServerPlayer player, Predicate<Object> matches) {
        //? if neoforge {
        var pipeline = player.connection.getConnection().channel().pipeline();
        //?} else {
        /*var pipeline = player.connection.connection.channel.pipeline();
        *///?}
        var listener = new GameTestEffects(pipeline, matches);
        pipeline.addLast(listener);
        return listener;
    }

    private GameTestEffects(ChannelPipeline pipeline, Predicate<Object> matches) {
        this.pipeline = pipeline;
        this.matches = matches;
    }

    @Override
    public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) throws Exception {
        if (matches.test(message)) {
            count++;
        }
        super.write(context, message, promise);
    }

    int count() {
        return count;
    }

    @Override
    public void close() {
        pipeline.remove(this);
    }
}
