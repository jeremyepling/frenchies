package net.j40climb.frenchies.playtest;

import net.j40climb.frenchies.Frenchies;
import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Items;
import net.minecraft.client.Screenshot;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.entity.player.Input;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.nio.file.Files;

@EventBusSubscriber(modid = Frenchies.MOD_ID, value = Dist.CLIENT)
public final class CompanionClient {
    private enum Step { WAIT, TIP, THROW, FETCH, TAKE }
    private static Step step = Step.WAIT;
    private static int frames;

    @SubscribeEvent
    public static void message(ClientChatReceivedEvent.System event) {
        switch (event.getMessage().getString()) {
            case "FF fixture tip" -> step = Step.TIP;
            case "FF fixture throw" -> step = Step.THROW;
            case "FF fixture fetch" -> step = Step.FETCH;
            default -> { return; }
        }
        frames = 0;
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (step == Step.WAIT || mc.level == null || mc.screen != null || mc.player == null || mc.gameMode == null) return;
        if (step == Step.THROW) {
            if (!mc.player.getMainHandItem().is(FrenchieRegistry.SQUEAKY_TOY.get())) return;
            mc.player.setYRot(100);
            mc.player.setXRot(35);
            if (++frames < 6) return;
            // Send the same crouch, item use and release packets as a player, in order,
            // even while the other client's window has focus.
            mc.player.connection.send(new ServerboundPlayerInputPacket(new Input(false, false, false, false, false, true, false)));
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            mc.player.connection.send(new ServerboundPlayerInputPacket(Input.EMPTY));
            com.mojang.logging.LogUtils.getLogger().info("Companion client threw squeaky toy");
            step = Step.WAIT;
            return;
        }
        var id = switch (step) {
            case FETCH, TAKE -> CompanionServer.RETRIEVER;
            default -> DeliveryMultiplayerServer.DOG;
        };
        var target = mc.level.getEntity(id);
        if (!(target instanceof FrenchieEntity dog)) return;
        mc.player.lookAt(Anchor.EYES, dog.getEyePosition());
        if (++frames < 7) return;
        switch (step) {
            case TIP -> {
                if (!mc.player.getOffhandItem().is(Items.GLOW_BERRIES) || !(mc.hitResult instanceof EntityHitResult hit) || hit.getEntity() != dog) return;
                KeyMapping.click(mc.options.keyUse.getKey());
                com.mojang.logging.LogUtils.getLogger().info("Recipient offered off-hand berries after signing");
            }
            case FETCH -> {
                if (!dog.getOffhandItem().is(FrenchieRegistry.SQUEAKY_TOY.get()) || mc.player.distanceToSqr(dog) > 8) return;
                //? if <26.3 {
                Screenshot.grab(mc.gameDirectory, "toy.png", mc.getMainRenderTarget(), 1, ignored -> {});
                //?} else {
                /*Screenshot.grab(mc.gameDirectory, "toy.png", mc.gameRenderer.mainRenderTarget(), 1, ignored -> {});
                *///?}
                step = Step.TAKE;
                return;
            }
            case TAKE -> {
                if (!Files.isRegularFile(mc.gameDirectory.toPath().resolve("screenshots/toy.png"))) return;
                mc.gameMode.interact(mc.player, dog, new EntityHitResult(dog, dog.getBoundingBox().getCenter()), InteractionHand.MAIN_HAND);
            }
        }
        step = Step.WAIT;
    }
}
