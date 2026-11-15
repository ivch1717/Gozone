package com.example.payments.api;

import com.example.payments.application.PaymentsService;
import com.example.payments.domain.Account;
import com.example.payments.domain.exception.AccountAlreadyExistsException;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import java.nio.charset.StandardCharsets;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PaymentsControllerTest {
    private final PaymentsService service = mock(PaymentsService.class);
    private final UUID id = UUID.randomUUID(), user = UUID.randomUUID();
    private final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    private MockMvc mvc;

    @BeforeEach void setUp() {
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new PaymentsController(service, new AccountApiMapper()))
                .setControllerAdvice(new GlobalExceptionHandler()).setValidator(validator)
                .setMessageConverters(new StringHttpMessageConverter(StandardCharsets.UTF_8),
                        new JacksonJsonHttpMessageConverter())
                .addFilters(new HttpLoggingFilter()).build();
    }
    @AfterEach void close() { validator.close(); }

    @Test void createsAccountAndReturnsIdentityAndBalance() throws Exception {
        when(service.createAccount(user)).thenReturn(new Account(id, user, BigDecimal.ZERO));
        mvc.perform(post("/payments/account").contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\""+user+"\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.userId").value(user.toString())).andExpect(jsonPath("$.balance").value(0));
    }

    @Test void duplicateAccountReturns409() throws Exception {
        when(service.createAccount(user)).thenThrow(new AccountAlreadyExistsException(user));
        mvc.perform(post("/payments/account").contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\""+user+"\"}")).andExpect(status().isConflict());
    }

    @Test void missingUserIsRejectedBeforeService() throws Exception {
        mvc.perform(post("/payments/account").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test void readsBalanceUsingQueryParameter() throws Exception {
        when(service.getBalance(user)).thenReturn(new BigDecimal("123.45"));
        mvc.perform(get("/payments/balance").param("userId", user.toString()))
                .andExpect(status().isOk()).andExpect(content().string("123.45"));
    }

    @Test void topsUpAccountUsingPathAndJsonBody() throws Exception {
        when(service.topUpAccount(user, BigDecimal.valueOf(25))).thenReturn(new Account(id, user, BigDecimal.valueOf(125)));
        mvc.perform(post("/payments/accounts/"+user+"/top-ups").contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":25}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.balance").value(125));
        verify(service).topUpAccount(user, BigDecimal.valueOf(25));
    }

    @ParameterizedTest @ValueSource(strings = {"{}", "{\"amount\":null}", "{\"amount\":0}", "{\"amount\":-1}"})
    void rejectsInvalidTopUpsBeforeService(String body) throws Exception {
        mvc.perform(post("/payments/accounts/"+user+"/top-ups").contentType(MediaType.APPLICATION_JSON)
                .content(body)).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test void missingAccountReturns404() throws Exception {
        when(service.getBalance(user)).thenThrow(new EntityNotFoundException("Not found"));
        mvc.perform(get("/payments/balance").param("userId", user.toString()))
                .andExpect(status().isNotFound()).andExpect(content().string("Not found"));
    }

    @Test void businessValidationReturns400() throws Exception {
        when(service.topUpAccount(user, BigDecimal.ONE)).thenThrow(new IllegalArgumentException("Invalid amount"));
        mvc.perform(post("/payments/accounts/"+user+"/top-ups").contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":1}")).andExpect(status().isBadRequest()).andExpect(content().string("Invalid amount"));
    }

    @Test void internalFailureIsNotExposed() throws Exception {
        when(service.getBalance(user)).thenThrow(new RuntimeException("secret SQL details"));
        mvc.perform(get("/payments/balance").param("userId", user.toString()))
                .andExpect(status().isInternalServerError()).andExpect(content().string("Внутренняя ошибка сервера"));
    }
}
