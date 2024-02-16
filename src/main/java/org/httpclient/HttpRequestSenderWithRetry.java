package org.httpclient;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Denotes an HTTP request sender with retry mechanism.
 */
public interface HttpRequestSenderWithRetry {

    /**
     * Sends a GET request and retries if needed.
     *
     * @param request            the request
     * @param bodyHandler        the body handler
     * @param retryConfiguration the retry configuration
     * @param <T>                the response body type
     * @return the http response
     * @throws ExternalServiceUnavailableException if the external service is currently not available
     */
    <T> HttpResponse<T> send(
        HttpRequest request,
        HttpResponse.BodyHandler<T> bodyHandler,
        HttpRetryConfiguration retryConfiguration) throws ExternalServiceUnavailableException;

    /**
     * Checks if the external service is available.
     *
     * @return true if the service is currently available, false otherwise
     */
    boolean isExternalServiceAvailable();
}
