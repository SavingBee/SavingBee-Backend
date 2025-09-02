package com.project.savingbee.productAlert.channel.compose;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.savingbee.common.entity.ProductAlertEvent;
import com.project.savingbee.common.entity.ProductAlertEvent.ProductKind;
import com.project.savingbee.common.entity.ProductAlertSetting;
import com.project.savingbee.common.entity.ProductAlertSetting.AlertType;
import com.project.savingbee.productAlert.payload.AlertPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertMessageComposer {
  private final ObjectMapper objectMapper;

  public AlertMessage compose(AlertType alertType, ProductAlertEvent event) {
    return switch (alertType) {
      case EMAIL -> composeEmail(event);
      case SMS -> composeSms(event);
      case PUSH -> composePush(event);
    };
  }

  private AlertMessage composeEmail(ProductAlertEvent event) {
//    String to = "test@example.com"; // 메일 발송 테스트용
    ProductAlertSetting setting = event.getProductAlertSetting();
    String to = setting.getUserEntity().getEmail();
    AlertPayload payload = parsePayload(event.getPayloadJson());

    String type = event.getProductKind() == ProductKind.DEPOSIT ? "예금" : "적금";
    String productName = payload.getProductName();
    String bankName = payload.getBankName();
    String rate = payload.getRate().toString();
    String saveTerm = setting.getMaxSaveTerm() + "개월";
    String when = payload.getSnapshotAt().toString();

    String subject = "[SavingBee] %s 상품 알림 - %s".formatted(type, productName);

    String body = """
            <h3>맞춤 조건에 맞는 상품이 나왔어요!</h3>
            <ul>
              <li><b>은행</b> : %s</li>
              <li><b>상품명</b> : %s</li>
              <li><b>금리</b> : %s%%</li>
              <li><b>기간</b> : %s</li>
              <li><b>확인 시각</b> : %s</li>
            </ul>
            """.formatted(bankName, productName, rate, saveTerm, when);

    return new AlertMessage(to, subject, body);
  }

  // 사용자 정보에 전화번호가 없으므로 현재는 SMS 비활성
  private AlertMessage composeSms(ProductAlertEvent event) {
    // String to = event.getProductAlertSetting().getUserEntity().getPhone();
    String to = "전화번호";
    String body = "내용";

    return new AlertMessage(to, null, body);
  }

  private AlertMessage composePush(ProductAlertEvent event) {
    String to = event.getProductAlertSetting().getUserEntity().getEmail();
    String subject = "제목";
    String body = "내용";

    return new AlertMessage(to, subject, body);
  }

  // payload 파싱
  private AlertPayload parsePayload(String payload) {
    try {
      return objectMapper.readValue(payload, AlertPayload.class);
    } catch (Exception e) {
      log.warn("parse payload failed", e);
      return new AlertPayload();
    }
  }
}
