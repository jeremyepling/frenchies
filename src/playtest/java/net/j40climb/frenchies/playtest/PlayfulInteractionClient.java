package net.j40climb.frenchies.playtest;

import net.j40climb.frenchies.Frenchies;
import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.j40climb.frenchies.common.entity.frenchie.client.FrenchieRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashSet;
import java.util.Set;

/** Captures actual tracked poses from a second client, while the owner drives the two click gestures. */
@EventBusSubscriber(modid = Frenchies.MOD_ID, value = Dist.CLIENT)
public final class PlayfulInteractionClient {
    private static final String ROLE = System.getProperty("florafauna_frenchies.playfulTest.client");
    private enum OwnerStep { IDLE, TRICK, BELLY, RUB, EXIT }
    private static OwnerStep ownerStep = OwnerStep.IDLE;
    private static int ownerTicks;
    private static final Set<String> captured = new HashSet<>();
    private static String current;
    private static int frames;
    private static boolean exit;
    private static boolean rejoined;
    private static boolean stickPortrait;
    private static int portraitTicks;

    private PlayfulInteractionClient() {}

    @SubscribeEvent
    public static void message(ClientChatReceivedEvent.System event) {
        if (ROLE == null) return;
        String text = event.getMessage().getString();
        if (text.equals(PlayfulInteractionServer.DONE)) {
            exit = true;
        } else if (text.equals(PlayfulInteractionServer.REJOIN)) {
            rejoined = true;
            portraitTicks = 0;
        } else if (text.equals(PlayfulInteractionServer.STICK)) {
            stickPortrait = true;
            portraitTicks = 0;
        } else if ("Owner".equals(ROLE)) {
            if (text.equals(PlayfulInteractionServer.TRICK)) ownerStep = OwnerStep.TRICK;
            else if (text.equals(PlayfulInteractionServer.BELLY)) ownerStep = OwnerStep.BELLY;
            else if (text.equals(PlayfulInteractionServer.RUB)) ownerStep = OwnerStep.RUB;
            ownerTicks = 0;
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (ROLE == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (exit) {
            mc.options.keyShift.setDown(false);
            mc.stop();
            return;
        }
        if (mc.level == null || mc.player == null || mc.gameMode == null || mc.screen != null) return;
        if (!(mc.level.getEntity(PlayfulInteractionServer.DOG) instanceof FrenchieEntity dog)) return;
        if ("Owner".equals(ROLE)) owner(mc, dog);
        else if ("Visitor".equals(ROLE)) observer(mc, dog);
    }

    private static void owner(Minecraft mc, FrenchieEntity dog) {
        switch (ownerStep) {
            case TRICK -> {
                if (!mc.player.getMainHandItem().is(Items.BONE)) return;
                mc.options.keyShift.setDown(true);
                mc.player.lookAt(Anchor.EYES, dog.getBoundingBox().getCenter());
                if (++ownerTicks < 8) return;
                mc.gameMode.interact(mc.player, dog,
                        new EntityHitResult(dog, dog.getBoundingBox().getCenter()), InteractionHand.MAIN_HAND);
                mc.options.keyShift.setDown(false);
                ownerStep = OwnerStep.IDLE;
            }
            case BELLY -> {
                if (!mc.player.getMainHandItem().isEmpty()) return;
                mc.options.keyShift.setDown(true);
                mc.player.lookAt(Anchor.EYES, dog.getBoundingBox().getCenter());
            }
            case RUB -> {
                mc.options.keyShift.setDown(false);
                if (dog.bellyStage() != 2 || ++ownerTicks < 6) return;
                mc.gameMode.interact(mc.player, dog,
                        new EntityHitResult(dog, dog.getBoundingBox().getCenter()), InteractionHand.MAIN_HAND);
                ownerStep = OwnerStep.IDLE;
            }
            case IDLE, EXIT -> mc.options.keyShift.setDown(false);
        }
    }

    private static void observer(Minecraft mc, FrenchieEntity dog) {
        //? if <26.3 {
        mc.options.hideGui = true;
        //?}
        if (!(mc.getEntityRenderDispatcher().getRenderer(dog) instanceof FrenchieRenderer)) return;
        mc.player.lookAt(Anchor.EYES, dog.getBoundingBox().getCenter());
        String stem = pose(dog);
        if (stem != null && (stem.equals("dig-stick") || stem.equals("reload-upright-stick"))
                && ++portraitTicks < 6) return;
        if (stem == null || captured.contains(stem)) {
            current = null;
            frames = 0;
            return;
        }
        if (!stem.equals(current)) {
            current = stem;
            frames = 0;
        }
        int age = switch (stem) {
            case "trick-spring", "trick-roll-up", "snow-pop" -> 4;
            case "trick-flop", "snow-dive" -> 5;
            case "dry-roll" -> 12;
            case "belly-kick-high" -> 2;
            case "belly-kick-low" -> 6;
            default -> 2;
        };
        long stageAge = stem.startsWith("trick-") ? dog.level().getGameTime() - dog.trickStageStart()
                : stem.startsWith("belly-") ? dog.level().getGameTime() - dog.bellyStageStart()
                : stem.startsWith("dry-") || stem.startsWith("snow-") ? (long) dog.weatherAge(0)
                : (long) dog.digStageAge(0);
        if (stageAge < age || ++frames < (stem.startsWith("belly-kick-") ? 1 : 2)) return;
        //? if <26.3 {
        Screenshot.grab(mc.gameDirectory, "visitor-" + stem + ".png", mc.getMainRenderTarget(), 1, ignored -> {});
        //?} else {
        /*Screenshot.grab(mc.gameDirectory, "visitor-" + stem + ".png", mc.gameRenderer.mainRenderTarget(), 1, ignored -> {});
        *///?}
        captured.add(stem);
        current = null;
        frames = 0;
    }

    private static String pose(FrenchieEntity dog) {
        return switch (dog.trickStage()) {
            case 1 -> "trick-crouch";
            case 2 -> "trick-spring";
            case 3 -> "trick-flop";
            case 4 -> "trick-wiggle";
            case 5 -> "trick-roll-up";
            case 6 -> "trick-lap";
            default -> switch (dog.bellyStage()) {
                case 2 -> "belly-up";
                case 3 -> dog.level().getGameTime() - dog.bellyStageStart() < 5
                        ? "belly-kick-high" : "belly-kick-low";
                default -> switch (dog.weatherStage()) {
                    case 1 -> "dry-shake";
                    case 3 -> "dry-shoulder";
                    case 4 -> "dry-roll";
                    case 5 -> "dry-drag";
                    case 7 -> "snow-dive";
                    case 8 -> "snow-plow";
                    case 9 -> "snow-pop";
                    default -> switch (dog.digStage()) {
                        case 2 -> "dig-paws";
                        default -> dog.getMouthItem().is(Items.STICK)
                                ? rejoined ? "reload-upright-stick" : stickPortrait ? "dig-stick" : null : null;
                    };
                };
            };
        };
    }
}
