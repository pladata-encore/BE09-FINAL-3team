package site.petful.advertiserservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import site.petful.advertiserservice.security.HeaderAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final HeaderAuthenticationFilter headerAuthenticationFilter;

    // 게이트웨이 리라이트 유무 모두 커버(둘 다 열어둠)
    private static final String[] PUBLIC_ENDPOINTS = {
            "/advertiser/signup/**",
            "/advertiser/login", 
            "/advertiser/upload", 
            "/advertiser/refresh-token",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/actuator/**",
            "/internal/**",
            "/advertiser/password/reset/request",
            "/advertiser/password/reset/verify",
            "/advertiser/password/reset",
            "/recommend/petStars"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // CORS & CSRF
                .cors(cors -> cors.disable()) // CORS 비활성화 (Gateway에서 처리)
                .csrf(csrf -> csrf.disable())

                // 예외 응답을 JSON으로 일관화
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(restAuthEntryPoint())
                        .accessDeniedHandler(restAccessDeniedHandler())
                )

                // 완전 무상태 세션
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 인가 규칙
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // 프리플라이트 허용
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers("/error").permitAll()                  // (선택) 기본 에러 핸들러 공개
                        .requestMatchers("/internal/**").permitAll() // 내부 서비스 간 통신 허용
                        .requestMatchers("/admin/**").permitAll()// 관리자 기능은 ADMIN 권한 필요
                        //.requestMatchers("/advertiser/**").hasAnyRole("ADVERTISER")
                        .requestMatchers("/advertiser-service/**").hasAnyRole("ADVERTISER") // 게이트웨이 경로 추가
                        .anyRequest().authenticated()
                )

                // 헤더기반 인증 필터
                .addFilterBefore(headerAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // --- JSON 401/403 핸들러 ---
    @Bean
    public AuthenticationEntryPoint restAuthEntryPoint() {
        return (request, response, ex) -> {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"Unauthorized\"}");
        };
    }

    @Bean
    public AccessDeniedHandler restAccessDeniedHandler() {
        return (request, response, ex) -> {
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"Forbidden\"}");
        };
    }
}
