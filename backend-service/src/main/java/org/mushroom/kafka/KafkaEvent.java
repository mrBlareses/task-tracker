package org.mushroom.kafka;

public record KafkaEvent<T>(T payload) {
}
