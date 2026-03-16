package com.gk.study.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtUtil {

    private static final Long EXPIRE_TIME = 1000 * 60 * 60 * 24L; // 1天
    private static final String SECRET_KEY = "your_jwt_secret";   // 建议放到配置中

    // 生成token
    public String generateToken(String username) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + EXPIRE_TIME);
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }

    // 解析token，获取subject(用户名)
    public String getUsernameFromToken(String token) {
        try {
            return Jwts.parser()           // 新版：用 parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()                       // 新版：需要调用 build()
                    .parseClaimsJws(token)          // 解析 token
                    .getBody()
                    .getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    // 新增：从token中提取用户名（与getUsernameFromToken相同）
    public String extractUsername(String token) {
        return getUsernameFromToken(token); // 直接复用现有的实现
    }

    // 检查token是否有效
    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(SECRET_KEY).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            // expired or invalid
            return false;
        }
    }
}

