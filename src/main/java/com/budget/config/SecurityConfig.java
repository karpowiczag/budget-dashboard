package com.budget.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration(proxyBeanMethods = false)
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, BudgetProperties properties, OAuth2UserService<OidcUserRequest, OidcUser> googleAllowlistOidcUserService) throws Exception {
        if (!properties.security().oauthEnabled()) {
            return http
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .build();
        }

        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**", "/login/**", "/oauth2/**", "/error").permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(oauth -> oauth.userInfoEndpoint(userInfo -> userInfo.oidcUserService(googleAllowlistOidcUserService)))
                .logout(logout -> logout.logoutSuccessUrl("/"))
                .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    OAuth2UserService<OidcUserRequest, OidcUser> googleAllowlistOidcUserService(BudgetProperties properties) {
        var delegate = new OidcUserService();
        return request -> {
            var user = delegate.loadUser(request);
            if (!"google".equals(request.getClientRegistration().getRegistrationId())) {
                return user;
            }
            var allowed = properties.security().allowedGoogleEmail();
            var email = user.getEmail();
            var verified = Boolean.TRUE.equals(user.getEmailVerified());
            if (!StringUtils.hasText(allowed) || !verified || !allowed.equalsIgnoreCase(email)) {
                var error = new OAuth2Error("access_denied", "Google account is not allowlisted", null);
                throw new OAuth2AuthenticationException(error);
            }
            return user;
        };
    }

    private static final class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
            var csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (csrfToken != null) {
                csrfToken.getToken();
            }
            filterChain.doFilter(request, response);
        }
    }
}
