package com.shopmicro.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopmicro.common.outbox.OutboxPublisher;
import com.shopmicro.common.outbox.OutboxRelay;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Tự động bật cơ chế Outbox CHỈ KHI service có đủ điều kiện:
 * <ul>
 *   <li>{@link KafkaTemplate} trên classpath (service có publish Kafka), VÀ</li>
 *   <li>có {@link EntityManagerFactory} (service dùng JPA để lưu outbox).</li>
 * </ul>
 *
 * <p>Nhờ vậy: auth/user (có JPA, không Kafka) và notification (có Kafka, không JPA)
 * tự động KHÔNG nạp outbox — không cần cấu hình gì thêm. Các service vừa có Kafka
 * vừa có JPA (order, payment, product, saga-orchestrator) sẽ tự động có outbox.
 *
 * <p>Class này nằm NGOÀI package được component-scan của các service
 * ({@code com.shopmicro.common}, {@code com.shopmicro.<service>}). Nó chỉ được nạp
 * qua cơ chế auto-configuration (file AutoConfiguration.imports) để các điều kiện
 * {@code @ConditionalOnBean} được đánh giá đúng thứ tự, không bị đăng ký trùng.
 *
 * <p>Service dùng outbox chỉ cần khai {@code @EntityScan("com.shopmicro")} để JPA
 * nạp được entity {@code OutboxEvent} (ở common-lib, khác package gốc của service).
 */
@AutoConfiguration(after = HibernateJpaAutoConfiguration.class)
@ConditionalOnClass(KafkaTemplate.class)
@ConditionalOnBean(EntityManagerFactory.class)
@EnableScheduling
public class OutboxAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public OutboxPublisher outboxPublisher(ObjectMapper objectMapper) {
    return new OutboxPublisher(objectMapper);
  }

  @Bean
  @ConditionalOnMissingBean
  public OutboxRelay outboxRelay(KafkaTemplate<String, Object> kafkaTemplate, ObjectMapper objectMapper) {
    return new OutboxRelay(kafkaTemplate, objectMapper);
  }
}
