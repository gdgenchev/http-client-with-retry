# Java HTTP Client With Retry

## Idea
I had a scenario where my app needed to request something from another external API.
If that API is not stable, you might want to implement a retry mechanism that tries a few times to get a response and
then gives up. Also, it spawns a background process that starts maintaining the availability status of the external API,
so you can recover in the future if feasible.

The client is very light and has no external dependencies except for ```org.slf4j.slf4j-api``` for logging.
The HttpClient is from ```java.net.http```

For now, you can just fork the repo and use the code! Maybe I can also create a library if there is interest.

## Example usage

```java
HttpClient client = HttpClient.newBuilder()
    .version(HttpClient.Version.HTTP_2)
    .build();
HttpRequest request = HttpRequest.newBuilder()
    .GET()
    .uri(URI.create("https://www.example-unstable-api.org"))
    .build();
HttpRetryConfiguration retryConfig = HttpRetryConfiguration.builder()
    .withRequestTimeout(Duration.ofSeconds(10)) // Http request timeout for receiving a response
    .withAttempts(3) // Attempts that will be made to initially get a response
    .withShouldCheckAvailability(true) // Whether you want to check availability in the background
    .withCheckAvailabilityInterval(Duration.ofMinutes(1)) // Interval between checking availability in background
    .build();
HttpRequestSenderWithRetry sender = new DefaultHttpRequestSenderWithRetry(client);

try {
    sender.send(request, BodyHandlers.ofString(), retryConfig);
} catch (ExternalServiceUnavailableException e) {
    // Handle exception
    throw e;
}

// You can also check if the service is currently available
if (sender.isExternalServiceAvailable()) {
    // Do something
} else {
    // Do something else
}
```
