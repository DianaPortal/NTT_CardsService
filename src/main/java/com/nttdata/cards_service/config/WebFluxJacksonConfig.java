package com.nttdata.cards_service.config;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.*;
import lombok.extern.slf4j.*;
import org.openapitools.jackson.nullable.*;
import org.springframework.boot.autoconfigure.jackson.*;
import org.springframework.boot.web.codec.*;
import org.springframework.context.annotation.*;
import org.springframework.http.codec.json.*;

import java.time.*;
import java.util.*;


@Configuration
@Slf4j
public class WebFluxJacksonConfig {

  @Bean
  public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
    log.debug("Aplicando customizer de Jackson para WebFlux");
    return builder -> builder
        .modules(new JavaTimeModule(), new JsonNullableModule())
        .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .timeZone(TimeZone.getTimeZone(ZoneOffset.UTC));
  }

  @Bean
  public CodecCustomizer jacksonCodecs(ObjectMapper mapper) {
    return cfg -> {
      cfg.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(mapper));
      cfg.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(mapper));
    };
  }
}