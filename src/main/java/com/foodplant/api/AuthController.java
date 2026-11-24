package com.foodplant.api;
import java.time.Instant;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
class AuthController {
    private final AuthenticationManager manager;
    private final JwtEncoder encoder;
    AuthController(AuthenticationManager manager, JwtEncoder encoder) { this.manager = manager; this.encoder = encoder; }
    record LoginRequest(@NotBlank @Size(max=64) String username, @NotBlank @Size(max=72) String password) {
        @Override public String toString() { return "LoginRequest[redacted]"; }
    }
    record TokenResponse(String accessToken, String tokenType, long expiresIn) {
        @Override public String toString() { return "TokenResponse[redacted]"; }
    }
    @PostMapping("/token") ResponseEntity<TokenResponse> token(@Valid @RequestBody LoginRequest request) {
        if (request.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).build();
        Authentication auth = manager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
        Instant now = Instant.now();
        List<String> roles = auth.getAuthorities().stream().map(a -> a.getAuthority().substring(5)).toList();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(SecurityConfig.ISSUER).subject(auth.getName()).issuedAt(now)
                .expiresAt(now.plusSeconds(900)).claim("roles", roles).build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("Pragma", "no-cache").body(new TokenResponse(token, "Bearer", 900));
    }
    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<Void> badCredentials() { return ResponseEntity.status(401).cacheControl(CacheControl.noStore()).build(); }
}
