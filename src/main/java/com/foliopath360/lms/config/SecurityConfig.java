package com.foliopath360.lms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource,
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) throws Exception {

        http
                .cors(cors ->
                        cors.configurationSource(corsConfigurationSource)
                )

                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // Swagger / OpenAPI
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/v3/api-docs",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()

                        // Authentication APIs (public)
                        .requestMatchers(
                                "/api/auth/student/register",
                                "/api/auth/student/verify-otp",
                                "/api/auth/student/resend-otp",
                                "/api/auth/student/forgot-password",
                                "/api/auth/student/reset-password",
                                "/api/auth/staff/set-password",
                                "/api/auth/captcha",
                                "/api/auth/login",
                                "/api/auth/refresh-token",
                                "/api/auth/logout"
                        ).permitAll()

                        // Contact Us: submissions are public, viewing is staff/admin only
                        .requestMatchers(
                                org.springframework.http.HttpMethod.POST,
                                "/api/contact-us"
                        ).permitAll()

                        .requestMatchers(
                                org.springframework.http.HttpMethod.GET,
                                "/api/contact-us"
                        ).hasAnyRole("SUPER_ADMIN", "STAFF")

                        // Super Admin APIs
                        .requestMatchers("/api/super-admin/**")
                        .hasRole("SUPER_ADMIN")

                        // Staff APIs
                        .requestMatchers("/api/staff/**")
                        .hasAnyRole("SUPER_ADMIN", "STAFF")

                        // Shared student reporting (SUPER_ADMIN + STAFF)
                        .requestMatchers("/api/reports/**")
                        .hasAnyRole("SUPER_ADMIN", "STAFF")

                        // Razorpay webhook: server-to-server, authenticated
                        // via the X-Razorpay-Signature HMAC header
                        .requestMatchers(
                                org.springframework.http.HttpMethod.POST,
                                "/api/payments/webhook"
                        ).permitAll()

                        // Student APIs
                        .requestMatchers("/api/student/**")
                        .hasRole("STUDENT")

                        // Course / module / lesson management (writes)
                        .requestMatchers(
                                org.springframework.http.HttpMethod.POST,
                                "/api/courses/**",
                                "/api/modules/**"
                        ).hasAnyRole("SUPER_ADMIN", "STAFF")

                        .requestMatchers(
                                org.springframework.http.HttpMethod.PUT,
                                "/api/courses/**",
                                "/api/modules/**"
                        ).hasAnyRole("SUPER_ADMIN", "STAFF")

                        .requestMatchers(
                                org.springframework.http.HttpMethod.PATCH,
                                "/api/courses/**",
                                "/api/modules/**"
                        ).hasAnyRole("SUPER_ADMIN", "STAFF")

                        .requestMatchers(
                                org.springframework.http.HttpMethod.DELETE,
                                "/api/courses/**",
                                "/api/modules/**"
                        ).hasAnyRole("SUPER_ADMIN", "STAFF")

                        // Public read-only course endpoints
                        .requestMatchers(
                                org.springframework.http.HttpMethod.GET,
                                "/api/courses",
                                "/api/courses/published",
                                "/api/courses/slug/**",
                                "/api/courses/**"
                        ).permitAll()

                        .requestMatchers(
                                org.springframework.http.HttpMethod.GET,
                                "/api/courses/*/modules",
                                "/api/courses/*/modules/**"
                        ).permitAll()

                        .requestMatchers(
                                org.springframework.http.HttpMethod.GET,
                                "/api/modules/*/lessons",
                                "/api/modules/*/lessons/**"
                        ).permitAll()

                        // Mock tests: public read-only (answers hidden for non-staff)
                        .requestMatchers(
                                org.springframework.http.HttpMethod.GET,
                                "/api/mock-tests/**",
                                "/api/modules/*/mock-tests/**"
                        ).permitAll()

                        // Mock test attempts: students only
                        // (must precede the staff/admin write rule below)
                        .requestMatchers(
                                org.springframework.http.HttpMethod.POST,
                                "/api/mock-tests/*/attempts"
                        ).hasRole("STUDENT")

                        // Mock test authoring: SUPER_ADMIN / STAFF
                        .requestMatchers(
                                org.springframework.http.HttpMethod.POST,
                                "/api/mock-tests/**",
                                "/api/modules/*/mock-tests/**"
                        ).hasAnyRole("SUPER_ADMIN", "STAFF")

                        .requestMatchers(
                                org.springframework.http.HttpMethod.PUT,
                                "/api/mock-tests/**",
                                "/api/modules/*/mock-tests/**"
                        ).hasAnyRole("SUPER_ADMIN", "STAFF")

                        .requestMatchers(
                                org.springframework.http.HttpMethod.PATCH,
                                "/api/mock-tests/**",
                                "/api/modules/*/mock-tests/**"
                        ).hasAnyRole("SUPER_ADMIN", "STAFF")

                        .requestMatchers(
                                org.springframework.http.HttpMethod.DELETE,
                                "/api/mock-tests/**",
                                "/api/modules/*/mock-tests/**"
                        ).hasAnyRole("SUPER_ADMIN", "STAFF")

                        .anyRequest().authenticated()
                )

                // Expired / invalid / missing token -> 401 so the frontend
                // can trigger its refresh-then-logout flow. (Role-based
                // denials for authenticated users still return 403.)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"status\":401,\"error\":\"Unauthorized\","
                                            + "\"message\":\"Session expired or invalid. Please log in again.\"}"
                            );
                        })
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
