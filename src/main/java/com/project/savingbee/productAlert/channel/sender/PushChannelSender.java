package com.project.savingbee.productAlert.channel.sender;

import com.project.savingbee.productAlert.channel.compose.AlertMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// 스텁 구현
@Component("pushSender")
@RequiredArgsConstructor
public class PushChannelSender implements ChannelSender {
  private static final Logger log = LoggerFactory.getLogger(PushChannelSender.class);

  @Override
  public void send(AlertMessage message) {
    log.info("[PUSH][STUB] to={} subject={} body={}",
        message.getTo(), message.getSubject(), shorten(message.getBody()));
  }

  private String shorten(String s){
    return s==null? "" : (s.length()>120 ? s.substring(0, 120) + "…" : s);
  }
}
