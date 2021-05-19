package me.learning.microservices.api.gateway.filter;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import org.apache.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthorizationHeaderFilter extends AbstractGatewayFilterFactory<AuthorizationHeaderFilter.Config> {

    @Autowired
    private Environment env;

    public static class Config {
        // Put configuration properties here
    }

    public AuthorizationHeaderFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            String authorizationHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authorizationHeader == null || !authorizationHeader.startsWith(env.getProperty("authorization.token.header.prefix"))) {
                return onError(exchange, HttpStatus.UNAUTHORIZED);
            }

            try {
                boolean isValid = isValidAuthorizationHeader(authorizationHeader);
                return isValid ? chain.filter(exchange) : onError(exchange, HttpStatus.UNAUTHORIZED);
            } catch (ExpiredJwtException e) {
                return onError(exchange, HttpStatus.valueOf(419));
            } catch (Exception e) {
                return onError(exchange, HttpStatus.UNAUTHORIZED);
            }
        };
    }

    private boolean isValidAuthorizationHeader(String authHeaderValue) throws ExpiredJwtException {
        String token = authHeaderValue.replace(env.getProperty("authorization.token.header.prefix"), "").trim();
        String userId = Jwts.parser()
            .setSigningKey(env.getProperty("authorization.token.secret"))
            .parseClaimsJws(token)
            .getBody()
            .getSubject();

        return userId != null && !userId.isEmpty();
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        return response.setComplete();
    }
}
