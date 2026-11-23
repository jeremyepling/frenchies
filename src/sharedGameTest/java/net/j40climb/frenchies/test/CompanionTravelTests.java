package net.j40climb.frenchies.test;

import net.j40climb.frenchies.common.entity.frenchie.AdventureOutfit;
import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
//? if <26.3 {
import net.minecraft.world.entity.EntityType;
//?} else {
/*import net.minecraft.world.entity.EntityTypes;
*///?}
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

final class CompanionTravelTests {
    private static final InteractionHand HAND = InteractionHand.MAIN_HAND;

    static ServerPlayer player(GameTestHelper h, double x, double z) {
        var player = GameTestPlayers.connect(h);
        player.setGameMode(GameType.SURVIVAL);
        player.getInventory().clearContent();
        player.snapTo(h.absoluteVec(new Vec3(x, 1, z)), 0, 0);
        return player;
    }

    static void disconnect(ServerPlayer... players) {
        for (ServerPlayer player : players) {
            player.level().getServer().getPlayerList().remove(player);
        }
    }

    static FrenchieEntity dog(GameTestHelper h, ServerPlayer owner) {
        var dog = h.spawn(FrenchieRegistry.FRENCHIE.get(), 5, 1, 5);
        dog.tame(owner);
        dog.standUp();
        return dog;
    }

    private static FrenchieEntity current(GameTestHelper h, UUID id) {
        var entity = h.getLevel().getEntity(id);
        h.assertTrue(entity instanceof FrenchieEntity, "Courier is still travelling");
        return (FrenchieEntity) entity;
    }

    static FrenchieEntity reload(GameTestHelper h, FrenchieEntity original) {
        var level = h.getLevel();
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        original.saveWithoutId(output);
        var saved = output.buildResult();
        var restored = FrenchieRegistry.FRENCHIE.get().create(level, EntitySpawnReason.LOAD);
        h.assertTrue(restored != null, "Could not reconstruct Frenchie");
        original.discard();
        restored.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
        h.assertTrue(level.addFreshEntity(restored) && restored.getUUID().equals(original.getUUID()),
                "Entity reload did not restore the same Frenchie");
        return restored;
    }

    static void fetch(GameTestHelper h) {
        var owner = player(h, 5.5, 3.5);
        var visitor = player(h, 6.5, 3.5);
        var dog = dog(h, owner);
        var id = dog.getUUID();
        var toy = new ItemStack(FrenchieRegistry.SQUEAKY_TOY.get());
        toy.set(DataComponents.CUSTOM_NAME, Component.literal("Pocket squeaker"));
        owner.setItemInHand(HAND, toy.copy());
        owner.setXRot(70);
        owner.setShiftKeyDown(true);
        owner.getMainHandItem().use(owner.level(), owner, HAND);
        owner.setShiftKeyDown(false);
        h.assertTrue(owner.getMainHandItem().isEmpty(), "Throw did not release the held toy");
        var loose = h.getLevel().getEntitiesOfClass(ItemEntity.class, owner.getBoundingBox().inflate(6),
                item -> ItemStack.matches(item.getItem(), toy));
        h.assertTrue(loose.size() == 1, "Throw should create exactly one named toy");
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(ItemStack.matches(dog.getOffhandItem(), toy), "Dog never picked up the toy"))
                .thenExecute(() -> {
                    h.assertTrue(loose.getFirst().isRemoved(), "Dog and ground both retained the thrown toy");
                    dog.sitDown();
                    var restored = reload(h, dog);
                    h.assertTrue(restored.isOrderedToSit() && ItemStack.matches(restored.getOffhandItem(), toy),
                            "Carried toy or sit order lost during reload");
                    visitor.snapTo(restored.position().add(1, 0, 0), 0, 0);
                    visitor.interactOn(restored, HAND, Vec3.ZERO);
                    h.assertTrue(visitor.getMainHandItem().isEmpty() && ItemStack.matches(restored.getOffhandItem(), toy),
                            "Visitor took the owner's toy");
                    owner.snapTo(restored.position().add(-1, 0, 0), 0, 0);
                    owner.interactOn(restored, HAND, Vec3.ZERO);
                    h.assertTrue(ItemStack.matches(owner.getMainHandItem(), toy) && current(h, id).getOffhandItem().isEmpty(),
                            "The owner should recover exactly one named toy from the seated dog");
                    disconnect(visitor, owner);
                }).thenSucceed();
    }

    static void captain(GameTestHelper h) {
        var owner = player(h, 4.5, 4.5);
        var guest = player(h, 6.5, 4.5);
        var dog = dog(h, owner);
        var boat = h.spawn(EntityType.OAK_BOAT, 5, 1, 6);
        h.assertTrue(!dog.hasFlotationVest(), "Frenchie showed its flotation vest on land");
        owner.interactOn(boat, HAND, Vec3.ZERO);
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(owner.getVehicle() == boat && dog.getVehicle() == boat,
                        "Following Frenchie did not automatically board the owner's boat"))
                .thenExecute(() -> {
                    h.assertTrue(!dog.hasFlotationVest(), "Captain showed a vest in a dry boat");
                    guest.interactOn(boat, HAND, Vec3.ZERO);
                    h.assertTrue(guest.getVehicle() == boat && boat.getControllingPassenger() == owner,
                            "Frenchie took the second player seat or displaced the driver");
                    // Three water blocks across, with a solid dock near either passenger.
                    for (var pos : BlockPos.betweenClosed(new BlockPos(3, 1, 4), new BlockPos(7, 1, 8))) {
                        boolean dock = pos.getX() == 3 || pos.getX() == 7 || pos.getZ() == 4 || pos.getZ() == 8;
                        h.setBlock(pos, dock ? Blocks.STONE : Blocks.WATER);
                    }
                })
                .thenWaitUntil(() -> h.assertTrue(dog.getVehicle() == boat && dog.hasFlotationVest()
                                && dog.adventureOutfit() == AdventureOutfit.CAPTAIN,
                        "Floating captain never showed its vest and cap"))
                .thenExecute(() -> {
                    Vec3 bow = new Vec3(0, 0, 1).yRot(-boat.getYRot() * Mth.DEG_TO_RAD);
                    h.assertTrue(dog.position().subtract(boat.position()).dot(bow) > 0.5,
                            "Dressed captain did not stay at the bow");
                })
                .thenExecute(owner::stopRiding)
                .thenWaitUntil(() -> h.assertTrue(!dog.isPassenger()
                                && h.getLevel().getBlockState(dog.blockPosition().below()).is(Blocks.STONE),
                        "Captain failed to disembark onto the safe dock when its owner left"))
                .thenExecute(() -> {
                    h.assertTrue(guest.getVehicle() == boat && !dog.hasFlotationVest(),
                            "Owner exit ejected the second player or left the dog wearing the vest");
                })
                .thenWaitUntil(() -> h.assertTrue(dog.adventureOutfit() != AdventureOutfit.CAPTAIN,
                        "Frenchie kept its captain hat after disembarking"))
                .thenExecute(() -> {
                    h.assertTrue(!reload(h, dog).hasFlotationVest(), "Reload restored a vest on dry ground");
                    disconnect(guest, owner);
                }).thenSucceed();
    }

}
