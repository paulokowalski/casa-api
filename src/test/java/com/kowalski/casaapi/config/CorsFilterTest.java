package com.kowalski.casaapi.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CorsFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private CorsFilter corsFilter;

    @Test
    @DisplayName("Deve adicionar headers CORS corretamente")
    void doFilterInternal_AdicionaHeadersCors() throws ServletException, IOException {
        // Act
        corsFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(response).addHeader("Access-Control-Allow-Origin", "*");
        verify(response).addHeader("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT, PATCH, HEAD");
        verify(response).addHeader("Access-Control-Allow-Headers", "Origin, Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers");
        verify(response).addHeader("Access-Control-Expose-Headers", "Access-Control-Allow-Origin, Access-Control-Allow-Credentials");
        verify(response).addHeader("Access-Control-Allow-Credentials", "true");
        verify(response).addIntHeader("Access-Control-Max-Age", 10);
        verify(filterChain).doFilter(request, response);
    }
} 