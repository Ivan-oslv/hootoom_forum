package com.hootoom.forum.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // 成本因子12符合项目安全基线，并保留未来通过 matches/upgradeEncoding 平滑升级的能力。
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityErrorWriter securityErrorWriter) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .logout(logout -> logout.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(securityErrorWriter)
                        .accessDeniedHandler(securityErrorWriter))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/actuator/health/**",
                                "/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/api/v1/health",
                                "/api/v1/auth/register",
                                "/api/v1/auth/email-verifications",
                                "/api/v1/auth/email-verifications/confirm",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/logout",
                                "/api/v1/auth/password-resets",
                                "/api/v1/auth/password-resets/confirm"
                        ).permitAll()
                        .requestMatchers("/api/v1/auth/sessions", "/api/v1/auth/sessions/**").authenticated()
                        .requestMatchers("/api/v1/users/me").authenticated()
                        .anyRequest().denyAll()
                )
                .build();
    }
}
