package net.j40climb.frenchies.test;

import io.netty.channel.embedded.EmbeddedChannel;
import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
//? if <26.3 {
import net.minecraft.world.entity.EntityType;
//?} else {
/*import net.minecraft.world.entity.EntityTypes;
*///?}
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static net.j40climb.frenchies.test.CompanionTravelTests.disconnect;
import static net.j40climb.frenchies.test.CompanionTravelTests.player;

final class CompanionSafetyTests {
    static void damage(GameTestHelper h) {
        var owner = player(h, 4.5, 4.5);
        var dog = CompanionWhistleTests.dog(h, owner, 4, 5);
        var recipient = player(h, 8.5, 7.5);
        var boots = CompanionWhistleTests.named(new ItemStack(Items.LEATHER_BOOTS), "Rain boots");
        var parcel = FrenchieDeliveryGameTests.parcel();
        dog.setItemSlot(EquipmentSlot.FEET, boots.copy());
        owner.setItemInHand(InteractionHand.MAIN_HAND, parcel.copy());
        FrenchieDeliveryGameTests.confirm(owner, recipient, dog);
        h.assertTrue(ItemStack.matches(dog.getParcel(), parcel), "Fixture did not transfer the parcel");
        float health = dog.getHealth();
        h.assertTrue(!dog.showVehicleHealth(), "Tamed Frenchie requested a health bar");
        var sources = dog.damageSources();
        var arrow = new Arrow(h.getLevel(), owner, new ItemStack(Items.ARROW), null);
        for (var hit : List.of(sources.playerAttack(owner), sources.arrow(arrow, owner), sources.explosion(null, null),
                sources.fall(), sources.inFire(), sources.onFire(), sources.lava(), sources.drown(), sources.inWall(),
                sources.freeze(), sources.cactus(), sources.wither(), sources.magic())) {
            dog.hurtServer(h.getLevel(), hit, 1000);
            h.assertTrue(dog.isAlive() && dog.getHealth() == health && dog.hurtTime == 0
                    && ItemStack.matches(dog.getItemBySlot(EquipmentSlot.FEET), boots) && ItemStack.matches(dog.getParcel(), parcel),
                    "Hazard " + hit + " damaged the dog or its possessions");
        }
        var wild = h.spawn(FrenchieRegistry.FRENCHIE.get(), 7, 1, 3);
        float wildHealth = wild.getHealth();
        wild.hurtServer(h.getLevel(), sources.cactus(), 2);
        h.assertTrue(wild.getHealth() < wildHealth, "Wild Frenchie gained the tamed damage protection");
        disconnect(recipient, owner);
        h.succeed();
    }

    static void reactions(GameTestHelper h) {
        var owner = player(h, 4.5, 4.5);
        var dog = CompanionWhistleTests.dog(h, owner, 5, 5);
        dog.standUp();
        var level = h.getLevel();
        float health = dog.getHealth();
        h.startSequence().thenWaitUntil(() -> h.assertTrue(dog.onGround(), "Dog has not landed"))
        .thenExecute(dog::tryCelebrate)
        .thenWaitUntil(() -> h.assertTrue(dog.celebrationStage() != 0, "Celebration never began"))
        .thenExecute(() -> {
            dog.hurtServer(level, dog.damageSources().playerAttack(owner), 10);
            h.assertTrue(dog.dangerReaction().name().equals("STARTLE"), "An accidental hit gave no startle");
        }).thenExecuteAfter(3, () -> h.assertTrue(dog.celebrationStage() == 0 && dog.hurtTime == 0, "Hit left the play goal active"))
        .thenExecuteAfter(20, () -> {
            dog.hurtServer(level, dog.damageSources().playerAttack(owner), 10);
            h.assertTrue(dog.dangerReaction().name().equals("NONE"), "Hits replayed the reaction during cooldown");
        }).thenExecuteAfter(50, () -> {
            level.explode(null, dog.getX() + 1, dog.getY(), dog.getZ(), 1.6F, Level.ExplosionInteraction.NONE);
            h.assertTrue(dog.dangerReaction().name().equals("DUCK") && dog.getDeltaMovement().horizontalDistance() < 0.3,
                    "Nearby blast lacked a duck or knocked the dog back");
        }).thenExecuteAfter(66, () -> {
            dog.causeFallDamage(12, 1F, dog.damageSources().fall());
            h.assertTrue(dog.dangerReaction().name().equals("TUMBLE"), "A serious fall gave no tumble");
        }).thenExecuteAfter(35, () -> {
            h.assertTrue(dog.dangerReaction().name().equals("NONE") && dog.getHealth() == health, "Dog stayed hurt or reacting");
            owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            owner.interactOn(dog, InteractionHand.MAIN_HAND, Vec3.ZERO);
            h.assertTrue(dog.isOrderedToSit(), "Recovered companion stopped accepting orders");
            disconnect(owner);
        }).thenSucceed();
    }

    static void combat(GameTestHelper h) {
        var owner = player(h, 2.5, 2.5);
        var dog = CompanionWhistleTests.dog(h, owner, 5, 5);
        var zombie = h.spawnWithNoFreeWill(EntityType.ZOMBIE, 8, 1, 2);
        var target = h.spawnWithNoFreeWill(EntityType.COW, 8, 1, 5);
        float health = target.getHealth();
        var at = dog.position().add(0, 0.26, 0);
        var ray = target.position().subtract(dog.position()).normalize();
        var from = at.subtract(ray.scale(2.8));
        var arrow = new Arrow(h.getLevel(), from.x, from.y, from.z, new ItemStack(Items.ARROW), null);
        arrow.setNoGravity(true);
        arrow.setDeltaMovement(ray.scale(1.8));
        h.assertTrue(h.getLevel().addFreshEntity(arrow), "Could not fire the arrow");
        zombie.setTarget(dog);
        h.assertTrue(zombie.getTarget() != dog && !zombie.canAttack(dog), "Tamed dog held hostile attention");
        float zombieHealth = zombie.getHealth();
        h.assertTrue(!dog.doHurtTarget(h.getLevel(), zombie) && zombie.getHealth() == zombieHealth, "Tamed dog damaged a hostile mob");
        h.startSequence().thenWaitUntil(() -> h.assertTrue(target.getHealth() < health, "Dog intercepted an arrow aimed through it"))
        .thenExecute(() -> {
            h.assertTrue(dog.getHealth() == dog.getMaxHealth() && zombie.getTarget() != dog && dog.getTarget() == null,
                    "Dog absorbed damage or retained a combat target");
            disconnect(owner);
        }).thenSucceed();
    }

    static void contact(GameTestHelper h, Block hazard) {
        var owner = player(h, 2.5, 2.5);
        var dog = CompanionWhistleTests.dog(h, owner, 7, 7);
        var id = dog.getUUID();
        var level = h.getLevel();
        var feet = dog.blockPosition();
        var toy = CompanionWhistleTests.named(new ItemStack(FrenchieRegistry.SQUEAKY_TOY.get()), "Yard squeaker");
        dog.setItemSlot(EquipmentSlot.OFFHAND, toy.copy());
        var health = dog.getHealth();
        h.startSequence().thenExecuteAfter(5, () -> {
            if (hazard == Blocks.WATER || hazard == Blocks.STONE) {
                // A sealed, two-block-deep shaft prevents floating to the surface or walking out.
                for (var pos : BlockPos.betweenClosed(feet.offset(-1, 0, -1), feet.offset(1, 2, 1))) {
                    boolean inside = pos.getX() == feet.getX() && pos.getZ() == feet.getZ() && pos.getY() < feet.getY() + 2;
                    level.setBlockAndUpdate(pos, inside ? hazard.defaultBlockState() : Blocks.OBSIDIAN.defaultBlockState());
                }
            } else {
                if (hazard == Blocks.CACTUS) level.setBlockAndUpdate(feet.below(), Blocks.SAND.defaultBlockState());
                var block = hazard == Blocks.MAGMA_BLOCK ? feet.below() : feet;
                level.setBlockAndUpdate(block, hazard.defaultBlockState());
                h.assertTrue(level.getBlockState(block).is(hazard), "Physical hazard did not survive placement");
            }
        }).thenWaitUntil(() -> {
            var current = level.getEntity(id);
            h.assertTrue(current instanceof FrenchieEntity rescued && rescued.isAlive()
                            && !rescued.blockPosition().equals(feet) && !rescued.isInWall()
                            && level.getFluidState(rescued.blockPosition()).isEmpty()
                            && !level.getBlockState(rescued.blockPosition()).is(hazard)
                            && (hazard != Blocks.MAGMA_BLOCK || !level.getBlockState(rescued.blockPosition().below()).is(hazard))
                            && rescued.getHealth() == health && !rescued.isOnFire() && !rescued.isFreezing()
                            && ItemStack.matches(toy, rescued.getOffhandItem()),
                    "Physical hazard did not release a healthy companion with its original toy: " + hazard);
            if (hazard == Blocks.MAGMA_BLOCK) {
                h.assertTrue(current == dog, "Single exposed magma block forced an emergency transfer despite four walkable exits");
            }
        }).thenExecute(() -> disconnect(owner)).thenSucceed();
    }

    static void falling(GameTestHelper h) {
        var owner = player(h, 3.5, 3.5);
        var dog = CompanionWhistleTests.dog(h, owner, 6, 5);
        var health = dog.getHealth();
        h.startSequence().thenWaitUntil(() -> h.assertTrue(dog.onGround(), "Drop fixture did not settle"))
                .thenExecute(() -> {
                    dog.setPos(dog.position().add(0, 12, 0));
                    dog.setOnGround(false);
                    dog.setDeltaMovement(0, -0.25, 0);
                }).thenWaitUntil(() -> h.assertTrue(dog.onGround() && dog.dangerReaction().name().equals("TUMBLE")
                                && dog.getHealth() == health && dog.hurtTime == 0,
                        "Falling twelve blocks did not end in a brief, harmless tumble"))
                .thenExecute(() -> disconnect(owner)).thenSucceed();
    }

    static ServerPlayer reconnect(GameTestHelper h, ServerPlayer owner) {
        var level = h.getLevel();
        var cookie = CommonListenerCookie.createInitial(owner.getGameProfile(), false);
        // ConfigSync distinguishes an embedded GameTest player by its subclass, as GameTestHelper does.
        var player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation()) {};
        var connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        GameTestPlayers.configure(player);
        player.snapTo(h.absoluteVec(new Vec3(2.5, 1, 2.5)), 0, 0);
        return player;
    }

    static void offlineVoid(GameTestHelper h) {
        var owner = player(h, 2.5, 2.5);
        var level = h.getLevel();
        var dog = CompanionWhistleTests.dog(h, owner, 6, 3);
        var id = dog.getUUID();
        var toy = CompanionWhistleTests.named(new ItemStack(FrenchieRegistry.SQUEAKY_TOY.get()), "Home squeaker");
        dog.setItemSlot(EquipmentSlot.OFFHAND, toy.copy());
        ServerPlayer[] returning = new ServerPlayer[1];
        h.startSequence().thenExecuteAfter(5, () -> {
            disconnect(owner);
            dog.snapTo(dog.getX(), level.getMinY() - 96, dog.getZ(), 0, 0);
        }).thenWaitUntil(() -> h.assertTrue(level.getEntity(id) == null, "Offline owner's dog remained in the void"))
        .thenExecuteAfter(60, () -> {
            h.assertTrue(level.getEntity(id) == null, "The missing owner caused a premature or unsafe arrival");
            returning[0] = reconnect(h, owner);
        }).thenWaitUntil(() -> {
            var entity = level.getEntity(id);
            h.assertTrue(entity instanceof FrenchieEntity back && back.isOwnedBy(returning[0])
                            && back.distanceToSqr(returning[0]) < 36 && back.getHealth() == back.getMaxHealth()
                            && ItemStack.matches(toy, back.getOffhandItem()),
                    "Reconnecting after an offline void rescue lost the dog or its toy");
        }).thenExecute(() -> disconnect(returning[0])).thenSucceed();
    }

    static void rescue(GameTestHelper h, boolean voidFall) {
        var owner = player(h, 2.5, 2.5);
        var dog = CompanionWhistleTests.dog(h, owner, 8, 8);
        var id = dog.getUUID();
        var toy = CompanionWhistleTests.named(new ItemStack(FrenchieRegistry.SQUEAKY_TOY.get()), "Pocket squeaker");
        dog.setItemSlot(EquipmentSlot.OFFHAND, toy.copy());
        float health = dog.getHealth();
        h.startSequence().thenExecuteAfter(6, () -> {
            if (voidFall) {
                dog.snapTo(dog.getX(), h.getLevel().getMinY() - 90, dog.getZ(), 0, 0);
            } else {
                for (var pos : BlockPos.betweenClosed(7, 1, 7, 9, 4, 9)) {
                    boolean wall = pos.getX() != 8 || pos.getZ() != 8 || pos.getY() == 4;
                    h.setBlock(pos, wall ? Blocks.OBSIDIAN : Blocks.LAVA);
                }
                dog.igniteForSeconds(100);
            }
        }).thenWaitUntil(() -> {
            var entity = h.getLevel().getEntity(id);
            h.assertTrue(entity instanceof FrenchieEntity rescued && rescued.isAlive() && rescued.distanceToSqr(owner) < 36
                            && h.getLevel().getFluidState(rescued.blockPosition()).isEmpty()
                            && !rescued.isOnFire() && rescued.getHealth() == health
                            && ItemStack.matches(rescued.getOffhandItem(), toy),
                    "Staying dog did not escape " + (voidFall ? "the void" : "the lava trap") + " with its original toy");
        }).thenExecute(() -> disconnect(owner)).thenSucceed();
    }
}
