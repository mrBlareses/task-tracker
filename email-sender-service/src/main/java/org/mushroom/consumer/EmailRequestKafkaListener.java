package org.mushroom.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.mushroom.model.EmailRequestedEvent;
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
            topics = "${email-ingestion.topic}",
            containerFactory = "emailKafkaListenerContainerFactory"
    )
    public void listen(
            ConsumerRecord<String, EmailRequestedEvent> record,
            Acknowledgment acknowledgment
    ) {
        boolean saved = emailIngestionService.ingest(record.value());

        acknowledgment.acknowledge();

        if (saved) {
            log.info("Kafka email event processed successfully: topic={}, partition={}, offset={}",
                    record.topic(), record.partition(), record.offset());
        }
    }
}