package net.j40climb.frenchies.test;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
//? if neoforge {
import net.neoforged.neoforge.network.registration.NetworkRegistry;
//?} else {
/*import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.world.phys.EntityHitResult;
*///?}

public final class GameTestPlayers {
    public static ServerPlayer connect(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        configure(player);
        return player;
    }

    public static void configure(ServerPlayer player) {
        //? if neoforge {
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        //?} else {
        /*player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        *///?}
    }

    static void interact(ServerPlayer player, Entity entity, InteractionHand hand) {
        // Fabric invokes this event in the server packet listener, before Player.interactOn.
        //? if fabric {
        /*var result = UseEntityCallback.EVENT.invoker().interact(player, player.level(), hand, entity, new EntityHitResult(entity));
        if (result != InteractionResult.PASS) return;
        *///?}
        player.interactOn(entity, hand, Vec3.ZERO);
    }
}
