package net.j40climb.frenchies.common.entity.frenchie;

import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

/** Server behavior and custody checks for the non-collectible stick. */
public final class DiggingAssistantGameTests {
    private DiggingAssistantGameTests() {}

    public static void sequenceAndCustody(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        for (int x = 4; x <= 8; ++x) for (int z = 3; z <= 7; ++z) {
            h.setBlock(new BlockPos(x, 0, z), Blocks.DIRT);
        }
        var owner = h.makeMockPlayer(GameType.SURVIVAL);
        owner.snapTo(h.absoluteVec(new Vec3(5.5, 1, 5.5)), 0, 0);
        var dog = h.spawn(FrenchieRegistry.FRENCHIE.get(), 6, 1, 5);
        dog.tame(owner);
        dog.standUp();
        var obstructed = h.spawnWithNoFreeWill(FrenchieRegistry.FRENCHIE.get(), 8, 1, 8);
        obstructed.tame(owner);
        obstructed.standUp();
        obstructed.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.DIAMOND));
        BlockPos dug = h.absolutePos(new BlockPos(2, 1, 5));
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(dog.onGround() && obstructed.onGround(), "Dogs have not landed"))
                .thenExecute(() -> {
                    h.assertTrue(!obstructed.requestDigging(dug), "Occupied mouth joined excavation");
                    h.assertTrue(obstructed.getOffhandItem().is(Items.DIAMOND), "Occupied mouth item was replaced");
                    h.assertTrue(dog.requestDigging(dug), "Safe nearby patch was not selected");
                    h.assertTrue(!dog.requestDigging(dug), "Repeated shovel event restarted the request");
                })
                .thenWaitUntil(() -> h.assertTrue(dog.digStage() == DiggingAssistantGoal.DIG,
                        "Frenchie did not reach its cosmetic digging patch; stage=" + dog.digStage()
                                + ", pos=" + h.relativePos(dog.blockPosition()) + ", awake=" + !dog.isFrenchieSleeping()
                                + ", order=" + dog.isOrderedToSit() + ", available=" + dog.canPlay()))
                .thenExecute(() -> h.assertTrue(!dog.getMouthItem().is(Items.STICK),
                        "Stick appeared before the reveal"))
                .thenWaitUntil(() -> h.assertTrue(dog.digStage() == DiggingAssistantGoal.REVEAL,
                        "Frenchie did not finish its short digging session"))
                .thenExecute(() -> {
                    h.assertTrue(dog.getMouthItem().is(Items.STICK), "Reusable toy stick did not appear");
                    h.assertTrue(dog.getOffhandItem().isEmpty(), "Toy stick became an equipment item");
                    h.assertTrue(h.getBlockState(new BlockPos(6, 0, 5)).is(Blocks.DIRT),
                            "Frenchie changed the digging patch");
                    h.assertTrue(h.getBlockState(new BlockPos(2, 1, 5)).isAir(),
                            "Frenchie changed the owner's excavation");
                    h.assertTrue(level.getEntitiesOfClass(ItemEntity.class, dog.getBoundingBox().inflate(8),
                            item -> item.getItem().is(Items.STICK)).isEmpty(), "Toy stick was dropped as loot");
                    dog.syncParcel(new ItemStack(Items.BUNDLE));
                    h.assertTrue(dog.getMouthItem().is(Items.BUNDLE), "Toy stick obscured delivery cargo");
                    dog.syncParcel(ItemStack.EMPTY);
                    dog.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.DIAMOND));
                    h.assertTrue(dog.getMouthItem().is(Items.DIAMOND), "Toy stick obscured an existing mouth item");
                    dog.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                })
                .thenWaitUntil(() -> h.assertTrue(dog.digStage() == DiggingAssistantGoal.NONE,
                        "Digging pose was left active"))
                .thenExecute(() -> {
                    h.assertTrue(!dog.requestDigging(dug), "Digging ignored its cooldown");
                    var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
                    dog.saveWithoutId(output);
                    var restored = FrenchieRegistry.FRENCHIE.get().create(level, EntitySpawnReason.LOAD);
                    h.assertTrue(restored != null, "Could not reload Frenchie");
                    restored.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), output.buildResult()));
                    h.assertTrue(restored.getMouthItem().is(Items.STICK) && restored.getOffhandItem().isEmpty(),
                            "Reload lost or duplicated the virtual stick");
                    h.assertTrue(restored.nextDigAt == dog.nextDigAt && restored.nextDigAt > level.getGameTime(),
                            "Reload reset the digging cooldown");
                })
                .thenSucceed();
    }

    public static void commandInterrupts(GameTestHelper h) {
        for (int x = 4; x <= 8; ++x) for (int z = 3; z <= 7; ++z) {
            h.setBlock(new BlockPos(x, 0, z), Blocks.DIRT);
        }
        var owner = h.makeMockPlayer(GameType.SURVIVAL);
        owner.snapTo(h.absoluteVec(new Vec3(5.5, 1, 5.5)), 0, 0);
        var dog = h.spawn(FrenchieRegistry.FRENCHIE.get(), 6, 1, 5);
        dog.tame(owner);
        dog.standUp();
        BlockPos dug = h.absolutePos(new BlockPos(2, 1, 5));
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(dog.onGround(), "Frenchie has not landed"))
                .thenExecute(() -> h.assertTrue(dog.requestDigging(dug), "Safe patch rejected"))
                .thenWaitUntil(() -> h.assertTrue(dog.digStage() == DiggingAssistantGoal.DIG,
                        "Frenchie did not begin digging"))
                .thenExecute(dog::sitDown)
                .thenWaitUntil(() -> h.assertTrue(dog.digStage() == DiggingAssistantGoal.NONE && dog.isOrderedToSit(),
                        "Sit order did not interrupt digging"))
                .thenExecuteFor(50, () -> h.assertTrue(dog.getMouthItem().isEmpty()
                                && dog.digStage() == DiggingAssistantGoal.NONE,
                        "Interrupted digging revealed a stick or restarted"))
                .thenSucceed();
    }
}
