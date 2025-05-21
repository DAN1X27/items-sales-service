package danix.app.authentication_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record JWTTokenDTO(@JsonProperty("jwt-token") String token) {
}
