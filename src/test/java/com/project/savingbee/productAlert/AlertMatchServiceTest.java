package com.project.savingbee.productAlert;

import static org.assertj.core.api.Assertions.assertThat;

import com.project.savingbee.common.entity.DepositInterestRates;
import com.project.savingbee.common.entity.DepositProducts;
import com.project.savingbee.common.entity.ProductAlertEvent;
import com.project.savingbee.common.entity.ProductAlertEvent.ProductKind;
import com.project.savingbee.common.entity.ProductAlertSetting;
import com.project.savingbee.common.entity.SavingsInterestRates;
import com.project.savingbee.common.entity.SavingsProducts;
import com.project.savingbee.common.repository.DepositInterestRatesRepository;
import com.project.savingbee.common.repository.DepositProductsRepository;
import com.project.savingbee.common.repository.ProductAlertEventRepository;
import com.project.savingbee.common.repository.ProductAlertSettingRepository;
import com.project.savingbee.common.repository.SavingsInterestRatesRepository;
import com.project.savingbee.common.repository.SavingsProductsRepository;
import com.project.savingbee.domain.user.entity.UserEntity;
import com.project.savingbee.domain.user.entity.UserRoleType;
import com.project.savingbee.productAlert.service.AlertMatchService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@DataJpaTest(properties = {
    // --- H2 임베디드 사용 ---
    "spring.datasource.url=jdbc:h2:mem:savingbee;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",

    // --- Hibernate가 H2 문법으로 DDL 생성하도록 강제 ---
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",

    // --- 스키마는 '생성'만 하게 (DROP/ALTER 방지) ---
    "spring.jpa.hibernate.ddl-auto=create",
    "spring.jpa.properties.jakarta.persistence.schema-generation.database.action=create",

    // Hibernate DDL 로그
    "logging.level.org.hibernate.tool.schema=DEBUG"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@EntityScan(basePackages = {
    "com.project.savingbee.domain",
    "com.project.savingbee.common"
})
@Import({ AlertMatchService.class })
class AlertMatchServiceTest {

  // 테스트 전용 JPA 슬라이스 설정. 필요한 도메인만 스캔
  @TestConfiguration
  @EnableJpaRepositories(basePackageClasses = {
      com.project.savingbee.common.repository.DepositInterestRatesRepository.class,
      com.project.savingbee.common.repository.DepositProductsRepository.class,
      com.project.savingbee.common.repository.ProductAlertEventRepository.class,
      com.project.savingbee.common.repository.ProductAlertSettingRepository.class,
      com.project.savingbee.common.repository.SavingsInterestRatesRepository.class,
      com.project.savingbee.common.repository.SavingsProductsRepository.class
  })
  @EntityScan(basePackageClasses = {
      com.project.savingbee.common.entity.ProductAlertSetting.class,
      com.project.savingbee.common.entity.ProductAlertEvent.class,
      com.project.savingbee.common.entity.DepositProducts.class,
      com.project.savingbee.common.entity.DepositInterestRates.class,
      com.project.savingbee.common.entity.SavingsProducts.class,
      com.project.savingbee.common.entity.SavingsInterestRates.class,
      com.project.savingbee.domain.user.entity.UserEntity.class
  })
  static class JpaSliceConfig {}

  @Autowired
  ProductAlertSettingRepository productAlertSettingRepository;
  @Autowired
  DepositProductsRepository depositProductsRepository;
  @Autowired
  DepositInterestRatesRepository depositInterestRatesRepository;
  @Autowired
  SavingsProductsRepository savingsProductsRepository;
  @Autowired
  SavingsInterestRatesRepository savingsInterestRatesRepository;
  @Autowired
  ProductAlertEventRepository productAlertEventRepository;
  @Autowired
  AlertMatchService alertMatchService;
  @Autowired
  EntityManager em;

  @Test
  @DisplayName("예금 매칭, 이벤트 생성")
  void scanAndEnqueue_createReadyEvent_deposit() {
      //given
    LocalDateTime now = LocalDateTime.now();

    // 예금 상품
    DepositProducts dp = new DepositProducts();
    dp.setFinPrdtCd("DEP001");
    dp.setFinPrdtNm("예금_테스트");
    dp.setIsActive(Boolean.TRUE);
    dp.setMinAmount(new BigDecimal("2000000"));
    dp.setMaxLimit(new BigDecimal("100000000"));
    dp.setUpdatedAt(now.minusHours(1));           // 상품 정보 최근 변경
    depositProductsRepository.save(dp);

    // 예금 금리 옵션 (12개월, 단리, 기본 2.5% / 우대 3.0%)
    DepositInterestRates r = new DepositInterestRates();
    r.setFinPrdtCd("DEP001");
    r.setSaveTrm(12);
    r.setIntrRateType("S");
    r.setIntrRate(new BigDecimal("2.50"));
    r.setIntrRate2(new BigDecimal("3.00"));   // 우대 금리
    r.setUpdatedAt(now.minusMinutes(30));         // 옵션 정보도 최근 변경
    depositInterestRatesRepository.save(r);

    UserEntity u = new UserEntity();
    u.setUsername("username");
    u.setPassword("password");
    u.setIsLock(false);
    u.setIsSocial(false);
    u.setRoleType(UserRoleType.USER);
    u.setCreatedDate(now);
    em.persist(u);
    em.flush();

    // 알림 설정 (예금, 12개월, 단리, 최소금리 3.0%, 금액 조건 X)
    ProductAlertSetting setting = new ProductAlertSetting();

    setting.setProductTypeDeposit(Boolean.TRUE);
    setting.setProductTypeSaving(Boolean.FALSE);
    setting.setMinInterestRate(new BigDecimal("3.00"));
    setting.setInterestCalcSimple(Boolean.TRUE);
    setting.setInterestCalcCompound(Boolean.FALSE);
    setting.setMaxSaveTerm(12);
    setting.setMinAmount(null);
    setting.setMaxLimit(null);
    setting.setLastEvaluatedAt(now.minusDays(2));
    setting.setUserEntity(u);

    productAlertSettingRepository.save(setting);

      //when
    int first = alertMatchService.scanAndEnqueue();   // 첫 번째 실행
    int second = alertMatchService.scanAndEnqueue();  // 두 번째 실행(중복)

      //then
    assertThat(first).isEqualTo(1);   // 생성
    assertThat(second).isEqualTo(0);  // 미생성(중복)
    assertThat(productAlertEventRepository.count()).isEqualTo(1);

    List<ProductAlertEvent> all = productAlertEventRepository.findAll();
    assertThat(all).isNotEmpty();

    ProductAlertEvent ev = all.get(0);
    assertThat(ev.getStatus()).isEqualTo(ProductAlertEvent.EventStatus.READY);
    assertThat(ev.getProductKind()).isEqualTo(ProductAlertEvent.ProductKind.DEPOSIT);
    assertThat(ev.getProductCode()).isEqualTo("DEP001");

    // 오전 9시 일괄 발송
    assertThat(ev.getSendNotBefore().getHour()).isEqualTo(9);

      //given
    UserEntity u2 = new UserEntity();
    u2.setUsername("username2");
    u2.setPassword("password2");
    u2.setIsLock(false);
    u2.setIsSocial(false);
    u2.setRoleType(UserRoleType.USER);
    u2.setCreatedDate(now);
    em.persist(u2);
    em.flush();

    // 알림 설정2 (예금, 12개월, 단리, 최소금리 2.8%, 최소 금액:3000000, 최대 한도:50000000)
    ProductAlertSetting setting2 = new ProductAlertSetting();

    setting2.setProductTypeDeposit(Boolean.TRUE);
    setting2.setProductTypeSaving(Boolean.FALSE);
    setting2.setMinInterestRate(new BigDecimal("2.80"));
    setting2.setInterestCalcSimple(Boolean.TRUE);
    setting2.setInterestCalcCompound(Boolean.FALSE);
    setting2.setMaxSaveTerm(12);
    setting2.setMinAmount(BigInteger.valueOf(3000000));
    setting2.setMaxLimit(BigInteger.valueOf(50000000));
    setting2.setLastEvaluatedAt(now.minusDays(2));
    setting2.setUserEntity(u2);

    productAlertSettingRepository.save(setting2);

      //when
    int created = alertMatchService.scanAndEnqueue();

      //then
    assertThat(created).isEqualTo(1);
    assertThat(productAlertEventRepository.count()).isEqualTo(2);

    // 유저별로 1건씩 생겼는지 확인
    List<ProductAlertEvent> ev2 = productAlertEventRepository.findAll();
    Map<Long, Long> byUser = ev2.stream()
        .collect(Collectors.groupingBy(ProductAlertEvent::getId, Collectors.counting()));
    assertThat(byUser).containsEntry(u.getUserId(), 1L).containsEntry(u2.getUserId(), 1L);

    //given
    UserEntity u3 = new UserEntity();
    u3.setUsername("username3");
    u3.setPassword("password3");
    u3.setIsLock(false);
    u3.setIsSocial(false);
    u3.setRoleType(UserRoleType.USER);
    u3.setCreatedDate(now);
    em.persist(u3);
    em.flush();

    // 알림 설정3 (예금, 12개월, 단리, 최소금리 2.8%, 최소 금액:1000000)
    ProductAlertSetting setting3 = new ProductAlertSetting();

    setting3.setProductTypeDeposit(Boolean.TRUE);
    setting3.setProductTypeSaving(Boolean.FALSE);
    setting3.setMinInterestRate(new BigDecimal("2.80"));
    setting3.setInterestCalcSimple(Boolean.TRUE);
    setting3.setInterestCalcCompound(Boolean.FALSE);
    setting3.setMaxSaveTerm(12);
    setting3.setMinAmount(BigInteger.valueOf(1000000));  // 조건에 해당하는 상품 X
    setting3.setLastEvaluatedAt(now.minusDays(2));
    setting3.setUserEntity(u3);

    productAlertSettingRepository.save(setting3);

    //when
    int created2 = alertMatchService.scanAndEnqueue();

    //then
    assertThat(created2).isEqualTo(0);  // 조건에 맞지 않으므로 미생성
    assertThat(productAlertEventRepository.count()).isEqualTo(2);
  }

  @Test
  @DisplayName("적금 매칭, 이벤트 생성")
  void scanAndEnqueue_createReadyEvent_savings() {
    //given
    LocalDateTime now = LocalDateTime.now();

    // 적금 상품
    SavingsProducts sp = new SavingsProducts();
    sp.setFinPrdtCd("SAV001");
    sp.setFinPrdtNm("적금_테스트");
    sp.setIsActive(Boolean.TRUE);
    sp.setUpdatedAt(now.minusHours(1));           // 상품 정보 최근 변경
    savingsProductsRepository.save(sp);

    // 적금 금리 옵션 (24개월, 복리, 자유적립, 기본 2.7% / 우대 3.2%)
    SavingsInterestRates r = new SavingsInterestRates();
    r.setFinPrdtCd("SAV001");
    r.setSaveTrm(24);
    r.setIntrRateType("M");
    r.setRsrvType("F");
    r.setIntrRate(new BigDecimal("2.70"));
    r.setIntrRate2(new BigDecimal("3.20"));   // 우대 금리
    r.setUpdatedAt(now.minusMinutes(30));         // 옵션 정보도 최근 변경
    savingsInterestRatesRepository.save(r);

    UserEntity u = new UserEntity();
    u.setUsername("username");
    u.setPassword("password");
    u.setIsLock(false);
    u.setIsSocial(false);
    u.setRoleType(UserRoleType.USER);
    u.setCreatedDate(now);
    em.persist(u);
    em.flush();

    // 알림 설정 (적금, 24개월, 복리, 최소금리 3.0%)
    ProductAlertSetting setting = new ProductAlertSetting();

    setting.setProductTypeDeposit(Boolean.FALSE);
    setting.setProductTypeSaving(Boolean.TRUE);
    setting.setMinInterestRate(new BigDecimal("3.00"));
    setting.setInterestCalcSimple(Boolean.FALSE);
    setting.setInterestCalcCompound(Boolean.TRUE);
    setting.setMaxSaveTerm(24);
    setting.setLastEvaluatedAt(now.minusDays(2));
    setting.setUserEntity(u);

    productAlertSettingRepository.save(setting);

    //when
    int first = alertMatchService.scanAndEnqueue();   // 첫 번째 실행
    int second = alertMatchService.scanAndEnqueue();  // 두 번째 실행(중복)

    //then
    assertThat(first).isEqualTo(1);   // 생성
    assertThat(second).isEqualTo(0);  // 미생성(중복)
    assertThat(productAlertEventRepository.count()).isEqualTo(1);

    List<ProductAlertEvent> all = productAlertEventRepository.findAll();
    assertThat(all).isNotEmpty();

    ProductAlertEvent ev = all.get(0);
    assertThat(ev.getStatus()).isEqualTo(ProductAlertEvent.EventStatus.READY);
    assertThat(ev.getProductKind()).isEqualTo(ProductKind.SAVINGS);
    assertThat(ev.getProductCode()).isEqualTo("SAV001");

    // 오전 9시 일괄 발송
    assertThat(ev.getSendNotBefore().getHour()).isEqualTo(9);

    //given
    UserEntity u2 = new UserEntity();
    u2.setUsername("username2");
    u2.setPassword("password2");
    u2.setIsLock(false);
    u2.setIsSocial(false);
    u2.setRoleType(UserRoleType.USER);
    u2.setCreatedDate(now);
    em.persist(u2);
    em.flush();

    // 알림 설정2 (적금, 24개월, 복리, 자유적립, 최소금리 2.8%)
    ProductAlertSetting setting2 = new ProductAlertSetting();

    setting2.setProductTypeDeposit(Boolean.FALSE);
    setting2.setProductTypeSaving(Boolean.TRUE);
    setting2.setMinInterestRate(new BigDecimal("2.80"));
    setting2.setInterestCalcSimple(Boolean.FALSE);
    setting2.setInterestCalcCompound(Boolean.TRUE);
    setting2.setMaxSaveTerm(24);
    setting2.setLastEvaluatedAt(now.minusDays(2));
    setting2.setUserEntity(u2);

    productAlertSettingRepository.save(setting2);

    //when
    int created = alertMatchService.scanAndEnqueue();

    //then
    assertThat(created).isEqualTo(1);
    assertThat(productAlertEventRepository.count()).isEqualTo(2);

    // 유저별로 1건씩 생겼는지 확인
    List<ProductAlertEvent> ev2 = productAlertEventRepository.findAll();
    Map<Long, Long> byUser = ev2.stream()
        .collect(Collectors.groupingBy(ProductAlertEvent::getId, Collectors.counting()));
    assertThat(byUser).containsEntry(u.getUserId(), 1L).containsEntry(u2.getUserId(), 1L);

    //given
    UserEntity u3 = new UserEntity();
    u3.setUsername("username3");
    u3.setPassword("password3");
    u3.setIsLock(false);
    u3.setIsSocial(false);
    u3.setRoleType(UserRoleType.USER);
    u3.setCreatedDate(now);
    em.persist(u3);
    em.flush();

    // 알림 설정3 (적금, 24개월, 자유적립, 최소금리 3.3%)
    ProductAlertSetting setting3 = new ProductAlertSetting();

    setting3.setProductTypeDeposit(Boolean.FALSE);
    setting3.setProductTypeSaving(Boolean.TRUE);
    setting3.setMinInterestRate(new BigDecimal("3.30"));  // 조건에 해당하는 상품 X
    setting3.setMaxSaveTerm(24);
    setting3.setLastEvaluatedAt(now.minusDays(2));
    setting3.setUserEntity(u2);

    productAlertSettingRepository.save(setting3);

    //when
    int created2 = alertMatchService.scanAndEnqueue();

    //then
    assertThat(created2).isEqualTo(0);  // 조건에 맞지 않으므로 미생성
    assertThat(productAlertEventRepository.count()).isEqualTo(2);
  }
}
