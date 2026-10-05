package com.plateforward.auth;

import static org.springframework.http.HttpStatus.*;

import com.plateforward.auth.AuthDtos.*;
import com.plateforward.user.*;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtEncoder jwtEncoder;
    @Value("${app.jwt-ttl}")
    private Duration ttl;

    public AuthResponse register(RegisterRequest r) {
        if (r.role() == User.Role.ADMIN) throw new ResponseStatusException(FORBIDDEN, "Admin accounts cannot be self-registered");
        var email = r.email().toLowerCase();
        if (users.existsByEmail(email)) throw new ResponseStatusException(CONFLICT, "That email is already registered");
        var u = new User();
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(r.password()));
        u.setName(r.name());
        u.setRole(r.role());
        u.setPhone(r.phone());
        u.setArea(r.area());
        u.setVerified(r.role() != User.Role.ORG);
        return respond(users.save(u));
    }

    public AuthResponse login(LoginRequest r) {
        return users.findByEmail(r.email().toLowerCase())
                .filter(u -> encoder.matches(r.password(), u.getPasswordHash()))
                .map(this::respond)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Wrong email or password"));
    }

    private AuthResponse respond(User u) {
        var now = Instant.now();
        var claims = JwtClaimsSet.builder().subject(u.getId().toString()).claim("role", u.getRole().name())
                .issuedAt(now).expiresAt(now.plus(ttl)).build();
        var token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new AuthResponse(token, UserDto.of(u));
    }
}
