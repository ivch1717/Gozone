package com.example.orders.api;

import com.example.orders.application.OrdersService;
import com.example.orders.domain.*;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import java.nio.charset.StandardCharsets;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OrdersControllerTest {
    private final OrdersService service = mock(OrdersService.class);
    private final UUID id = UUID.randomUUID(), user = UUID.randomUUID();
    private final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    private MockMvc mvc;
    private Order order() { return new Order(id, user, "Order", BigDecimal.TEN, OrderStatus.NEW, "Description"); }

    @BeforeEach void setUp() {
        validator.afterPropertiesSet();
        var resolver = new PageableHandlerMethodArgumentResolver();
        resolver.setMaxPageSize(100);
        mvc = MockMvcBuilders.standaloneSetup(new OrdersController(service, new OrderApiMapper()))
                .setControllerAdvice(new GlobalExceptionHandler()).setValidator(validator)
                .setMessageConverters(new StringHttpMessageConverter(StandardCharsets.UTF_8),
                        new JacksonJsonHttpMessageConverter())
                .setCustomArgumentResolvers(resolver).addFilters(new HttpLoggingFilter()).build();
    }
    @AfterEach void close() { validator.close(); }

    @Test void createsOrderFromJson() throws Exception {
        when(service.createOrder(user, "Order", BigDecimal.TEN, "Description")).thenReturn(order());
        mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(
                "{\"userId\":\""+user+"\",\"name\":\"Order\",\"amount\":10,\"description\":\"Description\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.userId").value(user.toString()))
                .andExpect(jsonPath("$.name").value("Order")).andExpect(jsonPath("$.amount").value(10))
                .andExpect(jsonPath("$.description").value("Description")).andExpect(jsonPath("$.status").value("NEW"));
    }

    @ParameterizedTest @ValueSource(strings = {"{}", "{\"name\":\"Order\",\"amount\":10}",
            "{\"userId\":\"11111111-1111-1111-1111-111111111111\",\"name\":\" \",\"amount\":10}",
            "{\"userId\":\"11111111-1111-1111-1111-111111111111\",\"name\":\"Order\",\"amount\":0}"})
    void rejectsInvalidBodyBeforeCallingService(String body) throws Exception {
        mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test void returnsDefaultPageWithStableResponseMetadata() throws Exception {
        when(service.getAllOrders(eq(user), any())).thenAnswer(call ->
                new PageImpl<>(List.of(order()), call.getArgument(1), 21));
        mvc.perform(get("/orders/users/"+user)).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(id.toString()))
                .andExpect(jsonPath("$.page.number").value(0)).andExpect(jsonPath("$.page.size").value(10))
                .andExpect(jsonPath("$.page.totalElements").value(21)).andExpect(jsonPath("$.page.totalPages").value(3));
        var captor = ArgumentCaptor.forClass(Pageable.class);
        verify(service).getAllOrders(eq(user), captor.capture());
        assertThat(captor.getValue().getSort()).isEqualTo(Sort.by("id"));
    }

    @Test void honorsPageAndSortAndCapsRequestedSize() throws Exception {
        when(service.getAllOrders(eq(user), any())).thenAnswer(call -> Page.empty(call.getArgument(1)));
        mvc.perform(get("/orders/users/"+user).param("page", "2").param("size", "10000")
                .param("sort", "amount,desc", "id,asc")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty()).andExpect(jsonPath("$.page.size").value(100));
        var captor = ArgumentCaptor.forClass(Pageable.class);
        verify(service).getAllOrders(eq(user), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(captor.getValue().getSort()).isEqualTo(Sort.by(Sort.Order.desc("amount"), Sort.Order.asc("id")));
    }

    @Test void rejectsUnsupportedSort() throws Exception {
        mvc.perform(get("/orders/users/"+user).param("sort", "password,asc")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test void findsOrderAndMapsAbsenceTo404() throws Exception {
        when(service.getOrderById(id)).thenReturn(order()).thenThrow(new EntityNotFoundException("Not found"));
        mvc.perform(get("/orders/"+id)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id.toString()));
        mvc.perform(get("/orders/"+id)).andExpect(status().isNotFound()).andExpect(content().string("Not found"));
    }

    @Test void internalFailureDoesNotExposeDetails() throws Exception {
        when(service.getOrderById(id)).thenThrow(new RuntimeException("secret SQL details"));
        mvc.perform(get("/orders/"+id)).andExpect(status().isInternalServerError())
                .andExpect(content().string("Внутренняя ошибка сервера"));
    }
}
