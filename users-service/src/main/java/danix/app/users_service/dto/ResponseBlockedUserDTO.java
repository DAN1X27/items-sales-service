package danix.app.users_service.dto;

public record ResponseBlockedUserDTO(Long id) {
    @Override
    public Long id() {
        return id;
    }
}
