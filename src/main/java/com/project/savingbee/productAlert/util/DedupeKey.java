package com.project.savingbee.productAlert.util;

import com.project.savingbee.common.entity.ProductAlertEvent.ProductKind;
import com.project.savingbee.common.entity.ProductAlertEvent.TriggerType;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public final class DedupeKey {

  private DedupeKey() {
  }

  // 중복 방지용 해시 키 생성
  public static String of(Long alertSettingId, TriggerType triggerType,
      ProductKind productKind, String productCode, LocalDateTime version) {

    String s = alertSettingId.toString() + "|" + triggerType.name() + "|"
        + productKind.name() + "|" + productCode + "|" +
        (version == null ? "0" : version.truncatedTo(ChronoUnit.SECONDS).toString()); // 초 단위 고정

    return sha256Hex(s);
  }

  public static String sha256Hex(String s) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] hash = md.digest(s.getBytes(StandardCharsets.UTF_8));
      StringBuilder hex = new StringBuilder(hash.length * 2);
      for (byte b : hash) {
        hex.append(String.format("%02x", b));
      }
      return hex.toString();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
