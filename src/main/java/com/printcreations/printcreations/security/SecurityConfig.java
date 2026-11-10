package com.printcreations.printcreations.security;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        var guestQuoteSubmission = PathPatternRequestMatcher.withDefaults()
                .matcher(HttpMethod.POST, "/api/quote-requests");

        return http
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.GET, "/", "/api/categories", "/api/products").permitAll()
                        .requestMatchers(HttpMethod.HEAD, "/", "/api/categories", "/api/products").permitAll()
                        .requestMatchers(guestQuoteSubmission).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/admin/quotes/{referenceNumber}").hasRole("ADMIN")
                        .anyRequest().denyAll()
                )
                .httpBasic(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.ignoringRequestMatchers(guestQuoteSubmission))
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    UserDetailsService users(Environment environment, PasswordEncoder passwordEncoder) {
        if (!environment.matchesProfiles("local-admin")) {
            return new InMemoryUserDetailsManager();
        }

        String password = environment.getRequiredProperty("PRINT_CREATIONS_ADMIN_PASSWORD");
        if (password.isBlank()) {
            throw new IllegalStateException("Set PRINT_CREATIONS_ADMIN_PASSWORD for local-admin.");
        }

        return new InMemoryUserDetailsManager(
                User.withUsername("admin")
                        .password(passwordEncoder.encode(password))
                        .roles("ADMIN")
                        .build()
        );
    }
}
