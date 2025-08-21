package com.project.savingbee.productAlert.channel.compose;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AlertMessage {
  private final String to;       // 수신 식별자(이메일/전화번호/푸시 토큰)
  private final String subject;  // 이메일/푸시 제목. SMS는 null 가능
  private final String body;     // 본문
}
