package net.j40climb.frenchies.playtest;

//? if <26.4 {
import eu.pb4.trinkets.api.TrinketsApi;
import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
//? if <26.3 {
import tld.unknown.baubles.api.BaubleType;
import tld.unknown.baubles.api.Baubles;
//?}
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

final class AccessoryFrontpackPlaytest {
    // Keep each optional API in its own class so the standalone client can load the scenario.
    private interface Slot {
        ItemStack get(Player player);
        void set(Player player, ItemStack stack);
    }

    static void addInstalledProviders(Playtest t) {
        if (ModList.get().isLoaded("curios")) steps(t, "curios", new CuriosBody());
        //? if <26.3 {
        if (ModList.get().isLoaded("baubles")) steps(t, "baubles", new BaublesBody());
        //?}
        if (ModList.get().isLoaded("trinkets_updated")) steps(t, "trinkets", new TrinketsCape());
    }

    private static void steps(Playtest t, String provider, Slot body) {
        Vec3[] platform = new Vec3[1];
        t.server("Equip the red pack in " + provider + " with an iron chestplate and a held lime spare", p -> {
            platform[0] = p.position();
            var red = p.getInventory().removeItemNoUpdate(3);
            var lime = p.getInventory().removeItemNoUpdate(1);
            lime.set(DataComponents.DYED_COLOR, new DyedItemColor(DyeColor.LIME.getTextureDiffuseColor() & 0xFFFFFF));
            body.set(p, red);
            p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
            p.setItemInHand(InteractionHand.OFF_HAND, lime);
        });
        t.awaitClient("Track the " + provider + " pack while wearing armor", mc ->
                FrontpackPlaytest.hasDye(body.get(mc.player), DyeColor.RED) && mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE));
        FrontpackPlaytest.pickUpDog(t);
        t.awaitClient("Pick up the dog with the " + provider + " slot", mc ->
                mc.player.getData(FrenchieRegistry.FRENCH_FRONTPACK_DATA).hasCarriedFrenchie());
        FrontpackPlaytest.framePlayer(t);
        t.screenshot("frontpack-" + provider + "-armored");
        t.server("Wear the lime spare in the chest slot", p -> {
            var armor = p.getItemBySlot(EquipmentSlot.CHEST);
            p.setItemSlot(EquipmentSlot.CHEST, p.getOffhandItem());
            p.setItemInHand(InteractionHand.OFF_HAND, armor);
        });
        t.awaitClient("Track both worn packs", mc -> FrontpackPlaytest.hasDye(mc.player.getItemBySlot(EquipmentSlot.CHEST), DyeColor.LIME)
                && FrontpackPlaytest.hasDye(body.get(mc.player), DyeColor.RED));
        FrontpackPlaytest.framePlayer(t);
        t.screenshot("frontpack-" + provider + "-both");
        t.server("Put the iron chestplate back over the accessory", p -> {
            var lime = p.getItemBySlot(EquipmentSlot.CHEST);
            p.setItemSlot(EquipmentSlot.CHEST, p.getOffhandItem());
            p.setItemInHand(InteractionHand.OFF_HAND, lime);
        });
        t.awaitClient("Keep carrying after removing the vanilla chest pack", mc ->
                mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE)
                        && mc.player.getData(FrenchieRegistry.FRENCH_FRONTPACK_DATA).hasCarriedFrenchie());
        FrontpackPlaytest.framePlayer(t);
        t.screenshot("frontpack-" + provider + "-fallback");
        t.awaitClient("Render the worn accessory's dye and prefer the worn chest pack with both equipped", mc ->
                correctDye(mc.gameDirectory.toPath().resolve("screenshots"), provider));
        t.server("Remove the last worn accessory into the offhand with no safe footing", p -> {
            p.setGameMode(GameType.CREATIVE);
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
            p.teleportTo(p.level(), p.getX(), p.getY() + 12, p.getZ(), Set.of(), 180, 0, true);
            p.getInventory().setItem(1, p.getOffhandItem());
            var red = body.get(p);
            body.set(p, ItemStack.EMPTY);
            p.setItemInHand(InteractionHand.OFF_HAND, red);
        });
        t.awaitServer("Retain the dog until the " + provider + " carrier finds safe ground", p -> {
            var pack = p.getData(FrenchieRegistry.FRENCH_FRONTPACK_DATA);
            return pack.hasCarriedFrenchie() && pack.releasePending() && body.get(p).isEmpty()
                    && FrontpackPlaytest.hasDye(p.getOffhandItem(), DyeColor.RED);
        });
        t.server("Return to the pen with the removed pack still in hand", p -> {
            var start = platform[0];
            p.teleportTo(p.level(), start.x, start.y, start.z, Set.of(), 180, 0, true);
            p.setGameMode(GameType.SURVIVAL);
        });
        t.awaitServer("Release the same dog once and retain both packs and the chestplate", p ->
                FrontpackPlaytest.isRestored(p) && p.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE)
                        && FrontpackPlaytest.hasDye(p.getOffhandItem(), DyeColor.RED)
                        && FrontpackPlaytest.hasDye(p.getInventory().getItem(1), DyeColor.LIME)
                        && p.getInventory().countItem(FrenchieRegistry.FRONT_PACK.get()) == 2);
        t.server("Stow the released accessory", p -> {
            p.getInventory().setItem(3, p.getOffhandItem());
            p.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        });
    }

    private static boolean correctDye(Path shots, String provider) {
        try {
            var armored = ImageIO.read(shots.resolve("frontpack-" + provider + "-armored.png").toFile());
            var both = ImageIO.read(shots.resolve("frontpack-" + provider + "-both.png").toFile());
            var fallback = ImageIO.read(shots.resolve("frontpack-" + provider + "-fallback.png").toFile());
            if (armored == null || both == null || fallback == null) return false;
            int h = both.getHeight(), center = both.getWidth() / 2, changed = 0;
            for (int y = h * 55 / 100; y < h * 66 / 100; y++) {
                for (int x = center - h / 30; x < center + h / 30; x++) {
                    if (redMinusGreen(armored.getRGB(x, y)) > 12 && redMinusGreen(both.getRGB(x, y)) < -12
                            && redMinusGreen(fallback.getRGB(x, y)) > 12) changed++;
                }
            }
            Playtest.require(changed > h * h / 6000, provider + " expected red, then lime, then red on the pouch: " + changed);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static int redMinusGreen(int color) { return ((color >> 16) & 255) - ((color >> 8) & 255); }

    private static final class CuriosBody implements Slot {
        public ItemStack get(Player p) {
            var inventory = CuriosApi.getCuriosInventoryOrNull(p);
            return inventory == null ? ItemStack.EMPTY : inventory.findCurio("body", 0).map(r -> r.stack()).orElse(ItemStack.EMPTY);
        }
        public void set(Player p, ItemStack stack) {
            var inventory = CuriosApi.getCuriosInventoryOrNull(p);
            Playtest.require(inventory != null && (stack.isEmpty() || CuriosApi.isStackValid(new SlotContext("body", p, 0, false, true), stack)), "Curios Body rejected the front pack");
            inventory.setEquippedCurio("body", 0, stack);
        }
    }

    //? if <26.3 {
    private static final class BaublesBody implements Slot {
        public ItemStack get(Player p) { return Baubles.API.getBaublesInventory(p).getBaubleInSlot(BaubleType.BODY); }
        public void set(Player p, ItemStack stack) {
            Playtest.require((stack.isEmpty() || BaubleType.BODY.isItemValid(stack))
                    && Baubles.API.getBaublesInventory(p).setBaubleInSlot(BaubleType.BODY, stack), "Baubles Body rejected the slot change");
        }
    }

    //?}

    private static final class TrinketsCape implements Slot {
        public ItemStack get(Player p) { return TrinketsApi.getAttachment(p).getSlotAccess("chest/cape", 0).get(); }
        public void set(Player p, ItemStack stack) {
            var slot = TrinketsApi.getAttachment(p).getSlotAccess("chest/cape", 0);
            Playtest.require(slot.isValid() && (stack.isEmpty() || slot.slotType().validatorCheck(stack, slot, p))
                    && slot.set(stack), "Trinkets cape rejected the slot change");
        }
    }
}
//?}
