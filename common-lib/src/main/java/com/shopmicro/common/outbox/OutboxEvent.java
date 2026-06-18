package com.shopmicro.common.outbox;

import com.shopmicro.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Bản ghi OUTBOX — trái tim của Transactional Outbox pattern.
 *
 * <p>Thay vì gửi thẳng message lên Kafka trong giao dịch nghiệp vụ (dễ "dual-write":
 * commit DB xong nhưng gửi Kafka lỗi → mất message), ta GHI message vào bảng này
 * <b>trong cùng transaction</b> với dữ liệu nghiệp vụ. Một tiến trình nền
 * ({@link OutboxRelay}) sau đó đọc các bản ghi chưa gửi và publish lên Kafka.
 *
 * <p>Nhờ vậy: hoặc cả dữ liệu nghiệp vụ + outbox cùng commit, hoặc cùng rollback —
 * không bao giờ có chuyện "đã đổi DB nhưng message biến mất". Đánh đổi: giao hàng
 * kiểu <b>at-least-once</b> (relay có thể gửi lặp khi crash giữa chừng) → consumer
 * BẮT BUỘC phải idempotent (hệ thống này đã đảm bảo điều đó).
 */
@Entity
@Table(name = "outbox_event", indexes = {
    @Index(name = "idx_outbox_unpublished", columnList = "published, createdAt")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent extends BaseEntity {

  /** Topic Kafka đích. */
  @Column(nullable = false)
  private String topic;

  /** Key Kafka (thường là orderId) — giữ thứ tự message theo từng đơn. */
  @Column(name = "msg_key")
  private String msgKey;

  /** Tên class đầy đủ của payload, để relay deserialize lại đúng kiểu khi publish. */
  @Column(nullable = false)
  private String eventType;

  /** Payload đã serialize sang JSON. */
  @Column(nullable = false, columnDefinition = "text")
  private String payload;

  /** false = chờ gửi; true = đã publish lên Kafka. */
  @Column(nullable = false)
  @Builder.Default
  private boolean published = false;

  private Instant publishedAt;
}
