package net.j40climb.frenchies.test;

import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.j40climb.frenchies.common.item.SqueakyToyItem;
import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;

import static net.j40climb.frenchies.test.CompanionTravelTests.disconnect;
import static net.j40climb.frenchies.test.CompanionTravelTests.player;

final class CompanionPlayTests {
    private static final InteractionHand HAND = InteractionHand.MAIN_HAND;

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
}
