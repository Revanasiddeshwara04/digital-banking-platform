package com.revana.bank.apigateway.filter;

import com.revana.bank.apigateway.jwt.JwtService;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationFilter
        implements GlobalFilter, Ordered {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(
            JwtService jwtService) {

        this.jwtService = jwtService;
    }

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain) {

        String path =
                exchange.getRequest()
                        .getURI()
                        .getPath();

        System.out.println("PATH = " + path);

        // Public APIs
        if (path.startsWith("/api/auth/login")
                || path.startsWith("/api/auth/register")
                || path.startsWith("/api/auth/refresh-token")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")) {

            return chain.filter(exchange);
        }

        String authHeader =
                exchange.getRequest()
                        .getHeaders()
                        .getFirst("Authorization");

        System.out.println("AUTH HEADER = "
                + authHeader);

        if (authHeader == null
                || !authHeader.startsWith("Bearer ")) {

            exchange.getResponse()
                    .setStatusCode(
                            HttpStatus.UNAUTHORIZED);

            return exchange.getResponse()
                    .setComplete();
        }

        String token =
                authHeader.substring(7);

        System.out.println("TOKEN = " + token);

        if (!jwtService.isTokenValid(token)) {

            exchange.getResponse()
                    .setStatusCode(
                            HttpStatus.UNAUTHORIZED);

            return exchange.getResponse()
                    .setComplete();
        }

        String role =
                jwtService.extractRole(token);

        String username =
                jwtService.extractUsername(token);

        Long userId =
                jwtService.extractUserId(token);

        System.out.println("ROLE = " + role);
        System.out.println("USERNAME = " + username);
        System.out.println("USER ID = " + userId);

        // Forward user details to downstream services
        exchange = exchange.mutate()
                .request(request -> request
                        .header("X-Username", username)
                        .header("X-Role", role)
                        .header(
                                "X-UserId",
                                String.valueOf(userId)))
                .build();

        // ADMIN ONLY APIs

        if (path.equals("/api/accounts")
                && exchange.getRequest().getMethod() == HttpMethod.GET
                && !"ADMIN".equals(role)) {

            exchange.getResponse()
                    .setStatusCode(
                            HttpStatus.FORBIDDEN);

            return exchange.getResponse()
                    .setComplete();
        }

        if (path.contains("/close")
                && !"ADMIN".equals(role)) {

            exchange.getResponse()
                    .setStatusCode(
                            HttpStatus.FORBIDDEN);

            return exchange.getResponse()
                    .setComplete();
        }

        if (path.contains("/search")
                && !"ADMIN".equals(role)) {

            exchange.getResponse()
                    .setStatusCode(
                            HttpStatus.FORBIDDEN);

            return exchange.getResponse()
                    .setComplete();
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {

        return -1;
    }
}