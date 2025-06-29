package danix.app.chats_service.config;

import danix.app.chats_service.repositories.UsersChatsRepository;
import danix.app.chats_service.repositories.SupportChatsRepository;
import danix.app.chats_service.models.User;
import danix.app.chats_service.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthChannelInterceptorAdapter implements ChannelInterceptor {

	private final UsersChatsRepository usersChatsRepository;

	private final SupportChatsRepository supportChatsRepository;

	private final SecurityUtil securityUtil;

	private final JwtDecoder jwtDecoder;

	private final JwtAuthConverter jwtAuthConverter;

	@Override
	public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
		StompHeaderAccessor accessor = Objects.requireNonNull(
				MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class)
		);
		switch (Objects.requireNonNull(accessor.getCommand())) {
			case CONNECT -> {
				String header = Objects.requireNonNull(accessor.getFirstNativeHeader("Authorization"));
				AbstractAuthenticationToken authToken = getAuthToken(header);
				accessor.setUser(authToken);
			}
			case SUBSCRIBE -> {
				Authentication authentication = Objects.requireNonNull((Authentication) accessor.getUser());
				User user = securityUtil.getCurrentUserFromAuthentication(authentication);
				String dest = Objects.requireNonNull(accessor.getDestination());
				if (dest.startsWith("/topic/chat/")) {
					long id = Long.parseLong(dest.substring(12));
					usersChatsRepository.findById(id).ifPresentOrElse(chat -> {
						if (chat.getUser1Id() != user.getId() && chat.getUser2Id() != user.getId()) {
							throw new IllegalArgumentException();
						}
					}, () -> {
						throw new IllegalArgumentException();
					});
				}
				else if (dest.startsWith("/topic/user/")) {
					long id = Long.parseLong(dest.substring(12, dest.lastIndexOf('/')));
					if (id != user.getId()) {
						throw new IllegalArgumentException();
					}
				}
				else if (dest.startsWith("/topic/support/")) {
					long id = Long.parseLong(dest.substring(15));
					supportChatsRepository.findById(id).ifPresentOrElse(chat -> {
						if (chat.getUserId() != user.getId() && !chat.getAdminId().equals(user.getId())) {
							throw new IllegalArgumentException();
						}
					}, () -> {
						throw new IllegalArgumentException();
					});
				} else {
					throw new IllegalArgumentException();
				}
			}
		}
		return message;
	}

	private AbstractAuthenticationToken getAuthToken(String header) {
		if (header.startsWith("Bearer ")) {
			String tokenValue = header.substring(7);
			Jwt jwt = jwtDecoder.decode(tokenValue);
			return jwtAuthConverter.convert(jwt);
		}
		throw new IllegalArgumentException();
	}

}