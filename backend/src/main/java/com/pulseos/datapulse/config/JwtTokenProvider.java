package com.pulseos.datapulse.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 令牌签发、解析与验签工具类。
 * <p>
 * 使用 HMAC-SHA512（HS512）对称签名算法生成无状态访问令牌，由 {@link com.pulseos.datapulse.service.impl.UserServiceImpl}
 * 在用户登录/注册成功后签发，并由 {@link JwtAuthFilter} 在每次请求时验签解包。
 */
@Component
public class JwtTokenProvider {

    /** 签名密钥（HS512 要求密钥长度不少于 512 位 / 64 字节，生产环境建议通过环境变量覆盖） */
    @Value("${jwt.secret:DataPulseSecretKeyForJwtAuthenticationMustBeAtLeast512BitsLongAndSecure2026}")
    private String jwtSecret;

    /** Token 有效期（毫秒），默认 86400000ms 即 24 小时 */
    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    /** 将配置字符串转为 JJWT 所需的 HMAC 签名密钥对象 */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 为指定用户签发 JWT：将用户名作为 Subject，并把 userId、realName、role 写入自定义载荷。
     */
    public String generateToken(String username, Long userId, String realName, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("realName", realName)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), Jwts.SIG.HS512)
                .compact();
    }

    /**
     * 验签并解析出 JWT 中的全部声明载荷（Claims）。
     */
    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 校验 JWT 签名是否合法且未过期；任何格式错误、篡改或超时异常均视为无效返回 {@code false}。
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
