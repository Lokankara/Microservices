package com.audio.processor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

class Mp3MetadataExtractorTest {

    private final Mp3MetadataExtractor extractor = new Mp3MetadataExtractor();

    @Test
    void extractWithInvalidDataThrowsException() {
        byte[] invalidData = new byte[0];
        assertThrows(IllegalArgumentException.class, () -> extractor.extract(invalidData));
    }

    @Test
    void extractWithNullDataThrowsException() {
        assertThrows(NullPointerException.class, () -> extractor.extract(null));
    }

    @ParameterizedTest
    @MethodSource("provideDurationFormats")
    void formatDurationFormatsCorrectly(String rawMs, String expected) {
        try {
            java.lang.reflect.Method method =
                    Mp3MetadataExtractor.class.getDeclaredMethod("formatDuration", String.class);
            method.setAccessible(true);
            String result = (String) method.invoke(extractor, rawMs);
            assertEquals(expected, result);
        } catch (Exception e) {
            fail("Reflection failed");
        }
    }

    private static Stream<Arguments> provideDurationFormats() {
        return Stream.of(
                Arguments.of("180000", "03:00"),
                Arguments.of("61000", "01:01"),
                Arguments.of(null, "00:00"),
                Arguments.of("", "00:00"),
                Arguments.of("invalid", "00:00")
        );
    }

    @ParameterizedTest
    @MethodSource("provideCoalesceValues")
    void coalesceReturnsFirstNonNull(String[] values, String expected) {
        try {
            java.lang.reflect.Method method = Mp3MetadataExtractor.class.getDeclaredMethod("coalesce", String[].class);
            method.setAccessible(true);
            String result = (String) method.invoke(extractor, (Object) values);
            assertEquals(expected, result);
        } catch (Exception e) {
            fail("Reflection failed");
        }
    }

    private static Stream<Arguments> provideCoalesceValues() {
        return Stream.of(
                Arguments.of(new String[] {"first", "second"}, "first"),
                Arguments.of(new String[] {null, "second"}, "second"),
                Arguments.of(new String[] {null, null, "third"}, "third"),
                Arguments.of(new String[] {"", "second"}, "second"),
                Arguments.of(new String[] {null}, null)
        );
    }
}
