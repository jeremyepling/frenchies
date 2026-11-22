package net.j40climb.frenchies.playtest;

import com.mojang.logging.LogUtils;
import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.j40climb.frenchies.common.entity.frenchie.play.PlayBubbleEntity;
import net.j40climb.frenchies.common.entity.frenchie.play.PlayPose;
import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.clock.ClockTimeMarkers;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Entity;
//? if <26.3 {
import net.minecraft.world.entity.EntityType;
//?} else {
/*import net.minecraft.world.entity.EntityTypes;
*///?}
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.item.ItemStack;
//? if >=26.3 {
/*import net.minecraft.world.level.ChunkPos;
*///?}
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;

final class DogPlayServer {
    static final UUID DOG = UUID.fromString("1971aa10-7b69-4584-958f-cac32867c31a");
    static final UUID SLIME = UUID.fromString("7b7a49f5-7ae0-467b-af7f-84653236315c");
    static final String BLOW = "Play fixture: blow bubbles";
    static final String WATCH_BUBBLES = "Play fixture: watch bubbles";
    static final String WATCH_BOW = "Play fixture: watch bow";
    static final String WATCH_NOSE = "Play fixture: watch nose";
    static final String WATCH_RELOAD = "Play fixture: watch restored dog";
    static final String BLOW_AGAIN = "Play fixture: blow again";
    private static final BlockPos YARD = new BlockPos(488, 220, 488);
    //? if <26.3 {
    private enum Step { JOIN, BUBBLES, OFFER, NOSE, BOW, RELOAD, RECRUIT, NIGHTFALL, SLEEP, WATCH, AWAY, RETURN, TARGET }
    //?} else {
    /*private enum Step { JOIN, BUBBLES, OFFER, NOSE, BOW, RELOAD, RECRUIT, NIGHTFALL, SLEEP, WATCH, AWAY, RETURN, SPAWN_SLIME, TARGET }
    *///?}
    private static Step step = Step.JOIN;
    private static long sleptAt;
    private static Vec3 bed;
    private static int watching;
    private static long nextPuff;
    private static FrenchieEntity departingDog;
    private static List<PlayBubbleEntity> departingPuff;
    //? if >=26.3 {
    /*private static Slime liveSlime;
    *///?}

    static boolean finished(ServerPlayer owner, ServerPlayer visitor, Path root, int phase) {
        var level = owner.level();
        var dog = (FrenchieEntity) level.getEntity(DOG);
        switch (step) {
            case JOIN -> {
                require(!owner.getUUID().equals(visitor.getUUID()), "Expected distinct observers");
                if (phase == 2) {
                    cue(owner, WATCH_RELOAD);
                    cue(visitor, WATCH_RELOAD);
                    advance(Step.RELOAD);
                    break;
                }
                level.getServer().setDifficulty(Difficulty.NORMAL, true);
                level.getServer().clockManager().moveToTimeMarker(level.dimensionType().defaultClock().orElseThrow(), ClockTimeMarkers.NOON);
                for (var pos : BlockPos.betweenClosed(YARD.offset(-9, -1, -9), YARD.offset(9, -1, 9))) {
                    level.setBlockAndUpdate(pos, Blocks.SMOOTH_STONE.defaultBlockState());
                }
                placePlayers(owner, visitor);
                owner.getInventory().clearContent();
                owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(FrenchieRegistry.BUBBLE_TOY.get()));
                dog = FrenchieRegistry.FRENCHIE.get().create(level, EntitySpawnReason.COMMAND);
                require(dog != null, "Could not create Frenchie");
                dog.setUUID(DOG);
                dog.setPersistenceRequired();
                dog.snapTo(YARD, 180, 0);
                require(level.addFreshEntity(dog), "Could not add Frenchie");
                cue(owner, BLOW);
                cue(visitor, WATCH_BUBBLES);
                advance(Step.BUBBLES);
            }
            case BUBBLES -> {
                if (!frames(root, "bubbles") || dog.bubblePlay().engaged()) break;
                require(owner.getMainHandItem().is(FrenchieRegistry.BUBBLE_TOY.get()), "Blowing consumed the toy");
                nextPuff = level.getGameTime() + 150;
                advance(Step.OFFER);
            }
            case OFFER -> {
                if (level.getGameTime() < nextPuff || !dog.onGround()) break;
                dog.snapTo(YARD, -90, 0);
                visitor.teleportTo(level, YARD.getX() + 1.65, YARD.getY() - 0.9, YARD.getZ() - 1.5,
                        Set.of(), 0, 0, true);
                var bubble = new PlayBubbleEntity(FrenchieRegistry.PLAY_BUBBLE.get(), level);
                bubble.setPos(dog.position().add(2, 0.84, 0));
                // Select the occasional nose outcome without bypassing pursuit or contact.
                long seed = 0;
                while (RandomSource.create(seed).nextInt(4) != 0) seed++;
                bubble.getRandom().setSeed(seed);
                require(level.addFreshEntity(bubble) && dog.bubblePlay().offer(bubble), "Could not offer the second bubble");
                cue(visitor, WATCH_NOSE);
                advance(Step.NOSE);
            }
            case NOSE -> {
                if (!Files.isRegularFile(root.resolve("Visitor/screenshots/visitor-nose.png")) || dog.bubblePlay().engaged()) break;
                dog.snapTo(YARD.west(3), -90, 0);
                visitor.teleportTo(level, YARD.getX() - 1.0, YARD.getY() - 0.85, YARD.getZ() - 2.7, Set.of(), 0, 0, true);
                var slime = EntityType.SLIME.create(level, EntitySpawnReason.COMMAND);
                require(slime != null, "Could not create slime");
                slime.setUUID(SLIME);
                slime.setSize(1, true);
                slime.setNoAi(true);
                slime.setPersistenceRequired();
                slime.snapTo(YARD, 0, 0);
                cue(owner, WATCH_BOW);
                cue(visitor, WATCH_BOW);
                require(level.addFreshEntity(slime), "Could not add slime");
                advance(Step.BOW);
            }
            case BOW -> {
                if (!frames(root, "slime-bow") || dog.playPose() == PlayPose.NONE) break;
                // Freeze during play. Remove the stimulus so a new session cannot mask
                // an incorrectly persisted pose when the two clients reconnect.
                level.getServer().tickRateManager().setFrozen(true);
                var slime = level.getEntity(SLIME);
                require(slime != null, "Frenchie harmed its playmate");
                slime.discard();
                require(level.getServer().saveEverything(false, true, true), "Failed to save during slime play");
                LogUtils.getLogger().info("Saved dog in pose {}", dog.playPose());
                return true;
            }
            case RELOAD -> {
                if (dog == null || !frames(root, "restored")) break;
                require(dog.playPose() == PlayPose.NONE && !dog.bubblePlay().engaged()
                        && level.getEntity(SLIME) == null, "Restart retained the missing play target");
                placePlayers(owner, visitor);
                dog.snapTo(YARD, 180, 0);
                cue(owner, BLOW_AGAIN);
                advance(Step.RECRUIT);
            }
            case RECRUIT -> {
                if (dog.playPose() != PlayPose.BUBBLE_APPROACH) break;
                level.getServer().clockManager().moveToTimeMarker(level.dimensionType().defaultClock().orElseThrow(), ClockTimeMarkers.MIDNIGHT);
                advance(Step.NIGHTFALL);
            }
            case NIGHTFALL -> {
                if (!dog.isFrenchieSleeping()) break;
                require(dog.playPose() == PlayPose.NONE && !dog.bubblePlay().engaged(),
                        "Going to sleep left a running bubble goal");
                sleptAt = level.getGameTime();
                advance(Step.SLEEP);
            }
            case SLEEP -> {
                if (level.getGameTime() < sleptAt + 150) break;
                require(dog.isFrenchieSleeping(), "Frenchie got up before the second puff");
                bed = dog.position();
                cue(owner, BLOW_AGAIN);
                advance(Step.WATCH);
            }
            case WATCH -> {
                if (dog.playPose() != PlayPose.BUBBLE_WATCH) break;
                require(dog.isFrenchieSleeping() && dog.position().distanceToSqr(bed) < 0.02,
                        "Sleeping spectator got up to chase the fresh bubbles");
                if (++watching < 6) break;
                departingDog = dog;
                departingPuff = level.getEntitiesOfClass(PlayBubbleEntity.class, new AABB(YARD).inflate(12));
                require(!departingPuff.isEmpty(), "No live bubbles at departure");
                // This wild persistent dog stays in the yard without an owner to follow.
                for (var player : new ServerPlayer[]{owner, visitor}) {
                    player.setGameMode(GameType.SPECTATOR);
                    player.teleportTo(level, YARD.getX() + 512, YARD.getY(), YARD.getZ() + 512, Set.of(), 0, 0, true);
                }
                advance(Step.AWAY);
            }
            case AWAY -> {
                if (dog != null || !departingDog.isRemoved() || yardChunksLoaded(level)
                        || departingPuff.stream().anyMatch(bubble -> !bubble.isRemoved() || level.getEntity(bubble.getUUID()) != null)) break;
                require(departingDog.getRemovalReason() == Entity.RemovalReason.UNLOADED_TO_CHUNK,
                        "Dog left for another reason: " + departingDog.getRemovalReason());
                placePlayers(owner, visitor);
                advance(Step.RETURN);
            }
            case RETURN -> {
                if (dog == null) break;
                require(dog != departingDog, "Server reused the original dog without unloading its entity");
                require(dog.playPose() == PlayPose.NONE, "Reload retained pose " + dog.playPose());
                require(!dog.bubblePlay().engaged(), "Reload retained an active bubble claim");
                require(!dog.isOrderedToSit(), "Reload imposed a sitting order on the sleeping dog");
                var restoredBubbles = level.getEntitiesOfClass(PlayBubbleEntity.class, new AABB(YARD).inflate(12));
                require(restoredBubbles.isEmpty(), "Reload retained " + restoredBubbles.size() + " temporary bubbles");
                level.getServer().clockManager().moveToTimeMarker(level.dimensionType().defaultClock().orElseThrow(), ClockTimeMarkers.NOON);
                visitor.setGameMode(GameType.SURVIVAL);
                visitor.teleportTo(level, YARD.getX() + 3.7, YARD.getY(), YARD.getZ() + 0.5, Set.of(), 90, 0, true);
            //? if <26.3 {
                var slime = EntityType.SLIME.create(level, EntitySpawnReason.COMMAND);
                require(slime != null, "Could not create live vanilla slime");
                slime.setSize(1, true);
                slime.setUUID(SLIME);
                slime.snapTo(YARD, 0, 0);
                require(level.addFreshEntity(slime), "Could not spawn vanilla slime");
            //?} else {
                /*advance(Step.SPAWN_SLIME);
            }
            case SPAWN_SLIME -> {
                var chunk = ChunkPos.containing(YARD);
                if (!level.areEntitiesActuallyLoadedAndTicking(chunk) || !level.isPositionEntityTicking(YARD)
                        || !visitor.connection.hasClientLoaded() || !owner.connection.hasClientLoaded()
                        || !level.getChunkSource().chunkMap.isChunkTracked(visitor, chunk.x(), chunk.z())) break;
                liveSlime = EntityType.SLIME.create(level, EntitySpawnReason.COMMAND);
                require(liveSlime != null, "Could not create live vanilla slime");
                liveSlime.setSize(1, true);
                liveSlime.snapTo(YARD, 0, 0);
                require(level.addFreshEntity(liveSlime), "Could not spawn vanilla slime");
            *///?}
                advance(Step.TARGET);
            }
            case TARGET -> {
                //? if <26.3 {
                var slime = (Slime) level.getEntity(SLIME);
                require(slime != null && slime.isAlive(), "Frenchie harmed the vanilla slime");
                return slime.getTarget() == visitor && slime.distanceToSqr(visitor) < 4;
                //?} else {
                /*require(!liveSlime.isDeadOrDying() && liveSlime.getHealth() == liveSlime.getMaxHealth(),
                        "Vanilla slime lost health after spawn: " + liveSlime.getHealth());
                require(!liveSlime.isRemoved(), "Vanilla slime removed after spawn: " + liveSlime.getRemovalReason());
                return level.getEntity(liveSlime.getUUID()) == liveSlime && liveSlime.getTarget() == visitor
                        && liveSlime.distanceToSqr(visitor) < 4;
                *///?}
            }
        }
        return false;
    }

    private static boolean yardChunksLoaded(ServerLevel level) {
        for (int x = -16; x <= 16; x += 16) {
            for (int z = -16; z <= 16; z += 16) {
                if (level.hasChunkAt(YARD.offset(x, 0, z))) return true;
            }
        }
        return false;
    }

    private static void placePlayers(ServerPlayer owner, ServerPlayer visitor) {
        owner.setGameMode(GameType.CREATIVE);
        owner.teleportTo(owner.level(), YARD.getX() + 0.5, YARD.getY(), YARD.getZ() - 3.0, Set.of(), 0, 12, true);
        visitor.setGameMode(GameType.SPECTATOR);
        visitor.teleportTo(visitor.level(), YARD.getX() + 3.0, YARD.getY() - 0.65, YARD.getZ() - 1.4, Set.of(), 0, 0, true);
    }

    private static void cue(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal(message));
    }

    private static boolean frames(Path root, String name) {
        return Files.isRegularFile(root.resolve("Owner/screenshots/owner-" + name + ".png"))
                && Files.isRegularFile(root.resolve("Visitor/screenshots/visitor-" + name + ".png"));
    }

    private static void advance(Step next) {
        step = next;
        LogUtils.getLogger().info("Play fixture reached {}", next);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
