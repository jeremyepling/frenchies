package net.j40climb.frenchies.playtest;

import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.j40climb.frenchies.common.entity.frenchie.FrenchieVariant;
import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.j40climb.frenchies.test.FrenchiePen;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

final class FrontpackPlaytest {
    private static final UUID DOG_ID = UUID.fromString("bd500dac-4311-4a77-a145-20824d17806e");
    private static final BlockPos PEN = new BlockPos(0, 201, 0);
    private static final AABB TEST_AREA = new AABB(PEN).inflate(8);
    private static final FrenchieVariant COAT = FrenchieVariant.BRINDLE;
    private static final int NOON_TICKS = 6000;

    private static void setHudHidden(Minecraft mc, boolean hidden) {
        //? if <26.3 {
        mc.options.hideGui = hidden;
        //?} else {
        /*if (mc.gui.hud.isHidden() != hidden) mc.gui.hud.toggle();
        *///?}
    }

    static void steps(Playtest t, int phase) {
        if (phase == 1) {
            t.server("Place the Frenchie plot and a tamed brindle in reach", p -> {
                var level = p.level().getServer().overworld();
                FrenchiePen.place(level, new BlockPos(-3, 200, -6));
                p.teleportTo(level, 0.5, 201, 2.5, Set.of(), 180, 0, true);
                p.setGameMode(GameType.SURVIVAL);
                p.getInventory().clearContent();
                level.getGameRules().set(GameRules.KEEP_INVENTORY, true, level.getServer());
                p.setItemInHand(InteractionHand.MAIN_HAND, dyedPack(DyeColor.RED));
                var dog = FrenchieRegistry.FRENCHIE.get().create(level, EntitySpawnReason.COMMAND);
                Playtest.require(dog != null, "Could not create Frenchie");
                var tag = new CompoundTag();
                tag.putInt("Variant", COAT.getId());
                dog.readAdditionalSaveData(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
                dog.setUUID(DOG_ID);
                dog.setNoAi(true);
                dog.tame(p);
                dog.snapTo(0.5, 201, 0.5, 0, 0);
                Playtest.require(level.addFreshEntity(dog), "Could not add Frenchie to plot");
            });
            usePackInAir(t, InteractionHand.MAIN_HAND, DyeColor.RED, false);
            t.awaitClient("Equip the held red pack before picking up the dog", mc ->
                    hasDye(mc.player.getItemBySlot(EquipmentSlot.CHEST), DyeColor.RED) && mc.player.getMainHandItem().isEmpty());
            pickUpDog(t);
        }
        t.awaitServer("Check the original dog is inside the pack and absent from the pen", p -> {
            var data = p.getData(FrenchieRegistry.FRENCH_FRONTPACK_DATA);
            return data.hasCarriedFrenchie() && data.frenchieNBT().read("UUID", UUIDUtil.CODEC).filter(DOG_ID::equals).isPresent()
                    && data.frenchieNBT().getIntOr("Variant", -1) == COAT.getId() && dogCount(p) == 0;
        });
        t.awaitClient("Check the tracked pack uses the brindle variant", mc -> {
            return mc.level.getEntity(DOG_ID) == null
                    && mc.player.getData(FrenchieRegistry.FRENCH_FRONTPACK_DATA).carriedCoat() == COAT;
        });
        if (phase == 1) {
            t.server("Kill the carrier while the Frenchie is packed", p -> p.kill(p.level()));
            t.awaitClient("Wait for the carrier's death screen", mc -> mc.screen instanceof DeathScreen);
            t.client("Request vanilla respawn", mc -> mc.player.respawn());
            t.awaitServer("Verify respawn retained the same packed dog", p -> p.isAlive()
                    && p.getData(FrenchieRegistry.FRENCH_FRONTPACK_DATA).frenchieNBT()
                    .read("UUID", UUIDUtil.CODEC).filter(DOG_ID::equals).isPresent());
            t.server("Return the carrier to the cleared plot", p ->
                    p.teleportTo(p.level().getServer().overworld(), 0.5, 201, 2.5, Set.of(), 180, 0, true));
            return;
        }
        t.server("Hold the copied world's lighting at clear noon for the visual checks", p -> {
            var level = p.level();
            var clock = level.dimensionType().defaultClock().orElseThrow();
            var clocks = level.getServer().clockManager();
            clocks.setPaused(clock, true);
            clocks.setTotalTicks(clock, NOON_TICKS);
            level.getServer().setWeatherParameters(24000, 0, false, false);
        });
        t.awaitClient("Wait for the clear noon lighting to reach the client", mc ->
                mc.level.getDefaultClockTime() == NOON_TICKS && mc.level.getRainLevel(1F) == 0F
                        && mc.level.getThunderLevel(1F) == 0F);
        t.awaitClient("Verify the red chest pack survived rejoin", mc ->
                hasDye(mc.player.getItemBySlot(EquipmentSlot.CHEST), DyeColor.RED));
        framePlayer(t);
        t.screenshot("frontpack-dyed-chest");
        FrontpackCloseups.capture(t, "red");
        t.server("Hold a lime front pack with the red pack still equipped", p ->
                p.setItemInHand(InteractionHand.OFF_HAND, dyedPack(DyeColor.LIME)));
        t.awaitClient("Wait for the lime offhand pack", mc -> hasDye(mc.player.getOffhandItem(), DyeColor.LIME));
        framePlayer(t);
        t.screenshot("frontpack-red-worn-lime-held");
        usePackInAir(t, InteractionHand.OFF_HAND, DyeColor.LIME, true);
        t.awaitServer("Swap the held pack without releasing the Frenchie to the empty main hand", p ->
                hasDye(p.getItemBySlot(EquipmentSlot.CHEST), DyeColor.LIME) && hasDye(p.getOffhandItem(), DyeColor.RED)
                        && p.getMainHandItem().isEmpty() && p.getData(FrenchieRegistry.FRENCH_FRONTPACK_DATA).hasCarriedFrenchie()
                        && p.level().getEntity(DOG_ID) == null && p.getInventory().countItem(FrenchieRegistry.FRONT_PACK.get()) == 2);
        t.awaitClient("Wear the lime carrier with the red spare in hand", mc ->
                hasDye(mc.player.getItemBySlot(EquipmentSlot.CHEST), DyeColor.LIME) && hasDye(mc.player.getOffhandItem(), DyeColor.RED));
        framePlayer(t);
        t.screenshot("frontpack-dyed-lime");
        FrontpackCloseups.capture(t, "lime");
        t.awaitClient("See the two straight back straps meeting the belt",
                mc -> FrontpackCloseups.checkBackStraps(mc.gameDirectory.toPath().resolve("screenshots")));
        swapHotbar(t, InventoryMenu.ARMOR_SLOT_START + 1, Inventory.SLOT_OFFHAND);
        t.awaitServer("Return the lime pack to the offhand for washing", p ->
                hasDye(p.getOffhandItem(), DyeColor.LIME) && hasDye(p.getItemBySlot(EquipmentSlot.CHEST), DyeColor.RED));
        t.server("Wash the offhand pack at a water cauldron beside the player", p -> {
            var pos = p.blockPosition().east();
            var water = Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 2);
            p.level().setBlockAndUpdate(pos, water);
            p.gameMode.useItemOn(p, p.level(), p.getOffhandItem(), InteractionHand.OFF_HAND,
                    new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
            Playtest.require(p.level().getBlockState(pos).getValue(LayeredCauldronBlock.LEVEL) == 1, "Wash did not use one water level");
        });
        t.awaitClient("Wait for the undyed offhand pack", mc -> mc.player.getOffhandItem().is(FrenchieRegistry.FRONT_PACK.get())
                && !mc.player.getOffhandItem().has(DataComponents.DYED_COLOR));
        swapHotbar(t, InventoryMenu.ARMOR_SLOT_START + 1, Inventory.SLOT_OFFHAND);
        t.awaitClient("Wear the washed carrier", mc -> mc.player.getItemBySlot(EquipmentSlot.CHEST).is(FrenchieRegistry.FRONT_PACK.get())
                && !mc.player.getItemBySlot(EquipmentSlot.CHEST).has(DataComponents.DYED_COLOR) && hasDye(mc.player.getOffhandItem(), DyeColor.RED));
        framePlayer(t);
        t.screenshot("frontpack-washed");
        t.client("Open the inventory to stow the packs", mc -> {
            setHudHidden(mc, false);
            mc.setScreen(new InventoryScreen(mc.player));
        });
        swapHotbar(t, InventoryMenu.ARMOR_SLOT_START + 1, 2);
        t.awaitServer("Removing the worn pack releases the original dog even with a spare in the offhand", p ->
                isRestored(p) && p.getItemBySlot(EquipmentSlot.CHEST).isEmpty() && hasDye(p.getOffhandItem(), DyeColor.RED)
                        && p.getInventory().getItem(2).is(FrenchieRegistry.FRONT_PACK.get()));
        swapHotbar(t, InventoryMenu.SHIELD_SLOT, 1);
        t.awaitServer("Stowing the held pack preserves both packs and leaves exactly one dog", p ->
                isRestored(p) && p.getItemBySlot(EquipmentSlot.CHEST).isEmpty() && p.getOffhandItem().isEmpty()
                        && hasDye(p.getInventory().getItem(1), DyeColor.RED)
                        && p.getInventory().getItem(2).is(FrenchieRegistry.FRONT_PACK.get())
                        && !p.getInventory().getItem(2).has(DataComponents.DYED_COLOR)
                        && p.getInventory().countItem(FrenchieRegistry.FRONT_PACK.get()) == 2);
        t.awaitClient("Show dyed and plain packs side by side in the inventory", mc -> mc.screen instanceof InventoryScreen
                && hasDye(mc.player.getInventory().getItem(1), DyeColor.RED)
                && mc.player.getInventory().getItem(2).is(FrenchieRegistry.FRONT_PACK.get()));
        t.screenshot("frontpack-inventory-dyed-and-plain");
        int[] dyedSlot = new int[3];
        t.client("Locate the red pack's inventory icon", mc -> {
            var inventory = (InventoryScreen) mc.screen;
            var slot = inventory.getMenu().getSlot(InventoryMenu.USE_ROW_SLOT_START + 1);
            int scale = mc.getWindow().getGuiScale();
            //? if <26.3 {
            dyedSlot[0] = (inventory.getGuiLeft() + slot.x) * scale;
            dyedSlot[1] = (inventory.getGuiTop() + slot.y) * scale;
            //?} else {
            /*dyedSlot[0] = (inventory.getLeftPos() + slot.x) * scale;
            dyedSlot[1] = (inventory.getTopPos() + slot.y) * scale;
            *///?}
            dyedSlot[2] = scale;
        });
        t.client("Close the inventory", mc -> mc.player.closeContainer());
        heldPack(t, 1, "dyed");
        heldPack(t, 2, "plain");
        t.awaitClient("See the dyed pouch in the middle of its slot and in the player's hand",
                mc -> visibleItem(mc.gameDirectory.toPath().resolve("screenshots"), dyedSlot));
        t.client("Select an empty hand", mc -> selectSlot(mc, 0));
        swapHotbar(t, InventoryMenu.ARMOR_SLOT_START + 1, 2);
        t.awaitClient("Verify the washed pack is equipped before pickup", mc ->
                mc.player.getItemBySlot(EquipmentSlot.CHEST).is(FrenchieRegistry.FRONT_PACK.get()));
        pickUpDog(t);
        t.awaitClient("Wait for the washed carrier after putting it on and picking up the dog", mc ->
                mc.player.getData(FrenchieRegistry.FRENCH_FRONTPACK_DATA).carriedCoat() == COAT);
        framePlayer(t);
        t.screenshot("frontpack-rejoined");
        t.awaitClient("Check red and lime fabric pixels returned to the original carrier color after washing",
                mc -> compareDyes(mc.gameDirectory.toPath().resolve("screenshots")));
        swapHotbar(t, InventoryMenu.ARMOR_SLOT_START + 1, 1);
        firstPersonViews(t, true);
        t.client("Crouch and aim at empty air", mc -> {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.options.keyShift.setDown(true);
            mc.player.setXRot(-15);
            mc.player.setYRot(90);
        });
        t.awaitClient("Raycast empty air with an empty hand", FrontpackPlaytest::aimingAtAir);
        t.client("Right-click the air to set the dog down", mc -> KeyMapping.click(mc.options.keyUse.getKey()));
        t.awaitServer("Verify the only dog has the same UUID, owner and coat after restart", FrontpackPlaytest::isRestored);
        t.awaitClient("Check the pack disappeared and the restored dog synchronized", mc ->
                !mc.player.getData(FrenchieRegistry.FRENCH_FRONTPACK_DATA).hasCarriedFrenchie()
                        && mc.level.getEntity(DOG_ID) instanceof FrenchieEntity dog && dog.isOwnedBy(mc.player) && dog.getVariant() == COAT);
        t.client("Turn away from the released dog", mc -> mc.player.setYRot(0));
        t.awaitClient("Verify the second click will also hit empty air", FrontpackPlaytest::aimingAtAir);
        t.client("Right-click empty air a second time", mc -> KeyMapping.click(mc.options.keyUse.getKey()));
        firstPersonViews(t, false);
        t.awaitClient("Compare the first-person view with the dog inside and outside the pack", mc -> {
            var shots = mc.gameDirectory.toPath().resolve("screenshots");
            return unobstructed(shots, "level") && unobstructed(shots, "down");
        });
        framePlayer(t);
        t.screenshot("frontpack-empty");
        FrontpackCloseups.checkEarsAfterRelease(t);
        swapHotbar(t, InventoryMenu.ARMOR_SLOT_START + 1, 3);
        t.awaitClient("Stow the red pack after setting the dog down", mc -> mc.player.getItemBySlot(EquipmentSlot.CHEST).isEmpty());
        framePlayer(t);
        t.screenshot("frontpack-unworn");
        t.awaitClient("Check the empty red carrier is visible only while worn", mc ->
                compareChest(mc.gameDirectory.toPath().resolve("screenshots")));
        t.server("Verify the repeat click left exactly one Frenchie", p -> Playtest.require(isRestored(p), "Dog was duplicated or lost"));
        //? if <26.4 {
        AccessoryFrontpackPlaytest.addInstalledProviders(t);
        //?}
        FrontpackFlightPlaytest.steps(t, DOG_ID);
    }

    private static void usePackInAir(Playtest t, InteractionHand hand, DyeColor dye, boolean crouching) {
        t.awaitClient("Aim the held " + dye + " pack into open sky", mc -> {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.options.keyShift.setDown(crouching);
            mc.player.setXRot(-80);
            return hasDye(mc.player.getItemInHand(hand), dye) && mc.player.isShiftKeyDown() == crouching
                    && mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.MISS;
        });
        t.awaitServer("Synchronize the carrier's crouch input before equipping", p -> p.isShiftKeyDown() == crouching);
        t.client("Right-click the " + hand + " front pack into the chest slot", mc -> KeyMapping.click(mc.options.keyUse.getKey()));
    }

    private static boolean aimingAtAir(Minecraft mc) {
        return mc.player.isShiftKeyDown() && mc.player.getMainHandItem().isEmpty()
                && mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.MISS;
    }

    static void pickUpDog(Playtest t) {
        t.client("Crouch in first person", mc -> {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.options.keyShift.setDown(true);
        });
        t.awaitClient("Aim the empty hand at the player's brindle Frenchie", mc -> {
            if (!(mc.level.getEntity(DOG_ID) instanceof FrenchieEntity dog)) return false;
            mc.player.lookAt(Anchor.EYES, dog.getEyePosition());
            return mc.player.isShiftKeyDown() && dog.isOwnedBy(mc.player) && dog.getVariant() == COAT
                    && mc.player.getMainHandItem().isEmpty()
                    && mc.hitResult instanceof EntityHitResult hit && hit.getEntity() == dog;
        });
        t.client("Right-click the dog to pick it up", mc -> KeyMapping.click(mc.options.keyUse.getKey()));
    }

    private static void swapHotbar(Playtest t, int equipmentSlot, int hotbarSlot) {
        t.client("Swap equipment with hotbar slot " + hotbarSlot, mc ->
                mc.gameMode.handleContainerInput(mc.player.inventoryMenu.containerId, equipmentSlot, hotbarSlot, ContainerInput.SWAP, mc.player));
    }

    private static void selectSlot(Minecraft mc, int slot) {
        mc.player.getInventory().setSelectedSlot(slot);
        mc.player.connection.send(new ServerboundSetCarriedItemPacket(slot));
    }

    private static void heldPack(Playtest t, int slot, String name) {
        int[] settle = new int[1];
        t.client("Hold the " + name + " pack in first person", mc -> {
            selectSlot(mc, slot);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.player.setXRot(8);
            mc.player.setYRot(180);
            settle[0] = mc.player.tickCount + 16;
        });
        t.awaitClient("Wait for the held pack", mc -> mc.screen == null && mc.player.tickCount >= settle[0]
                && mc.player.getMainHandItem().is(FrenchieRegistry.FRONT_PACK.get()));
        t.screenshot("frontpack-held-" + name);
    }

    private static boolean visibleItem(Path shots, int[] slot) {
        BufferedImage inventory, dyed, plain;
        try {
            inventory = ImageIO.read(shots.resolve("frontpack-inventory-dyed-and-plain.png").toFile());
            dyed = ImageIO.read(shots.resolve("frontpack-held-dyed.png").toFile());
            plain = ImageIO.read(shots.resolve("frontpack-held-plain.png").toFile());
        } catch (IOException e) {
            return false;
        }
        if (inventory == null || dyed == null || plain == null) return false;
        int scale = slot[2], iconPixels = 0;
        for (int y = 4 * scale; y < 13 * scale; y++) {
            for (int x = 3 * scale; x < 13 * scale; x++) {
                int color = inventory.getRGB(slot[0] + x, slot[1] + y);
                if (channel(color, 16) - channel(color, 8) > 14) iconPixels++;
            }
        }
        Playtest.require(iconPixels >= 8 * scale * scale, "Dyed pack clipped outside the inventory slot: " + iconPixels);
        int w = dyed.getWidth(), h = dyed.getHeight(), handPixels = 0;
        for (int y = h / 3; y < h * 9 / 10; y++) {
            for (int x = w / 2; x < w * 97 / 100; x++) {
                int a = dyed.getRGB(x, y), b = plain.getRGB(x, y);
                if (channel(a, 16) - channel(a, 8) > channel(b, 16) - channel(b, 8) + 14) handPixels++;
            }
        }
        Playtest.require(handPixels >= h * h / 500, "Dyed pack clipped outside the hand view: " + handPixels);
        return true;
    }

    private static void firstPersonViews(Playtest t, boolean packed) {
        String label = packed ? "packed" : "empty";
        for (int pitch : new int[]{0, 85}) {
            String angle = pitch == 0 ? "level" : "down";
            int[] settle = new int[1];
            t.client("Face north in first person looking " + angle, mc -> {
                mc.options.keyShift.setDown(false);
                setHudHidden(mc, false);
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                mc.setCameraEntity(mc.player);
                mc.player.setYRot(180);
                mc.player.setXRot(pitch);
                settle[0] = mc.player.tickCount + 16;
            });
            t.awaitClient("Wait for the standing player's " + label + " view", mc -> {
                mc.player.setYRot(180);
                mc.player.setXRot(pitch);
                return mc.player.tickCount >= settle[0] && !mc.player.isCrouching()
                            && mc.getCameraEntity() == mc.player && Math.abs(mc.gameRenderer.getMainCamera().xRot() - pitch) < 0.1
                            && hasDye(mc.player.getItemBySlot(EquipmentSlot.CHEST), DyeColor.RED)
                            && mc.player.getMainHandItem().isEmpty() && mc.player.getOffhandItem().isEmpty()
                            && mc.player.getData(FrenchieRegistry.FRENCH_FRONTPACK_DATA).hasCarriedFrenchie() == packed;
            });
            t.screenshot("frontpack-first-person-" + label + "-" + angle);
        }
    }

    private static boolean unobstructed(Path screenshots, String angle) {
        BufferedImage packed, empty;
        try {
            packed = ImageIO.read(screenshots.resolve("frontpack-first-person-packed-" + angle + ".png").toFile());
            empty = ImageIO.read(screenshots.resolve("frontpack-first-person-empty-" + angle + ".png").toFile());
        } catch (IOException e) {
            return false;
        }
        if (packed == null || empty == null) return false;
        int w = packed.getWidth(), h = packed.getHeight();
        int changed = 0, sampled = 0;
        // Compare the ground ahead and the space below the crosshair. Skip moving clouds,
        // the hand on the right, and the HUD at the bottom; all remain in the saved images.
        int top = angle.equals("level") ? h * 53 / 100 : h / 8;
        for (int y = top; y < h * 87 / 100; y++) {
            for (int x = w / 5; x < w * 7 / 10; x++) {
                int a = packed.getRGB(x, y), b = empty.getRGB(x, y);
                int delta = Math.abs(channel(a, 16) - channel(b, 16)) + Math.abs(channel(a, 8) - channel(b, 8))
                        + Math.abs(channel(a, 0) - channel(b, 0));
                if (delta > 60) changed++;
                sampled++;
            }
        }
        Playtest.require(changed < sampled / 100, "Packed Frenchie obstructs the first-person " + angle
                + " view: " + changed + " of " + sampled + " sampled pixels changed");
        return true;
    }

    private static ItemStack dyedPack(DyeColor dye) {
        var stack = new ItemStack(FrenchieRegistry.FRONT_PACK.get());
        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(dye.getTextureDiffuseColor() & 0xFFFFFF));
        return stack;
    }

    static boolean hasDye(ItemStack pack, DyeColor color) {
        return pack.is(FrenchieRegistry.FRONT_PACK.get())
                && DyedItemColor.getOrDefault(pack, 0) == color.getTextureDiffuseColor();
    }

    private static boolean compareDyes(Path screenshots) {
        BufferedImage chest, heldSpare, lime, washed, original;
        try {
            chest = ImageIO.read(screenshots.resolve("frontpack-dyed-chest.png").toFile());
            heldSpare = ImageIO.read(screenshots.resolve("frontpack-red-worn-lime-held.png").toFile());
            lime = ImageIO.read(screenshots.resolve("frontpack-dyed-lime.png").toFile());
            washed = ImageIO.read(screenshots.resolve("frontpack-washed.png").toFile());
            original = ImageIO.read(screenshots.resolve("frontpack-rejoined.png").toFile());
        } catch (IOException e) {
            return false;
        }
        if (chest == null || heldSpare == null || lime == null || washed == null || original == null) return false;
        int w = chest.getWidth(), h = chest.getHeight();
        int changed = 0;
        // Restrict the sample to the torso so a tinted inventory/held icon cannot pass.
        for (int y = h * 55 / 100; y < h * 66 / 100; y++) {
            for (int x = w / 2 - h / 30; x < w / 2 + h / 30; x++) {
                int redFrame = chest.getRGB(x, y), spareFrame = heldSpare.getRGB(x, y), greenFrame = lime.getRGB(x, y);
                int plain = original.getRGB(x, y), clean = washed.getRGB(x, y);
                if (channel(redFrame, 16) > channel(redFrame, 8) + 12
                        && channel(spareFrame, 16) > channel(spareFrame, 8) + 12
                        && channel(greenFrame, 8) > channel(greenFrame, 16) + 12
                        && channel(plain, 16) < 85
                        && Math.abs(channel(plain, 16) - channel(clean, 16)) < 10
                        && Math.abs(channel(plain, 8) - channel(clean, 8)) < 10
                        && Math.abs(channel(plain, 0) - channel(clean, 0)) < 10) changed++;
            }
        }
        Playtest.require(changed > Math.max(10, h * h / 6000), "Too few chest pixels went from red to lime to clean: " + changed);
        return true;
    }

    private static int channel(int color, int shift) {
        return (color >> shift) & 255;
    }

    private static int dogCount(ServerPlayer p) {
        return p.level().getEntitiesOfClass(FrenchieEntity.class, TEST_AREA).size();
    }

    static boolean isRestored(ServerPlayer p) {
        return !p.getData(FrenchieRegistry.FRENCH_FRONTPACK_DATA).hasCarriedFrenchie()
                && p.level().getEntity(DOG_ID) instanceof FrenchieEntity dog && dog.isOwnedBy(p)
                && dog.getVariant() == COAT && dog.getBoundingBox().intersects(TEST_AREA) && dogCount(p) == 1;
    }

    static void framePlayer(Playtest t) {
        int[] settle = new int[1];
        t.client("Frame the standing player's chest from the front", mc -> {
            mc.options.keyShift.setDown(false);
            setHudHidden(mc, true);
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            mc.player.setXRot(0);
            mc.player.setYRot(180);
            mc.player.yBodyRot = 180;
            settle[0] = mc.player.tickCount + 16;
        });
        t.awaitClient("Wait for the camera and standing pose to settle", mc -> !mc.player.isCrouching() && mc.player.tickCount >= settle[0]);
    }

    private static boolean compareChest(Path screenshots) {
        BufferedImage worn, unworn;
        try {
            worn = ImageIO.read(screenshots.resolve("frontpack-empty.png").toFile());
            unworn = ImageIO.read(screenshots.resolve("frontpack-unworn.png").toFile());
        } catch (IOException e) {
            return false; // The screenshot writer may still be encoding the last frame.
        }
        if (worn == null || unworn == null) return false;
        int w = worn.getWidth(), h = worn.getHeight();
        Playtest.require(w == unworn.getWidth() && h == unworn.getHeight(), "Viewport changed during frontpack test");
        int carrierPixels = 0;
        // At the default FOV the front-facing avatar's chest is just below the center of the frame.
        for (int y = h * 55 / 100; y < h * 65 / 100; y++) {
            for (int x = w / 2 - h / 30; x < w / 2 + h / 30; x++) {
                int a = worn.getRGB(x, y), b = unworn.getRGB(x, y);
                int r = a >> 16 & 255, g = a >> 8 & 255, blue = a & 255;
                if (r > g + 12 && r > blue + 12
                        && Math.abs(r - (b >> 16 & 255)) + Math.abs(g - (b >> 8 & 255)) + Math.abs(blue - (b & 255)) > 60) {
                    carrierPixels++;
                }
            }
        }
        Playtest.require(carrierPixels > Math.max(10, h * h / 5000), "Expected empty red carrier on chest, changed pixels=" + carrierPixels);
        return true;
    }
}
