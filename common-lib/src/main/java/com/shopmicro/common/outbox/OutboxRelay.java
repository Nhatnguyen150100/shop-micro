package com.shopmicro.common.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Tiến trình nền (message relay) của Outbox pattern: định kỳ quét các bản ghi
 * {@link OutboxEvent} chưa gửi, publish lên Kafka rồi đánh dấu đã gửi.
 *
 * <p>Quan trọng: chỉ đánh dấu {@code published=true} SAU KHI Kafka xác nhận
 * ({@code .get()}). Nếu publish lỗi → để nguyên, lần quét sau thử lại (at-least-once).
 *
 * <p>Lưu ý vận hành: nếu chạy nhiều instance cùng lúc, cần khoá hàng (vd
 * {@code SELECT ... FOR UPDATE SKIP LOCKED}) để tránh hai relay gửi trùng. Bản
 * học tập này chạy đơn instance nên giữ đơn giản.
 */
@Slf4j
@RequiredArgsConstructor
public class OutboxRelay {

  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final ObjectMapper objectMapper;

  @PersistenceContext
  private EntityManager entityManager;

  @Scheduled(fixedDelayString = "${outbox.relay.delay-ms:2000}")
  @Transactional
  public void publishPending() {
    List<OutboxEvent> batch = entityManager.createQuery(
            "select e from OutboxEvent e where e.published = false order by e.createdAt asc",
            OutboxEvent.class)
        .setMaxResults(100)
        .getResultList();

    if (batch.isEmpty()) {
      return;
    }

    for (OutboxEvent event : batch) {
      try {
        Class<?> type = Class.forName(event.getEventType());
        Object payload = objectMapper.readValue(event.getPayload(), type);
        // Gửi đồng bộ + chờ xác nhận trước khi đánh dấu đã gửi
        kafkaTemplate.send(event.getTopic(), event.getMsgKey(), payload).get();
        event.setPublished(true);
        event.setPublishedAt(Instant.now());
        log.debug("Outbox relay: đã publish {} -> {}", event.getId(), event.getTopic());
      } catch (Exception e) {
        // Không đánh dấu -> giữ lại để lần sau thử lại (Kafka/consumer idempotent)
        log.error("Outbox relay: publish {} thất bại, sẽ thử lại", event.getId(), e);
      }
    }
  }
}
