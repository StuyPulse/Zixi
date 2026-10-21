/************************ PROJECT ZIXI ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
package com.stuypulse.robot.util.talonfx.logged;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import org.junit.jupiter.api.Test;
import org.littletonrobotics.junction.LogTable;

public class LogTableUtilTest {
    private final LogTable BLANK_TABLE = new LogTable(0);

    @Test
    void testNoPublicConstructor() {
        final Constructor<?>[] constructors = LogTableUtil.class.getConstructors();
        assertTrue(
                constructors.length == 0,
                "Expected no public constructor(s), found " + constructors.length);
    }

    @Test
    void testPutThrowsWhenKeyOrTableNull() {
        assertThrows(
                NullPointerException.class,
                () -> {
                    LogTableUtil.put(null, "null", "null");
                });
        assertThrows(
                NullPointerException.class,
                () -> {
                    LogTableUtil.put(BLANK_TABLE, null, "null");
                });
    }

    @Test
    void testPutHandlesNullValue() {
        assertDoesNotThrow(
                () -> {
                    LogTableUtil.put(BLANK_TABLE, "null", null);
                });
    }

    @Test
    void testPutHandlesNullArrayValue() {
        assertDoesNotThrow(
                () -> {
                    LogTableUtil.put(BLANK_TABLE, "null", new Object[] {null});
                });
    }

    @Test
    void testGetThrowsWhenKeyOrTableNull() {
        assertThrows(
                NullPointerException.class,
                () -> {
                    LogTableUtil.get(null, "null", "null");
                });
        assertThrows(
                NullPointerException.class,
                () -> {
                    LogTableUtil.get(BLANK_TABLE, null, "null");
                });
    }

    @Test
    void testGetHandlesNullValue() {
        assertDoesNotThrow(
                () -> {
                    LogTableUtil.get(BLANK_TABLE, "null", null);
                });
    }

    @Test
    void testGetHandlesNullArrayValue() {
        assertDoesNotThrow(
                () -> {
                    LogTableUtil.get(BLANK_TABLE, "null", new Object[] {null});
                });
    }
}
