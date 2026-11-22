package net.j40climb.frenchies.test;

import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.j40climb.frenchies.common.entity.frenchie.frontpack.FrontpackEvents;
import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.j40climb.frenchies.common.entity.frenchie.frontpack.FrontpackAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.wolf.WolfSoundVariants;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Optional;
import java.util.Set;

final class FrenchieDeliveryGameTests {
    private static final InteractionHand HAND = InteractionHand.MAIN_HAND;

    static void unequipPackedCourier(GameTestHelper h) {
        var owner = GameTestPlayers.connect(h);
        var recipient = GameTestPlayers.connect(h);
        var level = h.getLevel();
        var dog = h.spawnWithNoFreeWill(FrenchieRegistry.FRENCHIE.get(), 2, 1, 2);
        var parcel = prepareDelivery(owner, recipient, dog).copy();
        dog.setCustomName(Component.literal("Pip"));
        var id = dog.getUUID();
        var feet = owner.blockPosition();
        platform(h, feet);
        owner.snapTo(feet, 0, 0);
        // Suspend the player above the arena. There is no safe footing in any direction
        // until we build the platform under them.
        var release = feet.above(12);
        var chest = namedPack("Pip's carrier", 0x9E3145);
        var offhand = namedPack("Spare carrier", 0x497487);
        owner.setItemSlot(EquipmentSlot.CHEST, chest.copy());
        owner.setItemInHand(InteractionHand.OFF_HAND, offhand.copy());

        // The embedded GameTest connection has no network tick loop to drive ServerPlayer.doTick().
        h.onEachTick(owner::doTick);
        h.startSequence().thenExecuteAfter(4, () -> {
            confirm(owner, recipient, dog);
            h.assertTrue(dog.isDelivering() && owner.getMainHandItem().isEmpty(), "Courier did not accept the marked parcel");
            owner.setShiftKeyDown(true);
            click(owner, dog);
            h.assertTrue(FrontpackAttachments.pack(owner).hasCarriedFrenchie() && dog.isRemoved(), "Courier was not packed");
            owner.inventoryMenu.clicked(InventoryMenu.SHIELD_SLOT, 2, ContainerInput.SWAP, owner);
        }).thenExecuteAfter(8, () -> {
            h.assertTrue(FrontpackAttachments.pack(owner).hasCarriedFrenchie() && level.getEntity(id) == null
                            && ItemStack.matches(chest, owner.getItemBySlot(EquipmentSlot.CHEST))
                            && ItemStack.matches(offhand, owner.getInventory().getItem(2)) && owner.getOffhandItem().isEmpty(),
                    "Removing the spare pack changed custody while the chest pack was still equipped");
            owner.setNoGravity(true);
            owner.snapTo(release, 0, 0);
            owner.setDeltaMovement(Vec3.ZERO);
            owner.inventoryMenu.clicked(InventoryMenu.SHIELD_SLOT, 2, ContainerInput.SWAP, owner);
            owner.inventoryMenu.clicked(InventoryMenu.ARMOR_SLOT_START + 1, 1, ContainerInput.SWAP, owner);
        }).thenExecuteAfter(15, () -> {
            h.assertTrue(FrontpackAttachments.pack(owner).hasCarriedFrenchie() && FrontpackAttachments.pack(owner).releasePending()
                            && level.getEntity(id) == null,
                    "A blocked release lost the carried dog or spawned it in midair");
            h.assertTrue(owner.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
                            && ItemStack.matches(chest, owner.getInventory().getItem(1))
                            && ItemStack.matches(offhand, owner.getOffhandItem()),
                    "Blocked release changed the removed chest pack");
            platform(h, release);
        }).thenWaitUntil(() -> h.assertTrue(!FrontpackAttachments.pack(owner).hasCarriedFrenchie() && level.getEntity(id) instanceof FrenchieEntity,
                "Clearing the obstruction did not release the original dog")).thenExecute(() -> {
            var restored = (FrenchieEntity) level.getEntity(id);
            h.assertTrue(restored.isOwnedBy(owner) && restored.getName().getString().equals("Pip") && restored.isDelivering(),
                    "Released courier lost its owner, name or parcel");
            owner.setShiftKeyDown(false);
            click(owner, restored);
        }).thenExecuteAfter(12, () -> {
            h.assertTrue(level.getEntitiesOfClass(FrenchieEntity.class, new AABB(release).inflate(5), d -> d.isOwnedBy(owner)).size() == 1
                            && !FrontpackAttachments.pack(owner).hasCarriedFrenchie() && ItemStack.matches(parcel, owner.getMainHandItem())
                            && owner.getInventory().countItem(Items.BUNDLE) == 1 && recipient.getInventory().countItem(Items.BUNDLE) == 0,
                    "Repeated release lost or duplicated the courier or its named bundle and contents");
            h.assertTrue(owner.getInventory().countItem(FrenchieRegistry.FRONT_PACK.get()) == 2
                            && ItemStack.matches(chest, owner.getInventory().getItem(1))
                            && ItemStack.matches(offhand, owner.getOffhandItem()),
                    "Release must retain the stowed chest pack and held spare, with the original name and dye");
            disconnect(recipient);
            disconnect(owner);
        }).thenSucceed();
    }

    private static ItemStack namedPack(String name, int color) {
        var stack = new ItemStack(FrenchieRegistry.FRONT_PACK.get());
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(color));
        return stack;
    }

    static void compassOrders(GameTestHelper h) {
        var owner = GameTestPlayers.connect(h);
        try {
            var dog = h.spawnWithNoFreeWill(FrenchieRegistry.FRENCHIE.get(), 1, 1, 1);
            owner.snapTo(dog.position(), 0, 0);
            owner.setGameMode(GameType.SURVIVAL);
            owner.setItemInHand(HAND, new ItemStack(Items.BONE));
            click(owner, dog);
            h.assertTrue(dog.isOwnedBy(owner) && dog.isOrderedToSit() && owner.getInventory().countItem(Items.BONE) == 0
                            && owner.getInventory().countItem(FrenchieRegistry.FRENCHIE_WHISTLE.get()) == 1,
                    "Using a bone should consume it, tame and sit the dog, and grant its first whistle");
            owner.setItemInHand(HAND, new ItemStack(Items.GLOW_BERRIES));
            click(owner, dog);
            h.assertTrue(dog.isInLove() && owner.getMainHandItem().isEmpty() && dog.isOrderedToSit(),
                    "Glow berries should feed the dog and preserve the sit order");
            var compass = new ItemStack(Items.COMPASS);
            owner.setItemInHand(HAND, compass);
            owner.setLastDeathLocation(Optional.empty());
            click(owner, dog);
            h.assertTrue(!dog.isDeathTracking() && dog.isOrderedToSit(), "No recorded death changed the sit order");
            owner.setLastDeathLocation(Optional.of(GlobalPos.of(owner.level().dimension(), dog.blockPosition().east(12))));
            click(owner, dog);
            h.assertTrue(dog.isDeathTracking() && !dog.isOrderedToSit() && owner.getMainHandItem() == compass,
                    "Compass interaction should start tracking without taking the compass");
            var parcel = parcel();
            owner.setItemInHand(HAND, parcel);
            click(owner, dog);
            h.assertTrue(dog.isDeathTracking() && !dog.isDelivering() && owner.getMainHandItem() == parcel,
                    "The bundle interrupted the active trail");
            owner.setItemInHand(HAND, ItemStack.EMPTY);
            click(owner, dog);
            h.assertTrue(!dog.isDeathTracking() && dog.isOrderedToSit(), "Empty hand should cancel tracking and sit");
        } finally {
            disconnect(owner);
        }
        h.succeed();
    }

    static void creativeParcel(GameTestHelper h) {
        var sender = GameTestPlayers.connect(h);
        var recipient = GameTestPlayers.connect(h);
        try {
            var dog = h.spawn(FrenchieRegistry.FRENCHIE.get(), 1, 1, 1);
            var expected = prepareDelivery(sender, recipient, dog).copy();
            sender.setGameMode(GameType.CREATIVE);
            confirm(sender, recipient, dog);
            h.assertTrue(dog.isDelivering() && sender.getMainHandItem().isEmpty(),
                    "Creative confirmation must transfer the exact bundle to the courier");
            click(sender, dog);
            h.assertTrue(!dog.isDelivering() && ItemStack.matches(expected, sender.getMainHandItem())
                            && sender.getInventory().countItem(Items.BUNDLE) == 1,
                    "Canceling a creative delivery must return the original contents exactly once");
        } finally {
            disconnect(recipient);
            disconnect(sender);
        }
        h.succeed();
    }

    static void packAndCancel(GameTestHelper h) {
        var owner = GameTestPlayers.connect(h);
        var recipient = GameTestPlayers.connect(h);
        try {
            var dog = h.spawn(FrenchieRegistry.FRENCHIE.get(), 5, 1, 5);
            var expected = prepareDelivery(owner, recipient, dog);
            String target = recipient.getStringUUID();
            order(owner, dog, "send " + target);
            h.assertTrue(!dog.isDelivering() && owner.getMainHandItem() == expected, "Unsolicited command took cargo");
            click(owner, dog);
            order(owner, dog, "choose " + target);
            h.assertTrue(!dog.isDelivering() && owner.getMainHandItem() == expected, "Preview took cargo");
            owner.setItemInHand(HAND, new ItemStack(Items.BUNDLE));
            order(owner, dog, "send " + target);
            h.assertTrue(!dog.isDelivering() && owner.getMainHandItem().is(Items.BUNDLE),
                    "A confirmation for the full bundle accepted a different bundle");
            owner.setItemInHand(HAND, expected.copy());
            confirm(owner, recipient, dog);
            h.assertTrue(dog.isDelivering() && owner.getMainHandItem().isEmpty(), "Confirmation did not transfer custody");

            owner.setItemInHand(HAND, new ItemStack(Items.COMPASS));
            owner.setLastDeathLocation(Optional.of(GlobalPos.of(owner.level().dimension(), dog.blockPosition().north(7))));
            click(owner, dog);
            h.assertTrue(dog.isDelivering() && !dog.isDeathTracking(), "Compass interrupted the active delivery");

            owner.setItemInHand(HAND, ItemStack.EMPTY);
            owner.setItemSlot(EquipmentSlot.CHEST, new ItemStack(FrenchieRegistry.FRONT_PACK.get()));
            owner.setShiftKeyDown(true);
            click(owner, dog);

            h.assertTrue(FrontpackAttachments.pack(owner).hasCarriedFrenchie() && dog.isRemoved(), "Pickup left a second courier in the world");
            var release = BlockPos.containing(owner.position().add(0, 0, 2));
            h.getLevel().setBlockAndUpdate(release.below(), Blocks.STONE.defaultBlockState());
            h.getLevel().setBlockAndUpdate(release, Blocks.AIR.defaultBlockState());
            FrontpackEvents.putDown(owner);
            owner.setShiftKeyDown(false);
            var restored = (FrenchieEntity) h.getLevel().getEntity(dog.getUUID());
            h.assertTrue(restored != null && restored.isDelivering() && !FrontpackAttachments.pack(owner).hasCarriedFrenchie(),
                    "Release lost the delivery or left a second copy in the pack");
            click(owner, restored);
            h.assertTrue(!restored.isDelivering() && restored.isOrderedToSit()
                            && ItemStack.isSameItemSameComponents(expected, owner.getMainHandItem()),
                    "Cancel after packing did not return the original parcel and sit");
            order(owner, restored, "send " + target);
            h.assertTrue(!restored.isDelivering() && owner.getInventory().countItem(Items.BUNDLE) == 1
                            && recipient.getInventory().countItem(Items.BUNDLE) == 0, "Repeated confirmation duplicated the parcel");
        } finally {
            disconnect(recipient);
            disconnect(owner);
        }
        h.succeed();
    }

    static void fullRecipient(GameTestHelper h) {
        var owner = GameTestPlayers.connect(h);
        var recipient = GameTestPlayers.connect(h);
        var stranger = GameTestPlayers.connect(h);
        var dog = h.spawn(FrenchieRegistry.FRENCHIE.get(), 3, 16, 2);
        var id = dog.getUUID();
        var level = h.getLevel();
        // Keep the owner, both recipients' positions and their landing pads in the same
        // test-loaded chunk. Adjacent terrain may still be generating in accelerated CI.
        BlockPos feet = ChunkPos.containing(dog.blockPosition()).getMiddleBlockPosition(dog.getBlockY());
        for (var pos : BlockPos.betweenClosed(feet.offset(-7, -1, -7), feet.offset(7, -1, 7))) {
            level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        }
        dog.snapTo(feet, 0, 0);
        var expected = prepareDelivery(owner, recipient, dog).copy();
        var start = dog.position();
        // The 96-block height difference requires a timed trip without a walkable connection.
        BlockPos address = feet.above(96).east(4);
        BlockPos moved = feet.above(96).west(4);
        platform(h, address);
        platform(h, moved);
        var bark = level.registryAccess().lookupOrThrow(Registries.WOLF_SOUND_VARIANT)
                .getOrThrow(WolfSoundVariants.PUGLIN).value().adultSounds().ambientSound();
        //? if <26.3 {
        var barkPosition = moved.getBottomCenter();
        //?} else {
        /*var barkPosition = Vec3.atBottomCenterOf(moved);
        *///?}
        var barks = GameTestEffects.sounds(recipient, bark.value(), barkPosition, 36);
        recipient.snapTo(address, 0, 0);
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            recipient.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
        }
        confirm(owner, recipient, dog);
        h.assertTrue(dog.isDelivering() && owner.getMainHandItem().isEmpty(),
                "The chooser should accept a player 96 blocks away");
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(!dog.isRemoved() && dog.position().distanceToSqr(start) >= 1,
                        "The dog never walked away from the owner"))
                .thenWaitUntil(() -> h.assertTrue(level.getEntity(id) == null, "Courier never departed"))
                .thenExecute(() -> recipient.snapTo(moved, 0, 0))
                // Even after a short walk-off, this distance needs substantially more than the three-second base time.
                .thenExecuteFor(120, () -> h.assertTrue(level.getEntity(id) == null
                        && recipient.getInventory().countItem(Items.BUNDLE) == 0,
                        "Courier arrived before the distant destination's timer"))
                .thenWaitUntil(() -> h.assertTrue(level.getEntity(id) instanceof FrenchieEntity arrived
                                && arrived.isDelivering() && arrived.distanceToSqr(recipient) < 36,
                        "Original Frenchie did not arrive at the recipient's new position"))
                .thenExecuteAfter(25, () -> {
                    var arrived = (FrenchieEntity) level.getEntity(id);
                    barks.close();
                    h.assertTrue(barks.count() == 1, "The arriving courier should greet the recipient with a single bark");
                    arrived = CompanionTravelTests.reload(h, arrived);
                    h.assertTrue(arrived.handoffPhase() == 1 && ItemStack.matches(arrived.getParcel(), expected),
                            "Reload did not preserve the waiting signature and its exact parcel");
                    click(recipient, arrived);
                    h.assertTrue(recipient.getInventory().countItem(Items.BUNDLE) == 0 && arrived.isDelivering(),
                            "A full inventory must leave the original parcel on the dog");
                    stranger.snapTo(arrived.position(), 0, 0);
                    click(stranger, arrived);
                    h.assertTrue(stranger.getInventory().countItem(Items.BUNDLE) == 0 && arrived.isDelivering(),
                            "A visitor should not be able to collect or recall someone else's delivery");
                    recipient.getInventory().setItem(7, ItemStack.EMPTY);
                })
                .thenExecuteAfter(25, () -> {
                    var arrived = (FrenchieEntity) level.getEntity(id);
                    h.assertTrue(arrived != null && arrived.isDelivering() && recipient.getInventory().getItem(7).isEmpty(),
                            "Making room must not accept the parcel automatically");
                    var held = recipient.getMainHandItem().copy();
                    click(recipient, arrived);
                    click(recipient, arrived);
                    h.assertTrue(arrived.handoffPhase() == 2 && arrived.getParcel().isEmpty(),
                            "Expected no cargo and an optional tip window after the recipient signed");
                    h.assertTrue(ItemStack.matches(recipient.getMainHandItem(), held), "Acceptance consumed the held item");
                })
                .thenWaitUntil(() -> h.assertTrue(ItemStack.matches(recipient.getInventory().getItem(7), expected)
                                && recipient.getInventory().countItem(Items.BUNDLE) == 1,
                        "A free slot should receive the original filled bundle exactly once"))
                .thenWaitUntil(() -> h.assertTrue(level.getEntity(id) == null, "Courier never departed for home"))
                .thenExecuteFor(80, () -> h.assertTrue(level.getEntity(id) == null, "Return journey skipped the timer"))
                .thenWaitUntil(() -> h.assertTrue(level.getEntity(id) instanceof FrenchieEntity returned
                                && !returned.isDelivering() && returned.isOwnedBy(owner) && returned.distanceToSqr(owner) < 36,
                        "Expected the courier beside its owner at " + owner.blockPosition() + ", found " + level.getEntity(id)))
                .thenExecute(() -> {
                    h.assertTrue(recipient.getInventory().countItem(Items.BUNDLE) == 1
                                    && owner.getInventory().countItem(Items.BUNDLE) == 0,
                            "Returning the dog must not transfer the delivered bundle again");
                    disconnect(recipient);
                    disconnect(stranger);
                    disconnect(owner);
                }).thenSucceed();
    }

    static void disconnectedRecipient(GameTestHelper h) {
        var owner = GameTestPlayers.connect(h);
        var recipient = GameTestPlayers.connect(h);
        var dog = h.spawn(FrenchieRegistry.FRENCHIE.get(), 2, 1, 2);
        var expected = prepareDelivery(owner, recipient, dog).copy();
        var id = dog.getUUID();
        confirm(owner, recipient, dog);
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(h.getLevel().getEntity(id) == null, "Courier never entered transit"))
                .thenExecute(() -> disconnect(recipient))
                .thenWaitUntil(() -> h.assertTrue(h.getLevel().getEntity(id) instanceof FrenchieEntity returned
                                && returned.isOwnedBy(owner) && returned.distanceToSqr(owner) < 36,
                        "A disconnect in transit should send the original courier back"))
                .thenExecute(() -> {
                    var returned = (FrenchieEntity) h.getLevel().getEntity(id);
                    if (returned.isDelivering()) click(owner, returned);
                    h.assertTrue(owner.getInventory().countItem(Items.BUNDLE) == 1
                                    && ItemStack.matches(owner.getMainHandItem(), expected)
                                    && !returned.isDelivering(),
                            "Missing recipient lost the parcel or duplicated the handoff");
                    disconnect(owner);
                }).thenSucceed();
    }

    static void crossDimension(GameTestHelper h) {
        var owner = GameTestPlayers.connect(h);
        var recipient = GameTestPlayers.connect(h);
        var dog = h.spawn(FrenchieRegistry.FRENCHIE.get(), 2, 1, 2);
        var expected = prepareDelivery(owner, recipient, dog).copy();
        var id = dog.getUUID();
        var level = h.getLevel();
        var nether = level.getServer().getLevel(Level.NETHER);
        h.assertTrue(nether != null, "The test world needs a Nether");
        var address = BlockPos.containing(dog.getX() / 8, 132, dog.getZ() / 8);
        var chunk = ChunkPos.containing(address);
        // Mock players do not acknowledge terrain loading like a game client.
        nether.getChunkSource().addTicketWithRadius(TicketType.FORCED, chunk, 2);
        for (var pos : BlockPos.betweenClosed(address.offset(-2, -1, -2), address.offset(2, -1, 2))) {
            nether.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        }
        h.assertTrue(recipient.teleportTo(nether, address.getX() + 0.5, address.getY(), address.getZ() + 0.5,
                Set.of(), 0, 0, true), "The recipient could not enter the Nether");
        // Keep the parcel on the dog at arrival so the original UUID can be observed in both dimensions.
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            recipient.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
        }
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(nether.areEntitiesActuallyLoadedAndTicking(chunk),
                        "The recipient did not finish entering the Nether"))
                .thenExecute(() -> {
                    confirm(owner, recipient, dog);
                    h.assertTrue(dog.isDelivering() && owner.getMainHandItem().isEmpty(),
                            "The chooser did not accept a player in the Nether");
                })
                .thenWaitUntil(() -> h.assertTrue(level.getEntity(id) == null
                                && nether.getEntity(id) instanceof FrenchieEntity arrived
                                && arrived.isDelivering() && arrived.distanceToSqr(recipient) < 36,
                        "The courier never appeared beside its Nether recipient"))
                .thenExecute(() -> {
                    recipient.getInventory().setItem(7, ItemStack.EMPTY);
                    var held = new ItemStack(Items.GLOW_BERRIES, 3);
                    recipient.setItemInHand(InteractionHand.OFF_HAND, held);
                    recipient.interactOn(nether.getEntity(id), InteractionHand.OFF_HAND, Vec3.ZERO);
                    h.assertTrue(held.getCount() == 3, "Off-hand acceptance fed the held berries to the dog");
                })
                .thenWaitUntil(() -> h.assertTrue(ItemStack.matches(recipient.getInventory().getItem(7), expected),
                        "Cross-dimension delivery lost the filled bundle"))
                .thenWaitUntil(() -> h.assertTrue(nether.getEntity(id) == null
                                && level.getEntity(id) instanceof FrenchieEntity returned && !returned.isDelivering()
                                && returned.isOwnedBy(owner) && returned.distanceToSqr(owner) < 36,
                        "The dog never returned from the Nether"))
                .thenExecute(() -> {
                    h.assertTrue(recipient.getInventory().countItem(Items.BUNDLE) == 1
                                    && owner.getInventory().countItem(Items.BUNDLE) == 0,
                            "Cross-dimension return duplicated the parcel");
                    nether.getChunkSource().removeTicketWithRadius(TicketType.FORCED, chunk, 2);
                    disconnect(recipient);
                    disconnect(owner);
                }).thenSucceed();
    }

    static void blockedDeparture(GameTestHelper h) {
        // A glass enclosure lets the owner click from inside but prevents the departure walk.
        for (BlockPos pos : BlockPos.betweenClosed(1, 0, 1, 5, 3, 5)) {
            boolean wall = pos.getX() == 1 || pos.getX() == 5 || pos.getZ() == 1 || pos.getZ() == 5;
            h.setBlock(pos, wall || pos.getY() == 0 || pos.getY() == 3 ? Blocks.GLASS : Blocks.AIR);
        }
        var owner = GameTestPlayers.connect(h);
        var recipient = GameTestPlayers.connect(h);
        var dog = h.spawn(FrenchieRegistry.FRENCHIE.get(), 3, 1, 3);
        var expected = prepareDelivery(owner, recipient, dog).copy();
        var landing = dog.blockPosition().above(40);
        platform(h, landing);
        recipient.snapTo(landing, 0, 0);
        var id = dog.getUUID();
        confirm(owner, recipient, dog);
        h.runAfterDelay(100, () -> h.assertTrue(h.getLevel().getEntity(id) == null,
                "An enclosed dog must be able to leave without finding a route"));
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(h.getLevel().getEntity(id) instanceof FrenchieEntity waiting
                                && waiting.distanceToSqr(recipient) < 16 && waiting.isDelivering()
                                && recipient.getInventory().countItem(Items.BUNDLE) == 0,
                        "The original dog must appear in front of the recipient before the bundle moves"))
                .thenExecute(() -> click(recipient, (FrenchieEntity) h.getLevel().getEntity(id)))
                .thenWaitUntil(() -> h.assertTrue(ItemStack.matches(recipient.getMainHandItem(), expected),
                        "Courier never arrived across the blocked departure route"))
                .thenWaitUntil(() -> h.assertTrue(h.getLevel().getEntity(id) instanceof FrenchieEntity returned
                                && !returned.isDelivering() && returned.distanceToSqr(owner) < 36,
                        "Courier never returned after completing its blocked-route delivery"))
                .thenExecute(() -> {
                    h.assertTrue(recipient.getInventory().countItem(Items.BUNDLE) == 1
                                    && owner.getInventory().countItem(Items.BUNDLE) == 0,
                            "Blocked departure must produce one completed delivery");
                    disconnect(recipient);
                    disconnect(owner);
                }).thenSucceed();
    }

    static void unloadedHomeBed(GameTestHelper h) {
        var level = h.getLevel();
        var owner = GameTestPlayers.connect(h);
        var dog = h.spawn(FrenchieRegistry.FRENCHIE.get(), 2, 1, 2);
        var id = dog.getUUID();
        BlockPos start = dog.blockPosition();
        // Keep both the bed and its landing floor in a previously untouched chunk.
        BlockPos bed = ChunkPos.containing(start.east(640)).getMiddleBlockPosition(start.getY() + 12);
        var chunk = ChunkPos.containing(bed);
        prepareRemoteBed(h, bed);
        owner.getInventory().clearContent();
        owner.setItemInHand(HAND, parcel());
        owner.setGameMode(GameType.SURVIVAL);
        dog.tame(owner);
        dog.sitDown();
        var respawn = LevelData.RespawnData.of(level.dimension(), bed, 0, 0);
        owner.setRespawnPosition(new ServerPlayer.RespawnConfig(respawn, false), false);
        owner.snapTo(dog.position().add(0, 0, -1), 0, 0);
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(!level.hasChunkAt(bed), "Bed chunk never unloaded"))
                .thenExecute(() -> {
                    click(owner, dog);
                    h.assertTrue(owner.getMainHandItem().is(Items.BUNDLE), "Preview took the held bundle");
                    order(owner, dog, "choose home");
                    owner.setRespawnPosition(null, false);
                    order(owner, dog, "send home");
                    h.assertTrue(!dog.hasCompanionJob() && ItemStack.matches(owner.getMainHandItem(), parcel()),
                            "Clearing the remote bed between preview and confirmation consumed the bundle");
                    owner.setRespawnPosition(new ServerPlayer.RespawnConfig(respawn, false), false);
                    click(owner, dog);
                    order(owner, dog, "choose home");
                    order(owner, dog, "send home");
                    h.assertTrue(dog.isDelivering() && owner.getMainHandItem().isEmpty(),
                            "The owner could not confirm an unloaded bed");
                    owner.setRespawnPosition(null, false);
                })
                .thenWaitUntil(() -> h.assertTrue(level.getEntity(id) == null, "Bed delivery never departed"))
                .thenExecuteAfter(200, () -> h.assertTrue(!level.hasChunkAt(bed) && level.getEntity(id) == null,
                        "The dog or destination chunk was simulated during long-distance transit"))
                .thenExecuteAfter(800, () -> {
                    // The owner visits after arrival, when the bed and waiting courier may have unloaded again.
                    level.getChunkSource().addTicketWithRadius(TicketType.PLAYER_SPAWN, chunk, 2);
                    level.getChunkAt(bed);
                    owner.snapTo(bed.north(2), 0, 0);
                })
                .thenWaitUntil(() -> h.assertTrue(level.getEntity(id) instanceof FrenchieEntity waiting
                                //? if <26.3 {
                                && waiting.isDelivering() && waiting.position().distanceToSqr(bed.getBottomCenter()) < 36,
                                //?} else {
                                /*&& waiting.isDelivering() && waiting.position().distanceToSqr(Vec3.atBottomCenterOf(bed)) < 36,
                                *///?}
                        "No waiting courier at the distant bed after the owner's visit"))
                .thenExecute(() -> {
                    var waiting = (FrenchieEntity) level.getEntity(id);
                    owner.snapTo(waiting.position().add(1, 0, 0), 0, 0);
                    click(owner, waiting);
                    h.assertTrue(!waiting.isDelivering() && waiting.isOrderedToSit()
                                    && ItemStack.matches(parcel(), owner.getMainHandItem())
                                    && owner.getInventory().countItem(Items.BUNDLE) == 1,
                            "The distant-bed delivery should return the original parcel once");
                    level.getChunkSource().removeTicketWithRadius(TicketType.PLAYER_SPAWN, chunk, 2);
                    disconnect(owner);
                }).thenSucceed();
    }

    static void legacyBedPickup(GameTestHelper h) {
        var owner = GameTestPlayers.connect(h);
        try {
            var level = h.getLevel();
            var dog = h.spawn(FrenchieRegistry.FRENCHIE.get(), 3, 1, 2);
            owner.getInventory().clearContent();
            dog.tame(owner);
            dog.standUp();
            var id = dog.getUUID();
            BlockPos bed = dog.blockPosition().east();
            var bedState = Blocks.BLUE_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.EAST);
            level.setBlock(bed.west(), bedState.setValue(BedBlock.PART, BedPart.FOOT), Block.UPDATE_CLIENTS);
            level.setBlock(bed, bedState.setValue(BedBlock.PART, BedPart.HEAD), Block.UPDATE_CLIENTS);
            dog.snapTo(bed.north(), 0, 0);
            var expected = parcel();
            var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
            dog.saveWithoutId(output);
            output.store("FrenchieHomeBed", GlobalPos.CODEC, GlobalPos.of(level.dimension(), bed));
            output.store("FrenchieParcel", ItemStack.CODEC, expected);
            output.putBoolean("FrenchieAtHandoff", true);
            var restored = FrenchieRegistry.FRENCHIE.get().create(level, EntitySpawnReason.LOAD);
            h.assertTrue(restored != null, "Could not create legacy courier");
            dog.discard();
            restored.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), output.buildResult()));
            h.assertTrue(level.addFreshEntity(restored) && id.equals(restored.getUUID()) && restored.isDelivering(),
                    "Loading a pre-timer bed delivery lost the courier or bundle");
            owner.snapTo(restored.position().add(0, 0, -1), 0, 0);
            click(owner, restored);
            h.assertTrue(!restored.isDelivering() && restored.isOrderedToSit()
                            && ItemStack.matches(expected, owner.getMainHandItem()) && owner.getInventory().countItem(Items.BUNDLE) == 1,
                    "The owner should still collect exactly one legacy parcel from the waiting dog");
        } finally {
            disconnect(owner);
        }
        h.succeed();
    }

    static void prepareRemoteBed(GameTestHelper h, BlockPos bed) {
        var level = h.getLevel();
        // Entity ticking needs a fully generated two-chunk border. Build that terrain before the
        // accelerated test starts so background world generation does not consume its tick budget.
        for (var nearby : ChunkPos.rangeClosed(ChunkPos.containing(bed), 2).toList()) {
            level.getChunk(nearby.x(), nearby.z());
        }
        platform(h, bed);
        for (var pos : BlockPos.betweenClosed(bed.offset(-2, 0, -2), bed.offset(2, 1, 2))) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
        var bedState = Blocks.BLUE_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.EAST);
        level.setBlock(bed.west(), bedState.setValue(BedBlock.PART, BedPart.FOOT), Block.UPDATE_CLIENTS);
        level.setBlock(bed, bedState.setValue(BedBlock.PART, BedPart.HEAD), Block.UPDATE_CLIENTS);
    }

    private static void platform(GameTestHelper h, BlockPos feet) {
        for (var pos : BlockPos.betweenClosed(feet.offset(-3, -1, -3), feet.offset(3, -1, 3))) {
            h.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        }
    }

    private static ItemStack prepareDelivery(ServerPlayer owner, ServerPlayer recipient, FrenchieEntity dog) {
        dog.tame(owner);
        dog.sitDown();
        owner.setGameMode(GameType.SURVIVAL);
        owner.snapTo(dog.position().add(0, 0, -1), 0, 0);
        recipient.snapTo(dog.position().add(2, 0, 0), 0, 0);
        owner.getInventory().clearContent();
        recipient.getInventory().clearContent();
        var parcel = parcel();
        owner.setItemInHand(HAND, parcel);
        return parcel;
    }

    static ItemStack parcel() {
        var stack = new ItemStack(Items.BUNDLE);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Violet's delivery"));
        stack.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(List.of(new ItemStackTemplate(Items.AMETHYST_SHARD, 7))));
        return stack;
    }

    static void confirm(ServerPlayer owner, ServerPlayer recipient, FrenchieEntity dog) {
        click(owner, dog);
        order(owner, dog, "choose " + recipient.getStringUUID());
        order(owner, dog, "send " + recipient.getStringUUID());
    }

    private static void click(ServerPlayer player, FrenchieEntity dog) {
        GameTestPlayers.interact(player, dog, HAND);
    }

    private static void order(ServerPlayer owner, FrenchieEntity dog, String choice) {
        owner.level().getServer().getCommands().performPrefixedCommand(owner.createCommandSourceStack(),
                "frenchiedelivery " + dog.getStringUUID() + " " + choice);
    }

    private static void disconnect(ServerPlayer player) {
        player.level().getServer().getPlayerList().remove(player);
    }
}
