package danix.app.authentication_service.feign;

import danix.app.authentication_service.keycloak_dto.KeycloakRegistrationDTO;
import danix.app.authentication_service.keycloak_dto.UserInfoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "keycloak", url = "${keycloak.url}")
public interface KeycloakAPI {

    @PostMapping("/admin/realms/items-sales-service/users")
    void registration(@RequestBody KeycloakRegistrationDTO body,
                      @RequestHeader(HttpHeaders.AUTHORIZATION) String accessToken);

    @PutMapping("/admin/realms/items-sales-service/users/{id}")
    void updateUserInfo(@PathVariable String id, @RequestBody UserInfoDTO body,
                        @RequestHeader(HttpHeaders.AUTHORIZATION) String accessToken);

    @PostMapping(value = "/realms/items-sales-service/protocol/openid-connect/token",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    Map<String, Object> getTokens(@RequestBody MultiValueMap<String, Object> body);

    @DeleteMapping("/admin/realms/items-sales-service/users/{id}")
    void deleteUser(@PathVariable String id, @RequestHeader(HttpHeaders.AUTHORIZATION) String accessToken);

    @PostMapping(value = "/realms/items-sales-service/protocol/openid-connect/logout",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    void logout(@RequestBody MultiValueMap<String, Object> body,
                @RequestHeader(HttpHeaders.AUTHORIZATION) String accessToken);

    @PutMapping("/admin/realms/items-sales-service/users/{id}/reset-password")
    void resetPassword(@PathVariable String id, @RequestBody Map<String, Object> body,
                       @RequestHeader(HttpHeaders.AUTHORIZATION) String accessToken);

    @GetMapping("/admin/realms/items-sales-service/users")
    List<UserInfoDTO> getUser(@RequestParam String email,
                              @RequestHeader(HttpHeaders.AUTHORIZATION) String accessToken);
}
