package com.foodplant.api;

import java.time.Duration;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

@Configuration
class SecurityConfig {
    static final String ISSUER = "food-plant";
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean UserDetailsService users(@Value("${plant.security.admin-hash}") String admin,
                                   @Value("${plant.security.user-hash}") String reader, @Value("${plant.security.staff-hash}") String staff) {
        if (!admin.matches("\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}") || !reader.matches("\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}") || !staff.matches("\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}"))
            throw new IllegalArgumentException("Both configured passwords must be BCrypt hashes");
        return new InMemoryUserDetailsManager(
                User.withUsername("admin").password(admin).roles("ADMIN").build(),
                User.withUsername("user").password(reader).roles("USER").build(),
                User.withUsername("staff").password(staff).roles("STAFF").build());
    }
    @Bean AuthenticationManager authenticationManager(UserDetailsService users, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }
    @Bean SecretKey jwtKey(@Value("${plant.security.secret}") String configured) {
        byte[] key;
        try { key = Base64.getDecoder().decode(configured); }
        catch (IllegalArgumentException ex) { throw new IllegalArgumentException("JWT secret must be Base64", ex); }
        if (key.length < 32) throw new IllegalArgumentException("JWT secret must decode to at least 32 bytes");
        return new SecretKeySpec(key, "HmacSHA256");
    }
    @Bean JwtEncoder jwtEncoder(SecretKey key) { return new NimbusJwtEncoder(new ImmutableSecret<>(key)); }
    @Bean JwtDecoder jwtDecoder(SecretKey key) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        JwtTimestampValidator timestamps = new JwtTimestampValidator(Duration.ZERO);
        OAuth2TokenValidator<Jwt> required = jwt -> jwt.getExpiresAt() != null && jwt.getIssuedAt() != null && jwt.getSubject() != null && !jwt.getSubject().isBlank()
                ? OAuth2TokenValidatorResult.success() : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(timestamps, new JwtIssuerValidator(ISSUER), required));
        return decoder;
    }
    @Bean org.springframework.boot.web.servlet.FilterRegistrationBean<RateLimitFilter> disableContainerRegistration(RateLimitFilter filter) {
        var registration = new org.springframework.boot.web.servlet.FilterRegistrationBean<>(filter); registration.setEnabled(false); return registration;
    }
    @Bean SecurityFilterChain security(HttpSecurity http, RateLimitFilter rateLimit) throws Exception {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles"); roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter(); converter.setJwtGrantedAuthoritiesConverter(roles);
        return http.csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .requestCache(c -> c.disable())
            .authorizeHttpRequests(a -> a.requestMatchers(HttpMethod.POST, "/api/v1/auth/token").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/**").hasAnyRole("USER", "STAFF", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/**").hasAnyRole("STAFF", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/**").hasAnyRole("STAFF", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/**").hasRole("ADMIN")
                .anyRequest().denyAll())
            .addFilterBefore(rateLimit, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
            .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(converter)))
            .httpBasic(b -> b.disable()).formLogin(f -> f.disable())
            .build();
    }
}
