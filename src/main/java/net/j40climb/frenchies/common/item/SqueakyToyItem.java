package net.j40climb.frenchies.common.item;

import java.util.Comparator;
import java.util.List;
import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class SqueakyToyItem extends Item {
    private static final int HEARING_RANGE = 12;
    private static final int MAX_LISTENERS = 6;
    private static final int MAX_AUDIENCE = 3;

    public SqueakyToyItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(held)) return InteractionResult.CONSUME;
        if (level instanceof ServerLevel server) {
            player.getCooldowns().addCooldown(held, 16);
            if (player.isShiftKeyDown()) {
                Vec3 aim = player.getLookAngle();
                ItemEntity toy = new ItemEntity(server, player.getX(), player.getEyeY() - 0.22, player.getZ(), held.copyWithCount(1));
                toy.setDeltaMovement(aim.scale(0.67).add(0, 0.12, 0));
                toy.setPickUpDelay(20);
                toy.setTarget(player.getUUID());
                toy.setUnlimitedLifetime();
                if (server.addFreshEntity(toy)) {
                    // A throw moves the actual toy, including in Creative.
                    held.shrink(1);
                    for (FrenchieEntity dog : listeners(server, player)) {
                        if (dog.isOwnedBy(player) && dog.toyPlay().fetch(toy)) break;
                    }
                }
            } else {
                squeak(server, player);
            }
        }
        return InteractionResult.SUCCESS;
    }

    public static void squeak(ServerLevel level, Entity source) {
        level.playSound(null, source.blockPosition(), FrenchieRegistry.TOY_SQUEAK.get(),
                source instanceof Player ? SoundSource.PLAYERS : SoundSource.NEUTRAL, 0.5F,
                0.93F + source.getRandom().nextFloat() * 0.14F);
        List<FrenchieEntity> listeners = listeners(level, source);
        int audience = 0;
        for (FrenchieEntity dog : listeners) {
            if (dog.toyPlay().watching(source)) audience++;
        }
        for (int i = 0; i < Math.min(MAX_LISTENERS, listeners.size()); i++) {
            FrenchieEntity dog = listeners.get(i);
            if (dog.toyPlay().hear(source, audience < MAX_AUDIENCE)) audience++;
        }
    }

    private static List<FrenchieEntity> listeners(ServerLevel level, Entity source) {
        List<FrenchieEntity> dogs = level.getEntitiesOfClass(FrenchieEntity.class,
                source.getBoundingBox().inflate(HEARING_RANGE), dog -> dog != source && dog.isAlive()
                        && dog.distanceToSqr(source) <= HEARING_RANGE * HEARING_RANGE && dog.hasLineOfSight(source));
        dogs.sort(Comparator.comparingDouble(dog -> dog.distanceToSqr(source)));
        return dogs;
    }
}
