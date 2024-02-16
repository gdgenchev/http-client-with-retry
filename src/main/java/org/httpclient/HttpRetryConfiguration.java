package org.httpclient;

import java.time.Duration;

public record HttpRetryConfiguration(Duration requestTimeout,
                                     int attempts,
                                     boolean shouldCheckAvailability,
                                     Duration checkAvailabilityInterval) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Duration requestTimeout;
        private int attempts;
        private boolean shouldCheckAvailability;
        private Duration checkAvailabilityInterval;

        public Builder withRequestTimeout(Duration requestTimeout) {
            this.requestTimeout = requestTimeout;
            return this;
        }

        public Builder withAttempts(int attempts) {
            this.attempts = attempts;
            return this;
        }

        public Builder withShouldCheckAvailability(boolean shouldCheckAvailability) {
            this.shouldCheckAvailability = shouldCheckAvailability;
            return this;
        }

        public Builder withCheckAvailabilityInterval(Duration checkAvailabilityInterval) {
            this.checkAvailabilityInterval = checkAvailabilityInterval;
            return this;
        }

        public HttpRetryConfiguration build() {
            return new HttpRetryConfiguration(
                requestTimeout,
                attempts,
                shouldCheckAvailability,
                checkAvailabilityInterval);
        }
    }
}
