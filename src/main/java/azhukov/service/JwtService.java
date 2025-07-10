package azhukov.service;

import azhukov.config.ApplicationProperties;
import azhukov.entity.Candidate;
import azhukov.entity.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Сервис для работы с JWT токенами. Поддерживает генерацию и валидацию токенов для администраторов
 * и кандидатов.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JwtService {

  private final ApplicationProperties applicationProperties;

  /** Генерирует JWT токен для кандидата */
  public String generateCandidateToken(Candidate candidate) {
    log.debug("Generating JWT token for candidate: {}", candidate.getId());

    Map<String, Object> claims = new HashMap<>();
    claims.put("role", "CANDIDATE");
    claims.put("candidateId", candidate.getId());
    claims.put("positionId", candidate.getPosition().getId());
    claims.put("email", candidate.getEmail());

    return generateToken(claims, "candidate-" + candidate.getId());
  }

  /** Генерирует JWT токен для администратора */
  public String generateAdminToken(UserEntity user) {
    log.debug("Generating JWT token for admin: {}", user.getId());

    Map<String, Object> claims = new HashMap<>();
    claims.put("role", "ADMIN");
    claims.put("userId", user.getId());
    claims.put("email", user.getEmail());
    claims.put("firstName", user.getFirstName());
    claims.put("lastName", user.getLastName());

    return generateToken(claims, user.getEmail());
  }

  /** Валидирует JWT токен */
  public boolean validateToken(String token) {
    try {
      if (token == null || token.trim().isEmpty()) {
        return false;
      }

      SecretKey key = getSigningKey();
      Jwts.parser().setSigningKey(key).build().parseClaimsJws(token);

      return true;
    } catch (JwtException | IllegalArgumentException e) {
      log.warn("Invalid JWT token: {}", e.getMessage());
      return false;
    }
  }

  /** Извлекает username из токена */
  public String extractUsername(String token) {
    try {
      Claims claims = extractAllClaims(token);
      return claims.getSubject();
    } catch (JwtException e) {
      log.warn("Failed to extract username from token: {}", e.getMessage());
      return null;
    }
  }

  /** Проверяет, является ли токен админским */
  public boolean isAdminToken(String token) {
    try {
      Claims claims = extractAllClaims(token);
      return "ADMIN".equals(claims.get("role"));
    } catch (JwtException e) {
      log.warn("Failed to check admin role from token: {}", e.getMessage());
      return false;
    }
  }

  /** Проверяет, является ли токен кандидатским */
  public boolean isCandidateToken(String token) {
    try {
      Claims claims = extractAllClaims(token);
      return "CANDIDATE".equals(claims.get("role"));
    } catch (JwtException e) {
      log.warn("Failed to check candidate role from token: {}", e.getMessage());
      return false;
    }
  }

  /** Извлекает ID пользователя из токена */
  public Long extractUserId(String token) {
    try {
      Claims claims = extractAllClaims(token);
      if (isAdminToken(token)) {
        return Long.valueOf(claims.get("userId").toString());
      } else if (isCandidateToken(token)) {
        return Long.valueOf(claims.get("candidateId").toString());
      }
      return null;
    } catch (JwtException e) {
      log.warn("Failed to extract user ID from token: {}", e.getMessage());
      return null;
    }
  }

  /** Извлекает email из токена */
  public String extractEmail(String token) {
    try {
      Claims claims = extractAllClaims(token);
      return claims.get("email", String.class);
    } catch (JwtException e) {
      log.warn("Failed to extract email from token: {}", e.getMessage());
      return null;
    }
  }

  /** Проверяет, истек ли токен */
  public boolean isTokenExpired(String token) {
    try {
      Claims claims = extractAllClaims(token);
      return claims.getExpiration().before(new Date());
    } catch (JwtException e) {
      log.warn("Failed to check token expiration: {}", e.getMessage());
      return true;
    }
  }

  /** Генерирует токен с заданными claims */
  private String generateToken(Map<String, Object> claims, String subject) {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime expiration =
        now.plus(applicationProperties.getSecurity().getJwt().getExpiration());

    return Jwts.builder()
        .setClaims(claims)
        .setSubject(subject)
        .setIssuedAt(Date.from(now.atZone(ZoneId.systemDefault()).toInstant()))
        .setExpiration(Date.from(expiration.atZone(ZoneId.systemDefault()).toInstant()))
        .signWith(getSigningKey(), SignatureAlgorithm.HS256)
        .compact();
  }

  /** Извлекает все claims из токена */
  private Claims extractAllClaims(String token) {
    SecretKey key = getSigningKey();
    return Jwts.parser().setSigningKey(key).build().parseClaimsJws(token).getBody();
  }

  /** Получает ключ для подписи токенов */
  private SecretKey getSigningKey() {
    String secret = applicationProperties.getSecurity().getJwt().getSecret();
    return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }
}
