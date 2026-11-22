package net.j40climb.frenchies.test;

import net.j40climb.frenchies.Frenchies;
import net.j40climb.frenchies.common.entity.frenchie.DiggingAssistantGameTests;
import net.j40climb.frenchies.common.entity.frenchie.AmbientSnowGameTests;
import net.j40climb.frenchies.common.entity.frenchie.PlayInterruptionGameTests;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.function.Consumer;

public final class CompanionTests {
    private static final Identifier EMPTY_STRUCTURE = Identifier.withDefaultNamespace("empty");

    public static void register(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> defaultEnv = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(Frenchies.MOD_ID, "default"),
                new TestEnvironmentDefinition.AllOf());
        var yard = Identifier.fromNamespaceAndPath(Frenchies.MOD_ID, "companion_yard");
        registerPlayTest(event, yard, "companion_whistle_unloaded_dismiss", 5000, CompanionWhistleTests::unloadedDismiss);
        registerPlayTest(event, yard, "companion_whistle_offline_void", 350, CompanionSafetyTests::offlineVoid);
        registerPlayTest(event, yard, "companion_whistle_fall", 180, CompanionSafetyTests::falling);
        registerPlayTest(event, yard, "companion_whistle_escape_magma", 250, h -> CompanionSafetyTests.contact(h, Blocks.MAGMA_BLOCK));
        registerPlayTest(event, yard, "companion_whistle_cactus", 250, h -> CompanionSafetyTests.contact(h, Blocks.CACTUS));
        registerPlayTest(event, yard, "companion_whistle_fire", 250, h -> CompanionSafetyTests.contact(h, Blocks.FIRE));
        registerPlayTest(event, yard, "companion_whistle_suffocation", 250, h -> CompanionSafetyTests.contact(h, Blocks.STONE));
        registerPlayTest(event, yard, "companion_whistle_drowning", 350, h -> CompanionSafetyTests.contact(h, Blocks.WATER));
        registerPlayTest(event, yard, "companion_whistle_freezing", 250, h -> CompanionSafetyTests.contact(h, Blocks.POWDER_SNOW));
        registerPlayTest(event, yard, "companion_whistle_selection", 650, CompanionWhistleTests::selection);
        registerPlayTest(event, yard, "companion_whistle_dimension", 4000, CompanionWhistleTests::dimension);
        registerPlayTest(event, yard, "companion_whistle_damage", 650, CompanionSafetyTests::damage);
        registerPlayTest(event, yard, "companion_whistle_reactions", 650, CompanionSafetyTests::reactions);
        registerPlayTest(event, yard, "companion_whistle_combat", 650, CompanionSafetyTests::combat);
        registerPlayTest(event, yard, "companion_whistle_void", 650, h -> CompanionSafetyTests.rescue(h, true));
        registerPlayTest(event, yard, "companion_whistle_trapped", 650, h -> CompanionSafetyTests.rescue(h, false));
        registerPlayTest(event, yard, "companion_whistle_packed_call", 650, h -> CompanionWhistleTests.courier(h, false, true));
        registerPlayTest(event, yard, "companion_whistle_packed_dismiss", 650, h -> CompanionWhistleTests.courier(h, true, true));
        registerPlayTest(event, yard, "companion_whistle_transit_call", 650, h -> CompanionWhistleTests.courier(h, false, false));
        registerPlayTest(event, yard, "companion_whistle_transit_dismiss", 650, h -> CompanionWhistleTests.courier(h, true, false));
        registerPlayTest(event, yard, "companion_whistle_boat_call", 650, h -> CompanionWhistleTests.boatOrPlay(h, true, false));
        registerPlayTest(event, yard, "companion_whistle_boat_dismiss", 650, h -> CompanionWhistleTests.boatOrPlay(h, true, true));
        registerPlayTest(event, yard, "companion_whistle_play_call", 650, h -> CompanionWhistleTests.boatOrPlay(h, false, false));
        registerPlayTest(event, yard, "companion_whistle_play_dismiss", 650, h -> CompanionWhistleTests.boatOrPlay(h, false, true));
        registerPlayTest(event, yard, "play_bubble_toy", 230, BubblePlayTests::toy);
        registerPlayTest(event, yard, "play_bubble_audience", 230, BubblePlayTests::audience);
        registerPlayTest(event, yard, "play_bubble_pops", 200, BubblePlayTests::pops);
        registerPlayTest(event, yard, "play_bubble_delivery", 150, BubblePlayTests::delivery);
        registerPlayTest(event, yard, "play_bubble_nose", 230, BubblePlayTests::nose);
        registerPlayTest(event, yard, "play_bubble_unreachable", 230, BubblePlayTests::unreachable);
        registerPlayTest(event, yard, "play_slime_sequence", 420, SlimePlayTests::sequence);
        registerPlayTest(event, yard, "play_slime_damage", 230, SlimePlayTests::damage);
        registerPlayTest(event, yard, "play_slime_warning", 200, SlimePlayTests::warning);
        registerPlayTest(event, yard, "play_slime_unsafe_footing", 150, SlimePlayTests::unsafeFooting);
        registerPlayTest(event, yard, "play_slime_blocked", 200, SlimePlayTests::blocked);
        registerPlayTest(event, yard, "play_slime_vanilla", 250, SlimePlayTests::vanilla);
        registerPlayTest(event, yard, "play_digging_assistant", 200, DiggingAssistantGameTests::sequenceAndCustody);
        registerPlayTest(event, yard, "play_digging_interrupt", 150, DiggingAssistantGameTests::commandInterrupts);
        registerPlayTest(event, yard, "play_weather_snowball", 250, WeatherPlayTests::snowball);
        registerPlayTest(event, yard, "play_weather_snow_ambient", 250, AmbientSnowGameTests::diveWithoutSnowball);
        registerPlayTest(event, yard, "play_weather_swim", 200, WeatherPlayTests::swim);
        registerPlayTest(event, yard, "play_trick_backflop", 200, TrickBellyTests::trick);
        registerPlayTest(event, yard, "play_belly_rub", 200, TrickBellyTests::belly);
        registerPlayTest(event, yard, "play_whistle_interruptions", 400, PlayInterruptionGameTests::whistleDuringPlay);
        registerPlayTest(event, yard, "companion_fetch", 550, CompanionTravelTests::fetch);
        registerStructureTest(event, defaultEnv, "companion_captain", yard, 550, CompanionTravelTests::captain);
        registerPlayTest(event, yard, "companion_zoomies_interrupt", 550, CompanionPlayTests::interruptCelebration);
        registerPlayTest(event, yard, "companion_audience", 550, CompanionPlayTests::audience);
        registerPlayTest(event, yard, "companion_zoomies_laps", 550, h -> CompanionPlayTests.zoomies(h, true));
        registerPlayTest(event, yard, "companion_zoomies_crops", 550, h -> CompanionPlayTests.zoomies(h, false));
        registerPlayTest(event, yard, "frenchie_warning_sight", 160, FrenchieAwarenessGameTests::warningSight);
        registerStructureTest(event, defaultEnv, "frenchie_bedtime", yard, 100, FrenchieAwarenessGameTests::bedtime);
        registerTest(event, defaultEnv, "frenchie_owner_orders", FrenchieGameTests::ownerOrders);
        registerTest(event, defaultEnv, "frenchie_death_trail", FrenchieTrackingGameTests::trackDeath);
        registerTest(event, defaultEnv, "frenchie_delivery_creative_parcel", FrenchieDeliveryGameTests::creativeParcel);
        registerTest(event, defaultEnv, "frenchie_compass_orders", FrenchieDeliveryGameTests::compassOrders);
        registerStructureTest(event, defaultEnv, "frenchie_home_recall",
                FrenchiePen.STRUCTURE, 3000, FrenchieRecallGameTests::recallParkedDog);
        registerStructureTest(event, defaultEnv, "frenchie_home_bed_interaction", yard, 100, FrenchieRecallGameTests::sendUsingBed);
        registerStructureTest(event, defaultEnv, "frenchie_delivery_packed_cancel", yard, 100, FrenchieDeliveryGameTests::packAndCancel);
        registerStructureTest(event, defaultEnv, "frenchie_delivery_recipient_full",
                FrenchiePen.STRUCTURE, 900, FrenchieDeliveryGameTests::fullRecipient);
        registerStructureTest(event, defaultEnv, "frenchie_delivery_recipient_disconnect",
                FrenchiePen.STRUCTURE, 500, FrenchieDeliveryGameTests::disconnectedRecipient);
        registerStructureTest(event, defaultEnv, "frenchie_delivery_cross_dimension",
                FrenchiePen.STRUCTURE, 2400, FrenchieDeliveryGameTests::crossDimension);
        registerStructureTest(event, defaultEnv, "frenchie_delivery_blocked_departure",
                FrenchiePen.STRUCTURE, 650, FrenchieDeliveryGameTests::blockedDeparture);
        registerTest(event, defaultEnv, "frenchie_delivery_legacy_bed_pickup", FrenchieDeliveryGameTests::legacyBedPickup);
        registerTest(event, defaultEnv, "frenchie_delivery_respawn_preview", FrenchieBedPreviewGameTest::respawnPreview);
        registerStructureTest(event, defaultEnv, "frenchie_delivery_unloaded_bed",
                FrenchiePen.STRUCTURE, 4000, FrenchieDeliveryGameTests::unloadedHomeBed);
        registerTest(event, defaultEnv, "frenchie_frontpack_respawn_release", FrenchieGameTests::frontpackRespawnRelease);
        registerTest(event, defaultEnv, "frenchie_frontpack_unequip_release", FrenchieDeliveryGameTests::unequipPackedCourier);
        registerTest(event, defaultEnv, "frenchie_frontpack_appearance", FrenchieGameTests::frontpackAppearance);
        registerTest(event, defaultEnv, "frenchie_frontpack_right_click_equip", FrontpackEquipGameTest::equipFromHand);
        registerTest(event, defaultEnv, "frenchie_frontpack_dye", FrontpackDyeGameTest::craftAndWash);
        registerTest(event, defaultEnv, "frenchie_frontpack_elytra", FrontpackElytraGameTest::craftAndRepair);
    }

    // Audience scans, production limits, and creeper warnings reach into adjacent structures.
    private static void registerPlayTest(RegisterGameTestsEvent event, Identifier yard, String name,
                                         int ticks, Consumer<GameTestHelper> test) {
        var environment = event.registerEnvironment(Identifier.fromNamespaceAndPath(Frenchies.MOD_ID, name),
                new TestEnvironmentDefinition.Weather(TestEnvironmentDefinition.Weather.Type.CLEAR));
        registerStructureTest(event, environment, name, yard, ticks, h -> {
            // The registered dimension clock is available when the server starts the case.
            var level = h.getLevel();
            var clock = level.dimensionType().defaultClock().orElseThrow();
            var daylight = TestEnvironmentDefinition.activate(new TestEnvironmentDefinition.ClockTime(clock, 6000), level);
            h.testInfo.addListener(new GameTestListener() {
                @Override public void testStructureLoaded(GameTestInfo info) {}
                @Override public void testAddedForRerun(GameTestInfo original, GameTestInfo copy, GameTestRunner runner) {}
                @Override public void testPassed(GameTestInfo info, GameTestRunner runner) { daylight.teardown(); }
                @Override public void testFailed(GameTestInfo info, GameTestRunner runner) { daylight.teardown(); }
            });
            test.accept(h);
        });
    }

    static void registerStructureTest(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> env,
            String name,
            Identifier structure,
            int maxTicks,
            Consumer<GameTestHelper> testFunction
    ) {
        Identifier testId = Identifier.fromNamespaceAndPath(Frenchies.MOD_ID, name);

        TestData<Holder<TestEnvironmentDefinition<?>>> testData = new TestData<>(
                env,
                structure,
                maxTicks,
                20,   // setupTicks - give block entities time to load
                true  // required
        );

        event.registerTest(testId, new SimpleGameTestInstance(testData, testFunction));
    }

    /**
     * Register a test with the default empty structure and settings.
     */
    static void registerTest(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> env,
            String name,
            Consumer<GameTestHelper> testFunction
    ) {
        Identifier testId = Identifier.fromNamespaceAndPath(Frenchies.MOD_ID, name);

        TestData<Holder<TestEnvironmentDefinition<?>>> testData = new TestData<>(
                env,
                EMPTY_STRUCTURE,
                100, // maxTicks
                0,   // setupTicks
                true  // required
        );

        // Create a SimpleGameTestInstance wrapper
        event.registerTest(testId, new SimpleGameTestInstance(testData, testFunction));
    }
}
