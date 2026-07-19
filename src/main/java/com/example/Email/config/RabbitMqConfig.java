package com.example.Email.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

	@Bean
	Queue emailVerificationQueue(@Value("${email.rabbitmq.queue}") String queueName) {
		return new Queue(queueName, true);
	}

	// @Bean
	// MessageConverter messageConverter() {
	// return new JacksonJsonMessageConverter();
	// }
}
