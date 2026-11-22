package net.j40climb.frenchies.common.entity.frenchie;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

public final class FrenchieCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("frenchie")
                .then(Commands.literal("whistle").executes(ctx -> CompanionRoster.replaceWhistle(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("select").executes(ctx -> CompanionRoster.list(ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("dog", UuidArgument.uuid()).executes(ctx ->
                                CompanionRoster.select(ctx.getSource().getPlayerOrException(),
                                        UuidArgument.getUuid(ctx, "dog")) ? 1 : 0)))
                .then(Commands.literal("home").executes(ctx -> {
                    var owner = ctx.getSource().getPlayerOrException();
                    var dog = nearbyDog(owner);
                    if (dog == null) {
                        DeliveryGoal.tell(owner, "no_nearby_dog");
                        return 0;
                    }
                    var bed = DeliveryDialog.ownerBed(owner);
                    if (bed == null) {
                        DeliveryGoal.tell(owner, "no_bed");
                        return 0;
                    }
                    return dog.sendHome(owner, bed) ? 1 : 0;
                }))
                .then(Commands.literal("recall").executes(ctx -> {
                    var owner = ctx.getSource().getPlayerOrException();
                    if (!DeliveryGoal.available(owner)) return 0;
                    int count = CourierTrips.recall(owner);
                    DeliveryGoal.tell(owner, count > 0 ? "recall_called" : "recall_none");
                    return count;
                })));
    }

    private static @Nullable FrenchieEntity nearbyDog(ServerPlayer owner) {
        if (!DeliveryGoal.available(owner)) return null;
        return owner.level().getEntitiesOfClass(FrenchieEntity.class, owner.getBoundingBox().inflate(12),
                dog -> dog.isAlive() && dog.isOwnedBy(owner) && !dog.hasCompanionJob() && owner.distanceToSqr(dog) <= 144)
                .stream().min(Comparator.comparingDouble(owner::distanceToSqr)).orElse(null);
    }

    public static InteractionResult useBed(ServerPlayer owner, InteractionHand hand, BlockPos pos) {
        if (!owner.isShiftKeyDown()
                || hand != InteractionHand.MAIN_HAND || !owner.getMainHandItem().isEmpty()) return InteractionResult.PASS;
        var state = owner.level().getBlockState(pos);
        if (!(state.getBlock() instanceof BedBlock)) return InteractionResult.PASS;
        if (state.getValue(BedBlock.PART) == BedPart.FOOT) pos = pos.relative(state.getValue(BedBlock.FACING));
        var bed = GlobalPos.of(owner.level().dimension(), pos);
        if (!bed.equals(DeliveryDialog.ownerBed(owner))) return InteractionResult.PASS;
        var dog = nearbyDog(owner);
        return dog != null && dog.sendHome(owner, bed) ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }
}
