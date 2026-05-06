package com.budget.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, BudgetProperties properties) throws Exception {
        if (!properties.security().oauthEnabled()) {
            return http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .build();
        }

        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**", "/login/**", "/oauth2/**", "/error").permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(Customizer.withDefaults())
                .logout(logout -> logout.logoutSuccessUrl("/"))
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/uploads", "/api/rebuild"));
        return http.build();
    }

    @Bean
    OAuth2UserService<OAuth2UserRequest, OAuth2User> githubAllowlistUserService(BudgetProperties properties) {
        var delegate = new DefaultOAuth2UserService();
        return request -> {
            var user = delegate.loadUser(request);
            if (!"github".equals(request.getClientRegistration().getRegistrationId())) {
                return user;
            }
            var allowed = properties.security().allowedGithubLogin();
            var login = String.valueOf(user.getAttributes().getOrDefault("login", ""));
            if (!StringUtils.hasText(allowed) || !allowed.equalsIgnoreCase(login)) {
                var error = new OAuth2Error("access_denied", "GitHub account is not allowlisted", null);
                throw new OAuth2AuthenticationException(error);
            }
            var attributes = user.getAttributes();
            return new DefaultOAuth2User(AuthorityUtils.createAuthorityList("ROLE_USER"), attributes, "login");
        };
    }
}
