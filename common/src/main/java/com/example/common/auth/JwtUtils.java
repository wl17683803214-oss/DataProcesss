package com.example.common.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;

/** JWT 创建与解析工具，签名密钥由 Nacos 配置统一提供。 */
public final class JwtUtils {
    private JwtUtils() {
    }

    /** 创建只包含用户标识和会话标识的访问令牌。 */
    public static String createToken(
            Long userId,
            String tokenId,
            String secret,
            long expireSeconds) {
        Date now = new Date();
        JwtBuilder builder = Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("tokenId", tokenId)
                .setIssuedAt(now);
        // 过期秒数大于零时写入有效期，零表示令牌永久有效。
        if (expireSeconds > 0) {
            builder.setExpiration(
                    new Date(now.getTime() + expireSeconds * 1000L));
        }
        return builder
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }

    /** 校验签名和有效期，并返回令牌声明。 */
    public static Claims parseToken(String token, String secret) {
        return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
    }
}
