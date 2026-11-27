package net.j40climb.frenchies.playtest;

import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
//? if >=26.3 {
/*import net.minecraft.world.item.Items;
*///?}
import net.minecraft.world.level.GameType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

final class CompanionServer {
    private CompanionServer() {}

    static final UUID CAPTAIN = UUID.fromString("6ead329d-1399-4e5b-a646-d01716fccfdc");
    static final UUID RETRIEVER = UUID.fromString("eadfad9e-2071-47fd-95c9-86405af347d1");
    private enum Stage { CAPTAIN, FETCH, FINISH, PICKUP, GLIDE }
    private static Stage stage = Stage.CAPTAIN;

    static boolean finished(ServerPlayer owner, ServerPlayer visitor, Path root) {
        var level = owner.level();
        var courier = (FrenchieEntity) level.getEntity(DeliveryMultiplayerServer.DOG);
        switch (stage) {
            case CAPTAIN -> {
                if (!CaptainCourse.finished(owner, visitor)) return false;
                visitor.stopRiding();
                var start = new BlockPos(9, 200, -2);
                spawn(visitor, start, RETRIEVER);
                visitor.teleportTo(level, start.getX() + 2.8, start.getY(), start.getZ() + 1.3, Set.of(), 100, 35, true);
                visitor.getInventory().setItem(8, visitor.getMainHandItem().copyAndClear());
                var toy = new ItemStack(FrenchieRegistry.SQUEAKY_TOY.get());
                toy.set(DataComponents.CUSTOM_NAME, Component.literal("The pocket squeaker"));
                visitor.setItemInHand(InteractionHand.MAIN_HAND, toy);
                visitor.sendSystemMessage(Component.literal("FF fixture throw"));
                stage = Stage.FETCH;
            }
            case FETCH -> {
                var dog = (FrenchieEntity) level.getEntity(RETRIEVER);
                if (!dog.getOffhandItem().is(FrenchieRegistry.SQUEAKY_TOY.get())) return false;
                require(visitor.getMainHandItem().isEmpty(), "Throw duplicated the toy");
                visitor.teleportTo(level, dog.getX() + 1.85, dog.getY(), dog.getZ() + 0.85, Set.of(), 110, 25, true);
                visitor.sendSystemMessage(Component.literal("FF fixture fetch"));
                stage = Stage.FINISH;
            }
            case FINISH -> {
                var dog = (FrenchieEntity) level.getEntity(RETRIEVER);
                if (!dog.getOffhandItem().isEmpty()) return false;
                require(frameExists(root, "courier-arrival") && frameExists(root, "toy"), "Missing companion frame");
                require(visitor.getInventory().countItem(FrenchieRegistry.SQUEAKY_TOY.get()) == 1
                        && Component.literal("The pocket squeaker").equals(visitor.getMainHandItem().get(DataComponents.CUSTOM_NAME)),
                        "Toy handoff lost, changed or duplicated the named toy");
                //? if <26.3 {
                owner.setItemSlot(EquipmentSlot.CHEST, new ItemStack(FrenchieRegistry.WINGED_FRONT_PACK.get()));
                //?}
                owner.teleportTo(level, courier.getX() + 1.3, courier.getY(), courier.getZ(), Set.of(), 90, 15, true);
                owner.sendSystemMessage(Component.literal("FF flight pickup"));
                stage = Stage.PICKUP;
            }
            case PICKUP -> {
                if (!owner.getData(FrenchieRegistry.FRENCH_FRONTPACK_DATA).hasCarriedFrenchie()) return false;
                require(level.getEntity(DeliveryMultiplayerServer.DOG) == null, "Front-pack pickup left a duplicate courier");
                //? if >=26.3 {
                /*owner.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
                *///?}
                owner.teleportTo(level, 48.5, 275, 0.5, Set.of(), 0, 0, true);
                visitor.setGameMode(GameType.SPECTATOR);
                visitor.setCamera(owner);
                owner.sendSystemMessage(Component.literal("FF flight glide"));
                visitor.sendSystemMessage(Component.literal("FF flight gesture"));
                stage = Stage.GLIDE;
            }
            case GLIDE -> {
                if (!frameExists(root, "elytra-gaze-recentered")) return false;
                //? if <26.3 {
                require(owner.isFallFlying() && owner.getItemBySlot(EquipmentSlot.CHEST).is(FrenchieRegistry.WINGED_FRONT_PACK.get())
                //?} else {
                /*require(owner.isFallFlying() && owner.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)
                *///?}
                                && owner.getData(FrenchieRegistry.FRENCH_FRONTPACK_DATA).hasCarriedFrenchie(),
                        "Completion requires the original carrier still gliding");
                return true;
            }
        }
        return false;
    }

    private static boolean frameExists(Path root, String name) {
        return Files.isRegularFile(root.resolve("Visitor/screenshots/" + name + ".png"));
    }

    static FrenchieEntity spawn(ServerPlayer owner, BlockPos pos, UUID id) {
        var dog = FrenchieRegistry.FRENCHIE.get().create(owner.level(), EntitySpawnReason.COMMAND);
        require(dog != null, "Frenchie spawn failed");
        dog.setUUID(id);
        dog.snapTo(pos, 180, 0);
        dog.tame(owner);
        require(owner.level().addFreshEntity(dog), "Could not add Frenchie to world");
        return dog;
    }

    private static void require(boolean condition, String detail) {
        if (!condition) throw new IllegalStateException(detail);
    }
}
