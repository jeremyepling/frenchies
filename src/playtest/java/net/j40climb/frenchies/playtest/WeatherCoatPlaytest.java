package net.j40climb.frenchies.playtest;

import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.j40climb.frenchies.common.entity.frenchie.PlayStages;
import net.j40climb.frenchies.common.entity.frenchie.client.FrenchieRenderer;
import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.commands.FillBiomeCommand;
import net.minecraft.world.clock.ClockTimeMarkers;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;

import java.util.Set;

final class WeatherCoatPlaytest {
    private static final BlockPos CENTER = new BlockPos(8, 300, 8);

    static void steps(Playtest t) {
        int[] id = {0};
        t.server("Prepare a fenced snowy-plains platform in the disposable Overworld", player -> {
            var level = player.level().getServer().overworld();
            level.getChunk(0, 0);
            var biome = level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.SNOWY_PLAINS);
            FillBiomeCommand.fill(level, new BlockPos(0, 288, 0), new BlockPos(15, 319, 15), biome);
            Playtest.require(level.getBiome(CENTER).is(Biomes.SNOWY_PLAINS), "Platform biome is not snowy plains");
            for (var pos : BlockPos.betweenClosed(CENTER.offset(-4, -1, -4), CENTER.offset(4, 10, 4))) {
                boolean fence = pos.getY() == CENTER.getY() && (Math.abs(pos.getX() - CENTER.getX()) == 4
                        || Math.abs(pos.getZ() - CENTER.getZ()) == 4);
                level.setBlockAndUpdate(pos, pos.getY() < CENTER.getY() ? Blocks.SMOOTH_STONE.defaultBlockState()
                        : fence ? Blocks.OAK_FENCE.defaultBlockState() : Blocks.AIR.defaultBlockState());
            }
            level.getServer().clockManager().moveToTimeMarker(level.dimensionType().defaultClock().orElseThrow(),
                    ClockTimeMarkers.NOON);
            level.getServer().setWeatherParameters(0, 6000, true, false);
            // A floating spectator can see the head and back without the Frenchie turning to face the camera.
            player.setGameMode(GameType.SPECTATOR);
            player.teleportTo(level, 7.2, 300.35, 7.4, Set.of(), -50, 42, true);
            player.getInventory().clearContent();
        });
        t.awaitServer("Wait for active snowy weather before comparing the coat", player -> player.level().isRaining());
        t.server("Seat a newly spawned dry-coated Frenchie in the snowfall", player -> {
            var dog = FrenchieRegistry.FRENCHIE.get().spawn(player.level(), CENTER, EntitySpawnReason.COMMAND);
            Playtest.require(dog != null && dog.snowLayer() == 0, "Fresh Frenchie did not have a bare coat");
            dog.tame(player);
            dog.sitDown();
            dog.setYRot(0);
            dog.yBodyRot = 0;
            dog.setYHeadRot(0);
            id[0] = dog.getId();
        });
        t.awaitClient("Frame the bare seated dog during snowfall", mc -> {
            var dog = frame(mc, id[0]);
            return dog != null && dog.snowLayer() == 0 && dog.isSitting();
        });
        t.screenshot("owner-three-quarter-coat-bare");
        t.awaitClient("Track a partial coat from the same camera", mc -> {
            var dog = frame(mc, id[0]);
            return dog != null && dog.snowLayer() == 2;
        });
        t.screenshot("owner-three-quarter-coat-partial");
        t.awaitClient("Track full coverage on the seated dog", mc -> {
            var dog = frame(mc, id[0]);
            return dog != null && dog.snowLayer() == 5;
        });
        t.screenshot("owner-three-quarter-coat-full");
        t.server("Release the sit order and let the dog shake naturally", player ->
                ((FrenchieEntity) player.level().getEntity(id[0])).standUp());
        t.awaitClient("Frame the full-body shake while it is moving", mc -> {
            var dog = frame(mc, id[0]);
            return dog != null && dog.weatherStage() == PlayStages.Weather.SHAKE && dog.weatherAge(0) >= 2;
        });
        t.screenshot("owner-three-quarter-coat-shaking");
        t.awaitClient("Observe the cleared coat during the shake", mc -> {
            var dog = frame(mc, id[0]);
            return dog != null && dog.weatherStage() == PlayStages.Weather.SHAKE && dog.snowLayer() == 0;
        });
        t.screenshot("owner-three-quarter-coat-after-burst");
        t.awaitClient("See fresh snow accumulate after the dog finishes shaking", mc -> {
            var dog = frame(mc, id[0]);
            return dog != null && dog.weatherStage() == 0 && dog.snowLayer() > 0;
        });
    }

    private static FrenchieEntity frame(Minecraft mc, int id) {
        if (!(mc.level.getEntity(id) instanceof FrenchieEntity dog)
                || !(mc.getEntityRenderDispatcher().getRenderer(dog) instanceof FrenchieRenderer)) return null;
        //? if <26.3 {
        mc.options.hideGui = true;
        //?} else {
        /*if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();
        *///?}
        mc.options.fov().set(36);
        // Maintain the same angle and projected scale throughout the shake, even if the dog wandered first.
        mc.player.setPos(dog.getX() - 1.3, dog.getY() + 0.35, dog.getZ() - 1.1);
        mc.player.lookAt(Anchor.EYES, dog.getBoundingBox().getCenter());
        return dog;
    }
}
