package azhukov.service;

import azhukov.entity.Candidate;
import azhukov.entity.UserEntity;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  public String generateCandidateToken(Candidate candidate) {
    // TODO: Реализовать генерацию JWT с ролью CANDIDATE
    return "mocked-jwt-token-for-candidate-" + candidate.getId();
  }

  public String generateAdminToken(UserEntity user) {
    // TODO: Реализовать генерацию JWT с ролью ADMIN
    return "mocked-jwt-token-for-admin-" + user.getId();
  }

  public boolean validateToken(String token) {
    return token != null
        && (token.startsWith("mocked-jwt-token-for-admin-")
            || token.startsWith("mocked-jwt-token-for-candidate-"));
  }

  public String extractUsername(String token) {
    if (token.startsWith("mocked-jwt-token-for-admin-")) {
      String userId = token.replace("mocked-jwt-token-for-admin-", "");
      return "admin-" + userId; // Временное решение
    } else if (token.startsWith("mocked-jwt-token-for-candidate-")) {
      String candidateId = token.replace("mocked-jwt-token-for-candidate-", "");
      return "candidate-" + candidateId; // Временное решение
    }
    return null;
  }

  public boolean isAdminToken(String token) {
    return token != null && token.startsWith("mocked-jwt-token-for-admin-");
  }

  public boolean isCandidateToken(String token) {
    return token != null && token.startsWith("mocked-jwt-token-for-candidate-");
  }
}
