package me.learning.microservices.photoapp.api.users.security.authorization;

public final class AuthorizationConstants {

    private AuthorizationConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final String AUTH_HEADER_NAME_PROPERTY = "authorization.token.header.name";

    public static final String AUTH_HEADER_TOKEN_PREFIX_PROPERTY = "authorization.token.header.prefix";

    public static final String AUTH_TOKEN_SECRET_PROPERTY = "authorization.token.secret";
}
