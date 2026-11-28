package net.j40climb.frenchies.playtest;

import net.j40climb.frenchies.Frenchies;
import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.j40climb.frenchies.common.entity.frenchie.delivery.DeliveryScreen;
import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = Frenchies.MOD_ID, value = Dist.CLIENT)
public final class WhistleClient {
    private static final String ROLE = System.getProperty("frenchies.whistleTest.client",
            System.getProperty("frenchies.legacyTest.client"));
    private enum Step { IDLE, DISMISS, CALL_ACTIVE, CALL_COURIER, RELEASE, SWING, BLAST, PEEK, RELAXED, TUMBLE, STARTLE, EXIT }
    private static Step step = Step.IDLE;
    private static int ticks;

    @SubscribeEvent
    public static void message(ClientChatReceivedEvent.System event) {
        if (ROLE == null) return;
        Step next = switch (event.getMessage().getString()) {
            case WhistleServer.DISMISS -> Step.DISMISS;
            case WhistleServer.CALL_ACTIVE -> Step.CALL_ACTIVE;
            case WhistleServer.CALL_COURIER -> Step.CALL_COURIER;
            case WhistleServer.SWING -> Step.SWING;
            case WhistleServer.LOOK -> Step.BLAST;
            case DeliveryMultiplayerServer.DONE -> Step.EXIT;
            default -> null;
        };
        if (next == null) return;
        event.setCanceled(true);
        step = next;
        ticks = 0;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (ROLE == null) return;
        var mc = Minecraft.getInstance();
        if (step == Step.EXIT) {
            mc.stop();
            return;
        }
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (mc.screen instanceof DeathScreen) {
            mc.player.respawn();
            return;
        }
        ticks++;
        switch (step) {
            case DISMISS, CALL_ACTIVE, CALL_COURIER -> {
                if (mc.screen instanceof DeliveryScreen) mc.setScreen(null);
                if (!mc.player.getMainHandItem().is(FrenchieRegistry.FRENCHIE_WHISTLE.get())) {
                    ticks = 0;
                    return;
                }
                boolean dismiss = step == Step.DISMISS;
                if (ticks == 1 && step != Step.CALL_COURIER) {
                    mc.player.connection.sendCommand("frenchie select " + (dismiss ? WhistleServer.COURIER : WhistleServer.COMPANION));
                }
                mc.options.keyShift.setDown(dismiss);
                mc.player.setXRot(-75);
                if (ticks < 12 || mc.player.isShiftKeyDown() != dismiss || mc.screen != null) return;
                KeyMapping.click(mc.options.keyUse.getKey());
                step = Step.RELEASE;
                ticks = 0;
            }
            case RELEASE -> {
                if (ticks >= 6) {
                    mc.options.keyShift.setDown(false);
                    step = Step.IDLE;
                }
            }
            case SWING -> {
                if (!(mc.level.getEntity(WhistleServer.COMPANION) instanceof FrenchieEntity dog)) return;
                mc.player.lookAt(Anchor.EYES, dog.getBoundingBox().getCenter());
                if (ticks < 10 || !(mc.hitResult instanceof EntityHitResult hit) || hit.getEntity() != dog) return;
                KeyMapping.click(mc.options.keyAttack.getKey());
                step = Step.IDLE;
            }
            case BLAST, PEEK, RELAXED, TUMBLE, STARTLE -> {
                if (!(mc.level.getEntity(WhistleServer.COMPANION) instanceof FrenchieEntity dog)) return;
                //? if <26.3 {
                mc.options.hideGui = true;
                //?} else {
                /*if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();
                *///?}
                var aim = dog.getBoundingBox().getCenter();
                var sight = aim.subtract(mc.player.getEyePosition());
                boolean framed = sight.lengthSqr() < 7 && mc.player.getLookAngle().dot(sight.normalize()) > 0.99;
                mc.player.lookAt(Anchor.EYES, aim);
                if (!framed) return;
                if (step == Step.BLAST && dog.dangerReaction().name().equals("DUCK") && dog.dangerReactionAge(0f) >= 3) {
                    capture(mc, "visitor-blast-duck.png");
                    step = Step.PEEK;
                    ticks = 0;
                } else if (step == Step.PEEK && dog.dangerReaction().name().equals("DUCK") && dog.dangerReactionAge(0f) >= 15) {
                    capture(mc, "visitor-cautious-peek.png");
                    step = Step.RELAXED;
                    ticks = 0;
                } else if (step == Step.RELAXED && ticks >= 15 && dog.dangerReaction().name().equals("NONE")) {
                    capture(mc, "visitor-after-blast.png");
                    step = Step.TUMBLE;
                } else if (step == Step.TUMBLE && dog.dangerReaction().name().equals("TUMBLE") && dog.dangerReactionAge(0f) >= 2) {
                    capture(mc, "visitor-tumble.png");
                    step = Step.STARTLE;
                } else if (step == Step.STARTLE && dog.dangerReaction().name().equals("STARTLE") && dog.dangerReactionAge(0f) >= 2) {
                    capture(mc, "visitor-accidental-hit.png");
                    step = Step.IDLE;
                }
            }
        }
    }

    private static void capture(Minecraft mc, String filename) {
        //? if <26.3 {
        Screenshot.grab(mc.gameDirectory, filename, mc.getMainRenderTarget(), 1, ignored -> {});
        //?} else {
        /*Screenshot.grab(mc.gameDirectory, filename, mc.gameRenderer.mainRenderTarget(), 1, ignored -> {});
        *///?}
    }
}
