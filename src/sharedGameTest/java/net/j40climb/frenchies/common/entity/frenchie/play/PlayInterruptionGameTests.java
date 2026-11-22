package net.j40climb.frenchies.common.entity.frenchie.play;

import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.j40climb.frenchies.common.entity.frenchie.recall.CompanionRoster;
import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import net.j40climb.frenchies.test.GameTestPlayers;

import java.util.UUID;

/** Real owner commands during play, followed by a table of every synced play stage. */
public final class PlayInterruptionGameTests {
    private static final InteractionHand HAND = InteractionHand.MAIN_HAND;

    private PlayInterruptionGameTests() {}

    public static void whistleDuringPlay(GameTestHelper h) {
        var owner = GameTestPlayers.connect(h);
        owner.setGameMode(GameType.SURVIVAL);
        owner.getInventory().clearContent();
        var dog = h.spawn(FrenchieRegistry.FRENCHIE.get(), 5, 1, 5);
        dog.tame(owner);
        dog.standUp();
        owner.snapTo(dog.position().add(2.5, 0, 0), 0, 0);
        CompanionRoster.track(dog);
        UUID id = dog.getUUID();
        var current = new FrenchieEntity[] {dog};
        var mouthItem = new ItemStack(Items.DIAMOND);
        mouthItem.set(DataComponents.CUSTOM_NAME, Component.literal("Playtest keepsake"));

        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(dog.onGround(), "Frenchie never settled"))
                .thenExecute(() -> {
                    owner.setShiftKeyDown(true);
                    owner.setItemInHand(HAND, new ItemStack(Items.BONE));
                    dog.mobInteract(owner, HAND);
                    owner.setShiftKeyDown(false);
                })
                .thenWaitUntil(() -> h.assertTrue(dog.trickStage() == PlayStages.Trick.FLOP,
                        "Owner trick did not reach the back landing"))
                .thenExecute(() -> {
                    dog.setHasToyStick(true);
                    dog.setItemSlot(EquipmentSlot.OFFHAND, mouthItem.copy());
                    whistle(owner, false);
                    assertClear(h, dog, "recall during trick");
                    assertCustody(h, dog, mouthItem, "recall during trick");
                })
                .thenWaitUntil(() -> {
                    current[0] = near(h, owner, id);
                    assertClear(h, current[0], "recalled trick");
                    assertCustody(h, current[0], mouthItem, "recalled trick");
                })
                .thenExecuteAfter(65, () -> {
                    owner.setItemInHand(HAND, ItemStack.EMPTY);
                    owner.setShiftKeyDown(true);
                })
                .thenExecuteFor(32, () -> {
                    var active = current[0];
                    active.getNavigation().stop();
                    active.setDeltaMovement(0, active.getDeltaMovement().y, 0);
                    owner.snapTo(active.position().add(1.5, 0, 0), 0, 0);
                })
                .thenWaitUntil(() -> h.assertTrue(current[0].bellyStage() == PlayStages.BellyRub.WAITING,
                        "Owner invitation did not reach the upside-down pose"))
                .thenExecute(() -> {
                    owner.setShiftKeyDown(false);
                    current[0].mobInteract(owner, HAND);
                    h.assertTrue(current[0].bellyStage() == PlayStages.BellyRub.RUBBING,
                            "Owner interaction did not start the leg kick");
                    whistle(owner, true);
                })
                .thenWaitUntil(() -> h.assertTrue(h.getLevel().getEntity(id) == null,
                        "Dismissal during belly rub left a live dog"))
                .thenExecute(() -> whistle(owner, false))
                .thenWaitUntil(() -> {
                    current[0] = near(h, owner, id);
                    assertClear(h, current[0], "returned belly rub");
                    assertCustody(h, current[0], mouthItem, "returned belly rub");
                })
                .thenExecute(() -> {
                    var active = current[0];
                    checkEveryStage(h, active, mouthItem);
                    var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING,
                            h.getLevel().registryAccess());
                    active.saveWithoutId(output);
                    active.discard();
                    var restored = FrenchieRegistry.FRENCHIE.get().create(h.getLevel(), EntitySpawnReason.LOAD);
                    h.assertTrue(restored != null, "Could not reload interrupted Frenchie");
                    restored.load(TagValueInput.create(ProblemReporter.DISCARDING,
                            h.getLevel().registryAccess(), output.buildResult()));
                    h.assertTrue(h.getLevel().addFreshEntity(restored) && restored.getUUID().equals(id),
                            "Reload did not restore the same Frenchie");
                    assertClear(h, restored, "save and return");
                    assertCustody(h, restored, mouthItem, "save and return");
                    assertCount(h, owner);
                    current[0] = restored;
                    owner.level().getServer().getPlayerList().remove(owner);
                })
                .thenSucceed();
    }

    private static void whistle(ServerPlayer owner, boolean dismiss) {
        owner.setItemInHand(HAND, new ItemStack(FrenchieRegistry.FRENCHIE_WHISTLE.get()));
        owner.setShiftKeyDown(dismiss);
        owner.getMainHandItem().use(owner.level(), owner, HAND);
        owner.setShiftKeyDown(false);
    }

    private static FrenchieEntity near(GameTestHelper h, ServerPlayer owner, UUID id) {
        var entity = h.getLevel().getEntity(id);
        h.assertTrue(entity instanceof FrenchieEntity dog && dog.isAlive() && dog.isOwnedBy(owner)
                        && dog.distanceToSqr(owner) < 36,
                "Selected Frenchie never returned to its owner");
        assertCount(h, owner);
        return (FrenchieEntity) entity;
    }

    private static void assertCount(GameTestHelper h, ServerPlayer owner) {
        h.assertTrue(h.getLevel().getEntities(FrenchieRegistry.FRENCHIE.get(),
                dog -> dog.isOwnedBy(owner)).size() == 1, "Play interruption duplicated or lost the dog");
    }

    private static void assertClear(GameTestHelper h, FrenchieEntity dog, String step) {
        h.assertTrue(dog.trickStage() == PlayStages.Trick.NONE && dog.bellyStage() == PlayStages.BellyRub.NONE
                        && dog.weatherStage() == PlayStages.Weather.NONE && dog.digStage() == PlayStages.Digging.NONE
                        && !dog.isBellyRubPose() && !dog.isOrderedToSit(),
                step + " left a synced play pose or sit order active");
    }

    private static void assertCustody(GameTestHelper h, FrenchieEntity dog, ItemStack mouthItem, String step) {
        h.assertTrue(ItemStack.matches(mouthItem, dog.getOffhandItem()) && dog.getParcel().isEmpty(),
                step + " replaced the named mouth item or parcel state");
        dog.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        h.assertTrue(dog.getMouthItem().is(Items.STICK), step + " lost the persistent virtual stick");
        dog.setItemSlot(EquipmentSlot.OFFHAND, mouthItem.copy());
    }

    private static void checkEveryStage(GameTestHelper h, FrenchieEntity dog, ItemStack mouthItem) {
        for (int stage = PlayStages.Trick.CROUCH; stage <= PlayStages.Trick.LAP; ++stage) {
            dog.setTrickStage(stage);
            dog.cancelForWhistle();
            assertClear(h, dog, "trick stage " + stage);
            assertCustody(h, dog, mouthItem, "trick stage " + stage);
        }
        for (int stage = PlayStages.BellyRub.ROLL_OVER; stage <= PlayStages.BellyRub.ROLL_UP; ++stage) {
            dog.setBellyStage(stage);
            dog.cancelForWhistle();
            assertClear(h, dog, "belly stage " + stage);
            assertCustody(h, dog, mouthItem, "belly stage " + stage);
        }
        for (int stage = PlayStages.Weather.SHAKE; stage <= PlayStages.Weather.SNOW_POP; ++stage) {
            dog.setWeatherStage(stage);
            dog.cancelForWhistle();
            assertClear(h, dog, "weather stage " + stage);
            assertCustody(h, dog, mouthItem, "weather stage " + stage);
        }
        for (int stage = PlayStages.Digging.APPROACH; stage <= PlayStages.Digging.REVEAL; ++stage) {
            dog.setDigStage(stage);
            dog.cancelForWhistle();
            assertClear(h, dog, "dig stage " + stage);
            assertCustody(h, dog, mouthItem, "dig stage " + stage);
        }
    }
}
