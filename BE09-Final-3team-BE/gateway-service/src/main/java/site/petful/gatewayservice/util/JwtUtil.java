package site.petful.gatewayservice.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import site.petful.gatewayservice.config.JwtConfig;

import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private static final String CLAIM_USER_NO = "userNo";
    private static final String CLAIM_USER_TYPE = "userType";
    private static final String CLAIM_ADVERTISER_NO = "advertiserNo";

    private final JwtConfig jwtConfig;

    public boolean validateToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return !isTokenExpired(claims);
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    private Claims parseClaims(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtConfig.getSecret().trim()));
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return jws.getPayload();
        } catch (Exception e) {
            log.error("Failed to parse token: {}", e.getMessage());
            throw e;
        }
    }

    private boolean isTokenExpired(Claims claims) {
        Date exp = claims.getExpiration();
        return exp == null || exp.before(new Date());
    }

    public String getUserNoFromToken(String token) {
        try {
            Claims claims = parseClaims(token);
            
            // 먼저 userNo 클레임 확인
            Object userNoObj = claims.get(CLAIM_USER_NO);
            if (userNoObj != null) {
                return userNoObj.toString();
            }
            
            // userNo가 없으면 advertiserNo 클레임 확인
            Object advertiserNoObj = claims.get(CLAIM_ADVERTISER_NO);
            if (advertiserNoObj != null) {
                log.debug("Using advertiserNo as userNo: {}", advertiserNoObj);
                return advertiserNoObj.toString();
            }
            
            return null;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Error extracting userNo from token: {}", e.getMessage());
            return null;
        }
    }

    public String getUserTypeFromToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.get(CLAIM_USER_TYPE, String.class);
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Error extracting userType from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 토큰에서 advertiserNo를 직접 추출하는 메서드
     */
    public String getAdvertiserNoFromToken(String token) {
        try {
            Claims claims = parseClaims(token);
            Object advertiserNoObj = claims.get(CLAIM_ADVERTISER_NO);
            if (advertiserNoObj == null) {
                return null;
            }
            return advertiserNoObj.toString();
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Error extracting advertiserNo from token: {}", e.getMessage());
            return null;
        }
    }
}