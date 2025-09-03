package com.project.savingbee.productAlert.channel.sender.sms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@AllArgsConstructor
@Jacksonized
@JsonInclude(Include.NON_NULL)
public class SensSmsRequestDto {

  private String type;  // SMS / LMS / MMS
  private String contentType; // COMM
  private String from;  // 발신번호
  private String subject; // LMS 제목
  private String content; // SMS 본문
  private List<Message> messages;

  // SMS는 상단 content 사용
  public static SensSmsRequestDto sms(String from, String to, String content) {
    return new SensSmsRequestDto("SMS", "COMM", from, null, content,
        List.of(new Message(to, null)));
  }

  // LMS는 message별 content 사용
  public static SensSmsRequestDto lms(String from, String to, String subject, String content) {
    return new SensSmsRequestDto("LMS", "COMM", from, subject, null,
        List.of(new Message(to, content)));
  }

  @Getter
  @AllArgsConstructor
  public static class Message {

    private String to;
    private String content;
  }
}
