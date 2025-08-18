package com.project.savingbee.productAlert;

import com.project.savingbee.common.entity.ProductAlertEvent;
import com.project.savingbee.common.entity.ProductAlertEvent.EventStatus;
import com.project.savingbee.common.repository.ProductAlertEventRepository;
import com.project.savingbee.productAlert.dto.AlertDispatchResponseDto;
import com.project.savingbee.productAlert.service.AlertDispatchService;
import java.util.ArrayList;
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

  @InjectMocks
  AlertDispatchService service;

  private ProductAlertEvent readyEvent(Long id) {
    ProductAlertEvent e = new ProductAlertEvent();
    e.setId(id);
    e.setStatus(EventStatus.READY);
    e.setAttempts(0);
    e.setCreatedAt(LocalDateTime.now().minusMinutes(10));
    return e;
  }

  @Test
  @DisplayName("READY -> SENDING -> SENT")
  void successProcessed() {
    // given
    List<ProductAlertEvent> content = List.of(readyEvent(1L));

    when(productAlertEventRepository.findByStatusAndSendNotBeforeLessThanEqual(
        eq(EventStatus.READY),
        any(LocalDateTime.class),
        any(Pageable.class))
    ).thenReturn(new PageImpl<>(List.of(readyEvent(1L))));

    List<EventStatus> savedStatuses = new ArrayList<>();
    List<Integer> attemptsSnap = new ArrayList<>();
    List<LocalDateTime> sentAtSnap = new ArrayList<>();

    // save 호출 시 확인할 항목
    doAnswer(inv -> {
      ProductAlertEvent e = inv.getArgument(0, ProductAlertEvent.class);
      savedStatuses.add(e.getStatus());
      attemptsSnap.add(e.getAttempts());
      sentAtSnap.add(e.getSentAt());
      return e;
    }).when(productAlertEventRepository).save(any(ProductAlertEvent.class));

    // when
    AlertDispatchResponseDto res = service.dispatchNow(100);

    // then
    ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

    verify(productAlertEventRepository, atLeastOnce())
        .findByStatusAndSendNotBeforeLessThanEqual(
            eq(EventStatus.READY), any(LocalDateTime.class), captor.capture());

    Pageable p = captor.getValue();
    assertThat(p.getPageNumber()).isZero();
    assertThat(p.getPageSize()).isEqualTo(100);
    assertThat(p.getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.ASC);

    assertThat(res.getProcessed()).isEqualTo(1);
    assertThat(res.getSent()).isEqualTo(1);
    assertThat(res.getFailed()).isEqualTo(0);

    assertThat(savedStatuses).containsExactly(EventStatus.SENDING, EventStatus.SENT); // SENDING → SENT 순서 검증
    assertThat(attemptsSnap.get(1)).isEqualTo(1);
    assertThat(sentAtSnap.get(1)).isNotNull();
    verify(productAlertEventRepository, times(2)).save(any(ProductAlertEvent.class));
  }
}
