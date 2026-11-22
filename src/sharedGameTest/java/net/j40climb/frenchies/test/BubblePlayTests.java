package net.j40climb.frenchies.test;

import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.j40climb.frenchies.common.entity.frenchie.PlayBubbleEntity;
import net.j40climb.frenchies.common.entity.frenchie.PlayPose;
import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.j40climb.frenchies.common.entity.frenchie.PlayPose.*;

final class BubblePlayTests {
    private static final InteractionHand HAND = InteractionHand.MAIN_HAND;

    private static ServerPlayer blower(GameTestHelper h, double x, double z) {
        var player = CompanionTravelTests.player(h, x, z);
        player.setItemInHand(HAND, new ItemStack(FrenchieRegistry.BUBBLE_TOY.get()));
        player.setXRot(16);
        return player;
    }

    private static void blow(ServerPlayer player) {
        player.getMainHandItem().use(player.level(), player, HAND);
    }

    private static List<PlayBubbleEntity> bubbles(ServerPlayer player) {
        return player.level().getEntitiesOfClass(PlayBubbleEntity.class, player.getBoundingBox().inflate(9));
    }

    static void toy(GameTestHelper h) {
        var player = blower(h, 5.5, 3.5);
        var toy = player.getMainHandItem().copy();
        List<PlayBubbleEntity> first;
        try (var sounds = GameTestEffects.sounds(player, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, player.position(), 4)) {
            blow(player);
            first = bubbles(player);
            h.assertTrue(first.size() == 3, "A first use created " + first.size() + " bubbles");
            for (int i = 0; i < 10; i++) blow(player);
            h.assertTrue(sounds.count() == 1, "One puff and ten cooldown clicks produced " + sounds.count() + " sounds");
        }
        h.assertTrue(bubbles(player).size() == first.size(), "Repeated use bypassed the toy cooldown");
        var start = first.getFirst().position();
        h.startSequence().thenExecuteFor(45, player.getCooldowns()::tick)
                .thenExecute(() -> {
                    h.assertTrue(first.getFirst().position().distanceToSqr(start) > 0.02, "Bubble never drifted");
                    blow(player);
                    var second = bubbles(player).stream().filter(bubble -> !first.contains(bubble)).toList();
                    h.assertTrue(!second.isEmpty() && bubbles(player).size() <= 6,
                            "Second use failed or exceeded the local cap");
                    h.assertTrue(ItemStack.matches(toy, player.getMainHandItem()), "Use consumed or damaged the toy");
                }).thenExecuteAfter(125, () -> {
                    h.assertTrue(first.stream().allMatch(PlayBubbleEntity::isRemoved) && bubbles(player).isEmpty(),
                            "Unattended bubbles failed to expire");
                    CompanionTravelTests.disconnect(player);
                }).thenSucceed();
    }

    static void audience(GameTestHelper h) {
        var owner = blower(h, 5.5, 2.5);
        var second = blower(h, 4.5, 2.5);
        var third = blower(h, 6.5, 2.5);
        var dogs = new ArrayList<FrenchieEntity>();
        for (int x = 2; x <= 7; x++) {
            var dog = h.spawn(FrenchieRegistry.FRENCHIE.get(), x, 1, 6);
            dog.tame(owner);
            dog.standUp();
            dogs.add(dog);
        }
        var sitter = dogs.getLast();
        sitter.snapTo(h.absoluteVec(new Vec3(8.5, 1, 8.5)), 0, 0);
        sitter.sitDown();
        h.startSequence().thenExecuteAfter(4, () -> {
            blow(owner);
            blow(second);
            blow(third);
            h.assertTrue(bubbles(owner).size() <= 6, "Overlapping players exceeded the local production cap");
            long count = dogs.stream().filter(dog -> dog.bubblePlay().engaged()).count();
            h.assertTrue(count >= 2 && count <= 3, "Expected a small shared audience, got " + count);
        }).thenExecuteFor(115, () -> {
            h.assertTrue(dogs.stream().filter(dog -> dog.bubblePlay().engaged()).count() <= 3,
                    "Repeated blowing exceeded the audience cap");
            h.assertTrue(sitter.isOrderedToSit() && sitter.isSitting(),
                    "Bubble play made an ordered sitter stand up");
        }).thenExecuteAfter(10, () -> {
            h.assertTrue(dogs.stream().noneMatch(d -> d.bubblePlay().engaged()), "Bubble audience got stuck");
            CompanionTravelTests.disconnect(owner, second, third);
        }).thenSucceed();
    }

    static void pops(GameTestHelper h) {
        var owner = blower(h, 5.5, 2.5);
        var sitter = h.spawn(FrenchieRegistry.FRENCHIE.get(), 5, 1, 4);
        sitter.tame(owner);
        sitter.sitDown();
        var seat = sitter.position();
        var left = h.spawn(FrenchieRegistry.FRENCHIE.get(), 2, 1, 5);
        var right = h.spawn(FrenchieRegistry.FRENCHIE.get(), 8, 1, 5);
        var puff = new ArrayList<PlayBubbleEntity>();
        Set<FrenchieEntity> successful = new HashSet<>();
        Set<PlayBubbleEntity> popped = new HashSet<>();
        Map<Integer, Set<PlayPose>> observed = Map.of(left.getId(), EnumSet.noneOf(PlayPose.class),
                right.getId(), EnumSet.noneOf(PlayPose.class));
        Map<PlayBubbleEntity, Integer> attached = new HashMap<>();
        h.startSequence().thenWaitUntil(() -> h.assertTrue(left.onGround() && right.onGround(), "Dogs have not landed"))
                .thenExecute(() -> {
                    blow(owner);
                    puff.addAll(bubbles(owner));
                    h.assertTrue(puff.size() == 3 && sitter.bubblePlay().engaged()
                            && left.bubblePlay().engaged() && right.bubblePlay().engaged(), "All three dogs should get an invitation");
                })
                .thenWaitUntil(() -> h.assertTrue(sitter.playPose() == BUBBLE_WATCH && sitter.isOrderedToSit(),
                        "Ordered sitter failed to watch its bubble"))
                .thenExecuteFor(110, () -> {
                    for (var dog : List.of(left, right)) observed.get(dog.getId()).add(dog.playPose());
                    h.assertTrue(sitter.isOrderedToSit() && sitter.isSitting()
                                    && (sitter.playPose() != BUBBLE_WATCH || sitter.position().distanceToSqr(seat) < 0.5),
                            "A seated watcher chased a bubble");
                    // A bubble disappearing above clear floor next to a pouncing dog
                    // before its natural lifetime is a nose pop. Count each dog and bubble once.
                    for (var bubble : puff) {
                        if (bubble.noseDogId() != -1) attached.put(bubble, bubble.noseDogId());
                        if (!bubble.isRemoved() || popped.contains(bubble)) continue;
                        for (var dog : List.of(left, right)) {
                            boolean nose = attached.getOrDefault(bubble, -1) == dog.getId();
                            boolean earlyPop = bubble.tickCount < 78 && bubble.getY() > seat.y + 0.15;
                            if ((nose || earlyPop) && dog.playPose() == BUBBLE_POUNCE
                                    && dog.distanceToSqr(bubble) < 1.5 && !successful.contains(dog)) {
                                successful.add(dog);
                                popped.add(bubble);
                                break;
                            }
                        }
                    }
                }).thenExecute(() -> {
                    h.assertTrue(successful.size() == 2 && popped.size() == 2,
                            "Two hunters must pop two distinct reachable bubbles, got " + successful.size()
                                    + "; dog poses " + observed + "; bubble ages " + puff.stream().map(b -> b.tickCount).toList());
                    CompanionTravelTests.disconnect(owner);
                }).thenSucceed();
    }

    static void delivery(GameTestHelper h) {
        var owner = blower(h, 5.5, 2.5);
        var recipient = CompanionTravelTests.player(h, 9.5, 8.5);
        var dog = h.spawn(FrenchieRegistry.FRENCHIE.get(), 3, 1, 5);
        dog.tame(owner);
        dog.standUp();
        h.startSequence().thenWaitUntil(() -> h.assertTrue(dog.onGround(), "Dog has not landed"))
                .thenExecute(() -> {
                    blow(owner);
                    h.assertTrue(dog.bubblePlay().engaged(), "Toy did not recruit courier candidate; puff="
                            + bubbles(owner).size() + ", sleeping=" + dog.isFrenchieSleeping()
                            + ", sitting=" + dog.isSitting() + ", warned=" + dog.isWarningFlash());
                })
                .thenWaitUntil(() -> h.assertTrue(dog.playPose() == BUBBLE_APPROACH, "Dog never pursued its bubble; " + CompanionPlayTests.playState(h, dog)))
                .thenExecute(() -> {
                    owner.snapTo(dog.position().add(0, 0, -1), 0, 0);
                    owner.setItemInHand(HAND, new ItemStack(Items.BUNDLE));
                    FrenchieDeliveryGameTests.confirm(owner, recipient, dog);
                }).thenExecuteAfter(5, () -> {
                    h.assertTrue(dog.isDelivering() && dog.playPose() == NONE && !dog.bubblePlay().engaged(),
                            "Confirmed delivery did not immediately preempt bubble pursuit");
                    CompanionTravelTests.disconnect(owner, recipient);
                }).thenSucceed();
    }

    private static PlayBubbleEntity bubble(GameTestHelper h, FrenchieEntity dog) {
        var bubble = new PlayBubbleEntity(FrenchieRegistry.PLAY_BUBBLE.get(), h.getLevel());
        bubble.setPos(dog.position().add(2, 0.84, 0));
        h.assertTrue(h.getLevel().addFreshEntity(bubble), "Could not place a reachable bubble");
        dog.bubblePlay().offer(bubble);
        h.assertTrue(dog.bubblePlay().engaged(), "Dog failed to accept a reachable bubble");
        return bubble;
    }

    static void nose(GameTestHelper h) {
        var dog = h.spawn(FrenchieRegistry.FRENCHIE.get(), 3, 1, 5);
        PlayBubbleEntity[] target = new PlayBubbleEntity[1];
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(dog.onGround(), "Frenchie has not landed"))
                .thenExecute(() -> {
                    target[0] = bubble(h, dog);
                    long seed = 0;
                    while (RandomSource.create(seed).nextInt(4) != 0) seed++;
                    target[0].getRandom().setSeed(seed);
                })
                .thenWaitUntil(() -> h.assertTrue(dog.playPose() == BUBBLE_BOW, "No bow before the pounce; " + CompanionPlayTests.playState(h, dog)))
                .thenWaitUntil(() -> h.assertTrue(dog.playPose() == BUBBLE_POUNCE, "Dog never pounced"))
                .thenWaitUntil(() -> h.assertTrue(dog.playPose() == BUBBLE_NOSE && target[0].noseDogId() == dog.getId(),
                        "Bubble never settled on the Frenchie's nose"))
                .thenWaitUntil(() -> h.assertTrue(target[0].isRemoved() && (!dog.onGround() || dog.getDeltaMovement().y > 0),
                        "The nose bubble failed to pop with a surprised hop"))
                .thenWaitUntil(() -> h.assertTrue(dog.onGround() && !dog.bubblePlay().engaged() && dog.playPose() == NONE,
                        "Frenchie never resumed normal activity after landing"))
                .thenSucceed();
    }

    static void unreachable(GameTestHelper h) {
        var dog = h.spawn(FrenchieRegistry.FRENCHIE.get(), 3, 1, 5);
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(dog.onGround(), "Frenchie has not landed"))
                .thenExecute(() -> {
                    var target = bubble(h, dog);
                    target.setPos(target.position().add(0, 4, 0));
                    target.setDeltaMovement(Vec3.ZERO);
                }).thenExecuteAfter(65, () -> h.assertTrue(!dog.bubblePlay().engaged() && dog.playPose() == NONE,
                        "Frenchie kept chasing a bubble too high to reach"))
                .thenSucceed();
    }
}
