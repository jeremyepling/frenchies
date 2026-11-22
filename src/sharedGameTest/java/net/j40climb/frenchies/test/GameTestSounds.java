package net.j40climb.frenchies.test;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.Vec3;

/** Count matching sound packets sent to this GameTest player during a specific action. */
final class GameTestSounds extends ChannelOutboundHandlerAdapter implements AutoCloseable {
    private final ChannelPipeline pipeline;
    private final SoundEvent sound;
    private final Vec3 position;
    private final double radiusSquared;
    private int count;

    static GameTestSounds observe(ServerPlayer player, SoundEvent sound, Vec3 position, double radiusSquared) {
        //? if neoforge {
        var pipeline = player.connection.getConnection().channel().pipeline();
        //?} else {
        /*var pipeline = player.connection.connection.channel.pipeline();
        *///?}
        var listener = new GameTestSounds(pipeline, sound, position, radiusSquared);
        pipeline.addLast(listener);
        return listener;
    }

    private GameTestSounds(ChannelPipeline pipeline, SoundEvent sound, Vec3 position, double radiusSquared) {
        this.pipeline = pipeline;
        this.sound = sound;
        this.position = position;
        this.radiusSquared = radiusSquared;
    }

    @Override
    public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) throws Exception {
        if (message instanceof ClientboundSoundPacket packet && packet.getSound().value() == sound
                && position.distanceToSqr(packet.getX(), packet.getY(), packet.getZ()) < radiusSquared) {
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
