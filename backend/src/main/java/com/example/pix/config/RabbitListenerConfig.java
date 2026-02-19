package com.example.pix.config;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitListenerConfig {

  @Bean
  public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory cf) {
    var f = new SimpleRabbitListenerContainerFactory();
    f.setConnectionFactory(cf);
    // important: quando lançar exceção, NÃO requeue (senão vira loop)
    f.setDefaultRequeueRejected(false);
    return f;
  }
}
