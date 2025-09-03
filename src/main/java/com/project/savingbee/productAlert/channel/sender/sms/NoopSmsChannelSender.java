package com.project.savingbee.productAlert.channel.sender.sms;

import com.project.savingbee.productAlert.channel.compose.AlertMessage;
import com.project.savingbee.productAlert.channel.exception.NonRetryableChannelException;
import com.project.savingbee.productAlert.channel.sender.ChannelSender;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component("smsSender")
@ConditionalOnProperty(prefix = "sms", name = "enabled", havingValue = "false", matchIfMissing = true)
class NoopSmsChannelSender implements ChannelSender {

  @Override
  public void send(AlertMessage message) {
    throw new NonRetryableChannelException("SMS 비활성화 상태입니다.");
  }
}
