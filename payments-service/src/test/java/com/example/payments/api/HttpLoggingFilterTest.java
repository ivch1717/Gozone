package com.example.payments.api;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.assertj.core.api.Assertions.*;

class HttpLoggingFilterTest {
    @Test void forwardsRequestAndDoesNotSwallowDownstreamFailure() throws Exception {
        var filter = new HttpLoggingFilter();
        var request = new MockHttpServletRequest("GET", "/test");
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> {
            assertThat(req).isSameAs(request);
            assertThat(res).isSameAs(response);
            ((jakarta.servlet.http.HttpServletResponse) res).setStatus(202);
        });
        assertThat(response.getStatus()).isEqualTo(202);
        ServletException failure = new ServletException("downstream failed");
        assertThatThrownBy(() -> filter.doFilter(request, response, (req, res) -> { throw failure; })).isSameAs(failure);
    }
}

