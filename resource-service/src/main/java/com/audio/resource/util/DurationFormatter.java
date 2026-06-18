package com.audio.resource.util;

public final class DurationFormatter {

    private DurationFormatter() {}

    public static String toMmSs(String durationSeconds) {
        if (durationSeconds == null || durationSeconds.isEmpty()) {
            return "00:00";
        }
        try {
            double seconds = Double.parseDouble(durationSeconds);
            int totalSeconds = (int) seconds;
            int minutes = totalSeconds / 60;
            int secs = totalSeconds % 60;
            return String.format("%02d:%02d", minutes, secs);
        } catch (NumberFormatException e) {
            return "00:00";
        }
    }
}
