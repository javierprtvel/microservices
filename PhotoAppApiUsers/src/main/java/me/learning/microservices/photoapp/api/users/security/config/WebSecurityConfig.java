package me.learning.microservices.photoapp.api.users.security.config;

import me.learning.microservices.photoapp.api.users.security.AuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;

import me.learning.microservices.photoapp.api.users.security.authorization.AuthorizationFilter;
import me.learning.microservices.photoapp.api.users.security.authorization.AuthorizationHeaderParser;
import me.learning.microservices.photoapp.api.users.service.UsersService;

@Configuration
@Profile("!security-disabled")
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    private final Environment env;

    private final AuthorizationHeaderParser authorizationHeaderParser;

    private final UsersService usersService;

    private final PasswordEncoder bCryptPasswordEncoder;

    @Autowired
    public WebSecurityConfig(Environment env, AuthorizationHeaderParser authorizationHeaderParser, UsersService usersService, PasswordEncoder bCryptPasswordEncoder) {
        this.env = env;
        this.authorizationHeaderParser = authorizationHeaderParser;
        this.usersService = usersService;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests()
                .antMatchers(HttpMethod.POST, "/users").hasIpAddress(env.getProperty("gateway.ip"))
                .antMatchers("/h2-console/**").permitAll()
                .anyRequest().authenticated()
            .and()
            .addFilter(getAuthenticationFilter())
            .addFilter(new AuthorizationFilter(authenticationManager(), authorizationHeaderParser))
            .headers().frameOptions().disable(); // H2 console runs within a frame
    }

    private AuthenticationFilter getAuthenticationFilter() throws Exception {
        AuthenticationFilter authFilter = new AuthenticationFilter(usersService, env, authenticationManager());
        authFilter.setFilterProcessesUrl(env.getProperty("login.url.path")); // another way of setting custom login path
        return authFilter;
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(usersService).passwordEncoder(bCryptPasswordEncoder);
    }

}
