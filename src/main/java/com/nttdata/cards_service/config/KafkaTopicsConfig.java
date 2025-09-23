package com.nttdata.cards_service.config;

import org.apache.kafka.clients.admin.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.*;
import org.springframework.kafka.config.*;

@Configuration
public class KafkaTopicsConfig {

  @Bean
  NewTopic cardDebitReq(@Value("${app.topics.card-debit-req}") String n) {
    return TopicBuilder.name(n).partitions(3).replicas(1).build();
  }

  @Bean
  NewTopic cardDebitApplied(@Value("${app.topics.card-debit-applied}") String n) {
    return TopicBuilder.name(n).partitions(3).replicas(1).build();
  }

  @Bean
  NewTopic cardDebitDenied(@Value("${app.topics.card-debit-denied}") String n) {
    return TopicBuilder.name(n).partitions(3).replicas(1).build();
  }

  @Bean
  NewTopic cardCreditReq(@Value("${app.topics.card-credit-req}") String n) {
    return TopicBuilder.name(n).partitions(3).replicas(1).build();
  }

  @Bean
  NewTopic cardCreditApplied(@Value("${app.topics.card-credit-applied}") String n) {
    return TopicBuilder.name(n).partitions(3).replicas(1).build();
  }

  @Bean
  NewTopic cardCreditDenied(@Value("${app.topics.card-credit-denied}") String n) {
    return TopicBuilder.name(n).partitions(3).replicas(1).build();
  }

  @Bean
  NewTopic cardPrimaryBalanceUpdated(@Value("${app.topics.card-primary-balance-updated}") String n) {
    return TopicBuilder.name(n).partitions(3).replicas(1).build();
  }
}