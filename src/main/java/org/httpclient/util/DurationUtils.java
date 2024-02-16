package org.httpclient.util;

import java.time.Duration;

public class DurationUtils {

    private DurationUtils() {
    }

    public static String getPrettyDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.minusHours(hours).toMinutes();
        long seconds = duration.minusHours(hours).minusMinutes(minutes).toSeconds();
        long millis = duration.minusHours(hours).minusMinutes(minutes).minusSeconds(seconds).toMillis();

        StringBuilder prettyDurationStringBuilder = new StringBuilder();

        if (hours > 0) {
            prettyDurationStringBuilder.append(hours).append("h ");
        }

        if (minutes > 0) {
            prettyDurationStringBuilder.append(minutes).append("m ");
        }

        if (seconds > 0) {
            prettyDurationStringBuilder.append(seconds).append("s ");
        }

        if (millis > 0) {
            prettyDurationStringBuilder.append(millis).append("ms");
        }

        String prettyDuration = prettyDurationStringBuilder.toString();

        if (prettyDuration.endsWith(" ")) {
            prettyDuration = prettyDuration.substring(0, prettyDuration.length() - 1);
        }

        return prettyDuration.isEmpty() ? "0ms" : prettyDuration;
    }

}
