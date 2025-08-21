package com.project.savingbee.productAlert.channel.router;

import com.project.savingbee.common.entity.ProductAlertSetting.AlertType;
import com.project.savingbee.productAlert.channel.compose.AlertMessage;
import com.project.savingbee.productAlert.channel.sender.EmailChannelSender;
import com.project.savingbee.productAlert.channel.sender.PushChannelSender;
import com.project.savingbee.productAlert.channel.sender.SmsChannelSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChannelRouter {
  private final EmailChannelSender emailSender;
  private final SmsChannelSender smsSender;
  private final PushChannelSender pushSender;

  public void send(AlertType alertType, AlertMessage message) {
    switch (alertType) {
      case EMAIL: emailSender.send(message);
      case SMS: smsSender.send(message);
      case PUSH: pushSender.send(message);
    }
  }
}
