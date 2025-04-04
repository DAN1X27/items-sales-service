package danix.app.authentication_service.services;

import danix.app.authentication_service.models.Token;
import danix.app.authentication_service.repositories.TokensRepository;
import danix.app.authentication_service.util.JWTUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class TokenService {
    private final TokensRepository tokensRepository;
    private final JWTUtil jwtUtil;

    @Transactional
    public void save(String token, Long userId) {
        Date expiredDate = Date.from(ZonedDateTime.now().plusDays(14).toInstant());
        tokensRepository.save(new Token(jwtUtil.getIdFromToken(token), userId, expiredDate));
    }

    @Transactional
    public void deleteUserTokens(Long id) {
        tokensRepository.deleteAllByUserId(id);
    }

    public String validateToken(String jwtToken) {
        String id = jwtUtil.getIdFromToken(jwtToken);
        tokensRepository.findById(id).ifPresentOrElse(token -> {
            if (token.getExpiredDate().before(new Date())) {
                throw new IllegalStateException("Invalid token");
            }
        }, () -> {
            throw new IllegalStateException("Token not found");
        });
        return jwtUtil.getEmailFromToken(jwtToken);
    }

    @Transactional
    public void deleteExpiredTokens() {
        tokensRepository.deleteAllByExpiredDateBefore(new Date());
    }
}
