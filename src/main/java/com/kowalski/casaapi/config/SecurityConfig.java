package com.kowalski.casaapi.config;

import com.kowalski.casaapi.config.security.ErroSegurancaJson;
import com.kowalski.casaapi.config.security.PessoaJwtConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String ADMIN = "ADMIN";
    private static final String USUARIO = "USUARIO";

    private final ApiKeyAuthFilter apiKeyAuthFilter;
    private final PessoaJwtConverter pessoaJwtConverter;
    private final ErroSegurancaJson erroSegurancaJson;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/v1/auth/login").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/v1/geracao-solar").hasAnyRole("INTEGRACAO", ADMIN)
                .requestMatchers(HttpMethod.POST, "/v1/pessoas").hasRole(ADMIN)
                .requestMatchers(HttpMethod.PUT, "/v1/pessoas/**").hasRole(ADMIN)
                .requestMatchers(HttpMethod.DELETE, "/v1/pessoas/**").hasRole(ADMIN)
                .requestMatchers("/v1/servidor/**").hasRole(ADMIN)
                .anyRequest().hasAnyRole(ADMIN, USUARIO)
            )
            .exceptionHandling(e -> e.authenticationEntryPoint(erroSegurancaJson).accessDeniedHandler(erroSegurancaJson))
            .oauth2ResourceServer(o -> o
                .jwt(j -> j.jwtAuthenticationConverter(pessoaJwtConverter))
                .authenticationEntryPoint(erroSegurancaJson)
                .accessDeniedHandler(erroSegurancaJson))
            .addFilterBefore(apiKeyAuthFilter, BearerTokenAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public FilterRegistrationBean<ApiKeyAuthFilter> apiKeyAuthFilterRegistration(ApiKeyAuthFilter filter) {
        FilterRegistrationBean<ApiKeyAuthFilter> registro = new FilterRegistrationBean<>(filter);
        registro.setEnabled(false);
        return registro;
    }
}
