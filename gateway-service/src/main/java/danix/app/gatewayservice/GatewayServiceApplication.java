package danix.app.gatewayservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.gateway.route.builder.UriSpec;
import org.springframework.context.annotation.Bean;

import java.util.function.Function;

@SpringBootApplication
public class GatewayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("authentication-service", r -> r.path("/auth/**")
                        .filters(corsFilter())
                        .uri("lb://authentication-service"))
                .route("users-service", r -> r.path("/users/**")
                        .filters(corsFilter())
                        .uri("lb://users-service"))
                .route("announcements-service", r -> r.path("/announcements/**")
                        .filters(corsFilter())
                        .uri("lb://announcements-service"))
                .route("chats-service", r -> r.path("/chats/**", "/ws/**")
                        .filters(corsFilter())
                        .uri("lb://chats-service"))
                .build();
    }

    private Function<GatewayFilterSpec, UriSpec> corsFilter() {
        return filter -> filter
                .dedupeResponseHeader("Access-Control-Allow-Origin", "RETAIN_UNIQUE")
                .dedupeResponseHeader("Access-Control-Allow-Credentials", "RETAIN_UNIQUE");
    }
}
