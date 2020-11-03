package me.learning.microservices.photoapp.api.users.security.authorization;

import java.io.IOException;
import java.util.Collections;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import io.jsonwebtoken.ExpiredJwtException;
import me.learning.microservices.photoapp.api.users.security.exception.AuthorizationHeaderException;

public class AuthorizationFilter extends BasicAuthenticationFilter {

    private final AuthorizationHeaderParser authorizationHeaderParser;

    public AuthorizationFilter(AuthenticationManager authenticationManager, AuthorizationHeaderParser authorizationHeaderParser) {
        super(authenticationManager);
        this.authorizationHeaderParser = authorizationHeaderParser;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {

        try {
            String token = authorizationHeaderParser.parseToken(request);

            UsernamePasswordAuthenticationToken auth = getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(auth);

            chain.doFilter(request, response);
        } catch (AuthorizationHeaderException e) {
            chain.doFilter(request, response);
        } catch (ExpiredJwtException e) {
            response.setStatus(419);
        }
    }

    private UsernamePasswordAuthenticationToken getAuthentication(String token) {
        String userId = authorizationHeaderParser.parseUserIdFromToken(token);
        return userId == null
            ? null
            : new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
    }
}
