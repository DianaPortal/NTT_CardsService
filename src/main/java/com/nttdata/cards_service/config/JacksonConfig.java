package com.nttdata.cards_service.config;

import com.fasterxml.jackson.databind.Module;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class JacksonConfig {
    @Bean
    public Module jsonNullableModule() {
        log.debug("Registrando JsonNullableModule en Jackson");
        return new JsonNullableModule();
    }
}
