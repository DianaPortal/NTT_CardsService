package com.nttdata.cards_service.config;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.jackson.nullable.JsonNullableModule;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.ZoneOffset;
import java.util.TimeZone;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import com. fasterxml. jackson. databind.ObjectMapper;
import org.springframework.boot.web.codec.CodecCustomizer;
import java.io.IOException;


@Configuration
@Slf4j
public class WebFluxJacksonConfig  {

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
        // Asegura que WebFlux use ESTE ObjectMapper (con los módulos arriba)
        return cfg -> {
            cfg.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(mapper));
            cfg.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(mapper));
        };
    }
}