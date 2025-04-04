package danix.app.chats_service.config;

import danix.app.chats_service.repositories.ChatsRepository;
import danix.app.chats_service.security.User;
import danix.app.chats_service.security.UserDetailsImpl;
import danix.app.chats_service.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthChannelInterceptorAdapter implements ChannelInterceptor {
    private final UserDetailsServiceImpl userDetailsService;
    private final ChatsRepository chatsRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        switch (accessor.getCommand()) {
            case CONNECT -> {
                String header = accessor.getFirstNativeHeader("Authorization");
                UserDetailsImpl userDetails = (UserDetailsImpl) userDetailsService.loadUserByUsername(header);
                UsernamePasswordAuthenticationToken token =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                accessor.setUser(token);
            }
            case SUBSCRIBE -> {
                String dest = accessor.getDestination();
                Authentication authentication = (Authentication) accessor.getUser();
                UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
                User user = userDetails.authentication();
                if (dest.startsWith("/topic/chat/")) {
                    long id = Long.parseLong(dest.substring(12));
                    chatsRepository.findById(id).ifPresentOrElse(chat -> {
                        if (chat.getUser1Id() != user.getId() && chat.getUser2Id() != user.getId()) {
                            throw new IllegalArgumentException();
                        }
                    }, () -> {
                        throw new IllegalArgumentException();
                    });
                } else if (dest.startsWith("/topic/user/")) {
                    long id = Long.parseLong(dest.substring(12, dest.lastIndexOf('/')));
                    if (id != user.getId()) {
                        throw new IllegalArgumentException();
                    }
                }
            }
        }
        return message;
    }
}