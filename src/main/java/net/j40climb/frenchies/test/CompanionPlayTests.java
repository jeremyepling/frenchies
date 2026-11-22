package net.j40climb.frenchies.test;

import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.j40climb.frenchies.common.item.SqueakyToyItem;
import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;

import static net.j40climb.frenchies.test.CompanionTravelTests.disconnect;
import static net.j40climb.frenchies.test.CompanionTravelTests.dog;
import static net.j40climb.frenchies.test.CompanionTravelTests.player;
import static net.j40climb.frenchies.test.CompanionTravelTests.reload;

final class CompanionPlayTests {
    private static final InteractionHand HAND = InteractionHand.MAIN_HAND;
    private static final int BOW = 1, RUN = 2, HOP = 3, SPLOOT = 4;

    static String playState(GameTestHelper h, FrenchieEntity dog) {
        var owner = dog.getOwner();
        var goals = dog.goalSelector.getAvailableGoals().stream().filter(goal -> goal.isRunning())
                .map(goal -> goal.getGoal().getClass().getSimpleName()).toList();
        var neighbors = dog.level().getEntities(dog, dog.getBoundingBox().inflate(8)).stream()
                .map(entity -> entity.getType().toShortString() + "@" + h.relativePos(entity.blockPosition())).toList();
        return "pose=" + dog.playPose() + ", dog=" + h.relativeVec(dog.position()) + ", ticks=" + dog.tickCount
                + ", owner=" + (owner == null ? "-" : h.relativeVec(owner.position()))
                + ", ground=" + dog.onGround() + ", asleep=" + dog.isFrenchieSleeping()
                + ", sit=" + dog.isOrderedToSit() + ", warned=" + dog.isWarningFlash()
                + ", goals=" + goals + ", nearby=" + neighbors;
    }

    static void interruptCelebration(GameTestHelper h) {
        var owner = player(h, 5.5, 4);
        var dog = dog(h, owner);
        h.startSequence().thenWaitUntil(() -> h.assertTrue(dog.onGround(), "Dog has not landed"))
                .thenExecute(dog::tryCelebrate)
                .thenWaitUntil(() -> h.assertTrue(dog.celebrationStage() != 0, "Celebration never began"))
                .thenExecute(() -> owner.interactOn(dog, HAND, Vec3.ZERO))
                .thenWaitUntil(() -> h.assertTrue(dog.isOrderedToSit() && dog.celebrationStage() == 0,
                        "Sit order failed to interrupt celebration"))
                .thenExecute(() -> {
                    var restored = reload(h, dog);
                    h.assertTrue(restored.isOrderedToSit() && restored.celebrationStage() == 0,
                            "A reload discarded the sit order or resumed an interrupted celebration");
                    disconnect(owner);
                }).thenSucceed();
    }

    static void audience(GameTestHelper h) {
        var owner = player(h, 5.5, 5.5);
        var listeners = new ArrayList<FrenchieEntity>();
        for (int x = 3; x <= 7; x++) {
            var listener = h.spawn(FrenchieRegistry.FRENCHIE.get(), x, 1, 6);
            listener.tame(owner);
            listener.standUp();
            listeners.add(listener);
        }
        var puppy = listeners.get(2);
        puppy.setAge(-24000);
        var seated = listeners.getLast();
        seated.sitDown();
        owner.setItemInHand(HAND, new ItemStack(FrenchieRegistry.SQUEAKY_TOY.get()));
        owner.getMainHandItem().use(owner.level(), owner, HAND);
        h.assertTrue(puppy.toyEarLift(0) > 0 && seated.toyEarLift(0) == 0
                        && listeners.stream().noneMatch(dog -> dog.toyPlay().watching(owner)),
                "First squeak should perk the puppy's ears and respect the sitting order");
        // GameTest's EmbeddedChannel is outside the server network tick loop. In a real
        // connection ServerGamePacketListenerImpl calls player.doTick(), advancing item cooldowns.
        h.startSequence().thenExecuteFor(20, owner.getCooldowns()::tick).thenExecute(() -> {
            h.assertTrue(!owner.getCooldowns().isOnCooldown(owner.getMainHandItem()), "Second squeak is still on cooldown");
            owner.getMainHandItem().use(owner.level(), owner, HAND);
            long count = listeners.stream().filter(dog -> dog.toyPlay().watching(owner)).count();
            h.assertTrue(count == 3 && !seated.toyPlay().watching(owner),
                    "Expected three available audience members, found " + count);
        }).thenExecuteAfter(80, () -> {
            SqueakyToyItem.squeak(h.getLevel(), puppy);
        }).thenExecuteAfter(18, () -> {
            SqueakyToyItem.squeak(h.getLevel(), puppy);
            h.assertTrue(listeners.stream().noneMatch(dog -> dog.toyPlay().watching(puppy)),
                    "A dog's own squeaks recruited another audience");
            disconnect(owner);
        }).thenSucceed();
    }

    static void zoomies(GameTestHelper h, boolean open) {
        var owner = player(h, 9.5, 9.5);
        var dog = dog(h, owner);
        var sequence = h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(dog.onGround(), "Frenchie has not landed"))
                .thenExecute(dog::tryCelebrate)
                .thenWaitUntil(() -> h.assertTrue(dog.celebrationStage() == BOW, "No opening play bow"));
        if (open) {
            sequence.thenWaitUntil(() -> h.assertTrue(dog.celebrationStage() == RUN, "No run on the clear stone yard"));
            double centerZ = dog.getZ();
            for (int lap = 0; lap < 2; lap++) {
                sequence.thenWaitUntil(() -> h.assertTrue(dog.celebrationStage() == RUN && dog.getZ() > centerZ + 1,
                                "Dog did not reach the south side of lap"))
                        .thenWaitUntil(() -> h.assertTrue(dog.celebrationStage() == RUN && dog.getZ() < centerZ - 1,
                                "Dog did not reach the north side of lap"));
            }
        } else {
            sequence.thenExecute(() -> {
                var crop = dog.blockPosition().west(2).south(2);
                h.getLevel().setBlockAndUpdate(crop.below(), Blocks.FARMLAND.defaultBlockState());
                h.getLevel().setBlockAndUpdate(crop, Blocks.WHEAT.defaultBlockState());
            });
        }
        boolean[] ran = {false};
        sequence.thenWaitUntil(() -> {
            ran[0] |= dog.celebrationStage() == RUN;
            h.assertTrue(dog.celebrationStage() == HOP, "No finish hop");
        }).thenWaitUntil(() -> h.assertTrue(dog.celebrationStage() == SPLOOT, "No belly-down landing"))
                .thenWaitUntil(() -> h.assertTrue(dog.celebrationStage() == 0, "Sploot became permanent"))
                .thenExecute(() -> {
                    h.assertTrue(open || !ran[0], "Dog ran a lap through crops");
                    h.assertTrue(!dog.isOrderedToSit() && !dog.isFrenchieSleeping(), "Celebration imposed a rest order");
                    dog.tryCelebrate();
                }).thenExecuteAfter(8, () -> {
                    h.assertTrue(dog.celebrationStage() == 0, "Celebration cooldown allowed an immediate restart");
                    disconnect(owner);
                }).thenSucceed();
    }
}
