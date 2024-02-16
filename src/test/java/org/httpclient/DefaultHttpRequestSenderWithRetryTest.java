package org.httpclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultHttpRequestSenderWithRetryTest {

    @Mock
    private HttpClient httpClient;

    @InjectMocks
    private DefaultHttpRequestSenderWithRetry httpRequestSender;

    @Test
    void isServiceAvailableInitiallyReturnsTrue() {
        assertTrue(httpRequestSender.isExternalServiceAvailable());
    }

    @Test
    void sendWhenServiceIsAvailableReturnsResponse()
        throws IOException, InterruptedException, ExternalServiceUnavailableException {
        HttpResponse<?> httpResponse = mock(HttpResponse.class);

        when(httpClient.send(any(HttpRequest.class), any(BodyHandler.class))).thenReturn(httpResponse);

        var response = httpRequestSender.send(createRequest(), createBodyHandler(), createRetryConfiguration());

        assertEquals(httpResponse, response);
    }

    @Test
    void sendWhenExternalServiceFailsSetsExternalServiceAvailableToFalseAndThrows()
        throws IOException, InterruptedException {
        when(httpClient.send(any(HttpRequest.class), any(BodyHandler.class))).thenThrow(IOException.class);

        assertThrows(ExternalServiceUnavailableException.class,
            () -> httpRequestSender.send(createRequest(), createBodyHandler(), createRetryConfiguration()));
        assertFalse(httpRequestSender.isExternalServiceAvailable());
    }

    @Test
    void sendWhenExternalServiceFailsAndThenSucceedsReturnsResponse()
        throws ExternalServiceUnavailableException, IOException, InterruptedException {
        HttpResponse<?> httpResponse = mock(HttpResponse.class);

        when(httpClient.send(any(HttpRequest.class), any(BodyHandler.class))).thenThrow(IOException.class)
            .thenReturn(httpResponse);

        var response = httpRequestSender.send(createRequest(), createBodyHandler(), createRetryConfiguration());

        assertEquals(httpResponse, response);
    }

    @Test
    void sendWhenExternalServiceFailsAndThenSucceedsSetsExternalServiceAvailableToTrue()
        throws ExternalServiceUnavailableException, IOException, InterruptedException {
        HttpResponse<?> httpResponse = mock(HttpResponse.class);

        when(httpClient.send(any(HttpRequest.class), any(BodyHandler.class))).thenThrow(IOException.class)
            .thenReturn(httpResponse);

        httpRequestSender.send(createRequest(), createBodyHandler(), createRetryConfiguration());

        assertTrue(httpRequestSender.isExternalServiceAvailable());
    }

    @Test
    void sendWhenServiceUnavailableAndRetryFailsThrows() throws IOException, InterruptedException {
        when(httpClient.send(any(HttpRequest.class), any(BodyHandler.class))).thenThrow(IOException.class);
        assertThrows(ExternalServiceUnavailableException.class,
            () -> httpRequestSender.send(createRequest(), createBodyHandler(), createRetryConfiguration()));
    }

    private HttpRequest createRequest() {
        return HttpRequest.newBuilder()
            .GET()
            .uri(URI.create("https://www.example-unstable-api.org"))
            .build();
    }

    private HttpResponse.BodyHandler<String> createBodyHandler() {
        return HttpResponse.BodyHandlers.ofString();
    }

    private HttpRetryConfiguration createRetryConfiguration() {
        return HttpRetryConfiguration.builder()
            .withRequestTimeout(Duration.ofSeconds(10))
            .withAttempts(3)
            .withShouldCheckAvailability(true)
            .withCheckAvailabilityInterval(Duration.ofSeconds(5))
            .build();
    }
}
