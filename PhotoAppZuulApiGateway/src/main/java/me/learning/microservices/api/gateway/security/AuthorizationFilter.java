package me.learning.microservices.api.gateway.security;

import java.io.IOException;
import java.util.Collections;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;

public class AuthorizationFilter extends BasicAuthenticationFilter {

    private final Environment env;

    public AuthorizationFilter(AuthenticationManager authenticationManager, Environment env) {
        super(authenticationManager);
        this.env = env;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        String authorizationHeader = request.getHeader(env.getProperty("authorization.token.header.name"));
        if (authorizationHeader == null || !authorizationHeader.startsWith(env.getProperty("authorization.token.header.prefix"))) {
            chain.doFilter(request, response);
            return;
        }

        try {
            UsernamePasswordAuthenticationToken auth = getAuthentication(request);
            SecurityContextHolder.getContext().setAuthentication(auth);

            chain.doFilter(request, response);
        } catch (ExpiredJwtException e) {
            response.setStatus(419);
        }
    }

    private UsernamePasswordAuthenticationToken getAuthentication(HttpServletRequest request) throws ExpiredJwtException {
        String authorizationHeader = request.getHeader(env.getProperty("authorization.token.header.name"));
        if (authorizationHeader == null) {
            return null;
        }

        String token = authorizationHeader.replace(env.getProperty("authorization.token.header.prefix"), "").trim();
        String userId = Jwts.parser()
            .setSigningKey(env.getProperty("authorization.token.secret"))
            .parseClaimsJws(token)
            .getBody()
            .getSubject();

        return userId == null
            ? null
            : new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
    }
}
