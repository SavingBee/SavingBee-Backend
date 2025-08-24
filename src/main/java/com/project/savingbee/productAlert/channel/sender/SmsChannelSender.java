package com.project.savingbee.productAlert.channel.sender;

import com.project.savingbee.productAlert.channel.compose.AlertMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// 스텁 구현
@Component("smsSender")
@RequiredArgsConstructor
public class SmsChannelSender implements ChannelSender {
  private static final Logger log = LoggerFactory.getLogger(SmsChannelSender.class);

  @Override
  public void send(AlertMessage message) {
    log.info("[SMS][STUB] to={} body={}", message.getTo(), shorten(message.getBody()));
  }

  private String shorten(String s){
    return s==null? "" : (s.length()>120 ? s.substring(0, 120) + "…" : s);
  }
}
