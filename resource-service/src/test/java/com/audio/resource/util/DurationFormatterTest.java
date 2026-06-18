package com.audio.resource.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DurationFormatterTest {

    @Test
    void toMmSs_ValidSeconds_FormatsCorrectly() {
        assertEquals("00:00", DurationFormatter.toMmSs("0"));
        assertEquals("00:01", DurationFormatter.toMmSs("1"));
        assertEquals("00:59", DurationFormatter.toMmSs("59"));
        assertEquals("01:00", DurationFormatter.toMmSs("60"));
        assertEquals("01:30", DurationFormatter.toMmSs("90"));
        assertEquals("02:05", DurationFormatter.toMmSs("125.5"));
        assertEquals("10:30", DurationFormatter.toMmSs("630"));
    }

    @Test
    void toMmSs_Null_ReturnsZero() {
        assertEquals("00:00", DurationFormatter.toMmSs(null));
    }

    @Test
    void toMmSs_Empty_ReturnsZero() {
        assertEquals("00:00", DurationFormatter.toMmSs(""));
    }

    @Test
    void toMmSs_InvalidNumber_ReturnsZero() {
        assertEquals("00:00", DurationFormatter.toMmSs("abc"));
        assertEquals("00:00", DurationFormatter.toMmSs("12.34.56"));
    }
}
