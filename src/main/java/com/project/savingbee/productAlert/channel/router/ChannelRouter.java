package com.project.savingbee.productAlert.channel.router;

import com.project.savingbee.common.entity.ProductAlertSetting.AlertType;
import com.project.savingbee.productAlert.channel.compose.AlertMessage;
import com.project.savingbee.productAlert.channel.sender.ChannelSender;
import com.project.savingbee.productAlert.channel.sender.email.EmailChannelSender;
import com.project.savingbee.productAlert.channel.sender.push.PushChannelSender;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class ChannelRouter {
  private final EmailChannelSender emailSender;
  private final ChannelSender smsSender;
  private final PushChannelSender pushSender;

  public ChannelRouter(EmailChannelSender emailSender,
      @Qualifier("smsSender") ChannelSender smsSender,
      PushChannelSender pushSender) {
    this.emailSender = emailSender;
    this.smsSender = smsSender;
    this.pushSender = pushSender;
  }

  public void send(AlertType alertType, AlertMessage message) {
    switch (alertType) {
      case EMAIL -> emailSender.send(message);
      case SMS -> smsSender.send(message);
      case PUSH -> pushSender.send(message);
    }
  }
}
