package net.j40climb.frenchies.test;

import net.j40climb.frenchies.Frenchies;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;

public final class FrenchiePen {
    public static final Identifier STRUCTURE = Identifier.fromNamespaceAndPath(Frenchies.MOD_ID, "frenchie_pen");

    public static void place(ServerLevel level, BlockPos origin) {
        //? if <26.3 {
        var pen = level.getStructureManager().get(STRUCTURE).orElseThrow();
        //?} else {
        /*var pen = level.getStructureTemplateManager().get(STRUCTURE).orElseThrow();
        *///?}
        var settings = new StructurePlaceSettings().setIgnoreEntities(true);
        settings.setBoundingBox(pen.getBoundingBox(settings, origin));
        if (!pen.placeInWorld(level, origin, origin, settings, level.getRandom(), Block.UPDATE_CLIENTS)) {
            throw new IllegalStateException("Could not place Frenchie pen at " + origin.toShortString());
        }
    }
}
