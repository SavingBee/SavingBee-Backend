package com.project.savingbee.productAlert.channel.compose;

import com.project.savingbee.common.entity.ProductAlertEvent;
import com.project.savingbee.common.entity.ProductAlertSetting.AlertType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AlertMessageComposer {
  public AlertMessage compose(AlertType alertType, ProductAlertEvent event) {
    return switch (alertType) {
      case EMAIL -> composeEmail(event);
      case SMS -> composeSms(event);
      case PUSH -> composePush(event);
    };
  }

  private AlertMessage composeEmail(ProductAlertEvent event) {
//    String to = "test@example.com"; // 메일 발송 테스트용
    String to = event.getProductAlertSetting().getUserEntity().getEmail();
    String subject = "조건에 맞는 상품이 있어요!";
    String body = """
            <h3>맞춤 조건에 맞는 상품이 나왔어요!</h3>
            <p><b>상품코드:</b> %s</p>
            """.formatted(event.getProductCode());

    return new AlertMessage(to, subject, body);
  }

  // 사용자 정보에 전화번호가 없으므로 현재는 SMS 비활성
  private AlertMessage composeSms(ProductAlertEvent event) {
    // String to = event.getProductAlertSetting().getUserEntity().getPhone();
    String to = null;
    String body = "내용";

    return new AlertMessage(to, null, body);
  }

  private AlertMessage composePush(ProductAlertEvent event) {
    String to = event.getProductAlertSetting().getUserEntity().getEmail();
    String subject = "제목";
    String body = "내용";

    return new AlertMessage(to, subject, body);
  }
}
