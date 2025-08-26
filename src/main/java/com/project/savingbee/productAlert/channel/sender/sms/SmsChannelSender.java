package com.project.savingbee.productAlert.channel.sender.sms;

import com.project.savingbee.productAlert.channel.compose.AlertMessage;
import com.project.savingbee.productAlert.channel.exception.NonRetryableChannelException;
import com.project.savingbee.productAlert.channel.exception.RetryableChannelException;
import com.project.savingbee.productAlert.channel.sender.ChannelSender;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component("smsSender")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "sms", name = "enabled", havingValue = "true")
public class SmsChannelSender implements ChannelSender {
  private final SensSmsGateway sensSmsGateway;

  @Override
  public void send(AlertMessage message) {
    final String to = message.getTo();
    final String body = message.getBody() == null ? "" : message.getBody();

    if (to == null || to.isBlank()) {
      throw new NonRetryableChannelException("수신 번호가 없습니다.");
    }

    try {
      sensSmsGateway.send(to, body);
    } catch (NonRetryableChannelException e) {
      throw new NonRetryableChannelException("SMS 전송 실패(요청/인증/번호 오류)", e);
    } catch (RetryableChannelException e) {
      throw new RetryableChannelException("SMS 전송 실패(서버/일시 장애)", e);
    } catch (Exception e) {
      throw new RetryableChannelException("SMS 전송 실패(기타)", e);
    }
  }
}
