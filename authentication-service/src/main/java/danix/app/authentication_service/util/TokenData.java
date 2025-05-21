package danix.app.authentication_service.util;

import java.util.Date;

public record TokenData(String id, Date expirationDate, String token) {
}
