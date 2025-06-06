package danix.app.gatewayservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class GatewayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("authentication-service", r -> r.path("/auth/**")
                        .uri("lb://authentication-service"))
                .route("users-service", r -> r.path("/users/**")
                        .uri("lb://users-service"))
                .route("announcements-service", r -> r.path("/announcements/**")
                        .uri("lb://announcements-service"))
                .route("chats-service", r -> r.path("/chats/**", "/ws/**")
                        .filters(filter -> filter
                                .dedupeResponseHeader("Access-Control-Allow-Origin", "RETAIN_UNIQUE")
                                .dedupeResponseHeader("Access-Control-Allow-Credentials", "RETAIN_UNIQUE"))
                        .uri("lb://chats-service"))
                .build();
    }
}
