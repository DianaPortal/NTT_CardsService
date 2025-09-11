package com.nttdata.cards_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.nttdata.cards_service.model.CardMovement;
import com.nttdata.cards_service.model.CardResponse;
import com.nttdata.cards_service.model.PrimaryAccountBalance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
  @Value("${spring.redis.host:localhost}")
  String host;
  @Value("${spring.redis.port:6379}")
  int port;

  /*@Bean
  public ObjectMapper objectMapper() {
    // Creamos una instancia de ObjectMapper y la configuramos
    ObjectMapper mapper = new ObjectMapper();

    // Registramos el módulo para los tipos de fecha y hora de Java 8
    JavaTimeModule javaTimeModule = new JavaTimeModule();

    // Opcional: Si quieres controlar el formato de serialización de LocalDate, puedes añadir un serializador
    // javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ISO_LOCAL_DATE));

    mapper.registerModule(javaTimeModule);

    return mapper;
  }*/

  @Bean
  public ReactiveRedisTemplate<String, CardResponse> cardResponseRedisTemplate(
      ReactiveRedisConnectionFactory factory, ObjectMapper objectMapper) {

    Jackson2JsonRedisSerializer<CardResponse> serializer =
        new Jackson2JsonRedisSerializer<>(CardResponse.class);
    serializer.setObjectMapper(objectMapper);

    RedisSerializationContext<String, CardResponse> ctx =
        RedisSerializationContext.<String, CardResponse>newSerializationContext(new StringRedisSerializer())
            .value(serializer).build();
    return new ReactiveRedisTemplate<>(factory, ctx);
  }

  @Bean
  public ReactiveRedisTemplate<String, PrimaryAccountBalance> primaryBalanceRedisTemplate(
      ReactiveRedisConnectionFactory factory,  ObjectMapper objectMapper) {

    Jackson2JsonRedisSerializer<PrimaryAccountBalance> serializer =
        new Jackson2JsonRedisSerializer<>(PrimaryAccountBalance.class);
    serializer.setObjectMapper(objectMapper);

    RedisSerializationContext<String, PrimaryAccountBalance> ctx =
        RedisSerializationContext.<String, PrimaryAccountBalance>newSerializationContext(new StringRedisSerializer())
            .value(serializer).build();
    return new ReactiveRedisTemplate<>(factory, ctx);
  }

  @Bean
  public ReactiveRedisTemplate<String, CardMovement[]> cardMovementsRedisTemplate(
      ReactiveRedisConnectionFactory factory, ObjectMapper objectMapper) {

    Jackson2JsonRedisSerializer<CardMovement[]> serializer =
        new Jackson2JsonRedisSerializer<>(CardMovement[].class);
    serializer.setObjectMapper(objectMapper);

    RedisSerializationContext<String, CardMovement[]> ctx =
        RedisSerializationContext.<String, CardMovement[]>newSerializationContext(new StringRedisSerializer())
            .value(serializer).build();
    return new ReactiveRedisTemplate<>(factory, ctx);
  }
}


