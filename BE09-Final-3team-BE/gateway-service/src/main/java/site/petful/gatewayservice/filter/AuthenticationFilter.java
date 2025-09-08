package site.petful.gatewayservice.filter;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import site.petful.gatewayservice.util.JwtUtil;

@Slf4j
@Component
public class AuthenticationFilter extends
    AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    @Override
    public String name() {
        return "Authentication";
    }

    private static final String HDR_USER_NO = "X-User-No";
    private static final String HDR_USER_TYPE = "X-User-Type";

    // 기본 화이트리스트 - 인증 없이 접근 가능한 경로들
    private static final List<String> DEFAULT_WHITELIST = List.of(
       "/api/v1/user-service/auth/login",
        "/api/v1/user-service/auth/signup",
        "/api/v1/user-service/auth/email/send",
        "/api/v1/user-service/auth/email/verify",
        "/api/v1/user-service/auth/password/reset",
        "/api/v1/user-service/auth/password/verify",
        "/api/v1/user-service/auth/password/change",

        // 광고주 인증 관련
        "/api/v1/advertiser-service/advertiser/signup",
        "/api/v1/advertiser-service/advertiser/signup/email/send",
        "/api/v1/advertiser-service/advertiser/signup/email/verify",
        "/api/v1/advertiser-service/advertiser/login",
        "/api/v1/advertiser-service/advertiser/password/reset/request",
        "/api/v1/advertiser-service/advertiser/password/reset/verify",
        "/api/v1/advertiser-service/recommend/petStars"

    );

    private final JwtUtil jwtUtil;

    public AuthenticationFilter(JwtUtil jwtUtil) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
    }

    @Override
    public GatewayFilter apply(Config cfg) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getPath().value();

            log.info("Processing path: {}", path); // 디버깅 로그 추가

            // OPTIONS 요청은 통과
            if (request.getMethod() == HttpMethod.OPTIONS) {
                log.info("OPTIONS request, skipping authentication");
                return chain.filter(exchange);
            }

            // 화이트리스트 경로는 통과
            if (isWhitelisted(cfg.getWhitelist(), path)) {
                log.info("Path {} is whitelisted, skipping authentication", path);
                return chain.filter(exchange);
            }

            log.info("Path {} requires authentication", path);

            // Authorization 헤더 검사
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.debug("Missing or invalid Authorization header for path {}", path);
                return cfg.required ? unauthorized(exchange) : chain.filter(exchange);
            }

            String token = authHeader.substring(7).trim();

            // 토큰 유효성 검사
            if (!jwtUtil.validateToken(token)) {
                log.debug("JWT validation failed for path {}", path);
                return cfg.required ? unauthorized(exchange) : chain.filter(exchange);
            }

            // userNo, userType 추출
            String userNo = jwtUtil.getUserNoFromToken(token);
            String userType = jwtUtil.getUserTypeFromToken(token);

            if (userNo == null || userType == null) {
                log.warn("Missing userNo or userType in token for path: {}", path);
                return cfg.required ? unauthorized(exchange) : chain.filter(exchange);
            }

            // advertiserNo를 userNo로 사용하는 경우
            String advertiserNo = jwtUtil.getAdvertiserNoFromToken(token);
            if (advertiserNo != null && !advertiserNo.equals(userNo)) {
                userNo = advertiserNo;
                log.debug("Using advertiserNo as userNo: {}", userNo);
            }

            // 헤더와 쿼리 파라미터 추가
            String currentQuery = request.getURI().getQuery();
            String userNoParam = "userNo=" + userNo;
            String newQuery = currentQuery == null ? userNoParam : currentQuery + "&" + userNoParam;
            
            ServerHttpRequest finalRequest = request.mutate()
                .header(HDR_USER_NO, userNo)
                .header(HDR_USER_TYPE, userType)
                .uri(request.getURI().resolve(path + "?" + newQuery))
                .build();

            log.debug("Authentication successful - userNo: {}, userType: {}, path: {}", userNo,
                userType, path);

            return chain.filter(exchange.mutate().request(finalRequest).build());
        };
    }

    private boolean isWhitelisted(List<String> whitelist, String path) {
        // 기본 화이트리스트 확인 - 정확한 매칭 또는 startsWith 체크
        boolean isDefaultWhitelisted = DEFAULT_WHITELIST.stream().anyMatch(pattern -> {
            if (pattern.endsWith("**")) {
                // ** 패턴인 경우 startsWith로 체크
                String prefix = pattern.substring(0, pattern.length() - 2);
                return path.startsWith(prefix);
            } else {
                // 정확한 매칭
                return path.equals(pattern);
            }
        });
        
        if (isDefaultWhitelisted) {
            log.info("Path {} matched DEFAULT_WHITELIST", path);
            return true;
        }

        // 설정된 화이트리스트 확인
        if (whitelist == null) {
            return false;
        }

        boolean isConfigWhitelisted = whitelist.stream().anyMatch(pattern -> {
            if (pattern.endsWith("**")) {
                String prefix = pattern.substring(0, pattern.length() - 2);
                return path.startsWith(prefix);
            } else {
                return path.equals(pattern);
            }
        });
        
        if (isConfigWhitelisted) {
            log.info("Path {} matched config whitelist", path);
        }
        
        return isConfigWhitelisted;
    }

    private reactor.core.publisher.Mono<Void> unauthorized(
        org.springframework.web.server.ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return response.setComplete();
    }

    public static class Config {
        private boolean required = true;
        private List<String> whitelist;

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }

        public List<String> getWhitelist() {
            return whitelist;
        }

        public void setWhitelist(List<String> whitelist) {
            this.whitelist = whitelist;
        }
    }
}