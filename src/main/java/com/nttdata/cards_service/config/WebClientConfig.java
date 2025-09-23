package com.nttdata.cards_service.config;

import io.netty.channel.*;
import io.netty.handler.timeout.*;
import lombok.extern.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.http.client.reactive.*;
import org.springframework.security.oauth2.server.resource.web.reactive.function.client.*;
import org.springframework.web.reactive.function.client.*;
import reactor.netty.http.client.*;

import java.time.*;
import java.util.concurrent.*;

@Configuration
@Slf4j
public class WebClientConfig {

  private WebClient.Builder rawBuilder() {
    HttpClient httpClient = HttpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
        .responseTimeout(Duration.ofSeconds(2))
        .doOnConnected(conn -> conn
            .addHandlerLast(new ReadTimeoutHandler(2, TimeUnit.SECONDS))
            .addHandlerLast(new WriteTimeoutHandler(2, TimeUnit.SECONDS)));

    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .filter(new ServerBearerExchangeFilterFunction())
        .filter(logRequest())
        .filter(logResponse())
        .exchangeStrategies(ExchangeStrategies.builder()
            .codecs(c -> c.defaultCodecs().maxInMemorySize(4 * 1024 * 1024))
            .build());
  }

  @Bean
  public WebClient accountsWebClient(
      @Value("${service.accounts.url:${service.accounts.base-url}}") String url) {
    String base = url.trim();
    log.info("[ACCOUNTS] baseUrl={}", base);
    return rawBuilder().baseUrl(base).build();
  }

  @Bean
  public WebClient creditsWebClient(
      @Value("${service.credits.url:${service.credits.base-url}}") String url) {
    String base = url.trim();
    log.info("[CREDITS] baseUrl={}", base);
    return rawBuilder().baseUrl(base).build();
  }

  @Bean
  public WebClient transactionsWebClient(
      @Value("${service.transactions.url:${service.transactions.base-url}}") String url) {
    String base = url.trim();
    log.info("[TRANSACTIONS] baseUrl={}", base);
    return rawBuilder().baseUrl(base).build();
  }

  private ExchangeFilterFunction logRequest() {
    return ExchangeFilterFunction.ofRequestProcessor(req -> {
      log.debug("WebClient Request: {} {}", req.method(), req.url());
      return reactor.core.publisher.Mono.just(req);
    });
  }

  private ExchangeFilterFunction logResponse() {
    return ExchangeFilterFunction.ofResponseProcessor(res -> {
      log.debug("WebClient Response: status={}", res.statusCode());
      return reactor.core.publisher.Mono.just(res);
    });
  }
}