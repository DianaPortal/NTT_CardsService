package com.nttdata.cards_service.config;


import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class ResilienceConfig {

  @Bean
  public CircuitBreakerRegistry circuitBreakerRegistry() {
    return CircuitBreakerRegistry.ofDefaults();
  }

  @Bean
  public TimeLimiterRegistry timeLimiterRegistry(
      @Value("${resilience4j.timelimiter.timeout-seconds:2}") long seconds) {
    return TimeLimiterRegistry.of(
        TimeLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(seconds)).build());
  }
}
