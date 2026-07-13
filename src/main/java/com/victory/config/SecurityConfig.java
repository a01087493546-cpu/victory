package com.victory.config;

import com.victory.security.JwtAuthenticationFilter;
import com.victory.security.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtUtil);

        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write("{\"message\":\"인증이 필요합니다.\"}");
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        // /error: 인증 없이도 접근 가능해야 함. 안 열어두면 보호된 경로에서 404 등 에러가 났을 때
                        // 서블릿 컨테이너의 /error 내부 재전달(forward)이 인증 없이 처리되어 실제 에러 대신
                        // 혼란스러운 401이 나감 (OncePerRequestFilter가 재전달 시 재인증을 스킵하기 때문).
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/feedback/**").permitAll()
                        .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                        // 자리 표시: 도메인별 API가 아직 없어 예시 경로만 반영, 실제 컨트롤러 생성 시 세부 경로로 교체 필요
                        .requestMatchers("/api/teachers/**").hasAuthority("teacher")
                        .requestMatchers("/api/students/**").hasAuthority("student")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
