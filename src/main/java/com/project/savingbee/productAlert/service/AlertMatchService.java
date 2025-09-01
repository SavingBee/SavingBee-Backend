package com.project.savingbee.productAlert.service;

import com.project.savingbee.common.entity.DepositInterestRates;
import com.project.savingbee.common.entity.DepositProducts;
import com.project.savingbee.common.entity.ProductAlertEvent;
import com.project.savingbee.common.entity.ProductAlertEvent.EventStatus;
import com.project.savingbee.common.entity.ProductAlertEvent.ProductKind;
import com.project.savingbee.common.entity.ProductAlertEvent.TriggerType;
import com.project.savingbee.common.entity.ProductAlertSetting;
import com.project.savingbee.common.entity.ProductAlertSetting.AlertType;
import com.project.savingbee.common.entity.SavingsInterestRates;
import com.project.savingbee.common.entity.SavingsProducts;
import com.project.savingbee.common.repository.DepositInterestRatesRepository;
import com.project.savingbee.common.repository.DepositProductsRepository;
import com.project.savingbee.common.repository.ProductAlertEventRepository;
import com.project.savingbee.common.repository.ProductAlertSettingRepository;
import com.project.savingbee.common.repository.SavingsInterestRatesRepository;
import com.project.savingbee.common.repository.SavingsProductsRepository;
import com.project.savingbee.domain.user.entity.UserEntity;
import com.project.savingbee.productAlert.util.DedupeKey;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AlertMatchService {
  private final ProductAlertSettingRepository productAlertSettingRepository;
  private final ProductAlertEventRepository productAlertEventRepository;
  private final DepositProductsRepository depositProductsRepository;
  private final DepositInterestRatesRepository depositInterestRatesRepository;
  private final SavingsProductsRepository savingsProductsRepository;
  private final SavingsInterestRatesRepository savingsInterestRatesRepository;

  /*
    알림 설정을 가져와 마지막 비교 시각 이후 변경된 상품만 스캔(상품 정보, 금리 옵션 정보 둘 다)
    조건에 맞는 상품에 대해 ProductAlertEvent(STATUS=READY) 생성
  */
  @Transactional
  public int scanAndEnqueue() {
    int created = 0;
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime dispatchAt = next9am(now, ZoneId.of("Asia/Seoul"));

    List<ProductAlertSetting> productAlertSettings = productAlertSettingRepository.findAll();

    for (ProductAlertSetting setting : productAlertSettings) {
      // 알림 OFF, 스캔 X
      UserEntity user = setting.getUserEntity();
      if (user == null || !Boolean.TRUE.equals(user.getAlarm())) {
        setting.setLastEvaluatedAt(now.minusSeconds(1));
        continue;
      }

      // 현재 User 정보에 전화번호가 없으므로, SMS 설정 시 스캔 X
      if (setting.getAlertType() == AlertType.SMS) {
        setting.setLastEvaluatedAt(now.minusSeconds(1));
        continue;
      }

      LocalDateTime since = setting.getLastEvaluatedAt();
      if (since == null) {
        since = now.minusSeconds(1);
      }

      // 후보 선정(예금)
      List<DepositProducts> deposits = new ArrayList<>(depositProductsRepository.findByUpdatedAtAfter(since));
      List<String> depCodesByRate = depositInterestRatesRepository.findDistinctFinPrdtCdUpdatedAfter(since);
      if (!depCodesByRate.isEmpty()) {
        deposits.addAll(depositProductsRepository.findByFinPrdtCdIn(depCodesByRate));
      }
      Set<String> seenDeposit = new HashSet<>();  // 중복 제거

      // 예금
      for (DepositProducts products : deposits) {
        if (!seenDeposit.add(products.getFinPrdtCd())) continue;
        if (!matchesDeposit(setting, products)) continue;

        LocalDateTime version = versionForDeposit(products, setting);

        String key = DedupeKey.of(setting.getId(), TriggerType.PRODUCT_CHANGE, ProductKind.DEPOSIT,
            products.getFinPrdtCd(), version);

        created += saveIfAbsent(setting.getId(), TriggerType.PRODUCT_CHANGE, ProductKind.DEPOSIT,
            products.getFinPrdtCd(), key, dispatchAt, simplePayload(products.getFinPrdtNm(), products.getFinCoNo()));
      }

      // 후보 선정(적금)
      List<SavingsProducts> savings = new ArrayList<>(savingsProductsRepository.findByUpdatedAtAfter(since));
      List<String> savCodesByRate = savingsInterestRatesRepository.findDistinctFinPrdtCdUpdatedAfter(since);
      if (!savCodesByRate.isEmpty()) {
        savings.addAll(savingsProductsRepository.findByFinPrdtCdIn(savCodesByRate));
      }
      Set<String> seenSaving = new HashSet<>();  // 중복 제거

      // 적금
      for (SavingsProducts products : savings) {
        if (!seenSaving.add(products.getFinPrdtCd())) continue;
        if (!matchesSavings(setting, products)) continue;

        LocalDateTime version = versionForSavings(products, setting);

        String key = DedupeKey.of(setting.getId(), TriggerType.PRODUCT_CHANGE, ProductKind.SAVINGS,
            products.getFinPrdtCd(), version);

        created += saveIfAbsent(setting.getId(), TriggerType.PRODUCT_CHANGE, ProductKind.SAVINGS,
            products.getFinPrdtCd(), key, dispatchAt, simplePayload(products.getFinPrdtNm(), products.getFinCoNo()));
      }

      // 비교 시간 갱신
      setting.setLastEvaluatedAt(now.minusSeconds(1));
    }

    return created;
  }

  // 예금
  private boolean matchesDeposit(ProductAlertSetting setting, DepositProducts product) {
    // 활성 상태 확인
    if (!Boolean.TRUE.equals(product.getIsActive())) return false;

    String prdCode = product.getFinPrdtCd();
    Integer maxSaveTerm = setting.getMaxSaveTerm();
    List<String> intRate = settingTypes(setting);
    Optional<DepositInterestRates> o;

    if (maxSaveTerm == null) return false;  // 기간은 필수 설정 값

    // 이자계산방식 설정 시
    if (intRate.size() == 1) {
      o = depositInterestRatesRepository.findTopByFinPrdtCdAndSaveTrmAndIntrRateTypeInOrderByIntrRate2DescIntrRateDesc(
          prdCode, maxSaveTerm, intRate);
    } else {
      // 이자계산방식 미설정(혹은 모두 설정) 시
      o = depositInterestRatesRepository.findTopByFinPrdtCdAndSaveTrmOrderByIntrRate2DescIntrRateDesc(
          prdCode, maxSaveTerm);
    }

    // 우대금리가 있을 경우 우대금리, 없을 경우 기본 금리
    BigDecimal bestRate =
        o.map(r -> r.getIntrRate2() != null ? r.getIntrRate2() : r.getIntrRate())
            .orElse(null);

    // 금리 조건 비교
    if (setting.getMinInterestRate() != null) {
      if (bestRate == null || bestRate.compareTo(setting.getMinInterestRate()) < 0) {
        return false;
      }
    }

    // 예치 금액 설정값이 상품 값 범위에 포함 되는 지 확인
    if (setting.getMinAmount() != null || setting.getMaxLimit() != null) {
      Long prodMin = toLongExact(product.getMinAmount());
      Long prodMax = toLongExact(product.getMaxLimit());
      Long settingMin = toLongExact(setting.getMinAmount());
      Long settingMax = toLongExact(setting.getMaxLimit());

      if (!contains(settingMin, settingMax, prodMin, prodMax)) {
        return false;
      }
    }

    return true;
  }

  // 적금
  private boolean matchesSavings(ProductAlertSetting setting, SavingsProducts product) {
    // 활성 상태 확인
    if (!Boolean.TRUE.equals(product.getIsActive())) return false;

    String prdCode = product.getFinPrdtCd();
    Integer maxSaveTerm = setting.getMaxSaveTerm();
    List<String> intRate = settingTypes(setting);
    Optional<SavingsInterestRates> o;

    if (maxSaveTerm == null) return false;  // 기간은 필수 설정 값

    // 이자계산방식 설정 시
    if (intRate.size() == 1) {
      o = savingsInterestRatesRepository.findTopByFinPrdtCdAndSaveTrmAndIntrRateTypeInOrderByIntrRate2DescIntrRateDesc(
          prdCode, maxSaveTerm, intRate);
    } else {
      o = savingsInterestRatesRepository.findTopByFinPrdtCdAndSaveTrmOrderByIntrRate2DescIntrRateDesc(
          prdCode, maxSaveTerm);
    }

    // 우대금리가 있을 경우 우대금리, 없을 경우 기본 금리
    BigDecimal bestRate =
        o.map(r -> r.getIntrRate2() != null ? r.getIntrRate2() : r.getIntrRate())
            .orElse(null);

    // 금리 조건 비교
    if (setting.getMinInterestRate() != null) {
      if (bestRate == null || bestRate.compareTo(setting.getMinInterestRate()) < 0) {
        return false;
      }
    }

    // 최소 가입 금액, 최대 한도는 적금 상품은 해당 없음

    return true;
  }

  // 알림 이벤트를 중복 없이 한 건만 큐에 적재
  private int saveIfAbsent(Long settingId, TriggerType trigger, ProductKind kind,
      String productCode, String dedupeKey, LocalDateTime sendNotBefore, String payload) {

    if (productAlertEventRepository.existsByDedupeKey(dedupeKey)) return 0; // dedupeKey 중복으로 적재X

    ProductAlertEvent productAlertEvent = ProductAlertEvent.builder()
                                            .alertSettingId(settingId)
                                            .triggerType(trigger)
                                            .productKind(kind)
                                            .productCode(productCode)
                                            .dedupeKey(dedupeKey)
                                            .status(EventStatus.READY)
                                            .attempts(0)
                                            .sendNotBefore(sendNotBefore)
                                            .payloadJson(payload)
                                            .build();
    try {
      productAlertEventRepository.save(productAlertEvent);
      return 1;
    } catch (DataIntegrityViolationException dup) {
      return 0;   // dedupeKey 중복으로 적재X
    }
  }

  // 오전 9시 일괄 발송
  private LocalDateTime next9am(LocalDateTime now, ZoneId zone) {
    ZonedDateTime zNow = now.atZone(zone);
    ZonedDateTime nine = zNow.toLocalDate().atTime(9, 0).atZone(zone);
    if (!zNow.isBefore(nine)) nine = nine.plusDays(1);
    return nine.toLocalDateTime();
  }

  /*
    템플릿/발송에 필요한 최소 정보만 담음 JSON 문자열 (임시)
    필드 : name(상품명), finCoNo(금융회사코드)
  */
  private String simplePayload(String productName, String finCoNo) {
    String n = productName == null ? "" : productName.replace("\"","\\\"");
    String c = finCoNo == null ? "" : finCoNo.replace("\"","\\\"");
    return "{\"name\":\"" + n + "\",\"finCoNo\":\"" + c + "\"}";
  }

  // 이자계산방식(단리/복리) 설정 조건 확인
  private List<String> settingTypes(ProductAlertSetting setting) {
    ArrayList<String> list = new ArrayList<>();
    if (Boolean.TRUE.equals(setting.getInterestCalcSimple()))   list.add("S");
    if (Boolean.TRUE.equals(setting.getInterestCalcCompound())) list.add("M");
    return list;
  }

  // 예치 금액 설정값이 상품 값 범위에 포함 되는 지 확인
  private boolean contains(Long settingMin, Long settingMax, Long productMin, Long productMax) {
    // 최소 가입금액 설정 시
    if (settingMin != null) {
      if (settingMin < productMin || settingMin > productMax) return false;
    }

    // 최대 한도 설정 시
    if (settingMax != null) {
      if (settingMax < productMin || settingMax > productMax) return false;
    }

    return true;
  }

  // BigDecimal -> Long 변환
  private static Long toLongExact(BigDecimal v) {
    if (v == null) return null;

    return v.longValueExact();
  }

  // BigInteger -> Long 변환
  private static Long toLongExact(BigInteger v) {
    if (v == null) return null;

    return v.longValueExact();
  }

  // dedupeKey 생성용 버전 시각 구하기(예금)
  private LocalDateTime versionForDeposit(DepositProducts products, ProductAlertSetting setting) {
    Integer term = setting.getMaxSaveTerm();          // 필수(1/3/6/12/24/36)
    var intr = settingTypes(setting);                 // ["S"] or ["M"]; 둘 다 허용이면 필터X

    Optional<DepositInterestRates> latest = (intr.size()==1)
            ? depositInterestRatesRepository.findTopByFinPrdtCdAndSaveTrmAndIntrRateTypeInOrderByUpdatedAtDesc(
            products.getFinPrdtCd(), term, intr)
            : depositInterestRatesRepository.findTopByFinPrdtCdAndSaveTrmOrderByUpdatedAtDesc(
                products.getFinPrdtCd(), term);

    LocalDateTime optTs = latest.map(DepositInterestRates::getUpdatedAt).orElse(null);
    return maxNonNull(products.getUpdatedAt(), optTs);
  }

  // dedupeKey 생성용 버전 시각 구하기(적금)
  private LocalDateTime versionForSavings(SavingsProducts products, ProductAlertSetting setting) {
    Integer term = setting.getMaxSaveTerm();
    List<String> intrTypes = settingTypes(setting);         // 이자유형 S/M

    Optional<SavingsInterestRates> latest;

    if (intrTypes.size()==1) {
      latest = savingsInterestRatesRepository.findTopByFinPrdtCdAndSaveTrmAndIntrRateTypeInOrderByUpdatedAtDesc(
          products.getFinPrdtCd(), term, intrTypes);
    } else {
      latest = savingsInterestRatesRepository.findTopByFinPrdtCdAndSaveTrmOrderByUpdatedAtDesc(
          products.getFinPrdtCd(), term);
    }

    LocalDateTime optTs = latest.map(SavingsInterestRates::getUpdatedAt).orElse(null);
    return maxNonNull(products.getUpdatedAt(), optTs);
  }

  // 두 시각 중 더 늦은 것 반환(null 허용)
  private static LocalDateTime maxNonNull(LocalDateTime a, LocalDateTime b) {
    if (a == null) return b;
    if (b == null) return a;
    return a.isAfter(b) ? a : b;
  }
}
