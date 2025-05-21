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

	private final Algorithm ALGORITHM;
	private final int STORAGE_DAYS;

	public JWTUtil(@Value("${jwt_secret}") String secret, @Value("${tokens-storage-days}") int storageDays) {
		this.ALGORITHM = Algorithm.HMAC256(secret);
		this.STORAGE_DAYS = storageDays;
	}

	public TokenData generateToken(String email) {
		Date expirationDate = Date.from(ZonedDateTime.now().plusDays(STORAGE_DAYS).toInstant());
		String id = UUID.randomUUID().toString();
		String token = JWT.create()
			.withSubject("User details")
			.withClaim("email", email)
			.withJWTId(id)
			.withIssuedAt(new Date())
			.withIssuer("items-sales-service")
			.withExpiresAt(expirationDate)
			.sign(ALGORITHM);
		return new TokenData(id, expirationDate, token);
	}

	public String getEmailFromToken(String token) throws JWTVerificationException {
		JWTVerifier verifier = JWT.require(ALGORITHM)
			.withSubject("User details")
			.withIssuer("items-sales-service")
			.build();
		DecodedJWT jwt = verifier.verify(token);
		return jwt.getClaim("email").asString();
	}

	public String getIdFromToken(String token) throws JWTVerificationException {
		JWTVerifier verifier = JWT.require(ALGORITHM)
			.withSubject("User details")
			.withIssuer("items-sales-service")
			.build();
		DecodedJWT jwt = verifier.verify(token);
		return jwt.getClaim("jti").asString();
	}

}
