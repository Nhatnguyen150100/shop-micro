package com.shopmicro.notification.messaging;

import com.shopmicro.common.event.EventTopics;
import com.shopmicro.common.event.OrderStatusEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * BƯỚC CUỐI của Saga: lắng nghe trạng thái đơn hàng và "gửi email" cho khách.
 *
 * <p>Đây là minh hoạ kinh điển cho giá trị của event-driven: notification-service
 * hoàn toàn KHÔNG biết gì về order/payment/product, chỉ phản ứng với sự kiện.
 * Muốn thêm SMS/push? Chỉ cần thêm 1 consumer mới, không sửa các service khác.
 *
 * <p>Ở đây ghi log thay cho việc gửi email thật (chỗ tích hợp SMTP/SendGrid).
 */
@Slf4j
@Component
public class NotificationListener {

  @KafkaListener(topics = EventTopics.ORDER_CONFIRMED, groupId = "notification-service")
  public void onConfirmed(OrderStatusEvent event) {
    sendEmail(event.email(),
        "Đơn hàng " + event.orderId() + " đã được xác nhận",
        "Cảm ơn bạn! " + event.reason());
  }

  @KafkaListener(topics = EventTopics.ORDER_CANCELLED, groupId = "notification-service")
  public void onCancelled(OrderStatusEvent event) {
    sendEmail(event.email(),
        "Đơn hàng " + event.orderId() + " đã bị huỷ",
        "Rất tiếc, đơn hàng bị huỷ. Lý do: " + event.reason());
  }

  private void sendEmail(String to, String subject, String body) {
    // TODO: tích hợp SMTP/SendGrid thật ở đây
    log.info("📧 GỬI EMAIL -> {}\n   Tiêu đề: {}\n   Nội dung: {}", to, subject, body);
  }
}
