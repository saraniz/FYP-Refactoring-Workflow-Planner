package com.amie.refactor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MainTest {

    @Test
    void testBasicCalculation() {
        int result = 10 + 20;

        assertEquals(30, result);
    }
}