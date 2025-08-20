package com.project.savingbee.productAlert;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.project.savingbee.productAlert.dto.AlertDispatchResponseDto;
import com.project.savingbee.productAlert.scheduler.AlertScheduler;
import com.project.savingbee.productAlert.service.AlertDispatchService;
import com.project.savingbee.productAlert.service.AlertMatchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

@ExtendWith(MockitoExtension.class)
public class AlertSchedulerTest {
  @Mock
  AlertDispatchService alertDispatchService;
  @Mock
  AlertMatchService alertMatchService;

  @InjectMocks
  AlertScheduler scheduler;

  @Test
  @DisplayName("05:00 매칭, 한 번 호출되어 READY 이벤트 생성")
  void scanAfterRefresh() {
      // given
    when(alertMatchService.scanAndEnqueue()).thenReturn(39);

      // when
    scheduler.scanAfterRefresh();

      // then
    verify(alertMatchService, times(1)).scanAndEnqueue();
    verifyNoInteractions(alertDispatchService);
  }

  @Test
  @DisplayName("09:00 슬롯, processed == batchSize면 계속, 작아지면 종료")
  void dispatch0900() {
      //given
    // 두번째 호출 시 processed < batchSize로 루프 종료
    when(alertDispatchService.dispatchNow(anyInt()))
        .thenAnswer(new Answer<AlertDispatchResponseDto>() {
          int call = 0;

          @Override
          public AlertDispatchResponseDto answer(InvocationOnMock inv) {
            call++;
            int batch = inv.getArgument(0, Integer.class);

            return (call == 1)
                ? new AlertDispatchResponseDto(batch, batch - 1, 1) // 계속
                : new AlertDispatchResponseDto(batch - 1, batch - 1, 0); // 종료
          }
        });

      //when
    scheduler.dispatch0900();

      //then
    verify(alertDispatchService, times(2)).dispatchNow(anyInt());
    verifyNoInteractions(alertMatchService);
  }
}
