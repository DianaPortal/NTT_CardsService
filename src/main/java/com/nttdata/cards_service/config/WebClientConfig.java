package com.nttdata.cards_service.config;

import io.netty.channel.*;
import io.netty.handler.timeout.*;
import lombok.extern.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.http.client.reactive.*;
import org.springframework.web.reactive.function.client.*;
import reactor.netty.http.client.*;

import java.time.*;
import java.util.concurrent.*;

@Configuration
@Slf4j
public class WebClientConfig {

  private WebClient.Builder webClientBuilder() {
    HttpClient httpClient = HttpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
        .responseTimeout(Duration.ofSeconds(2))
        .doOnConnected(conn -> conn
            .addHandlerLast(new ReadTimeoutHandler(2, TimeUnit.SECONDS))
            .addHandlerLast(new WriteTimeoutHandler(2, TimeUnit.SECONDS)));
    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .filter(logRequest())
        .filter(logResponse())
        .exchangeStrategies(ExchangeStrategies.builder()
            .codecs(c -> c.defaultCodecs().maxInMemorySize(4 * 1024 * 1024))
            .build());
  }


  //Cliente hacia el microservice de cuentas
  @Bean
  WebClient accountsWebClient(@Value("${service.accounts.base-url}") String url) {
    return webClientBuilder().baseUrl(url).build();
  }

  //Cliente hacia el microservice de créditos
  @Bean
  WebClient creditsWebClient(@Value("${service.credits.base-url}") String url) {
    return webClientBuilder().baseUrl(url).build();
  }

  //Cliente hacia el microservice de transacciones
  @Bean
  WebClient transactionsWebClient(@Value("${service.transactions.base-url}") String url) {
    return webClientBuilder().baseUrl(url).build();
  }

  private ExchangeFilterFunction logRequest() {
    return ExchangeFilterFunction.ofRequestProcessor(req -> {
      log.debug("WebClient Request: {} {}", req.method(), req.url());
      req.headers().forEach((name, values) -> values.forEach(value -> log.debug("{}={}", name, value)));
      return reactor.core.publisher.Mono.just(req);
    });
  }

  private ExchangeFilterFunction logResponse() {
    return ExchangeFilterFunction.ofResponseProcessor(res -> {
      log.debug("WebClient Response: status={}", res.statusCode());
      res.headers().asHttpHeaders()
          .forEach((name, values) -> values.forEach(value -> log.debug("{}={}", name, value)));
      return reactor.core.publisher.Mono.just(res);
    });
  }
}
