package net.j40climb.frenchies.common.entity.frenchie.frontpack;

import net.j40climb.frenchies.Frenchies;
import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public final class FrontpackClient {
    static final Identifier WARNING_HUD = Identifier.fromNamespaceAndPath(Frenchies.LEGACY_NAMESPACE, "frontpack_warning");
    private static final Identifier CREEPER_SPRITE = Identifier.fromNamespaceAndPath(Frenchies.LEGACY_NAMESPACE, "frenchie/creeper");

    static void drawWarning(GuiGraphicsExtractor gui, DeltaTracker delta) {
        var mc = Minecraft.getInstance();
        //? if <26.3 {
        boolean hideGui = mc.options.hideGui;
        //?} else {
        /*boolean hideGui = mc.gui.hud.isHidden();
        *///?}
        if (mc.player == null || hideGui || !mc.options.getCameraType().isFirstPerson()) return;
        var warning = FrontpackAttachments.warningOrNull(mc.player);
        long now = mc.player.level().getGameTime();
        if (warning == null || !warning.visible(now)) return;
        var pack = FrontpackAttachments.packOrNull(mc.player);
        if (pack == null || !pack.hasCarriedFrenchie()) return;
        gui.blitSprite(RenderPipelines.GUI_TEXTURED, CREEPER_SPRITE, gui.guiWidth() / 2 - 8, gui.guiHeight() - 51, 16, 16);
    }

    public static boolean shouldPutDown(Player player) {
        if (!player.isShiftKeyDown() || player.getOffhandItem().is(FrontpackEquipment.PACKS)
                || player.getOffhandItem().is(FrenchieRegistry.FRENCHIE_WHISTLE.get())) return false;
        var pack = FrontpackAttachments.packOrNull(player);
        return pack != null && pack.hasCarriedFrenchie();
    }
}
