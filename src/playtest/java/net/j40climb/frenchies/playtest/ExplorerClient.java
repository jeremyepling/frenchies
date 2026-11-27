package net.j40climb.frenchies.playtest;

import com.mojang.logging.LogUtils;
import net.j40climb.frenchies.Frenchies;
import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.j40climb.frenchies.common.entity.frenchie.client.FrenchieRenderer;
import net.j40climb.frenchies.common.entity.frenchie.travel.ExplorerOutfit;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Drives Minecraft's use key and captures the tracked poses from another player's client. */
@EventBusSubscriber(modid = Frenchies.MOD_ID, value = Dist.CLIENT)
public final class ExplorerClient {
    private static final String ROLE = System.getProperty("frenchies.explorerTest.client");
    private static String cue;
    private static int readyTicks;

    private ExplorerClient() {}

    @SubscribeEvent
    public static void message(ClientChatReceivedEvent.System event) {
        if (ROLE == null) return;
        String text = event.getMessage().getString();
        if (!text.startsWith(ExplorerServer.PREFIX)) return;
        String next = text.substring(ExplorerServer.PREFIX.length());
        if (next.equals(cue)) return;
        cue = next;
        readyTicks = 0;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (cue == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (cue.equals("done")) {
            mc.stop();
            return;
        }
        if (mc.level == null || mc.player == null || mc.gameMode == null || mc.screen != null
                || !(mc.level.getEntity(ExplorerServer.DOG) instanceof FrenchieEntity dog)) return;
        mc.player.lookAt(Anchor.EYES, dog.getBoundingBox().getCenter());
        if ("Owner".equals(ROLE)) {
            useMap(mc, dog);
        } else if ("Visitor".equals(ROLE)) {
            capture(mc, dog);
        }
    }

    private static void useMap(Minecraft mc, FrenchieEntity dog) {
        int slot = switch (cue) {
            case "use-0" -> 0;
            case "use-1" -> 1;
            default -> -1;
        };
        if (slot < 0 || !mc.player.getInventory().getItem(slot).is(Items.FILLED_MAP)) return;
        mc.player.getInventory().setSelectedSlot(slot);
        if (++readyTicks < 5 || !(mc.hitResult instanceof EntityHitResult hit) || hit.getEntity() != dog) return;
        KeyMapping.click(mc.options.keyUse.getDefaultKey());
        LogUtils.getLogger().info("Explorer fixture used hotbar slot {} with the use key and a Frenchie hit result", slot);
        cue = null;
    }

    private static void capture(Minecraft mc, FrenchieEntity dog) {
        //? if <26.3 {
        mc.options.hideGui = true;
        //?} else {
        /*if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();
        *///?}
        if (!(mc.getEntityRenderDispatcher().getRenderer(dog) instanceof FrenchieRenderer renderer)) return;
        ExplorerOutfit outfit = dog.explorerOutfit();
        float pitch = Float.NaN;
        boolean ready = switch (cue) {
            case "explorer" -> outfit == ExplorerOutfit.EXPLORER;
            case "pirate" -> outfit == ExplorerOutfit.PIRATE;
            case "dig" -> outfit == ExplorerOutfit.PIRATE && dog.digStage() == 2 && dog.digStageAge(0) >= 3;
            case "boat" -> outfit == ExplorerOutfit.PIRATE && dog.hasFlotationVest();
            case "swim" -> outfit == ExplorerOutfit.PIRATE && dog.isUnderWater();
            case "head-before", "head-after" -> !dog.isAvoidingEnderman()
                    && dog.endermanLook(0.5F) == 0
                    && Math.abs(pitch = headPitch(renderer, dog)) <= 5;
            case "head-down" -> dog.isAvoidingEnderman()
                    && (pitch = headPitch(renderer, dog)) >= 34 && pitch <= 46;
            default -> false;
        };
        if (!ready) {
            readyTicks = 0;
            return;
        }
        if (++readyTicks < 3) return;
        //? if <26.3 {
        Screenshot.grab(mc.gameDirectory, "visitor-" + cue + ".png", mc.getMainRenderTarget(), 1, ignored -> {});
        //?} else {
        /*Screenshot.grab(mc.gameDirectory, "visitor-" + cue + ".png", mc.gameRenderer.mainRenderTarget(), 1, ignored -> {});
        *///?}
        LogUtils.getLogger().info("Explorer fixture captured {} with outfit {}, dig stage {} and head pitch {} degrees",
                cue, outfit, dog.digStage(), pitch);
        cue = null;
    }

    private static float headPitch(FrenchieRenderer renderer, FrenchieEntity dog) {
        var state = renderer.createRenderState();
        renderer.extractRenderState(dog, state, 0.5F);
        var model = renderer.getModel();
        model.setupAnim(state);
        var head = model.root().getChild("root").getChild("body").getChild("head");
        return (float) Math.toDegrees(head.xRot);
    }
}
