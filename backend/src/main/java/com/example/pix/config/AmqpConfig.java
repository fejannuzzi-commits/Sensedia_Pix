package com.example.pix.config;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AmqpConfig {

  @Bean
  MessageConverter messageConverter() {
    return new SimpleMessageConverter();
  }

  @Bean
  RabbitTemplate rabbitTemplate(org.springframework.amqp.rabbit.connection.ConnectionFactory cf, MessageConverter mc) {
    RabbitTemplate t = new RabbitTemplate(cf);
    t.setMessageConverter(mc);
    return t;
  }
}
