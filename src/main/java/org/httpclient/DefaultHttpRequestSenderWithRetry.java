package org.httpclient;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.httpclient.util.DurationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultHttpRequestSenderWithRetry implements HttpRequestSenderWithRetry {

    private static final Logger log = LoggerFactory.getLogger(DefaultHttpRequestSenderWithRetry.class);

    private final HttpClient httpClient;

    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    private final AtomicBoolean isServiceAvailable = new AtomicBoolean(true);
    private final AtomicBoolean isBackgroundRetryRunning = new AtomicBoolean(false);

    public DefaultHttpRequestSenderWithRetry(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public <T> HttpResponse<T> send(
        HttpRequest request,
        BodyHandler<T> bodyHandler,
        HttpRetryConfiguration retryConfiguration) throws ExternalServiceUnavailableException {
        request = HttpRequest.newBuilder(request, (headerName, headerValue) -> true)
            .timeout(retryConfiguration.requestTimeout())
            .build();

        try {
            return httpClient.send(request, bodyHandler);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }

            log.info("Failed to send request {}. Service is unavailable. Will retry", request, e);

            setServiceUnavailable();

            var retryResponse = retrySendingRequest(request, bodyHandler, retryConfiguration);

            if (retryResponse.isPresent()) {
                return retryResponse.get();
            }

            if (retryConfiguration.shouldCheckAvailability()) {
                checkAvailabilityInBackground(request, bodyHandler, retryConfiguration);
            }

            throw new ExternalServiceUnavailableException(e);
        }
    }

    private void setServiceUnavailable() {
        isServiceAvailable.set(false);
    }

    private <T> Optional<HttpResponse<T>> retrySendingRequest(
        HttpRequest request,
        BodyHandler<T> bodyHandler,
        HttpRetryConfiguration retryConfiguration) {
        int currentRetry = 0;

        while (currentRetry < retryConfiguration.attempts()) {
            try {
                HttpResponse<T> response = httpClient.send(request, bodyHandler);
                isServiceAvailable.set(true);
                isBackgroundRetryRunning.set(false);
                scheduledExecutorService.shutdown();

                log.info("Retry: service is now available");

                return Optional.of(response);
            } catch (IOException | InterruptedException exc) {
                if (exc instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }

                currentRetry++;
                log.debug("Retry: sending request {} failed {} times.", request, currentRetry, exc);
            }
        }

        log.info("Giving up sending request {} after {} retries.", request, retryConfiguration.attempts());

        return Optional.empty();
    }

    private <T> void checkAvailabilityInBackground(
        HttpRequest request,
        BodyHandler<T> bodyHandler,
        HttpRetryConfiguration retryConfiguration) {
        if (backgroundTaskAlreadyRunning()) {
            return;
        }

        String prettyDuration = DurationUtils.getPrettyDuration(retryConfiguration.checkAvailabilityInterval());
        log.debug("Starting background task to monitor service availability every {} ", prettyDuration);

        scheduledExecutorService.scheduleAtFixedRate(
            getRetryTask(request, bodyHandler, retryConfiguration),
            retryConfiguration.checkAvailabilityInterval().toSeconds(),
            retryConfiguration.checkAvailabilityInterval().toSeconds(),
            TimeUnit.SECONDS);
    }

    private <T> Runnable getRetryTask(
        HttpRequest request,
        BodyHandler<T> bodyHandler,
        HttpRetryConfiguration retryConfiguration) {
        return () -> retrySendingRequest(request, bodyHandler, retryConfiguration);
    }

    private boolean backgroundTaskAlreadyRunning() {
        return !isBackgroundRetryRunning.compareAndSet(false, true);
    }

    @Override
    public boolean isExternalServiceAvailable() {
        return isServiceAvailable.get();
    }
}
