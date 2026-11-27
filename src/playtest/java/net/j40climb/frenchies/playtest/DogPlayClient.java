package net.j40climb.frenchies.playtest;

import net.j40climb.frenchies.Frenchies;
import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.j40climb.frenchies.common.entity.frenchie.play.PlayBubbleEntity;
import net.j40climb.frenchies.common.entity.frenchie.play.PlayPose;
import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.Locale;

@EventBusSubscriber(modid = Frenchies.MOD_ID, value = Dist.CLIENT)
public final class DogPlayClient {
    private static final String ROLE = System.getProperty("frenchies.playTest.client");
    private enum Step { WAIT, BLOW, BLOW_AGAIN, BUBBLES, NOSE, BOW, RESTORED, EXIT }
    private static Step step = Step.WAIT;
    private static int frames;

    @SubscribeEvent
    public static void message(ClientChatReceivedEvent.System event) {
        if (ROLE == null) return;
        Step next = switch (event.getMessage().getString()) {
            case DogPlayServer.BLOW -> Step.BLOW;
            case DogPlayServer.BLOW_AGAIN -> Step.BLOW_AGAIN;
            case DogPlayServer.WATCH_BUBBLES -> Step.BUBBLES;
            case DogPlayServer.WATCH_NOSE -> Step.NOSE;
            case DogPlayServer.WATCH_BOW -> Step.BOW;
            case DogPlayServer.WATCH_RELOAD -> Step.RESTORED;
            case DeliveryMultiplayerServer.DONE -> Step.EXIT;
            default -> null;
        };
        if (next == null) return;
        event.setCanceled(true);
        step = next;
        frames = 0;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (ROLE == null || step == Step.WAIT) return;
        var mc = Minecraft.getInstance();
        if (step == Step.EXIT) {
            mc.stop();
            return;
        }
        if (mc.level == null || mc.player == null || mc.screen != null || mc.gameMode == null
                || !(mc.level.getEntity(DogPlayServer.DOG) instanceof FrenchieEntity dog)) return;
        mc.getToastManager().clear();
        //? if <26.3 {
        if (ROLE.equals("Visitor")) mc.options.hideGui = true;
        //?} else {
        /*if (ROLE.equals("Visitor") && !mc.gui.hud.isHidden()) mc.gui.hud.toggle();
        *///?}
        var aim = dog.getBoundingBox().getCenter();
        mc.player.lookAt(Anchor.EYES, aim);
        switch (step) {
            case BLOW, BLOW_AGAIN -> {
                if (!mc.player.getMainHandItem().is(FrenchieRegistry.BUBBLE_TOY.get()) || ++frames < 8) return;
                mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                step = step == Step.BLOW ? Step.BUBBLES : Step.WAIT;
                frames = 0;
            }
            case BUBBLES -> {
                var visible = mc.level.getEntitiesOfClass(PlayBubbleEntity.class, dog.getBoundingBox().inflate(7));
                if (dog.playPose() == PlayPose.NONE || visible.isEmpty()) return;
                if (ROLE.equals("Visitor")) {
                    var puff = visible.getFirst().position();
                    for (int i = 1; i < visible.size(); i++) puff = puff.add(visible.get(i).position());
                    mc.player.lookAt(Anchor.EYES, aim.lerp(puff.scale(1.0 / visible.size()), 0.58));
                }
                if (++frames < 6) return;
                capture(mc, "bubbles");
            }
            case BOW -> {
                var slime = mc.level.getEntity(DogPlayServer.SLIME);
                if (slime != null && ROLE.equals("Visitor")) {
                    mc.player.lookAt(Anchor.EYES, aim.lerp(slime.getBoundingBox().getCenter(), 0.27));
                }
                if (slime == null || dog.playPose() != PlayPose.SLIME_BOW) {
                    frames = 0;
                    return;
                }
                if (++frames >= 3) capture(mc, "slime-bow");
            }
            case NOSE -> {
                var attached = mc.level.getEntitiesOfClass(PlayBubbleEntity.class, dog.getBoundingBox().inflate(1.5),
                        bubble -> bubble.noseDogId() == dog.getId());
                if (attached.size() != 1 || dog.playPose() != PlayPose.BUBBLE_NOSE || !dog.onGround()) {
                    frames = 0;
                    return;
                }
                mc.player.lookAt(Anchor.EYES, aim.lerp(attached.getFirst().position(), 0.55));
                if (++frames >= 3) capture(mc, "nose");
            }
            case RESTORED -> {
                if (dog.playPose() == PlayPose.NONE && mc.level.getEntity(DogPlayServer.SLIME) == null && ++frames >= 4) {
                    capture(mc, "restored");
                }
            }
        }
    }

    private static void capture(Minecraft mc, String stem) {
        String name = ROLE.toLowerCase(Locale.ROOT) + "-" + stem + ".png";
        //? if <26.3 {
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), 1, ignored -> {});
        //?} else {
        /*Screenshot.grab(mc.gameDirectory, name, mc.gameRenderer.mainRenderTarget(), 1, ignored -> {});
        *///?}
        step = Step.WAIT;
    }
}
