package com.nttdata.cards_service.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class WebClientConfig {

    private ReactorClientHttpConnector connector() {
        reactor.netty.http.client.HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                .responseTimeout(Duration.ofSeconds(5))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(5))
                        .addHandlerLast(new WriteTimeoutHandler(5)));
        return new ReactorClientHttpConnector(httpClient);
    }

    private WebClient.Builder base() {
        log.debug("Creando WebClient base (timeouts y buffers configurados)");
        return WebClient.builder()
                .clientConnector(connector())
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(c -> c.defaultCodecs().maxInMemorySize(4 * 1024 * 1024))
                        .build());
    }

    //Cliente hacia el microservice de cuentas
    @Bean WebClient accountsWebClient(@Value("${service.accounts.base-url}") String url) {
        return base().baseUrl(url).build();
    }
    //Cliente hacia el microservice de créditos
    @Bean WebClient creditsWebClient(@Value("${service.credits.base-url}") String url) {
        return base().baseUrl(url).build();
    }
    //Cliente hacia el microservice de transacciones
    @Bean WebClient transactionsWebClient(@Value("${service.transactions.base-url}") String url) {
        return base().baseUrl(url).build();
    }
}
