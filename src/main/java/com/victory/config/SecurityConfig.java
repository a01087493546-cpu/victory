package com.victory.config;

import com.victory.security.JwtAuthenticationFilter;
import com.victory.security.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationFilter jwtAuthenticationFilter =
                new JwtAuthenticationFilter(jwtUtil);

        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(
                                (request, response, authException) -> {
                                    response.setStatus(
                                            HttpServletResponse.SC_UNAUTHORIZED
                                    );
                                    response.setContentType(
                                            MediaType.APPLICATION_JSON_VALUE
                                    );
                                    response.setCharacterEncoding("UTF-8");
                                    response.getWriter().write(
                                            "{\"message\":\"인증이 필요합니다.\"}"
                                    );
                                }
                        )
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/feedback/**").permitAll()
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login"
                        ).permitAll()
                        .requestMatchers(
                                "/api/teachers/register"
                        ).permitAll()

                        // 온책읽기 책 조회: 교사와 학생 모두 가능
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/classes/*/reading-range"
                        )
                        .hasAnyAuthority("teacher", "student")

                        // 온책읽기 책 등록·수정: 교사만 가능
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/classes/*/reading-range"
                        )
                        .hasAuthority("teacher")

                        // 온책읽기 읽기 진행(쪽수)만 수정: 교사만 가능
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/classes/*/reading-range"
                        )
                        .hasAuthority("teacher")

                        // 연습읽기 달성도 조회: 교사(학급 대시보드)와 학생 본인 모두 가능
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/students/*/practice-progress"
                        )
                        .hasAnyAuthority("teacher", "student")

                        .requestMatchers("/api/teachers/**")
                        .hasAuthority("teacher")

                        .requestMatchers("/api/students/**")
                        .hasAuthority("student")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}