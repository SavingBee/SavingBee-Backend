package com.project.savingbee.productAlert;

import com.project.savingbee.common.entity.ProductAlertEvent;
import com.project.savingbee.common.entity.ProductAlertEvent.EventStatus;
import com.project.savingbee.common.entity.ProductAlertSetting;
import com.project.savingbee.common.entity.ProductAlertSetting.AlertType;
import com.project.savingbee.common.repository.ProductAlertEventRepository;
import com.project.savingbee.productAlert.channel.compose.AlertMessage;
import com.project.savingbee.productAlert.channel.compose.AlertMessageComposer;
import com.project.savingbee.productAlert.channel.exception.NonRetryableChannelException;
import com.project.savingbee.productAlert.channel.exception.RetryableChannelException;
import com.project.savingbee.productAlert.channel.router.ChannelRouter;
import com.project.savingbee.productAlert.dto.AlertDispatchResponseDto;
import com.project.savingbee.productAlert.service.AlertDispatchService;
import com.project.savingbee.productAlert.service.AlertEventStateService;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AlertDispatchServiceTest {

  @Mock
  ProductAlertEventRepository productAlertEventRepository;
  @Mock
  AlertEventStateService alertEventStateService;
  @Mock
  ChannelRouter channelRouter;
  @Mock
  AlertMessageComposer alertMessageComposer;

  @InjectMocks
  AlertDispatchService service;

  private ProductAlertEvent readyEvent(Long id) {
    ProductAlertSetting s = new ProductAlertSetting();
    s.setId(100L);
    s.setAlertType(AlertType.EMAIL);

    ProductAlertEvent e = new ProductAlertEvent();
    e.setId(id);
    e.setStatus(EventStatus.READY);
    e.setAttempts(0);
    e.setCreatedAt(LocalDateTime.now().minusMinutes(10));
    e.setProductAlertSetting(s);
    return e;
  }

  @Test
  @DisplayName("READY -> SENDING -> SENT")
  void successDispatch() {
    // given
    ProductAlertEvent e = readyEvent(1L);

    when(productAlertEventRepository.findByStatusInAndSendNotBeforeLessThanEqual(
        eq(List.of(EventStatus.READY, EventStatus.FAILED)),
        any(LocalDateTime.class),
        any(Pageable.class))
    ).thenReturn(new PageImpl<>(List.of(e)));

    when(alertEventStateService.toSending(eq(e), any(LocalDateTime.class)))
        .thenReturn(true);

    when(alertMessageComposer.compose(any(), any()))
        .thenReturn(AlertMessage.builder().to("u@test").subject("s").body("b").build());
    doNothing().when(channelRouter).send(any(), any());

    ArgumentCaptor<ProductAlertEvent> captor = ArgumentCaptor.forClass(ProductAlertEvent.class);
    doAnswer(inv -> inv.getArgument(0))
        .when(productAlertEventRepository).save(captor.capture());

    // when
    AlertDispatchResponseDto res = service.dispatchNow(100);

    // then
    verify(productAlertEventRepository, atLeastOnce())
        .findByStatusInAndSendNotBeforeLessThanEqual(eq(List.of(EventStatus.READY, EventStatus.FAILED)),
            any(LocalDateTime.class), any(Pageable.class));

    verify(alertEventStateService, times(1))
        .toSending(eq(e), any(LocalDateTime.class));

    assertThat(res.getProcessed()).isEqualTo(1);
    assertThat(res.getSent()).isEqualTo(1);
    assertThat(res.getFailed()).isEqualTo(0);

    ProductAlertEvent saved = captor.getValue();
    assertThat(saved.getStatus()).isEqualTo(EventStatus.SENT);
    assertThat(saved.getAttempts()).isEqualTo(1);
    assertThat(saved.getSentAt()).isNotNull();
  }

  @Test
  @DisplayName("재시도 가능한 실패 발생 시")
  void retryableFailure() {
    // given
    ProductAlertEvent e = readyEvent(3L);

    when(productAlertEventRepository.findByStatusInAndSendNotBeforeLessThanEqual(
        eq(List.of(EventStatus.READY, EventStatus.FAILED)),
        any(LocalDateTime.class),
        any(Pageable.class))
    ).thenReturn(new PageImpl<>(List.of(e)));

    when(alertEventStateService.toSending(eq(e), any(LocalDateTime.class))).thenReturn(true);
    doThrow(new RetryableChannelException("재시도 가능")).when(channelRouter).send(any(), any());

    ArgumentCaptor<ProductAlertEvent> captor = ArgumentCaptor.forClass(ProductAlertEvent.class);
    doAnswer(inv -> inv.getArgument(0))
        .when(productAlertEventRepository).save(captor.capture());

    // when
    AlertDispatchResponseDto res = service.dispatchNow(100);

    // then
    assertThat(res.getProcessed()).isEqualTo(1);
    assertThat(res.getFailed()).isEqualTo(1);

    ProductAlertEvent saved = captor.getAllValues().get(captor.getAllValues().size() - 1);
    assertThat(saved.getStatus()).isEqualTo(EventStatus.FAILED);
    assertThat(saved.getAttempts()).isEqualTo(1);
  }

  @Test
  @DisplayName("재시도 불가능한 실패 발생 시 -> sendNotBefore를 먼 미래의 시간으로 설정하여 재시도 방지")
  void nonRetryableFailure() {
    // given
    ProductAlertEvent e = readyEvent(3L);

    when(productAlertEventRepository.findByStatusInAndSendNotBeforeLessThanEqual(
        eq(List.of(EventStatus.READY, EventStatus.FAILED)),
        any(LocalDateTime.class),
        any(Pageable.class))
    ).thenReturn(new PageImpl<>(List.of(e)));

    when(alertEventStateService.toSending(eq(e), any(LocalDateTime.class))).thenReturn(true);
    doThrow(new NonRetryableChannelException("재시도 불가")).when(channelRouter).send(any(), any());

    ArgumentCaptor<ProductAlertEvent> captor = ArgumentCaptor.forClass(ProductAlertEvent.class);
    doAnswer(inv -> inv.getArgument(0))
        .when(productAlertEventRepository).save(captor.capture());

    // when
    service.dispatchNow(100);

    // then
    ProductAlertEvent saved = captor.getAllValues().get(captor.getAllValues().size() - 1);
    assertThat(saved.getStatus()).isEqualTo(EventStatus.FAILED);
    assertThat(saved.getAttempts()).isEqualTo(1);
    assertThat(saved.getSendNotBefore().getYear()).isGreaterThan(LocalDate.now().getYear() + 99);
  }
}
