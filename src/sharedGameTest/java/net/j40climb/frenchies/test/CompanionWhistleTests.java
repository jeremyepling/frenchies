package net.j40climb.frenchies.test;

import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.j40climb.frenchies.common.entity.frenchie.frontpack.FrontpackAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.InteractionHand;
//? if <26.3 {
import net.minecraft.world.entity.EntityType;
//?} else {
/*import net.minecraft.world.entity.EntityTypes;
*///?}
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.j40climb.frenchies.test.CompanionTravelTests.disconnect;
import static net.j40climb.frenchies.test.CompanionTravelTests.player;

final class CompanionWhistleTests {
    private static final InteractionHand HAND = InteractionHand.MAIN_HAND;

    static void command(ServerPlayer owner, String command) {
        owner.level().getServer().getCommands().performPrefixedCommand(owner.createCommandSourceStack(), command);
    }

    static void equip(ServerPlayer owner) {
        owner.setItemInHand(HAND, new ItemStack(FrenchieRegistry.FRENCHIE_WHISTLE.get()));
    }

    static void whistle(ServerPlayer owner, boolean dismiss) {
        owner.setShiftKeyDown(dismiss);
        owner.getMainHandItem().use(owner.level(), owner, HAND);
        owner.setShiftKeyDown(false);
    }

    static ItemStack named(ItemStack stack, String name) {
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }

    static FrenchieEntity dog(GameTestHelper h, ServerPlayer owner, int x, int z) {
        var dog = h.spawn(FrenchieRegistry.FRENCHIE.get(), x, 1, z);
        dog.tame(owner);
        dog.sitDown();
        return dog;
    }

    private static FrenchieEntity near(GameTestHelper h, ServerPlayer owner, UUID id) {
        var entity = owner.level().getEntity(id);
        h.assertTrue(entity instanceof FrenchieEntity dog && dog.isAlive() && dog.isOwnedBy(owner)
                        && dog.distanceToSqr(owner) < 36,
                "Selected Frenchie never reached its owner");
        return (FrenchieEntity) entity;
    }

    static void selection(GameTestHelper h) {
        var owner = player(h, 2.5, 5.5);
        var visitor = player(h, 1.5, 2.5);
        var first = h.spawn(FrenchieRegistry.FRENCHIE.get(), 3, 1, 5);
        owner.setItemInHand(HAND, new ItemStack(Items.BONE));
        owner.interactOn(first, HAND, Vec3.ZERO);
        h.assertTrue(first.isOwnedBy(owner) && owner.getInventory().countItem(FrenchieRegistry.FRENCHIE_WHISTLE.get()) == 1,
                "First bone taming did not grant access to one whistle");
        owner.getInventory().clearContent();
        command(owner, "frenchie whistle");
        h.assertTrue(owner.getInventory().countItem(FrenchieRegistry.FRENCHIE_WHISTLE.get()) == 1,
                "Lost whistle could not be replaced");
        var second = dog(h, owner, 8, 5);
        var id = second.getUUID();
        equip(owner);
        equip(visitor);
        h.startSequence().thenExecuteAfter(5, () -> {
            command(visitor, "frenchie select " + id);
            whistle(visitor, false);
            whistle(visitor, true);
        }).thenExecuteAfter(35, () -> {
            h.assertTrue(h.getLevel().getEntity(id) == second && second.isOrderedToSit() && first.isOrderedToSit(),
                    "A visitor called or dismissed an unowned Frenchie");
            command(owner, "frenchie select " + id);
            whistle(owner, false);
        }).thenWaitUntil(() -> h.assertTrue(!second.isOrderedToSit() && second.distanceToSqr(owner) < 9,
                "Selected nearby sitting dog never approached on foot"))
        .thenExecute(() -> {
            h.assertTrue(first.isOrderedToSit() && h.getLevel().getEntity(id) == second,
                    "Calling this dog moved the other dog or replaced the nearby live entity");
            whistle(owner, true);
        }).thenWaitUntil(() -> h.assertTrue(h.getLevel().getEntity(id) == null, "Dismissed dog stayed in the world"))
        .thenExecute(() -> {
            whistle(visitor, false);
            whistle(owner, true);
        }).thenExecuteAfter(40, () -> {
            h.assertTrue(h.getLevel().getEntity(id) == null && first.isAlive() && first.isOrderedToSit(),
                    "Repeated dismissal or foreign whistle affected another Frenchie");
            whistle(owner, false);
            whistle(owner, false);
        }).thenWaitUntil(() -> near(h, owner, id)).thenExecuteAfter(40, () -> {
            h.assertTrue(h.getLevel().getEntities(FrenchieRegistry.FRENCHIE.get(), d -> d.isOwnedBy(owner)).size() == 2,
                    "Whistling repeatedly changed the number of owned Frenchies");
            h.assertTrue(owner.getMainHandItem().getDamageValue() == 0 && owner.getMainHandItem().getCount() == 1,
                    "Whistling consumed the item or damaged it");
            disconnect(visitor, owner);
        }).thenSucceed();
    }

    static void courier(GameTestHelper h, boolean dismiss, boolean pack) {
        var owner = player(h, 3.5, 4.5);
        var recipient = player(h, 8.5, 7.5);
        var dog = dog(h, owner, 3, 5);
        var id = dog.getUUID();
        var variant = dog.getVariant();
        dog.setCustomName(Component.literal("Violet"));
        var parcel = FrenchieDeliveryGameTests.parcel();
        var equipment = named(new ItemStack(Items.LEATHER_BOOTS), "Violet's boots");
        dog.setItemSlot(EquipmentSlot.FEET, equipment.copy());
        owner.setItemInHand(HAND, parcel.copy());
        h.startSequence().thenExecuteAfter(4, () -> {
            FrenchieDeliveryGameTests.confirm(owner, recipient, dog);
            h.assertTrue(dog.isDelivering() && owner.getMainHandItem().isEmpty()
                            && ItemStack.matches(parcel, dog.getParcel()),
                    "Confirmation did not transfer the component-bearing bundle to the dog");
            if (pack) {
                owner.setItemSlot(EquipmentSlot.CHEST, new ItemStack(FrenchieRegistry.FRONT_PACK.get()));
                owner.setShiftKeyDown(true);
                GameTestPlayers.interact(owner, dog, HAND);
                owner.setShiftKeyDown(false);
                h.assertTrue(FrontpackAttachments.pack(owner).hasCarriedFrenchie(), "Courier never entered pack");
            }
        }).thenWaitUntil(() -> h.assertTrue(h.getLevel().getEntity(id) == null, "Courier did not leave the world for travel or packing"))
        .thenExecute(() -> {
            for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) owner.getInventory().setItem(i, new ItemStack(Items.STONE, 64));
            equip(owner);
            whistle(owner, dismiss);
        }).thenExecuteAfter(45, () -> {
            if (dismiss) {
                h.assertTrue(h.getLevel().getEntity(id) == null && !FrontpackAttachments.pack(owner).hasCarriedFrenchie(),
                        "Dismissed courier still had a physical or packed copy");
                whistle(owner, false);
            }
        }).thenWaitUntil(() -> near(h, owner, id)).thenExecuteAfter(45, () -> {
            // Arrival was observed above. A following dog can wander while the player keeps still.
            var restored = (FrenchieEntity) h.getLevel().getEntity(id);
            h.assertTrue(restored != null && restored.isAlive() && restored.isOwnedBy(owner), "Restored courier disappeared");
            h.assertTrue(!FrontpackAttachments.pack(owner).hasCarriedFrenchie()
                            && !restored.isDelivering() && !restored.isGoingHome()
                            && restored.getVariant() == variant && restored.getName().getString().equals("Violet")
                            && ItemStack.matches(equipment, restored.getItemBySlot(EquipmentSlot.FEET))
                            && ItemStack.matches(parcel, restored.getParcel()), "Whistle lost identity or custody, or left the old job running");
            h.assertTrue(owner.getInventory().getFreeSlot() == -1 && owner.getInventory().countItem(Items.STONE) == 64 * (Inventory.INVENTORY_SIZE - 1)
                            && owner.getInventory().countItem(Items.BUNDLE) == 0 && recipient.getInventory().countItem(Items.BUNDLE) == 0,
                    "Whistling changed full inventory or transferred the parcel");
            owner.setItemInHand(HAND, ItemStack.EMPTY);
            owner.snapTo(restored.position().add(1, 0, 0), 0, 0);
            owner.interactOn(restored, HAND, Vec3.ZERO);
            h.assertTrue(ItemStack.matches(parcel, owner.getMainHandItem()) && restored.getParcel().isEmpty()
                            && owner.getInventory().countItem(Items.BUNDLE) == 1
                            && h.getLevel().getEntitiesOfClass(ItemEntity.class, owner.getBoundingBox().inflate(12),
                                    item -> item.getItem().is(Items.BUNDLE)).isEmpty(),
                    "The owner could not retrieve exactly one unchanged parcel");
        }).thenExecuteAfter(100, () -> {
            h.assertTrue(h.getLevel().getEntities(FrenchieRegistry.FRENCHIE.get(), d -> d.isOwnedBy(owner)).size() == 1
                            && recipient.getInventory().countItem(Items.BUNDLE) == 0 && owner.getInventory().countItem(Items.BUNDLE) == 1,
                    "The interrupted delivery later recreated its dog or bundle");
            disconnect(recipient, owner);
        }).thenSucceed();
    }

    static void dimension(GameTestHelper h) {
        var owner = player(h, 4.5, 4.5);
        var dog = dog(h, owner, 5, 5);
        var id = dog.getUUID();
        var nether = h.getLevel().getServer().getLevel(Level.NETHER);
        h.assertTrue(nether != null, "Missing test Nether");
        var landing = new BlockPos(40, 150, 40);
        var chunk = ChunkPos.containing(landing);
        // The test server sprints ticks. Finish generation around the endpoint before starting its deadline.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) nether.getChunk(chunk.x() + dx, chunk.z() + dz);
        }
        nether.getChunkSource().addTicketWithRadius(TicketType.FORCED, chunk, 2);
        for (var pos : BlockPos.betweenClosed(landing.offset(-5, -1, -5), landing.offset(5, -1, 5)))
            nether.setBlockAndUpdate(pos, Blocks.MAGMA_BLOCK.defaultBlockState());
        equip(owner);
        h.startSequence().thenExecuteAfter(6, () -> {
            owner.setNoGravity(true);
            owner.teleportTo(nether, landing.getX() + .5, landing.getY(), landing.getZ() + .5, Set.of(), 0, 0, true);
        }).thenWaitUntil(() -> h.assertTrue(nether.areEntitiesActuallyLoadedAndTicking(chunk), "Nether fixture did not load"))
        .thenExecute(() -> whistle(owner, false))
        .thenExecuteFor(60, () -> h.assertTrue(nether.getEntity(id) == null, "Whistle teleported onto hazardous ground or over a drop"))
        .thenExecute(() -> {
            for (var pos : BlockPos.betweenClosed(landing.offset(-2, -1, -2), landing.offset(2, -1, 2)))
                nether.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
            whistle(owner, false);
        }).thenWaitUntil(() -> near(h, owner, id)).thenExecute(() -> {
            var arrived = near(h, owner, id);
            h.assertTrue(!arrived.isOrderedToSit() && h.getLevel().getEntity(id) == null
                            && nether.getBlockState(arrived.blockPosition().below()).is(Blocks.STONE),
                    "Cross-dimension whistle retained the old entity, sit order, or unsafe footing");
            nether.getChunkSource().removeTicketWithRadius(TicketType.FORCED, chunk, 2);
            disconnect(owner);
        }).thenSucceed();
    }

    static void unloadedDismiss(GameTestHelper h) {
        var owner = player(h, 4.5, 4.5);
        var level = h.getLevel();
        var kennel = ChunkPos.containing(owner.blockPosition().east(880)).getMiddleBlockPosition(owner.getBlockY() + 14);
        var remote = ChunkPos.containing(kennel);
        var tickets = level.getChunkSource();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) level.getChunk(remote.x() + dx, remote.z() + dz);
        }
        tickets.addTicketWithRadius(TicketType.FORCED, remote, 2);
        for (var floor : BlockPos.betweenClosed(kennel.offset(-2, -1, -2), kennel.offset(2, -1, 2))) {
            level.setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
        }
        var dog = dog(h, owner, 5, 5);
        var id = dog.getUUID();
        var toy = named(new ItemStack(FrenchieRegistry.SQUEAKY_TOY.get()), "Away squeaker");
        dog.setItemSlot(EquipmentSlot.OFFHAND, toy.copy());
        equip(owner);
        ServerPlayer[] returning = new ServerPlayer[1];
        AtomicBoolean reopened = new AtomicBoolean();
        h.startSequence().thenWaitUntil(() -> h.assertTrue(level.areEntitiesActuallyLoadedAndTicking(remote), "Kennel did not load"))
        .thenExecute(() -> dog.snapTo(kennel, 0, 0))
        .thenExecuteAfter(30, () -> {
            h.assertTrue(dog.isOrderedToSit() && dog.chunkPosition().equals(remote), "Stay failed before the owner whistled");
            tickets.removeTicketWithRadius(TicketType.FORCED, remote, 2);
        }).thenWaitUntil(() -> h.assertTrue(!level.hasChunkAt(kennel) && level.getEntity(id) == null,
                "The remote dog's chunk never unloaded"))
        .thenExecute(() -> {
            var stopWatching = KennelReload.watch(level, remote, id, () -> reopened.set(true));
            h.testInfo.addListener(new GameTestListener() {
                @Override public void testStructureLoaded(GameTestInfo info) {}
                @Override public void testAddedForRerun(GameTestInfo original, GameTestInfo copy, GameTestRunner runner) {}
                @Override public void testPassed(GameTestInfo info, GameTestRunner runner) { stopWatching.run(); }
                @Override public void testFailed(GameTestInfo info, GameTestRunner runner) { stopWatching.run(); }
            });
            whistle(owner, true);
            h.assertTrue(!reopened.get(), "The owner must leave before the remote entity is loaded");
            disconnect(owner);
        })
        .thenWaitUntil(() -> h.assertTrue(reopened.get() && level.getEntity(id) == null,
                "Remote dismissal never read and put away the unloaded dog"))
        .thenExecute(() -> tickets.addTicketWithRadius(TicketType.FORCED, remote, 2))
        .thenWaitUntil(() -> h.assertTrue(level.areEntitiesActuallyLoadedAndTicking(remote), "Test visit did not reopen kennel"))
        .thenExecuteAfter(30, () -> {
            h.assertTrue(level.getEntity(id) == null, "Reopening the kennel left a dismissed dog physically behind");
            returning[0] = CompanionSafetyTests.reconnect(h, owner);
            // GameTest's embedded player does not restore inventory on reconnect. Obtain a real replacement.
            command(returning[0], "frenchie whistle");
            var inventory = returning[0].getInventory();
            for (int slot = 0; slot < 9; slot++) {
                if (inventory.getItem(slot).is(FrenchieRegistry.FRENCHIE_WHISTLE.get())) inventory.setSelectedSlot(slot);
            }
            h.assertTrue(returning[0].getMainHandItem().is(FrenchieRegistry.FRENCHIE_WHISTLE.get()),
                    "Reconnected owner could not obtain a whistle for the remotely dismissed dog");
            whistle(returning[0], false);
        }).thenWaitUntil(() -> near(h, returning[0], id))
        .thenExecute(() -> {
            var restored = near(h, returning[0], id);
            h.assertTrue(ItemStack.matches(toy, restored.getOffhandItem())
                            && level.getEntities(FrenchieRegistry.FRENCHIE.get(), d -> d.isOwnedBy(returning[0])).size() == 1,
                    "Distant dismissal recreated the original or its named toy on return");
            tickets.removeTicketWithRadius(TicketType.FORCED, remote, 2);
            disconnect(returning[0]);
        }).thenSucceed();
    }

    static void boatOrPlay(GameTestHelper h, boolean boat, boolean dismiss) {
        var owner = player(h, 4.5, 4.5);
        var dog = dog(h, owner, 5, 5);
        dog.standUp();
        var id = dog.getUUID();
        var sequence = h.startSequence();
        if (boat) {
            var vessel = h.spawn(EntityType.OAK_BOAT, 5, 1, 6);
            owner.interactOn(vessel, HAND, Vec3.ZERO);
            sequence.thenWaitUntil(() -> h.assertTrue(dog.getVehicle() == vessel, "Frenchie did not board owner's boat"));
        } else {
            sequence.thenWaitUntil(() -> h.assertTrue(dog.onGround(), "Dog has not landed")).thenExecute(dog::tryCelebrate)
                    .thenWaitUntil(() -> h.assertTrue(dog.celebrationStage() != 0, "Dog did not begin play"));
        }
        sequence.thenExecute(() -> {
            equip(owner);
            whistle(owner, dismiss);
        });
        if (dismiss) sequence.thenWaitUntil(() -> h.assertTrue(h.getLevel().getEntity(id) == null, "Playing/riding dog failed to dismiss"))
                .thenExecute(() -> { owner.stopRiding(); whistle(owner, false); });
        sequence.thenWaitUntil(() -> {
            var current = near(h, owner, id);
            h.assertTrue(current.celebrationStage() == 0 && !current.isPassenger(), "Call left play or boat travel active");
        }).thenExecute(() -> {
            owner.stopRiding();
            h.assertTrue(h.getLevel().getEntities(FrenchieRegistry.FRENCHIE.get(), d -> d.isOwnedBy(owner)).size() == 1,
                    "Interrupting play or riding duplicated the dog");
            disconnect(owner);
        }).thenSucceed();
    }
}
