package net.j40climb.frenchies.common.entity.frenchie;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdventureOutfitTest {
    @Test
    void traversalEquipmentOverridesDimensionAndWeatherGear() {
        assertEquals(AdventureOutfit.SCUBA, AdventureOutfit.select(true, true, true, true, true, true, true));
        assertEquals(AdventureOutfit.AVIATOR, AdventureOutfit.select(false, true, false, true, false, false, false));
        assertEquals(AdventureOutfit.CAPTAIN, AdventureOutfit.select(false, false, true, false, true, false, true));
        assertEquals(AdventureOutfit.MINING, AdventureOutfit.select(false, false, false, false, false, true, true));
        assertTrue(AdventureOutfit.SCUBA.hasHeadwear());
        assertFalse(AdventureOutfit.WINTER.hasHeadwear());
    }

    @Test
    void isolatedEntranceAndExitSamplesDoNotSwitchOutfits() {
        var cave = new AdventureContext.Hold(40, 60);
        for (int i = 0; i < 3; i++) cave.sample(true);
        assertFalse(cave.active());
        cave.sample(false);
        for (int i = 0; i < 4; i++) cave.sample(true);
        assertTrue(cave.active());
        for (int i = 0; i < 5; i++) cave.sample(false);
        assertTrue(cave.active());
        cave.sample(true);
        for (int i = 0; i < 6; i++) cave.sample(false);
        assertFalse(cave.active());
    }
}
