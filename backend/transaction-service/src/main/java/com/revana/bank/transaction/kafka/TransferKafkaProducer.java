package com.revana.bank.transaction.kafka;

// ════════════════════════════════════════════════════════════════════════════
// ── OLD CODE — no Kafka producer existed in transaction-service before F3.
//              account-service KafkaProducerService (reference pattern):
//
// @Service
// @RequiredArgsConstructor
// public class KafkaProducerService {
//     private final KafkaTemplate<String, Object> kafkaTemplate;
//
//     public void publishTransferEvent(TransferEvent event) {
//         kafkaTemplate.send("bank-transfer-topic", event)
//             .whenComplete((result, ex) -> {
//                 if (ex == null) System.out.println("MESSAGE SENT SUCCESSFULLY");
//                 else            ex.printStackTrace();
//             });
//     }
// }
// ════════════════════════════════════════════════════════════════════════════

// ── NEW CODE ─────────────────────────────────────────────────────────────────

import com.revana.bank.transaction.dto.FundTransferCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka producer for the fund-transfer feature.
 *
 * Publishes {@link FundTransferCompletedEvent} to the 'bank-transfer-topic'.
 *
 * Consumers on this topic:
 *   - notification-service  → sends customer email/SMS
 *   - audit-service         → persists a cross-service audit record
 *
 * Design notes:
 *   - The message key is the transactionReference so that events for the same
 *     transfer always land on the same partition, preserving order.
 *   - Publish is fire-and-forget; failures are logged but never propagate to
 *     the caller — a failed Kafka publish must not roll back a completed transfer.
 *   - Type headers are disabled in application.yaml so consumers are not
 *     forced to have this service's classes on their classpath.
 */
@Service
@RequiredArgsConstructor
public class TransferKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(TransferKafkaProducer.class);

    /** Matches the topic name used by account-service and notification-service. */
    private static final String TOPIC = "bank-transfer-topic";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publishes a FundTransferCompletedEvent after a transfer reaches a
     * terminal state (SUCCESS or FAILED).
     *
     * @param event fully-populated event payload
     */
    public void publishTransferCompleted(FundTransferCompletedEvent event) {

        log.info("Publishing transfer event to [{}]: ref={} status={}",
                TOPIC, event.getTransactionId(), event.getStatus());

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(TOPIC, event.getTransactionId(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Transfer event published successfully: ref={} partition={} offset={}",
                        event.getTransactionId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                // Log but never throw — Kafka failure must not affect the committed transfer
                log.error("Failed to publish transfer event: ref={} error={}",
                        event.getTransactionId(), ex.getMessage(), ex);
            }
        });
    }
}
