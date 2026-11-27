package net.j40climb.frenchies.test;

import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.j40climb.frenchies.common.entity.frenchie.play.PlayPose;
import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.world.InteractionHand;
//? if <26.3 {
import net.minecraft.world.entity.EntityType;
//?} else {
/*import net.minecraft.world.entity.EntityTypes;
*///?}
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.animal.wolf.WolfSoundVariants;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.phys.Vec3;

import static net.j40climb.frenchies.test.CompanionTravelTests.disconnect;
import static net.j40climb.frenchies.test.CompanionTravelTests.player;

final class CompanionReactionTests {
    private CompanionReactionTests() {}

    private static boolean running(FrenchieEntity dog, String goalName) {
        return dog.goalSelector.getAvailableGoals().stream()
                .anyMatch(entry -> entry.isRunning() && entry.getGoal().getClass().getSimpleName().equals(goalName));
    }

    static void thunder(GameTestHelper h) {
        var owner = player(h, 8.5, 8.5);
        var dog = CompanionWhistleTests.dog(h, owner, 2, 7);
        var stay = CompanionWhistleTests.dog(h, owner, 2, 9);
        var parked = stay.position();
        var whine = dog.registryAccess().lookupOrThrow(Registries.WOLF_SOUND_VARIANT)
                .getOrThrow(WolfSoundVariants.PUGLIN).value().adultSounds().whineSound().value();
        var sounds = GameTestEffects.sounds(owner, whine, owner.position(), 150);
        dog.standUp();
        h.startSequence().thenWaitUntil(() -> h.assertTrue(dog.onGround(), "Dog is airborne"))
                .thenExecute(() -> h.spawn(EntityType.LIGHTNING_BOLT, 7, 1, 1))
                .thenWaitUntil(() -> h.assertTrue(running(dog, "OwnerConcernGoal")
                                && dog.distanceToSqr(owner) <= 6.25,
                        "Nearby thunder did not bring the dog beside its owner"))
                .thenExecuteFor(60, () -> h.assertTrue(dog.distanceToSqr(owner) <= 9 && stay.isOrderedToSit()
                                && stay.position().distanceToSqr(parked) < 1,
                        "Frightened companion wandered away or pulled a seated dog off station"))
                .thenWaitUntil(() -> h.assertTrue(!running(dog, "OwnerConcernGoal"), "Thunder fear did not expire"))
                .thenExecute(() -> {
                    h.assertTrue(sounds.count() >= 1 && sounds.count() <= 5, "Missing or excessive thunder whimpers");
                    sounds.close();
                    disconnect(owner);
                }).thenSucceed();
    }

    static void fall(GameTestHelper h) {
        var owner = player(h, 8.5, 7.5);
        owner.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        var dog = CompanionWhistleTests.dog(h, owner, 2, 5);
        dog.standUp();
        var level = h.getLevel();
        h.startSequence().thenWaitUntil(() -> h.assertTrue(dog.onGround(), "Dog is airborne"))
                .thenExecute(() -> h.assertTrue(owner.hurtServer(level, owner.damageSources().fall(), 1),
                        "Small fall did not deal damage in the fixture"))
                .thenExecuteFor(25, () -> h.assertTrue(!running(dog, "OwnerConcernGoal"),
                        "One point of fall damage started a rescue"))
                .thenExecute(() -> h.assertTrue(owner.hurtServer(level, owner.damageSources().fall(), 6),
                        "Large fall did not deal damage in the fixture"))
                .thenWaitUntil(() -> h.assertTrue(running(dog, "OwnerConcernGoal") && dog.distanceToSqr(owner) <= 9,
                        "Dog did not reach and check on the injured owner"))
                .thenWaitUntil(() -> h.assertTrue(!running(dog, "OwnerConcernGoal"), "Fall reaction never finished"))
                .thenExecute(() -> disconnect(owner)).thenSucceed();
    }

    static void ravine(GameTestHelper h) {
        var owner = player(h, 8.5, 5.5);
        owner.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        for (var pos : BlockPos.betweenClosed(new BlockPos(2, 1, 3), new BlockPos(5, 4, 7))) {
            h.setBlock(pos, Blocks.STONE);
        }
        var dog = h.spawn(FrenchieRegistry.FRENCHIE.get(), 4, 5, 5);
        dog.tame(owner);
        dog.standUp();
        int ledgeY = h.absolutePos(new BlockPos(4, 5, 5)).getY();
        h.startSequence().thenWaitUntil(() -> h.assertTrue(dog.onGround(), "Dog did not settle on the ledge"))
                .thenExecute(() -> h.assertTrue(owner.hurtServer(h.getLevel(), owner.damageSources().fall(), 6),
                        "The owner below the cliff did not take fall damage"))
                .thenWaitUntil(() -> h.assertTrue(running(dog, "OwnerConcernGoal"), "Dog did not notice the ravine fall"))
                .thenExecuteFor(60, () -> h.assertTrue(dog.getY() >= ledgeY - 0.25,
                        "Concerned Frenchie jumped from an unreachable four-block ledge"))
                .thenWaitUntil(() -> h.assertTrue(!running(dog, "OwnerConcernGoal"),
                        "Dog remained stuck trying to reach the ravine floor"))
                .thenExecute(() -> disconnect(owner)).thenSucceed();
    }

    static void lava(GameTestHelper h) {
        var owner = player(h, 5.5, 8.5);
        var dog = CompanionWhistleTests.dog(h, owner, 5, 5);
        dog.standUp();
        var edge = new BlockPos(6, 0, 5);
        var center = Vec3.atBottomCenterOf(h.absolutePos(edge.above()));
        h.startSequence().thenWaitUntil(() -> h.assertTrue(dog.onGround(), "Dog is airborne"))
                .thenExecute(() -> h.setBlock(edge, Blocks.LAVA))
                .thenWaitUntil(() -> h.assertTrue(running(dog, "LavaEdgeGoal"),
                        "A lava source one block from the dog did not trigger avoidance"))
                .thenWaitUntil(() -> h.assertTrue(dog.position().distanceToSqr(center) >= 6.25,
                        "Dog stopped at the lava lip instead of retreating"))
                .thenExecuteFor(30, () -> h.assertTrue(!dog.isInLava() && dog.blockPosition().getY() >= center.y - 1,
                        "Retreat stepped into the lava"))
                .thenExecute(() -> disconnect(owner)).thenSucceed();
    }

    static void quiet(GameTestHelper h) {
        var owner = player(h, 6.5, 6.5);
        var dog = CompanionWhistleTests.dog(h, owner, 3, 5);
        var sheltered = new BlockPos(4, 1, 6);
        var open = new BlockPos(6, 1, 4);
        var campfire = new BlockPos(8, 1, 5);
        h.setBlock(sheltered.above(3), Blocks.STONE);
        // The old preference ranked the open spot near the lit campfire ahead of shelter.
        h.setBlock(campfire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        var shelteredSpot = Vec3.atBottomCenterOf(h.absolutePos(sheltered));
        dog.standUp();
        h.startSequence().thenWaitUntil(() -> h.assertTrue(dog.playPose() == PlayPose.AMBIENT_SIT
                                && dog.distanceToSqr(owner) <= 12 && !dog.isOrderedToSit(),
                        "Idle dog never sat beside its owner voluntarily"))
                .thenExecute(() -> {
                    h.assertTrue(!h.getLevel().canSeeSky(h.absolutePos(sheltered))
                                    && h.getLevel().canSeeSky(h.absolutePos(open)),
                            "Fixture needs a sheltered spot and an open spot beside the owner");
                    h.assertTrue(dog.distanceToSqr(shelteredSpot) < 0.8,
                            "Lit campfire drew the Frenchie away from the sheltered resting spot");
                })
                .thenWaitUntil(() -> h.assertTrue(dog.playPose() == PlayPose.AMBIENT_REST && !dog.isOrderedToSit(),
                        "Longer idle period did not lead to a resting pose"))
                .thenExecute(() -> owner.snapTo(h.absoluteVec(new Vec3(2.5, 1, 2.5)), 90, 0))
                .thenWaitUntil(() -> h.assertTrue(dog.playPose() == PlayPose.NONE && !dog.isOrderedToSit()
                                && !running(dog, "QuietCompanyGoal"),
                        "Moving away stranded the dog in voluntary rest"))
                .thenExecute(() -> disconnect(owner)).thenSucceed();
    }

    static void herd(GameTestHelper h, boolean sheep) {
        var owner = player(h, 7.5, 6.5);
        var dog = CompanionWhistleTests.dog(h, owner, 2, 3);
        dog.standUp();
        var animal = sheep ? h.spawn(EntityType.SHEEP, 3, 1, 6) : h.spawn(EntityType.COW, 3, 1, 6);
        var health = animal.getHealth();
        var start = dog.position();
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT));
        int[] steps = {0};
        boolean[] worked = {false};
        // Give the owner a long flat lane. A side ditch separates the destination from unsafe ground.
        for (int x = 10; x <= 16; x++) for (int z = 2; z <= 8; z++)
            h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        h.setBlock(new BlockPos(9, 0, 8), Blocks.LAVA);
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(animal.onGround() && !animal.getNavigation().isDone(),
                        "Livestock never started its vanilla food-follow path"))
                .thenExecuteFor(50, () -> {
                    owner.snapTo(h.absoluteVec(new Vec3(7.5 + ++steps[0] * 0.14, 1, 6.5)), 90, 0);
                    if (running(dog, "HerdLivestockGoal") && dog.position().distanceToSqr(start) > 0.1)
                        worked[0] = true;
                    h.assertTrue(animal.getHealth() == health && !animal.isInLava() && !dog.isInLava(),
                            "Herding injured the livestock or crossed the lava edge");
                })
                .thenExecute(() -> {
                    h.assertTrue(worked[0], "Dog never moved into position behind the food-led animal");
                    owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                })
                .thenExecuteAfter(8, () -> {
                    h.assertTrue(!running(dog, "HerdLivestockGoal") && animal.getHealth() == health,
                            "Herding persisted after the owner put away the wheat");
                    disconnect(owner);
                }).thenSucceed();
    }

    static void stick(GameTestHelper h) {
        var owner = player(h, 6.5, 6.5);
        var dog = CompanionWhistleTests.dog(h, owner, 4, 5);
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; ++slot) {
            owner.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
        }
        dog.standUp();
        var log = new BlockPos(5, 1, 5);
        h.setBlock(log, Blocks.OAK_LOG);
        ItemStack three = CompanionWhistleTests.named(new ItemStack(Items.STICK, 3), "Windfall");
        ItemStack one = three.copyWithCount(1);
        var feet = h.absoluteVec(new Vec3(4.5, 1, 5.5));
        var drop = new ItemEntity(h.getLevel(), feet.x, feet.y, feet.z, three.copy());
        drop.setDeltaMovement(Vec3.ZERO);
        var restored = new FrenchieEntity[1];
        h.startSequence().thenWaitUntil(() -> h.assertTrue(dog.onGround(), "Fetching dog is airborne"))
                .thenExecute(() -> {
                    // Seed only the one-in-five invitation. Movement, pickup and delivery run through normal AI.
                    for (long seed = 0; ; seed++) {
                        dog.getRandom().setSeed(seed);
                        if (dog.getRandom().nextInt(5) == 0) {
                            dog.getRandom().setSeed(seed);
                            break;
                        }
                    }
                    h.assertTrue(owner.gameMode.destroyBlock(h.absolutePos(log)), "Owner could not chop the trunk");
                    h.assertTrue(h.getLevel().addFreshEntity(drop), "Could not stage the named stick drop");
                    owner.snapTo(h.absoluteVec(new Vec3(9.5, 1, 9.5)), 0, 0);
                })
                .thenWaitUntil(() -> h.assertTrue(ItemStack.matches(dog.getOffhandItem(), one),
                        "After a chopped log the dog never took one of the dropped sticks"))
                .thenExecute(() -> {
                    h.assertTrue(drop.isAlive() && ItemStack.matches(drop.getItem(), three.copyWithCount(2))
                                    && owner.getInventory().countItem(Items.STICK) == 0,
                            "Pickup did not leave exactly two named sticks on the ground");
                })
                .thenWaitUntil(() -> h.assertTrue(dog.distanceToSqr(owner) <= 3.2,
                        "Fetching dog never brought the stick to its owner"))
                .thenExecuteAfter(8, () -> {
                    h.assertTrue(owner.getInventory().countItem(Items.STICK) == 0 && ItemStack.matches(dog.getOffhandItem(), one),
                            "An automatic handoff to a full inventory lost or duplicated the stick");
                    restored[0] = CompanionTravelTests.reload(h, dog);
                    h.assertTrue(ItemStack.matches(restored[0].getOffhandItem(), one), "Reload lost the genuine stick");
                    owner.getInventory().setItem(7, ItemStack.EMPTY);
                })
                .thenWaitUntil(() -> h.assertTrue(ItemStack.matches(owner.getInventory().getItem(7), one),
                        "The dog did not automatically deliver its named stick after a slot opened"))
                .thenExecute(() -> {
                    h.assertTrue(owner.getInventory().countItem(Items.STICK) == 1 && restored[0].getOffhandItem().isEmpty()
                                    && ItemStack.matches(drop.getItem(), three.copyWithCount(2)),
                            "Automatic delivery changed components, lost a stick or created an extra one");
                    disconnect(owner);
                }).thenSucceed();
    }
}
