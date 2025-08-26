package com.project.savingbee.productAlert.channel.sender.sms;

import com.project.savingbee.productAlert.channel.exception.NonRetryableChannelException;
import com.project.savingbee.productAlert.channel.exception.RetryableChannelException;
import com.project.savingbee.productAlert.channel.sender.sms.dto.SensSmsRequestDto;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "sms", name = "enabled", havingValue = "true")
public class SensSmsGateway {
  private final SmsProperties smsProperties;
  private final WebClient webClient;

  public void send(String to, String content) {
    final String path = "/sms/v2/services/" + smsProperties.getServiceId() + "/messages";
    final String timestamp = String.valueOf(Instant.now().toEpochMilli());
    final String sig = makeSignature("POST", path, timestamp, smsProperties.getAccessKey(), smsProperties.getSecretKey());

    final boolean lms = isLms(content);
    final SensSmsRequestDto requestDto =
        lms ? SensSmsRequestDto.lms(smsProperties.getFrom(), to, "SavingBee 알림", content)
        : SensSmsRequestDto.sms(smsProperties.getFrom(), to, content);

    webClient.post()
          .uri(path)
          .contentType(MediaType.APPLICATION_JSON)
          .header("x-ncp-apigw-timestamp", timestamp)
          .header("x-ncp-iam-access-key", smsProperties.getAccessKey())
          .header("x-ncp-apigw-signature-v2", sig)
          .bodyValue(requestDto)
          .retrieve()
          // 408/429/5xx -> 재시도
          .onStatus(s -> s.value() == 408 || s.value() == 429 || s.is5xxServerError(),
              r -> r.bodyToMono(String.class)
                  .map(b -> new RetryableChannelException("SENS retryable: " + r.statusCode() + " " + b)))
          // 그 외 4xx -> 영구 실패
          .onStatus(HttpStatusCode::is4xxClientError,
              r -> r.bodyToMono(String.class)
                  .map(b -> new NonRetryableChannelException("SENS non-retryable: " + r.statusCode() + " " + b)))
          .toBodilessEntity()
          .onErrorMap(t -> (t instanceof RetryableChannelException || t instanceof NonRetryableChannelException) ?
            t : new RetryableChannelException("SENS transport error", t))
          .block();
  }

  // SENS 시그니처 생성
  private String makeSignature(String method, String path, String timestamp, String accessKey, String secretKey) {
    try {
      String message = method + " " + path + "\n" + timestamp + "\n" + accessKey;
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] rawHmac = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(rawHmac);
    } catch (Exception e) {
      throw new NonRetryableChannelException("Failed to make SENS signature", e);
    }
  }

  // SMS/LMS 판정 - 90B 초과 시 LMS
  private boolean isLms(String content) {
    if (content == null) {
      return false;
    }
    int bytes = content.getBytes(StandardCharsets.UTF_8).length;
    return bytes > 90;
  }
}
