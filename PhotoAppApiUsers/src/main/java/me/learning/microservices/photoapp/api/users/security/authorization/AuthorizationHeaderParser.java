package me.learning.microservices.photoapp.api.users.security.authorization;

import static me.learning.microservices.photoapp.api.users.security.authorization.AuthorizationConstants.AUTH_HEADER_NAME_PROPERTY;
import static me.learning.microservices.photoapp.api.users.security.authorization.AuthorizationConstants.AUTH_HEADER_TOKEN_PREFIX_PROPERTY;
import static me.learning.microservices.photoapp.api.users.security.authorization.AuthorizationConstants.AUTH_TOKEN_SECRET_PROPERTY;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import io.jsonwebtoken.Jwts;
import me.learning.microservices.photoapp.api.users.security.exception.AuthorizationHeaderException;

@Service
public class AuthorizationHeaderParser {

    @Autowired
    private Environment env;

    public String getAuthorizationHeaderFromContext() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
            .getRequest();
        return getAuthorizationHeader(request);
    }

    public String getAuthorizationHeader(HttpServletRequest request) {
        return request.getHeader(env.getProperty(AUTH_HEADER_NAME_PROPERTY));
    }

    public String parseToken(HttpServletRequest request) throws AuthorizationHeaderException {
        String authorizationHeader = request.getHeader(env.getProperty(AUTH_HEADER_NAME_PROPERTY));
        if (!isValidAuthorizationHeader(authorizationHeader)) {
            throw new AuthorizationHeaderException("Authorization header is not valid");
        }
        return authorizationHeader.replace(env.getProperty(AUTH_HEADER_TOKEN_PREFIX_PROPERTY), "")
            .trim();
    }

    public String parseUserIdFromToken(String token) {
        return Jwts.parser()
            .setSigningKey(env.getProperty(AUTH_TOKEN_SECRET_PROPERTY))
            .parseClaimsJws(token)
            .getBody()
            .getSubject();
    }

    public boolean isValidAuthorizationHeader(String authorizationHeader) {
        return authorizationHeader != null && authorizationHeader.startsWith(env.getProperty(AUTH_HEADER_TOKEN_PREFIX_PROPERTY));
    }
}
