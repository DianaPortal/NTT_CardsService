package com.nttdata.cards_service.integration.transactions;

import com.nttdata.cards_service.integration.transactions.dto.TxGet;
import com.nttdata.cards_service.integration.transactions.dto.TxPost;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class TransactionsClientTimeoutTest {

  private MockWebServer server;
  private TransactionsClient client;

  @BeforeEach
  void setUp() throws IOException {
    server = new MockWebServer();
    server.start();

    WebClient wc = WebClient.builder().baseUrl(server.url("/").toString()).build();
    CircuitBreakerRegistry cb = CircuitBreakerRegistry.ofDefaults();
    TimeLimiterRegistry tl = TimeLimiterRegistry.of(TimeLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(2)).build());

    client = new TransactionsClient(wc, cb, tl);
  }

  @AfterEach
  void tearDown() throws IOException {
    server.shutdown();
  }

  @Test
  void create_timesOut_mapsTo504() {
    server.enqueue(new MockResponse()
        .setBody("{\"id\":\"T1\"}")
        .addHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        .setBodyDelay(3, java.util.concurrent.TimeUnit.SECONDS));

    TxPost post = new TxPost();
    post.setType("purchase"); post.setAmount(10.0);

    Mono<TxGet> mono = client.create(post);

    StepVerifier.create(mono)
        .expectErrorSatisfies(ex -> {
          assertInstanceOf(org.springframework.web.server.ResponseStatusException.class, ex);
          Assertions.assertEquals(504, ((ResponseStatusException) ex).getStatus().value());
        })
        .verify();
  }

  @Test
  void findByProduct_ok() {
    server.enqueue(new MockResponse()
        .setBody("{\"transactions\":[{\"id\":\"T1\"}]}")
        .addHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE));
    StepVerifier.create(client.findByProduct("P1"))
        .expectNextMatches(tx -> "T1".equals(tx.getId()))
        .verifyComplete();
  }
}
