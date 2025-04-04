package danix.app.authentication_service.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.UUID;

@Component
public class JWTUtil {

    private final String SECRET;

    public JWTUtil(@Value("${jwt_secret}") String SECRET) {
        this.SECRET = SECRET;
    }

    public String generateToken(String email) {
        Date expirationDate = Date.from(ZonedDateTime.now().plusDays(14).toInstant());
        return JWT.create()
                .withSubject("User details")
                .withClaim("email", email)
                .withJWTId(UUID.randomUUID().toString())
                .withIssuedAt(new Date())
                .withIssuer("items-sales-service")
                .withExpiresAt(expirationDate)
                .sign(Algorithm.HMAC256(SECRET));
    }

    public String getEmailFromToken(String token) throws JWTVerificationException {
        JWTVerifier verifier = JWT.require(Algorithm.HMAC256(SECRET))
                .withSubject("User details")
                .withIssuer("items-sales-service")
                .build();
        DecodedJWT jwt = verifier.verify(token);
        return jwt.getClaim("email").asString();
    }

    public String getIdFromToken(String token) throws JWTVerificationException {
        JWTVerifier verifier = JWT.require(Algorithm.HMAC256(SECRET))
                .withSubject("User details")
                .withIssuer("items-sales-service")
                .build();
        DecodedJWT jwt = verifier.verify(token);
        return jwt.getClaim("jti").asString();
    }
}
