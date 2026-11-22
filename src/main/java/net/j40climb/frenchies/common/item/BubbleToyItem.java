package net.j40climb.frenchies.common.item;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.j40climb.frenchies.common.entity.frenchie.play.PlayBubbleEntity;
import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class BubbleToyItem extends Item {
    private static final int MAX_BUBBLES = 6;
    private static final int MAX_DOGS = 3;

    public BubbleToyItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        var toy = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(toy)) return InteractionResult.CONSUME;
        if (level instanceof ServerLevel server) {
            player.getCooldowns().addCooldown(toy, 40);
            blow(server, player);
        }
        return InteractionResult.SUCCESS;
    }

    private static void blow(ServerLevel level, Player player) {
        var area = player.getBoundingBox().inflate(16);
        int existing = level.getEntitiesOfClass(PlayBubbleEntity.class, area).size();
        int count = Math.min(3, MAX_BUBBLES - existing);
        if (count <= 0) return;

        Vec3 forward = Vec3.directionFromRotation(0, player.getYRot());
        Vec3 side = new Vec3(forward.z, 0, -forward.x);
        Vec3 mouth = player.getEyePosition().add(0, -0.34, 0);
        List<PlayBubbleEntity> bubbles = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double fan = i - (count - 1) / 2.0;
            PlayBubbleEntity bubble = new PlayBubbleEntity(FrenchieRegistry.PLAY_BUBBLE.get(), level);
            Vec3 start = mouth.add(forward.scale(0.67)).add(side.scale(fan * 0.24));
            bubble.setPos(start);
            if (!level.noBlockCollision(bubble, bubble.getBoundingBox().expandTowards(mouth.subtract(start)))) continue;
            double lift = Mth.clamp(player.getLookAngle().y * 0.07, -0.03, 0.035);
            bubble.setDeltaMovement(forward.scale(0.095 + level.getRandom().nextDouble() * 0.025)
                    .add(side.scale(fan * 0.105)).add(0, lift + level.getRandom().nextDouble() * 0.009, 0));
            if (level.addFreshEntity(bubble)) bubbles.add(bubble);
        }
        if (bubbles.isEmpty()) return;
        if (existing == 0) level.playSound(null, player.blockPosition(), SoundEvents.BUBBLE_COLUMN_BUBBLE_POP,
                SoundSource.PLAYERS, 0.28F, 0.82F);

        var dogs = level.getEntitiesOfClass(FrenchieEntity.class, area, FrenchieEntity::isAlive);
        int audience = 0;
        for (FrenchieEntity dog : dogs) {
            if (dog.bubblePlay().engaged()) audience++;
        }
        dogs.sort(Comparator.comparingDouble(dog -> dog.distanceToSqr(player)));
        for (FrenchieEntity dog : dogs) {
            if (audience >= MAX_DOGS || bubbles.isEmpty()) break;
            if (dog.distanceToSqr(player) > 100 || dog.bubblePlay().engaged()) continue;
            bubbles.sort(Comparator.comparingDouble(bubble -> dog.distanceToSqr(bubble)));
            for (var iterator = bubbles.iterator(); iterator.hasNext();) {
                if (dog.bubblePlay().offer(iterator.next())) {
                    iterator.remove();
                    audience++;
                    break;
                }
            }
        }
    }
}
