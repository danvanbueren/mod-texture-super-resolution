package me.danvb10.mtsr.config.components;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RichWindowTypesTest {

    @Test
    void declaresAllExpectedConstants() {
        assertEquals(7, RichWindowTypes.values().length);
    }

    @Test
    void constantsAreDeclaredInExpectedOrder() {
        RichWindowTypes[] expected = {
                RichWindowTypes.GENERAL_SETTINGS_WINDOW,
                RichWindowTypes.MODEL_SETTINGS_WINDOW,
                RichWindowTypes.QUICK_ACTIONS_WINDOW,
                RichWindowTypes.TEXTURE_MANAGER_WINDOW,
                RichWindowTypes.ACTIVITY_MONITOR_WINDOW,
                RichWindowTypes.ACTIVITY_LOG_WINDOW,
                RichWindowTypes.DEFAULT_WINDOW,
        };
        assertEquals(expected.length, RichWindowTypes.values().length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(i, expected[i].ordinal());
        }
    }

    @Test
    void valueOfResolvesEachConstantByName() {
        for (RichWindowTypes type : RichWindowTypes.values()) {
            assertSame(type, RichWindowTypes.valueOf(type.name()));
        }
    }

    @Test
    void valueOfRejectsUnknownName() {
        assertThrows(IllegalArgumentException.class, () -> RichWindowTypes.valueOf("NOT_A_WINDOW"));
    }

    @Test
    void hasDefaultFallbackConstant() {
        assertNotNull(RichWindowTypes.DEFAULT_WINDOW);
    }
}
