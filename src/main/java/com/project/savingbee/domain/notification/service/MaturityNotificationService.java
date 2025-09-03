package com.project.savingbee.domain.notification.service;

import com.project.savingbee.common.entity.UserProduct;
import com.project.savingbee.common.repository.UserProductRepository;
import com.project.savingbee.domain.notification.dto.MaturityNotificationDTO;
import com.project.savingbee.domain.recommendation.service.RecommendationService;
import com.project.savingbee.domain.user.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 만기 알림 스케줄링 서비스
 * D-30, D-7, D-1 시점에 이메일 알림 전송 및 추천 상품 안내
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MaturityNotificationService {

  private final UserProductRepository userProductRepository;
  private final RecommendationService recommendationService;
  private final EmailService emailService;

  /**
   * 매일 오전 9시에 만기 알림 체크 및 발송
   * D-30, D-7, D-1 시점의 상품들을 확인하여 알림 전송
   */
  @Scheduled(cron = "0 0 9 * * ?") // 매일 오전 9시
  @Transactional
  public void sendMaturityNotifications() {
    log.info("만기 알림 배치 작업 시작");

    // D-30 알림 전송
    sendNotificationsForDaysBeforeMaturity(30);

    // D-7 알림 전송
    sendNotificationsForDaysBeforeMaturity(7);

    // D-1 알림 전송
    sendNotificationsForDaysBeforeMaturity(1);

    log.info("만기 알림 배치 작업 완료");
  }

  /**
   * 특정 일수 전 만기 상품들에 대한 알림 전송
   */
  private void sendNotificationsForDaysBeforeMaturity(int daysBefore) {
    List<MaturityNotificationDTO> notifications =
        recommendationService.getMaturityNotifications(daysBefore);

    for (MaturityNotificationDTO notification : notifications) {
      try {
        sendMaturityNotificationEmail(notification, daysBefore);
        log.info("만기 알림 전송 성공: userProductId={}, daysBefore={}",
            notification.getUserProductId(), daysBefore);
      } catch (Exception e) {
        log.error("만기 알림 전송 실패: userProductId={}, error={}",
            notification.getUserProductId(), e.getMessage(), e);
      }
    }

    log.info("D-{} 만기 알림 {} 건 처리 완료", daysBefore, notifications.size());
  }

  /**
   * 만기 알림 이메일 전송
   */
  private void sendMaturityNotificationEmail(MaturityNotificationDTO notification, int daysBefore) {
    // 사용자 정보 조회
    UserProduct userProduct = userProductRepository.findById(notification.getUserProductId())
        .orElseThrow(() -> new IllegalArgumentException("보유 상품을 찾을 수 없습니다."));

    String userEmail = userProduct.getUserEntity().getEmail();
    String userName = userProduct.getUserEntity().getUsername();

    // 이메일 제목 생성
    String subject = createEmailSubject(daysBefore, notification.getProductName());

    // 이메일 내용 생성
    String content = createEmailContent(notification, daysBefore);

    // 이메일 전송
    sendMaturityEmail(userEmail, userName, subject, content);
  }

  /**
   * 이메일 제목 생성
   */
  private String createEmailSubject(int daysBefore, String productName) {
    String timePhrase;
    switch (daysBefore) {
      case 30:
        timePhrase = "30일 후";
        break;
      case 7:
        timePhrase = "7일 후";
        break;
      case 1:
        timePhrase = "내일";
        break;
      default:
        timePhrase = daysBefore + "일 후";
    }

    return String.format("[SavingBee] %s 상품이 %s 만료됩니다", productName, timePhrase);
  }

  /**
   * 이메일 내용 생성
   */
  private String createEmailContent(MaturityNotificationDTO notification, int daysBefore) {
    StringBuilder content = new StringBuilder();

    content.append("안녕하세요.\n\n");
    content.append("보유하신 예적금 상품의 만기가 임박했습니다.\n\n");

    // 기본 정보
    content.append("■ 만기 임박 상품 정보\n");
    content.append(String.format("- 상품명: %s\n", notification.getProductName()));
    content.append(String.format("- 은행명: %s\n", notification.getBankName()));
    content.append(String.format("- 현재 금리: %.2f%%\n", notification.getCurrentRate().doubleValue()));
    content.append(
        String.format("- 가입 금액: %,.0f원\n", notification.getDepositAmount().doubleValue()));
    content.append(String.format("- 만기일: %s\n", notification.getMaturityDate()));
    content.append(String.format("- 만기까지: %d일\n\n", notification.getDaysToMaturity()));

    // 추천 상품 정보
    if (!notification.getAlternativeProducts().isEmpty()) {
      content.append("■ 추천 대안 상품\n");
      content.append("현재 상품보다 더 나은 조건의 상품들을 발견했습니다:\n\n");

      notification.getAlternativeProducts().stream()
          .limit(3) // 상위 3개만 표시
          .forEach(product -> {
            content.append(
                String.format("• %s (%s)\n", product.getProductName(), product.getBankName()));
            content.append(String.format("  금리: %.2f%% (현재보다 +%.2f%%)\n",
                product.getMaxInterestRate().doubleValue(),
                product.getRateDifference().doubleValue()));
            content.append(String.format("  예상 추가 수익: 연 %,.0f원\n\n",
                product.getEstimatedExtraInterest().doubleValue()));
          });
    }

    // 행동 지침
    content.append("■ 권장 사항\n");
    if (daysBefore == 30) {
      content.append("- 만기 전 충분한 시간이 있으니 다양한 상품을 검토해보세요.\n");
      content.append("- 더 나은 조건의 상품으로 갈아타는 것을 고려해보세요.\n");
    } else if (daysBefore == 7) {
      content.append("- 만기가 일주일 남았습니다. 갱신 또는 이전 계획을 세우세요.\n");
      content.append("- 필요한 서류를 미리 준비하시기 바랍니다.\n");
    } else if (daysBefore == 1) {
      content.append("- 만기가 내일입니다. 즉시 조치가 필요합니다.\n");
      content.append("- 자동 갱신 여부를 확인하고 필요시 은행에 연락하세요.\n");
    }

    content.append("\n자세한 상품 정보는 SavingBee 웹사이트에서 확인하실 수 있습니다.\n\n");
    content.append("감사합니다.\n");
    content.append("SavingBee 팀");

    return content.toString();
  }

  /**
   * 실제 이메일 전송 (EmailService 활용)
   */
  private void sendMaturityEmail(String toEmail, String username, String subject, String content) {
    try {
      emailService.sendMaturityNotificationEmail(toEmail, username, subject, content);
    } catch (Exception e) {
      log.error("만기 알림 이메일 전송 실패: email={}, error={}", toEmail, e.getMessage(), e);
      throw new RuntimeException("만기 알림 이메일 전송에 실패했습니다.", e);
    }
  }

  /**
   * 수동 만기 알림 전송 (테스트용)
   */
  @Transactional
  public void sendManualMaturityNotifications(int daysBefore) {
    log.info("수동 만기 알림 전송 시작: daysBefore={}", daysBefore);
    sendNotificationsForDaysBeforeMaturity(daysBefore);
    log.info("수동 만기 알림 전송 완료: daysBefore={}", daysBefore);
  }

  /**
   * 특정 사용자의 만기 임박 상품 조회
   */
  public List<MaturityNotificationDTO> getUserMaturityNotifications(Long userId) {
    List<UserProduct> userProducts = userProductRepository.findByUserIdAndIsActiveTrue(userId);
    LocalDate now = LocalDate.now();

    return userProducts.stream()
        .filter(product -> {
          long daysToMaturity = now.until(product.getMaturityDate()).getDays();
          return daysToMaturity >= 0 && daysToMaturity <= 30; // 30일 이내 만기
        })
        .map(product -> {
          long daysToMaturity = now.until(product.getMaturityDate()).getDays();
          return recommendationService.getMaturityNotifications((int) daysToMaturity)
              .stream()
              .filter(notification -> notification.getUserProductId()
                  .equals(product.getUserProductId()))
              .findFirst()
              .orElse(null);
        })
        .filter(java.util.Objects::nonNull)
        .collect(java.util.stream.Collectors.toList());
  }
}
