package net.j40climb.frenchies.test;

import java.util.List;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.j40climb.frenchies.common.entity.frenchie.frontpack.FrontpackAttachments;
import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
//? if <26.3 {
import net.minecraft.world.entity.EntityType;
//?} else {
/*import net.minecraft.world.entity.EntityTypes;
*///?}
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public final class FabricFrenchieTests {
    @GameTest(structure = "frenchies:companion_yard")
    public void hostileTargets(GameTestHelper helper) {
        var owner = helper.makeMockPlayer(GameType.SURVIVAL);
        var dog = helper.spawnWithNoFreeWill(FrenchieRegistry.FRENCHIE.get(), 3, 1, 3);
        var zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 6, 1, 3);
        var piglin = helper.spawnWithNoFreeWill(EntityType.PIGLIN, 6, 1, 6);
        var memory = MemoryModuleType.ATTACK_TARGET;
        zombie.setTarget(dog);
        piglin.getBrain().setMemory(memory, dog);
        helper.assertTrue(zombie.getTarget() == dog && piglin.getBrain().getMemory(memory).orElse(null) == dog,
                "Hostiles must still be able to target an untamed dog");
        dog.tame(owner);
        zombie.setTarget(dog);
        piglin.getBrain().setMemory(memory, dog);
        helper.assertTrue(zombie.getTarget() == null && piglin.getBrain().getMemory(memory).isEmpty(),
                "A tamed dog retained a direct or brain attack target");
        piglin.getBrain().setMemoryWithExpiry(memory, dog, 10);
        helper.assertTrue(piglin.getBrain().getMemory(memory).isEmpty(), "A timed attack memory accepted the companion");
        helper.succeed();
    }

    @GameTest(structure = "frenchies:companion_yard", maxTicks = 60)
    public void packedCourier(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = helper.makeMockServerPlayerInLevel();
        // Vanilla ignores damage until the client acknowledges loading the world.
        owner.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        var dog = helper.spawnWithNoFreeWill(FrenchieRegistry.FRENCHIE.get(), 5, 1, 5);
        owner.setGameMode(GameType.SURVIVAL);
        owner.snapTo(dog.position().add(0, 0, -1), 0, 0);
        owner.getInventory().clearContent();
        dog.tame(owner);
        var id = dog.getUUID();

        // A recalled courier keeps its parcel until the owner collects it.
        var parcel = new ItemStack(Items.BUNDLE);
        parcel.set(DataComponents.CUSTOM_NAME, Component.literal("Pip's courier parcel"));
        parcel.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(List.of(new ItemStackTemplate(Items.AMETHYST_SHARD, 7))));
        var saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        dog.saveWithoutId(saved);
        saved.store("FrenchieParcel", ItemStack.CODEC, parcel);
        saved.putBoolean("FrenchieParcelRetained", true);
        dog.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved.buildResult()));
        helper.assertTrue(ItemStack.matches(parcel, dog.getParcel()), "Fixture courier never received the named bundle");

        owner.setItemSlot(EquipmentSlot.CHEST, new ItemStack(FrenchieRegistry.FRONT_PACK.get()));
        owner.setShiftKeyDown(true);
        var result = UseEntityCallback.EVENT.invoker().interact(owner, level, InteractionHand.MAIN_HAND, dog, new EntityHitResult(dog));
        var pack = FrontpackAttachments.pack(owner);
        var carried = TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), pack.frenchieNBT());
        helper.assertTrue(result.consumesAction() && dog.isRemoved() && pack.hasCarriedFrenchie()
                        && ItemStack.matches(parcel, carried.read("FrenchieParcel", ItemStack.CODEC).orElse(ItemStack.EMPTY))
                        && owner.getInventory().countItem(Items.BUNDLE) == 0,
                "Fabric pickup must move the courier and its named, filled bundle into the pack");
        owner.setShiftKeyDown(false);
        owner.setRespawnPosition(new ServerPlayer.RespawnConfig(
                LevelData.RespawnData.of(level.dimension(), owner.blockPosition(), 0, 0), true), false);
        var rules = level.getGameRules();
        boolean previousKeepInventory = rules.get(GameRules.KEEP_INVENTORY);
        try {
            rules.set(GameRules.KEEP_INVENTORY, true, level.getServer());
            owner.kill(level);
            helper.assertTrue(!owner.isAlive(), "Fixture carrier survived the death");
            owner.connection.handleClientCommand(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
        } finally {
            rules.set(GameRules.KEEP_INVENTORY, previousKeepInventory, level.getServer());
        }
        var respawned = level.getServer().getPlayerList().getPlayer(owner.getUUID());
        helper.assertTrue(respawned != null && respawned != owner && respawned.isAlive() && respawned.level() == level
                        && respawned.getItemBySlot(EquipmentSlot.CHEST).is(FrenchieRegistry.FRONT_PACK.get()),
                "Respawn must replace the player and retain the worn front pack");
        respawned.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        // The roster processes recalls every five ticks. Check after it can consume a mistaken recall.
        helper.startSequence().thenExecuteAfter(10, () -> {
            var paused = FrontpackAttachments.pack(respawned);
            helper.assertTrue(paused.hasCarriedFrenchie() && paused.frenchieNBT().read("UUID", UUIDUtil.CODEC).filter(id::equals).isPresent()
                            && level.getServer().overworld().getEntityInAnyDimension(id) == null,
                    "Respawn recalled or lost the packed courier before its owner removed the pack");
            respawned.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        }).thenWaitUntil(() -> helper.assertTrue(level.getEntity(id) instanceof FrenchieEntity
                        && !FrontpackAttachments.pack(respawned).hasCarriedFrenchie(),
                "Removing the last pack never released the original courier"))
                .thenExecute(() -> {
                    var returned = (FrenchieEntity) level.getEntity(id);
                    helper.assertTrue(returned.isOwnedBy(respawned) && ItemStack.matches(parcel, returned.getParcel()),
                            "Released courier lost its owner or parcel components");
                    respawned.interactOn(returned, InteractionHand.MAIN_HAND, Vec3.ZERO);
                    helper.assertTrue(ItemStack.matches(parcel, respawned.getMainHandItem()) && returned.getParcel().isEmpty()
                                    && respawned.getInventory().countItem(Items.BUNDLE) == 1,
                            "Collecting the released courier's parcel must return its contents exactly once");
                    level.getServer().getPlayerList().remove(respawned);
                }).thenSucceed();
    }
}
