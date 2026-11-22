package net.j40climb.frenchies.test;

import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
//? if <26.3 {
import net.minecraft.world.entity.EntityType;
//?} else {
/*import net.minecraft.world.entity.EntityTypes;
*///?}
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import static net.j40climb.frenchies.common.entity.frenchie.play.PlayPose.*;

final class SlimePlayTests {
    private static Slime stationarySlime(GameTestHelper h, int size) {
        var slime = h.spawn(EntityType.SLIME, 5, 1, 5);
        slime.setSize(size, true);
        slime.setNoAi(true);
        return slime;
    }

    static void sequence(GameTestHelper h) {
        var dog = h.spawn(FrenchieRegistry.FRENCHIE.get(), 2, 1, 5);
        var slime = stationarySlime(h, 1);
        Vec3[] firstSide = new Vec3[1];
        Vec3[] hopOrigin = new Vec3[1];
        float health = slime.getHealth();
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(dog.playPose() == SLIME_NOTICE, "Frenchie never noticed the tiny slime"))
                .thenWaitUntil(() -> h.assertTrue(dog.playPose() == SLIME_APPROACH, "Notice never became an approach"))
                .thenWaitUntil(() -> h.assertTrue(dog.playPose() == SLIME_BOW, "Frenchie never bowed to the slime"))
                .thenExecute(() -> firstSide[0] = bowDirection(h, dog, slime))
                .thenWaitUntil(() -> h.assertTrue(dog.playPose() == SLIME_HOP, "Bow never became a tease"))
                .thenExecute(() -> hopOrigin[0] = dog.position())
                .thenWaitUntil(() -> h.assertTrue(!dog.onGround() && dog.playPose() == SLIME_HOP
                                && dog.position().subtract(hopOrigin[0]).horizontalDistance() > 0.09,
                        "Tease must physically hop sideways"))
                .thenExecute(() -> slime.setPos(slime.position().add(0, 0, 0.6)))
                .thenWaitUntil(() -> h.assertTrue(dog.playPose() == SLIME_CIRCLE, "No circle; current pose "
                        + dog.playPose() + ", dog " + h.relativePos(dog.blockPosition())
                        + ", slime " + h.relativePos(slime.blockPosition())))
                .thenWaitUntil(() -> h.assertTrue(dog.playPose() == SLIME_BOW, "No bow from the second side"))
                .thenExecute(() -> h.assertTrue(firstSide[0].dot(bowDirection(h, dog, slime)) < 0.77,
                        "Both invitations came from the same side"))
                .thenWaitUntil(() -> h.assertTrue(dog.playPose() == SLIME_HOP, "Second invitation lacked its tease hops"))
                .thenWaitUntil(() -> h.assertTrue(dog.playPose() == NONE, "Frenchie never lost interest"))
                .thenExecuteFor(75, () -> h.assertTrue(dog.playPose() == NONE && dog.getTarget() == null
                                && slime.isAlive() && slime.getHealth() == health,
                        "Frenchie restarted during cooldown, attacked, or damaged the slime"))
                .thenSucceed();
    }

    private static Vec3 bowDirection(GameTestHelper h, FrenchieEntity dog, Slime slime) {
        var direction = dog.position().subtract(slime.position()).multiply(1, 0, 1);
        h.assertTrue(direction.length() >= 1.1 && direction.length() <= 3.2,
                "Dog bowed " + direction.length() + " blocks from the slime");
        return direction.normalize();
    }

    static void damage(GameTestHelper h) {
        var dog = h.spawn(FrenchieRegistry.FRENCHIE.get(), 2, 1, 5);
        var slime = stationarySlime(h, 1);
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(dog.playPose() == SLIME_BOW, "Frenchie did not reach its first bow"))
                .thenExecute(() -> h.assertTrue(dog.hurtServer(h.getLevel(), dog.damageSources().generic(), 1),
                        "Fixture failed to damage the Frenchie"))
                .thenExecuteAfter(4, () -> h.assertTrue(dog.playPose() == NONE, "Taking damage failed to interrupt play"))
                .thenExecuteFor(45, () -> h.assertTrue(dog.playPose() == NONE && slime.isAlive(),
                        "Injured Frenchie resumed teasing"))
                .thenSucceed();
    }

    static void blocked(GameTestHelper h) {
        var slime = stationarySlime(h, 2);
        var dog = h.spawn(FrenchieRegistry.FRENCHIE.get(), 2, 1, 5);
        h.startSequence().thenExecuteFor(55, () -> h.assertTrue(dog.playPose() == NONE, "A large slime invited play"))
                .thenExecute(() -> {
                    for (var pos : BlockPos.betweenClosed(4, 1, 4, 6, 3, 6)) {
                        if (pos.getX() != 5 || pos.getZ() != 5) h.setBlock(pos, Blocks.GLASS);
                    }
                    dog.snapTo(h.absolutePos(new BlockPos(2, 1, 5)), 0, 0);
                    slime.setSize(1, true);
                })
                .thenExecuteFor(65, () -> h.assertTrue(dog.playPose() != SLIME_BOW && dog.playPose() != SLIME_HOP,
                        "Frenchie played through the wall"))
                .thenExecute(() -> h.assertTrue(dog.playPose() == NONE, "Inaccessible slime left the dog in pursuit"))
                .thenSucceed();
    }

    static void warning(GameTestHelper h) {
        var owner = CompanionTravelTests.player(h, 9, 9);
        var dog = h.spawn(FrenchieRegistry.FRENCHIE.get(), 2, 1, 5);
        dog.tame(owner);
        dog.standUp();
        stationarySlime(h, 1);
        h.startSequence().thenWaitUntil(() -> h.assertTrue(dog.playPose() == SLIME_BOW, "No bow before the warning; " + CompanionPlayTests.playState(h, dog)))
                .thenExecute(() -> {
                    var threat = h.spawn(EntityType.CREEPER, 3, 1, 2);
                    threat.setNoAi(true);
                }).thenWaitUntil(() -> h.assertTrue(dog.isWarningFlash() && dog.playPose() == NONE,
                        "Visible creeper failed to take priority over the bow"))
                .thenWaitUntil(() -> h.assertTrue(!dog.isWarningFlash(), "Missing gap between warning barks"))
                .thenWaitUntil(() -> h.assertTrue(dog.isWarningFlash() && dog.playPose() == NONE, "Missing second warning"))
                .thenExecute(() -> CompanionTravelTests.disconnect(owner)).thenSucceed();
    }

    static void unsafeFooting(GameTestHelper h) {
        var dog = h.spawn(FrenchieRegistry.FRENCHIE.get(), 1, 1, 5);
        var slime = stationarySlime(h, 1);
        var center = h.absolutePos(new BlockPos(5, 1, 5));
        int[] direction = new int[1];
        int[] hot = new int[1];
        Vec3[] start = new Vec3[1];
        boolean[] crossed = {false};
        h.startSequence().thenWaitUntil(() -> h.assertTrue(dog.playPose() == SLIME_NOTICE, "Dog did not notice the slime"))
                .thenExecute(() -> {
                    // The dog may wander before noticing. Keep its entire current footprint
                    // and at least one spare column of stone, then cut its route to the slime.
                    start[0] = dog.position();
                    direction[0] = dog.getX() < center.getX() + 0.5 ? 1 : -1;
                    var box = dog.getBoundingBox();
                    double front = direction[0] == 1 ? box.maxX : box.minX;
                    hot[0] = (int) Math.floor(front) + 2 * direction[0];
                    slime.snapTo(new BlockPos(hot[0] + 2 * direction[0], center.getY(), dog.getBlockZ()), 0, 0);
                    for (int z = center.getZ() - 5; z <= center.getZ() + 5; z++) {
                        var magma = new BlockPos(hot[0], center.getY() - 1, z);
                        h.getLevel().setBlockAndUpdate(magma, Blocks.MAGMA_BLOCK.defaultBlockState());
                        h.getLevel().setBlockAndUpdate(magma.offset(direction[0], 0, 0), Blocks.AIR.defaultBlockState());
                    }
                })
                .thenWaitUntil(() -> {
                    crossed[0] |= direction[0] == 1 ? dog.getBoundingBox().maxX > hot[0] : dog.getBoundingBox().minX < hot[0] + 1;
                    h.assertTrue(dog.playPose() == NONE, "Frenchie kept pursuing across unsafe footing");
                })
                .thenExecute(() -> h.assertTrue(!crossed[0] && dog.getHealth() == dog.getMaxHealth(),
                        "Frenchie crossed unsafe footing: started " + start[0] + ", hot column " + hot[0]
                                + ", ended " + dog.position() + ", health " + dog.getHealth()))
                .thenSucceed();
    }

    static void vanilla(GameTestHelper h) {
        // The GameTest mock server player always reports Creative. A passive fixture
        // golem exercises the other vanilla slime target without killing the tiny slime.
        var golem = h.spawn(EntityType.IRON_GOLEM, 8, 1, 5);
        golem.setNoAi(true);
        var dog = h.spawn(FrenchieRegistry.FRENCHIE.get(), 2, 1, 5);
        var slime = h.spawn(EntityType.SLIME, 5, 1, 5);
        slime.setSize(1, true);
        var start = slime.position();
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(slime.getTarget() == golem && slime.position().distanceToSqr(start) > 0.1,
                        "Vanilla slime must still acquire and hop toward an iron golem beside the Frenchie"))
                .thenExecute(() -> {
                    h.assertTrue(dog.getTarget() == null && slime.isAlive() && slime.getHealth() == slime.getMaxHealth(),
                            "Frenchie attacked or harmed the tiny slime");
                }).thenSucceed();
    }
}
