package net.j40climb.frenchies.common.entity.frenchie.frontpack;

import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.j40climb.frenchies.common.entity.frenchie.delivery.CourierTrips;
import net.j40climb.frenchies.common.entity.frenchie.recall.CompanionRoster;
import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Event handler for Frenchie frontpack pickup/putdown interactions.
 *
 * Pickup: Shift-right-click on tamed Frenchie (owned by you)
 * Put down: Shift-right-click air, or remove the last equipped front pack.
 */
public class FrontpackEvents {

    /**
     * Handle shift-right-click on Frenchie to pick it up.
     */
    public static InteractionResult interact(Player player, Entity target, InteractionHand hand) {
        if (player.getItemInHand(hand).is(FrenchieRegistry.FRENCHIE_WHISTLE.get())
                || player.getMainHandItem().isEmpty() && player.getOffhandItem().is(FrenchieRegistry.FRENCHIE_WHISTLE.get())) return InteractionResult.PASS;
        // Pickup conditions
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!(target instanceof FrenchieEntity frenchie)) return InteractionResult.PASS;
        if (!frenchie.isTame()) return InteractionResult.PASS;
        if (!frenchie.isOwnedBy(player)) return InteractionResult.PASS;
        var heldItem = player.getItemInHand(hand);
        if (frenchie.isBaby() || frenchie.isPassenger() || frenchie.isBellyRubPose()
                || heldItem.is(net.minecraft.world.item.Items.BONE)
                || heldItem.is(FrenchieRegistry.SQUEAKY_TOY.get())
                || heldItem.is(FrenchieRegistry.BUBBLE_TOY.get())) return InteractionResult.PASS;
        if (FrontpackEquipment.wornBy(player).isEmpty()) {
            var held = player.getItemInHand(hand);
            if (held.isEmpty() || held.is(FrontpackEquipment.PACKS)) {
                // Keep a failed pickup from toggling Sit or canceling the dog's current job.
                if (!player.level().isClientSide()) {
                    player.sendOverlayMessage(Component.translatable("frenchie.frenchies.frontpack.equip_first"));
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }

        // Check if already carrying a Frenchie
        FrontpackData currentData = FrontpackAttachments.pack(player);
        if (currentData.hasCarriedFrenchie()) return InteractionResult.PASS;

        // Client: just cancel event and return success
        if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        // Server: serialize Frenchie to NBT and store in attachment
        TagValueOutput output = TagValueOutput.createWithContext(
            ProblemReporter.DISCARDING,
            player.level().registryAccess()
        );
        CompanionRoster.packing(frenchie);
        frenchie.saveWithoutId(output);
        CompoundTag frenchieNBT = output.buildResult();

        FrontpackData newData = new FrontpackData(
            true,
            frenchieNBT,
            player.level().getGameTime()
        );

        FrontpackAttachments.setPack(player, newData);
        if (frenchie.hasCompanionJob()) player.sendOverlayMessage(Component.translatable("frenchie.frenchies.order.packed"));

        // Despawn the entity
        CourierTrips.forget(frenchie);
        frenchie.discard();

        // Play pickup sound
        player.playSound(SoundEvents.ITEM_PICKUP, 1.0f, 1.0f);

        return InteractionResult.SUCCESS;
    }

    public static void releaseWhenUnequipped(ServerPlayer player) {
        if (!player.isAlive()) return;
        var data = FrontpackAttachments.packOrNull(player);
        if (data == null || !data.hasCarriedFrenchie()) return;
        if (!FrontpackEquipment.wornBy(player).isEmpty()) {
            if (data.releasePending()) FrontpackAttachments.setPack(player, data.withReleasePending(false));
            return;
        }
        if (data.releasePending() && player.tickCount % 10 != 0) return;
        if (!releaseNearby(player, data) && !data.releasePending()) {
            FrontpackAttachments.setPack(player, data.withReleasePending(true));
            player.sendOverlayMessage(Component.translatable("frenchie.frenchies.frontpack.wait_for_ground"));
        }
    }

    private static boolean releaseNearby(ServerPlayer player, FrontpackData data) {
        var position = CourierTrips.findLanding(player.level(), player.blockPosition(), player);
        return position != null && releaseAt(player, player.level(), data, position);
    }

    public static void putDown(Player player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        FrontpackData data = FrontpackAttachments.pack(player);
        if (!data.hasCarriedFrenchie()) return;

        var type = FrenchieRegistry.FRENCHIE.get();
        // Use yaw for the ground target so looking down never puts the dog inside the floor.
        Vec3 forward = Vec3.directionFromRotation(0, player.getYRot());
        BlockPos target = BlockPos.containing(player.position().add(forward.scale(2)));
        if (!level.isInWorldBounds(target)) return;
        Vec3 position = DismountHelper.findSafeDismountLocation(type, level, target, true);
        if (position != null) releaseAt(player, level, data, position);
    }

    private static boolean releaseAt(Player player, ServerLevel level, FrontpackData data, Vec3 position) {
        var dog = FrenchieRegistry.FRENCHIE.get().create(level, EntitySpawnReason.TRIGGERED);
        if (dog == null) return false;
        dog.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), data.frenchieNBT()));
        dog.snapTo(position, player.getYRot(), 0);
        if (!level.noCollision(dog, dog.getBoundingBox()) || !level.addFreshEntity(dog)) return false;

        FrontpackAttachments.setPack(player, FrontpackData.DEFAULT);
        if (dog.hasCompanionJob()) player.sendOverlayMessage(Component.translatable("frenchie.frenchies.order.resumed"));
        player.playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1.0f, 1.0f);
        return true;
    }
}
