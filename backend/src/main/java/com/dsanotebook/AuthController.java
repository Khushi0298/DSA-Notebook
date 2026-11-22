package com.dsanotebook;

import java.time.Instant;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    public record Credentials(String email, String password) {}
    public record AuthResponse(String token, String email, String role) {}

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final String adminEmail;

    AuthController(UserRepository users, PasswordEncoder encoder, JwtService jwt,
                   @Value("${app.admin-email:}") String adminEmail) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.adminEmail = adminEmail.trim().toLowerCase();
    }

    /** The account whose email equals ADMIN_EMAIL becomes the first admin. Everyone else starts as USER. */
    private boolean isAdminEmail(String email) {
        return !adminEmail.isBlank() && adminEmail.equals(email);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@RequestBody Credentials c) {
        String email = c.email() == null ? "" : c.email().trim().toLowerCase();
        if (!email.contains("@") || email.length() > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a valid email address");
        }
        if (c.password() == null || c.password().length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 8 characters");
        }
        if (users.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        AppUser u = new AppUser();
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(c.password()));
        u.setRole(isAdminEmail(email) ? "ADMIN" : "USER");
        u.setEnabled(true);
        u.setCreatedAt(LocalDate.now());
        u.setLastLoginAt(Instant.now());
        users.save(u);
        return new AuthResponse(jwt.create(u.getId()), email, u.effectiveRole());
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody Credentials c) {
        String email = c.email() == null ? "" : c.email().trim().toLowerCase();
        AppUser u = users.findByEmail(email)
                .filter(x -> c.password() != null && encoder.matches(c.password(), x.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (!u.active()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account has been disabled. Contact the admin.");
        }
        if (isAdminEmail(email)) u.setRole("ADMIN");
        u.setLastLoginAt(Instant.now());
        users.save(u);
        return new AuthResponse(jwt.create(u.getId()), email, u.effectiveRole());
    }
}
