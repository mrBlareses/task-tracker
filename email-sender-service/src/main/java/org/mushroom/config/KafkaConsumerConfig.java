package org.mushroom.config;

import lombok.RequiredArgsConstructor;
import org.mushroom.exception.EmailEventDeserializationException;
import org.mushroom.exception.EmailEventValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@RequiredArgsConstructor
public class KafkaConsumerConfig {

    @Value("${email-ingestion.concurrency}")
    private Integer concurrency;

    @Value("${email-ingestion.retry-interval-ms}")
    private Long retryIntervalMs;

    @Value("${email-ingestion.retry-max-attempts}")
    private Long retryMaxAttempts;

    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        DefaultErrorHandler handler =
                new DefaultErrorHandler(new FixedBackOff(retryIntervalMs, retryMaxAttempts));

        handler.addNotRetryableExceptions(
                EmailEventValidationException.class,
                EmailEventDeserializationException.class,
                IllegalArgumentException.class
        );

        return handler;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<Object, Object> emailKafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> consumerFactory,
            DefaultErrorHandler kafkaErrorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        configurer.configure(factory, consumerFactory);

        factory.setConcurrency(concurrency);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setCommonErrorHandler(kafkaErrorHandler);

        return factory;
    }
}