package com.dsanotebook;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class MeController {
    public record Me(String email, String role) {}

    private final UserRepository users;

    MeController(UserRepository users) { this.users = users; }

    /** The frontend calls this on load to know who is logged in and whether to show the admin tab. */
    @GetMapping("/api/me")
    public Me me(@AuthenticationPrincipal Long userId) {
        AppUser u = users.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return new Me(u.getEmail(), u.effectiveRole());
    }
}
