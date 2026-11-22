package net.j40climb.frenchies.common.entity.frenchie;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.j40climb.frenchies.Frenchies;
import net.j40climb.frenchies.common.entity.frenchie.frontpack.FrontpackAttachments;
import net.j40climb.frenchies.common.entity.frenchie.frontpack.FrontpackData;
import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Live dogs have only a locator here. Only a dismissed dog or one awaiting safe arrival has an entity save here. */
public final class CompanionRoster extends SavedData {
    private enum State { ACTIVE, CALLING, APPROACHING, DISMISSING, DISMISSED, RETURNING }

    private static final class Entry {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("owner").forGetter(e -> e.owner),
                Codec.STRING.fieldOf("name").forGetter(e -> e.name),
                GlobalPos.CODEC.fieldOf("location").forGetter(e -> e.location),
                Codec.BOOL.fieldOf("following").forGetter(e -> e.following),
                Codec.STRING.xmap(State::valueOf, State::name).fieldOf("state").forGetter(e -> e.state),
                Codec.LONG.fieldOf("due").forGetter(e -> e.due),
                CompoundTag.CODEC.optionalFieldOf("dog").forGetter(e -> Optional.ofNullable(e.saved))
        ).apply(i, Entry::new));

        final UUID owner;
        String name;
        GlobalPos location;
        boolean following;
        State state;
        long due;
        @Nullable CompoundTag saved;
        boolean warned;

        Entry(UUID owner, String name, GlobalPos location, boolean following, State state, long due, Optional<CompoundTag> saved) {
            this.owner = owner;
            this.name = name;
            this.location = location;
            this.following = following;
            this.state = state;
            this.due = due;
            this.saved = saved.orElse(null);
        }
    }

    private static final SavedDataType<CompanionRoster> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Frenchies.LEGACY_NAMESPACE, "companions"),
            () -> new CompanionRoster(Map.of(), Map.of(), List.of()), RecordCodecBuilder.create(i -> i.group(
                    Codec.unboundedMap(UUIDUtil.STRING_CODEC, Entry.CODEC).fieldOf("dogs").forGetter(r -> r.dogs),
                    Codec.unboundedMap(UUIDUtil.STRING_CODEC, UUIDUtil.CODEC).fieldOf("selected").forGetter(r -> r.selected),
                    Codec.STRING.listOf().optionalFieldOf("indexed", List.of()).forGetter(r -> List.copyOf(r.indexed))
            ).apply(i, CompanionRoster::new)), null);

    private final Map<UUID, Entry> dogs;
    private final Map<UUID, UUID> selected;

    private final Set<String> indexed;
    int scansInProgress;

    private CompanionRoster(Map<UUID, Entry> dogs, Map<UUID, UUID> selected, List<String> indexed) {
        this.indexed = new HashSet<>(indexed);
        this.dogs = new HashMap<>(dogs);
        this.selected = new HashMap<>(selected);
    }

    boolean indexed(ServerLevel level) {
        return indexed.contains(level.dimension().identifier().toString());
    }

    void finishedIndex(ServerLevel level, boolean complete) {
        scansInProgress--;
        if (complete && indexed.add(level.dimension().identifier().toString())) setDirty();
    }

    void indexLegacy(UUID id, UUID owner, String name, GlobalPos pos, boolean following) {
        if (!dogs.containsKey(id)) register(id, owner, name, pos, following);
    }

    static CompanionRoster get(ServerLevel level) {
        return level.getServer().getDataStorage().computeIfAbsent(TYPE);
    }

    private Entry register(UUID id, UUID owner, String name, GlobalPos pos, boolean following) {
        var e = dogs.get(id);
        if (e == null || !owner.equals(e.owner)) {
            if (e != null) selected.remove(e.owner, id);
            e = new Entry(owner, name, pos, following, State.ACTIVE, 0, Optional.empty());
            dogs.put(id, e);
            selected.putIfAbsent(owner, id);
            setDirty();
        }
        return e;
    }

    public static void track(FrenchieEntity dog) {
        if (dog.getOwnerReference() == null || !(dog.level() instanceof ServerLevel level)) return;
        var roster = get(level);
        UUID owner = dog.getOwnerReference().getUUID();
        var e = roster.dogs.get(dog.getUUID());
        boolean following = !dog.isOrderedToSit() && !dog.hasCompanionJob() && !dog.mayBeLeashed();
        if (e == null || !e.owner.equals(owner)) {
            roster.register(dog.getUUID(), owner, dog.getName().getString(), GlobalPos.of(level.dimension(), dog.blockPosition()), following);
            return;
        }
        if (e.following != following || !e.location.dimension().equals(level.dimension())
                || !ChunkPos.containing(e.location.pos()).equals(dog.chunkPosition())) {
            e.location = GlobalPos.of(level.dimension(), dog.blockPosition());
            e.following = following;
            roster.setDirty();
        }
        if (dog.tickCount % 20 == 0) {
            String name = dog.getName().getString();
            if (!e.name.equals(name)) {
                e.name = name;
                roster.setDirty();
            }
        }
    }

    public static void packing(FrenchieEntity dog) {
        track(dog);
        var roster = get((ServerLevel) dog.level());
        var entry = roster.dogs.get(dog.getUUID());
        if (entry != null) {
            entry.following = false;
            // A new deliberate pack order supersedes a previous walk toward the player.
            if (entry.state == State.APPROACHING) entry.state = State.ACTIVE;
            roster.setDirty();
        }
    }

    public static void leave(Entity entity, ServerLevel level) {
        if (!(entity instanceof FrenchieEntity dog)) return;
        if (dog.getRemovalReason() == Entity.RemovalReason.UNLOADED_TO_CHUNK && dog.isTame()) track(dog);
        if (dog.getRemovalReason() == Entity.RemovalReason.KILLED && dog.getOwnerReference() != null) {
            var roster = get(level);
            roster.dogs.remove(dog.getUUID());
            roster.selected.remove(dog.getOwnerReference().getUUID(), dog.getUUID());
            roster.setDirty();
        }
    }

    void indexStored(ServerPlayer owner, CompoundTag tag, GlobalPos pos) {
        var id = tag.read("UUID", UUIDUtil.CODEC);
        if (id.isEmpty() || dogs.containsKey(id.get()) || !ownedTag(tag, owner.getUUID())) return;
        String name = TagValueInput.create(ProblemReporter.DISCARDING, owner.registryAccess(), tag)
                .read("CustomName", ComponentSerialization.CODEC).orElse(FrenchieRegistry.FRENCHIE.get().getDescription()).getString();
        register(id.get(), owner.getUUID(), name, pos, false);
    }

    void indexStop(ServerPlayer owner, UUID dog, GlobalPos pos) {
        register(dog, owner.getUUID(), FrenchieRegistry.FRENCHIE.get().getDescription().getString(), pos, false);
    }

    private static boolean ownedTag(CompoundTag tag, UUID owner) {
        return tag.read("Owner", UUIDUtil.CODEC).filter(owner::equals).isPresent();
    }

    /** Import older frontpacks, courier trips and waiting stops without creating a second dog. */
    private static CompanionRoster discover(ServerPlayer owner) {
        var roster = get(owner.level());
        var pack = FrontpackAttachments.packOrNull(owner);
        if (pack != null && pack.hasCarriedFrenchie()) roster.indexStored(owner, pack.frenchieNBT(),
                GlobalPos.of(owner.level().dimension(), owner.blockPosition()));
        CourierTrips.index(owner, roster);
        if (!roster.selected.containsKey(owner.getUUID())) {
            for (var level : owner.level().getServer().getAllLevels()) {
                for (var dog : level.getEntities(FrenchieRegistry.FRENCHIE.get(), d -> ownedBy(d, owner.getUUID()))) track(dog);
            }
            roster.dogs.entrySet().stream().filter(e -> e.getValue().owner.equals(owner.getUUID()))
                    .map(Map.Entry::getKey).min(UUID::compareTo).ifPresent(id -> {
                        if (roster.selected.putIfAbsent(owner.getUUID(), id) == null) roster.setDirty();
                    });
        }
        return roster;
    }

    static void tamed(ServerPlayer owner, FrenchieEntity dog) {
        var roster = discover(owner);
        track(dog);
        if (dog.getUUID().equals(roster.selected.get(owner.getUUID()))) giveWhistle(owner);
    }

    public static int replaceWhistle(ServerPlayer owner) {
        var roster = discover(owner);
        if (!roster.selected.containsKey(owner.getUUID())) {
            tell(owner, roster.scansInProgress > 0 ? "indexing" : "none");
            return 0;
        }
        return giveWhistle(owner);
    }

    private static int giveWhistle(ServerPlayer owner) {
        var item = FrenchieRegistry.FRENCHIE_WHISTLE.get();
        if (owner.getInventory().contains(stack -> stack.is(item))) {
            tell(owner, "have");
            return 1;
        }
        if (!owner.getInventory().add(new ItemStack(item))) {
            tell(owner, "make_room");
            return 0;
        }
        tell(owner, "received");
        return 1;
    }

    public static boolean select(ServerPlayer owner, UUID id) {
        var roster = discover(owner);
        var e = roster.dogs.get(id);
        if (e == null || !e.owner.equals(owner.getUUID())) {
            tell(owner, "none");
            return false;
        }
        roster.selected.put(owner.getUUID(), id);
        roster.setDirty();
        tell(owner, "selected", e.name);
        return true;
    }

    public static int list(ServerPlayer owner) {
        var roster = discover(owner);
        var entries = roster.dogs.entrySet().stream().filter(e -> e.getValue().owner.equals(owner.getUUID()))
                .sorted(Comparator.comparing((Map.Entry<UUID, Entry> e) -> e.getValue().name).thenComparing(Map.Entry::getKey)).toList();
        if (entries.isEmpty()) tell(owner, roster.scansInProgress > 0 ? "indexing" : "none");
        else owner.sendSystemMessage(Component.translatable("frenchie.florafauna.whistle.choose"));
        for (var row : entries) {
            UUID id = row.getKey();
            boolean current = id.equals(roster.selected.get(owner.getUUID()));
            owner.sendSystemMessage(Component.literal((current ? "✓ " : "") + row.getValue().name + " · " + id.toString().substring(0, 8))
                    .withStyle(style -> style.withColor(current ? ChatFormatting.GREEN : ChatFormatting.AQUA)
                            .withUnderlined(true).withClickEvent(new ClickEvent.RunCommand("/frenchie select " + id))));
        }
        return entries.size();
    }

    private static boolean ownedBy(FrenchieEntity dog, UUID owner) {
        return dog.isTame() && dog.isAlive() && dog.getOwnerReference() != null && dog.getOwnerReference().getUUID().equals(owner);
    }

    private static void tell(ServerPlayer player, String key, Object... args) {
        player.sendOverlayMessage(Component.translatable("frenchie.florafauna.whistle." + key, args));
    }

    public static void whistle(ServerPlayer owner, boolean dismiss) {
        if (!DeliveryGoal.available(owner)) return;
        var roster = discover(owner);
        UUID id = roster.selected.get(owner.getUUID());
        var e = roster.dogs.get(id);
        if (e == null || !e.owner.equals(owner.getUUID())) {
            tell(owner, roster.scansInProgress > 0 ? "indexing" : "none");
            return;
        }
        if (dismiss && e.state == State.DISMISSING || !dismiss && (e.state == State.CALLING
                || e.state == State.APPROACHING || e.state == State.RETURNING)) return;
        owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.NOTE_BLOCK_FLUTE.value(),
                SoundSource.PLAYERS, 0.65f, dismiss ? 1.15f : 1.75f);
        tell(owner, dismiss ? "dismiss" : "call", e.name);
        if (dismiss && e.state == State.DISMISSED) return;
        long now = owner.level().getServer().overworld().getGameTime();
        e.state = dismiss ? State.DISMISSING : State.CALLING;
        e.due = now;
        e.warned = false;
        roster.setDirty();
        roster.process(owner.level().getServer(), owner, id, e, now);
    }

    public static void respawn(ServerPlayer owner) {
        var roster = discover(owner);
        var pack = FrontpackAttachments.packOrNull(owner);
        UUID packedId = pack != null && pack.hasCarriedFrenchie()
                ? pack.frenchieNBT().read("UUID", UUIDUtil.CODEC).orElse(null) : null;
        for (var row : roster.dogs.entrySet()) {
            // A packed companion has already rejoined the respawned owner, including its paused job.
            if (row.getKey().equals(packedId)) continue;
            var e = row.getValue();
            if (e.owner.equals(owner.getUUID()) && e.state != State.DISMISSED && e.state != State.DISMISSING) {
                e.state = State.CALLING;
                e.due = 0;
                e.warned = false;
                roster.setDirty();
            }
        }
    }

    public static void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        if (now % 5 != 0) return;
        var roster = server.getDataStorage().get(TYPE);
        if (roster == null) return;
        for (var row : List.copyOf(roster.dogs.entrySet())) {
            var e = row.getValue();
            if (e.state == State.DISMISSED || e.state == State.ACTIVE && (!e.following || now % 40 != 0)) continue;
            var owner = server.getPlayerList().getPlayer(e.owner);
            if (!DeliveryGoal.available(owner) && e.state != State.DISMISSING) continue;
            if (e.state == State.ACTIVE) {
                if (e.location.dimension().equals(owner.level().dimension())
                        && ChunkPos.containing(e.location.pos()).getChessboardDistance(owner.chunkPosition()) <= 2) continue;
                e.state = State.CALLING;
                e.due = now;
                roster.setDirty();
            }
            roster.process(server, owner, row.getKey(), e, now);
        }
    }

    private void process(MinecraftServer server, @Nullable ServerPlayer owner, UUID id, Entry e, long now) {
        // A dismissal requested before logout only needs the source endpoint to finish.
        if (owner == null && e.state != State.DISMISSING) return;
        if (e.state == State.RETURNING) {
            if (now >= e.due) arrive(owner, id, e, now);
            return;
        }
        var live = server.overworld().getEntityInAnyDimension(id);
        if (live instanceof FrenchieEntity dog && ownedBy(dog, e.owner)) {
            if (e.state == State.APPROACHING) {
                if (dog.isOrderedToSit() || dog.hasCompanionJob() || dog.level() == owner.level() && dog.distanceToSqr(owner) <= 6.25) {
                    e.state = State.ACTIVE;
                    setDirty();
                    return;
                }
                if (now < e.due && dog.level() == owner.level()) return;
            } else if (e.state == State.CALLING && dog.level() == owner.level() && dog.distanceToSqr(owner) <= 16 * 16
                    && !dog.isPassenger() && !dog.mayBeLeashed() && dog.hasLineOfSight(owner) && dog.onGround()) {
                dog.cancelForWhistle();
                dog.perkToyEars();
                FrenchieAwareness.bark(dog, 1.24f);
                dog.getLookControl().setLookAt(owner);
                if (dog.delivery().hasParcel()) tell(owner, "collect", e.name);
                dog.getNavigation().moveTo(owner, 1.25);
                e.state = State.APPROACHING;
                e.due = now + 50;
                setDirty();
                return;
            }
            if (e.state == State.DISMISSING && owner != null && dog.level() == owner.level() && dog.distanceToSqr(owner) <= 256) {
                FrenchieAwareness.bark(dog, 1.10f);
            }
            storeLive(dog, e);
        } else if (e.saved == null) {
            var pack = owner == null ? null : FrontpackAttachments.packOrNull(owner);
            if (pack != null && pack.hasCarriedFrenchie() && matches(pack.frenchieNBT(), id, e.owner)) {
                e.saved = pack.frenchieNBT();
                FrontpackAttachments.setPack(owner, FrontpackData.DEFAULT);
            } else {
                e.saved = CourierTrips.take(server, e.owner, id);
            }
            if (e.saved == null) {
                // Load only the last known endpoint. Never manufacture a replacement for an unresolved UUID.
                var level = server.getLevel(e.location.dimension());
                if (level != null) {
                    var chunk = ChunkPos.containing(e.location.pos());
                    level.getChunkSource().addTicketWithRadius(FrenchieRegistry.COURIER_STOP.get(), chunk, 2);
                    if (!level.areEntitiesActuallyLoadedAndTicking(chunk) || now < e.due + 100) return;
                }
                if (owner != null) tell(owner, "unavailable", e.name);
                e.state = State.ACTIVE;
                e.following = false;
                setDirty();
                return;
            }
        }
        e.state = e.state == State.DISMISSING ? State.DISMISSED : State.RETURNING;
        e.following = false;
        e.due = now + 8;
        setDirty();
    }

    private static boolean matches(CompoundTag tag, UUID id, UUID owner) {
        return tag.read("UUID", UUIDUtil.CODEC).filter(id::equals).isPresent() && ownedTag(tag, owner);
    }

    private void storeLive(FrenchieEntity dog, Entry entry) {
        dog.cancelForWhistle();
        dog.stopRiding();
        dog.ejectPassengers();
        var out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, dog.registryAccess());
        dog.saveWithoutId(out);
        entry.saved = out.buildResult();
        // The saved leash comes back attached to the owner. Removing its live link must not drop a second lead.
        dog.removeLeash();
        dog.discard();
        setDirty();
    }

    /** An unloaded dog can wake before the server's next roster tick. Honor dismissal before running its old job. */
    static boolean finishPendingDismissal(FrenchieEntity dog, ServerLevel level) {
        var roster = level.getServer().getDataStorage().get(TYPE);
        if (roster == null) return false;
        var entry = roster.dogs.get(dog.getUUID());
        if (entry == null || entry.state != State.DISMISSING || !ownedBy(dog, entry.owner)) return false;
        roster.storeLive(dog, entry);
        entry.state = State.DISMISSED;
        entry.following = false;
        roster.setDirty();
        return true;
    }

    static void emergency(FrenchieEntity dog) {
        if (dog.isRemoved() || dog.getOwnerReference() == null || !(dog.level() instanceof ServerLevel level)) return;
        track(dog);
        var roster = get(level);
        var e = roster.dogs.get(dog.getUUID());
        roster.storeLive(dog, e);
        e.state = e.state == State.DISMISSING ? State.DISMISSED : State.RETURNING;
        e.following = false;
        e.due = 0;
        roster.setDirty();
    }

    private void arrive(ServerPlayer owner, UUID id, Entry e, long now) {
        if (e.saved == null || !matches(e.saved, id, e.owner)) return;
        Vec3 landing = safeLanding(owner);
        if (landing == null) {
            if (!e.warned) tell(owner, "unsafe", e.name);
            e.warned = true;
            e.due = now + 20;
            return;
        }
        var level = owner.level();
        if (level.getEntityInAnyDimension(id) != null) return;
        var dog = FrenchieRegistry.FRENCHIE.get().create(level, EntitySpawnReason.LOAD);
        if (dog == null) return;
        dog.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), e.saved));
        dog.cancelForWhistle();
        dog.snapTo(landing, owner.getYRot(), 0);
        dog.setDeltaMovement(Vec3.ZERO);
        dog.resetFallDistance();
        dog.clearFire();
        dog.setTicksFrozen(0);
        dog.setAirSupply(dog.getMaxAirSupply());
        if (!level.addFreshEntity(dog)) return;
        if (dog.mayBeLeashed()) dog.setLeashedTo(owner, true);
        e.saved = null;
        e.state = State.ACTIVE;
        e.warned = false;
        track(dog);
        setDirty();
        dog.perkToyEars();
        dog.getLookControl().setLookAt(owner);
        if (dog.delivery().hasParcel()) tell(owner, "collect", e.name);
    }

    public static @Nullable Vec3 safeLanding(ServerPlayer owner) {
        var level = owner.level();
        for (var pos : BlockPos.withinManhattan(owner.blockPosition(), 4, 3, 4)) {
            var landing = safeAt(level, pos);
            if (landing != null && level.clip(new ClipContext(landing.add(0, 0.4, 0), owner.getEyePosition(),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner)).getType() == HitResult.Type.MISS) return landing;
        }
        return null;
    }

    public static @Nullable Vec3 safeAt(ServerLevel level, BlockPos pos) {
        var type = FrenchieRegistry.FRENCHIE.get();
        if (!level.isInWorldBounds(pos) || !level.hasChunkAt(pos)) return null;
        var floor = pos.below();
        var support = level.getBlockState(floor);
        if (!support.isFaceSturdy(level, floor, Direction.UP) || type.isBlockDangerous(support)
                || support.is(Blocks.POWDER_SNOW) || support.is(Blocks.POINTED_DRIPSTONE)
                || !support.getFluidState().isEmpty()) return null;
        if (!level.getFluidState(pos).isEmpty() || type.isBlockDangerous(level.getBlockState(pos))
                || level.getBlockState(pos).is(Blocks.NETHER_PORTAL) || level.getBlockState(pos).is(Blocks.END_PORTAL)
                || level.getBlockState(pos).is(Blocks.END_GATEWAY) || level.getBlockState(pos).is(Blocks.POWDER_SNOW)) return null;
        Vec3 safe = DismountHelper.findSafeDismountLocation(type, level, pos, true);
        if (safe == null) return null;
        var bounds = type.getDimensions().makeBoundingBox(safe);
        if (!level.getWorldBorder().isWithinBounds(bounds) || !level.noCollision(null, bounds) || level.containsAnyLiquid(bounds)
                || !level.getEntities((Entity) null, bounds, other -> !other.isSpectator() && (other instanceof LivingEntity || other.isPickable())).isEmpty()) return null;
        return safe;
    }
}
