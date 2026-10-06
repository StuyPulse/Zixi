/************************ PROJECT PHIL ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
package com.stuypulse.robot;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.wpilib.hardware.hal.HAL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RobotContainerTest {
    @BeforeEach
    public void setup() {
        assert HAL.initialize();
    }

    @Test
    public void testRobotContainer() {
        assertDoesNotThrow(
                () -> {
                    new RobotContainer();
                });
    }
}
