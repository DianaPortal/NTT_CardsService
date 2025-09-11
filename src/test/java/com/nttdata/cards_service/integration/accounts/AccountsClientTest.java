package com.nttdata.cards_service.integration.accounts;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AccountsClientTest {

  @Test
  void getAccount_enviaPathCorrecto_yDevuelveDTO() throws Exception {
    MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setResponseCode(200)
        .setHeader("Content-Type","application/json")
        .setBody("{\"id\":\"A1\",\"balance\":123.45}"));
    server.start();
    String base = server.url("/api/v1").toString();

    AccountsClient client = new AccountsClient(
        WebClient.builder().build(),
        CircuitBreakerRegistry.ofDefaults(),
        TimeLimiterRegistry.of(TimeLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(2)).build())
    );
    ReflectionTestUtils.setField(client, "baseUrl", base);

    StepVerifier.create(client.getAccount("A1"))
        .expectNextMatches(a -> "A1".equals(a.getId()) && a.getBalance() == 123.45)
        .verifyComplete();

    RecordedRequest req = server.takeRequest();
    assertEquals("/api/v1/accounts/A1", req.getRequestUrl().encodedPath());
    server.shutdown();
  }

  @Test
  void getAccount_timeout_2s_mapea504() throws Exception {
    MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setBodyDelay(3, TimeUnit.SECONDS)
        .setResponseCode(200)
        .setHeader("Content-Type","application/json")
        .setBody("{\"id\":\"A1\",\"balance\":100}"));
    server.start();
    String base = server.url("/api/v1").toString();

    AccountsClient client = new AccountsClient(
        WebClient.builder().build(),
        CircuitBreakerRegistry.ofDefaults(),
        TimeLimiterRegistry.of(TimeLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(2)).build())
    );
    ReflectionTestUtils.setField(client, "baseUrl", base);

    StepVerifier.create(client.getAccount("A1"))
        .expectError(ResponseStatusException.class) // 504
        .verify();

    server.shutdown();
  }
}
