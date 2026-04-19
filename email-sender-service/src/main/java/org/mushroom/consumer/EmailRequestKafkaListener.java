package org.mushroom.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.mushroom.exception.EmailEventDeserializationException;
import org.mushroom.exception.EmailEventValidationException;
import org.mushroom.service.EmailIngestionService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailRequestKafkaListener {

    private final EmailIngestionService emailIngestionService;

    @KafkaListener(
            topics = "${email-ingestion.topics.user-registered}",
            containerFactory = "emailKafkaListenerContainerFactory"
    )
    public void listenUserRegistered(
            ConsumerRecord<String, JsonNode> record,
            Acknowledgment acknowledgment
    ) throws EmailEventValidationException, EmailEventDeserializationException {

        emailIngestionService.ingestUserRegistered(record.value());

        acknowledgment.acknowledge();

        log.info("Kafka email event processed successfully: topic={}, partition={}, offset={}",
                record.topic(), record.partition(), record.offset());
    }

    @KafkaListener(
            topics = "${email-ingestion.topics.daily-report}",
            containerFactory = "emailKafkaListenerContainerFactory"
    )
    public void listenDailyReport(
            ConsumerRecord<String, JsonNode> record,
            Acknowledgment acknowledgment
    ) throws EmailEventValidationException, EmailEventDeserializationException {

        emailIngestionService.ingestDailyReport(record.value());

        acknowledgment.acknowledge();

        log.info("Kafka email event processed successfully: topic={}, partition={}, offset={}",
                record.topic(), record.partition(), record.offset());
    }
}