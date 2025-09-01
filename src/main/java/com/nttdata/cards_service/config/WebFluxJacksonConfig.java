package com.nttdata.cards_service.config;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneOffset;
import java.util.TimeZone;

@Configuration
@Slf4j
public class WebFluxJacksonConfig  {
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer webfluxJacksonCustomizer() {
        log.debug("Aplicando customizer de Jackson para WebFlux");
        return builder -> builder
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .timeZone(TimeZone.getTimeZone(ZoneOffset.UTC))
                .simpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    }
}