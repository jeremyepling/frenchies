package net.j40climb.frenchies.test;

import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

final class TrickBellyTests {
    static void trick(GameTestHelper h) {
        var owner = h.makeMockServerPlayerInLevel();
        var dog = h.spawn(FrenchieRegistry.FRENCHIE.get(), 5, 1, 5);
        dog.tame(owner);
        dog.standUp();
        owner.snapTo(dog.position().add(2.5, 0, 0), 0, 0);
        owner.setShiftKeyDown(true);
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BONE));
        float health = dog.getHealth();
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(dog.onGround(), "Frenchie never settled"))
                .thenExecute(() -> dog.mobInteract(owner, InteractionHand.MAIN_HAND))
                .thenWaitUntil(() -> h.assertTrue(dog.trickStage() == 1, "Trick did not crouch"))
                .thenWaitUntil(() -> h.assertTrue(dog.trickStage() == 2, "Trick did not spring"))
                .thenWaitUntil(() -> h.assertTrue(dog.trickStage() == 3, "Trick did not flop"))
                .thenWaitUntil(() -> h.assertTrue(dog.trickStage() == 4, "Trick did not wiggle"))
                .thenWaitUntil(() -> h.assertTrue(dog.trickStage() == 5, "Trick did not roll upright"))
                .thenWaitUntil(() -> h.assertTrue(dog.trickStage() == 6, "Safe open ground did not allow a victory lap"))
                .thenWaitUntil(() -> h.assertTrue(dog.trickStage() == 0, "Trick did not finish"))
                .thenExecute(() -> h.assertTrue(dog.getHealth() == health && !dog.isOrderedToSit()
                        && dog.onGround(), "Trick damaged or stranded the Frenchie"))
                .thenSucceed();
    }

    static void belly(GameTestHelper h) {
        var owner = h.makeMockServerPlayerInLevel();
        var dog = h.spawn(FrenchieRegistry.FRENCHIE.get(), 5, 1, 5);
        dog.tame(owner);
        dog.standUp();
        owner.snapTo(dog.position().add(1.5, 0, 0), 0, 0);
        owner.setShiftKeyDown(true);
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(dog.onGround(), "Frenchie never settled"))
                .thenExecuteFor(10, () -> h.assertTrue(dog.bellyStage() == 0, "Walking by was treated as a belly invitation"))
                .thenExecuteFor(32, () -> {
                    dog.getNavigation().stop();
                    dog.setDeltaMovement(0, dog.getDeltaMovement().y, 0);
                    owner.snapTo(dog.position().add(1.5, 0, 0), 0, 0);
                })
                .thenWaitUntil(() -> h.assertTrue(dog.bellyStage() == 2, "Crouching beside the dog did not invite a belly pose"))
                .thenExecute(() -> {
                    owner.setShiftKeyDown(false);
                    dog.mobInteract(owner, InteractionHand.MAIN_HAND);
                    h.assertTrue(dog.bellyStage() == 3 && !dog.isOrderedToSit(), "Rub toggled the sit order");
                })
                .thenExecute(() -> owner.snapTo(dog.position().add(6, 0, 0), 0, 0))
                .thenWaitUntil(() -> h.assertTrue(dog.bellyStage() == 0, "Walking away left the dog upside down"))
                .thenExecute(() -> h.assertTrue(!dog.isOrderedToSit(), "Belly rub imposed a sit order"))
                .thenSucceed();
    }
}
