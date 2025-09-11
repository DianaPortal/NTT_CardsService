package com.nttdata.cards_service.config;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class JacksonTimeConfig {
  @Bean
  public Module javaTimeModule() {
    log.debug("Registrando JavaTimeModule en Jackson");
    return new JavaTimeModule();
  }
}
