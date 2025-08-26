package com.project.savingbee.productAlert.channel.sender.sms;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "sms")
public class SmsProperties {
  private boolean enabled = false;  // 현재 사용자 정보에 전화번호가 없으므로 SMS 비활성

  private String provider = "sens";

  // SENS 공통
  private String serviceId; // SENS 콘솔의 서비스 ID
  private String accessKey; // NCP Access Key
  private String secretKey; // NCP Secret Key
  private String from;  // 발신번호(인증 완료된 번호)
  private String baseUrl = "https://sens.apigw.ntruss.com";
}
