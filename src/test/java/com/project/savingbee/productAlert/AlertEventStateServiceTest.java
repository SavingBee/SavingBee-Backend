package com.project.savingbee.productAlert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.project.savingbee.common.entity.ProductAlertEvent;
import com.project.savingbee.common.entity.ProductAlertEvent.EventStatus;
import com.project.savingbee.common.repository.ProductAlertEventRepository;
import com.project.savingbee.productAlert.service.AlertEventStateService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AlertEventStateServiceTest {

  @Mock
  ProductAlertEventRepository productAlertEventRepository;

  @InjectMocks
  private AlertEventStateService alertEventStateService;

  private ProductAlertEvent event(EventStatus es, int attempts) {
    ProductAlertEvent e = new ProductAlertEvent();
    e.setId(3L);
    e.setStatus(es);
    e.setAttempts(attempts);

    return e;
  }

  @Test
  @DisplayName("READY -> SENDING")
  void successToSending() {
    // given
    LocalDateTime now = LocalDateTime.now();
    ProductAlertEvent e = event(EventStatus.READY, 0);

    // when
    boolean ok = alertEventStateService.toSending(e, now);

    // then
    assertThat(ok).isTrue();
    assertThat(e.getStatus()).isEqualTo(EventStatus.SENDING);

    verify(productAlertEventRepository, atLeastOnce()).save(e);
  }

  @Test
  @DisplayName("SENDING 타임아웃 복구")
  void successRecoverStuckSending() {
      //given
    LocalDateTime now = LocalDateTime.now();

    ProductAlertEvent e = event(EventStatus.SENDING, 0);
    e.setUpdatedAt(now.minusMinutes(2));

    when(productAlertEventRepository.findByStatusAndUpdatedAtBefore(
        eq(EventStatus.SENDING), any(LocalDateTime.class)))
        .thenReturn(List.of(e));

      //when
    alertEventStateService.recoverStuckSending(now);

      //then
    assertThat(e.getStatus()).isEqualTo(EventStatus.FAILED);  // SENDING -> FAILED
    assertThat(e.getAttempts()).isEqualTo(1);         // attempts + 1

    ArgumentCaptor<List<ProductAlertEvent>> cap = ArgumentCaptor.forClass(List.class);
    verify(productAlertEventRepository).saveAll(cap.capture());
    assertThat(cap.getValue()).hasSize(1);
    assertThat(cap.getValue().get(0).getStatus()).isEqualTo(EventStatus.FAILED);

  }
}
