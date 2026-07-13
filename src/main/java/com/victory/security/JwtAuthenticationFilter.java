package com.victory.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/*
 * 의도적으로 @Component를 붙이지 않음: Filter 타입 빈으로 등록되면 Spring Boot가
 * 서블릿 컨테이너 레벨에도 자동으로 중복 등록해서 Security 체인 안의(addFilterBefore)
 * 실행이 OncePerRequestFilter 가드에 걸려 스킵되는 문제가 있었음.
 * SecurityConfig에서 직접 new로 생성해서 체인에만 등록한다.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader(AUTH_HEADER);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            Claims claims = jwtUtil.validateAndGetClaims(token);

            Long userId = claims.get("userId", Long.class);
            String role = claims.get("role", String.class);

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    List.of(new SimpleGrantedAuthority(role))
            );

            // getContext()가 반환하는 객체를 그 자리에서 수정하면 안 됨: STATELESS 세션에서는
            // SecurityContextHolderFilter가 컨텍스트를 지연 공급자(deferred supplier)로 관리해서,
            // 다음 필터가 getContext()를 다시 호출하면 새 빈 컨텍스트를 받아 방금 설정한 인증 정보가
            // 사라진다. setContext()로 스레드로컬의 공급자 자체를 고정값으로 교체해야 유지된다.
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            filterChain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"message\":\"유효하지 않거나 만료된 토큰입니다.\"}");
        }
    }
}
