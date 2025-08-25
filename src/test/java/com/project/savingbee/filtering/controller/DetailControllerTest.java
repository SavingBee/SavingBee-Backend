package com.project.savingbee.filtering.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;


import com.project.savingbee.common.entity.DepositProducts;
import com.project.savingbee.common.entity.FinancialCompanies;
import com.project.savingbee.common.entity.SavingsProducts;
import com.project.savingbee.common.repository.DepositProductsRepository;
import com.project.savingbee.common.repository.FinancialCompaniesRepository;
import com.project.savingbee.common.repository.SavingsProductsRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("DetailController 통합 테스트")
public class DetailControllerTest {

  @MockitoBean
  private ClientRegistrationRepository clientRegistrationRepository;

  @Autowired
  private WebApplicationContext context;

  private MockMvc mockMvc;

  @Autowired
  private DepositProductsRepository depositProductsRepository;

  @Autowired
  private SavingsProductsRepository savingsProductsRepository;

  @Autowired
  private FinancialCompaniesRepository financialCompaniesRepository;

  private String testDepositId = "DEPOSIT001";
  private String testSavingsId = "SAVINGS001";
  private String testFinCoNo = "0010001";

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

    // 테스트 데이터 설정 (DetailServiceTest와 동일)
    FinancialCompanies financialCompany = FinancialCompanies.builder()
        .finCoNo(testFinCoNo)
        .korCoNm("테스트은행")
        .orgTypeCode("1")
        .build();
    financialCompaniesRepository.save(financialCompany);

    DepositProducts deposit = DepositProducts.builder()
        .finPrdtCd(testDepositId)
        .finPrdtNm("테스트정기예금")
        .finCoNo(testFinCoNo)
        .financialCompany(financialCompany)
        .joinDeny("1")
        .isActive(true)
        .build();
    depositProductsRepository.save(deposit);

    SavingsProducts savings = SavingsProducts.builder()
        .finPrdtCd(testSavingsId)
        .finPrdtNm("테스트정기적금")
        .finCoNo(testFinCoNo)
        .financialCompany(financialCompany)
        .joinDeny("1")
        .isActive(true)
        .build();
    savingsProductsRepository.save(savings);
  }

  @Test
  @DisplayName("예금 상품 정상 조회")
  void getProductDetail_Success() throws Exception {
    mockMvc.perform(get("/products/" + testDepositId))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fin_prdt_cd").value(testDepositId))
        .andExpect(jsonPath("$.product_type").value("deposit"))
        .andExpect(jsonPath("$.kor_co_nm").value("테스트은행"));
  }

  @Test
  @DisplayName("적금 상품 정상 조회")
  void getSavingsProductDetail_Success() throws Exception {
    mockMvc.perform(get("/products/" + testSavingsId))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fin_prdt_cd").value(testSavingsId))
        .andExpect(jsonPath("$.product_type").value("saving"))
        .andExpect(jsonPath("$.kor_co_nm").value("테스트은행"));
  }

  @Test
  @DisplayName("GET /products/{productId} - 존재하지 않는 상품")
  void getProductDetail_NotFound() throws Exception {
    mockMvc.perform(get("/products/NONEXISTENT"))
        .andExpect(status().isBadRequest());
  }
}

