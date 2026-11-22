package com.dsanotebook;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Admin-only endpoints (the security config requires the ADMIN role for /api/admin/**). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    public record UserSummary(Long id, String email, String role, boolean enabled, String createdAt,
                              String lastLogin, long problems, long mastered, String lastAdded) {}
    /** Metadata only: admins see what was solved, never the notes or code. */
    public record ProblemMeta(Long id, String title, String topic, String difficulty, int stage, String due, String created) {}
    public record Stats(long users, long admins, long disabled, long problems, Map<String, Long> topics) {}
    public record RoleChange(String role) {}
    public record EnabledChange(boolean enabled) {}

    private final UserRepository users;
    private final ProblemRepository problems;

    AdminController(UserRepository users, ProblemRepository problems) {
        this.users = users;
        this.problems = problems;
    }

    @GetMapping("/users")
    public List<UserSummary> listUsers() {
        Map<Long, Object[]> sums = new HashMap<>();
        for (Object[] r : problems.summaryByUser()) sums.put((Long) r[0], r);
        return users.findAll(Sort.by("id")).stream().map(u -> {
            Object[] s = sums.get(u.getId());
            return new UserSummary(u.getId(), u.getEmail(), u.effectiveRole(), u.active(),
                    str(u.getCreatedAt()), str(u.getLastLoginAt()),
                    s == null ? 0 : ((Number) s[1]).longValue(),
                    s == null ? 0 : ((Number) s[2]).longValue(),
                    s == null ? null : str(s[3]));
        }).toList();
    }

    @GetMapping("/users/{id}/problems")
    public List<ProblemMeta> userProblems(@PathVariable Long id) {
        return problems.findByUserIdOrderByCreatedAtDescIdDesc(id).stream()
                .map(p -> new ProblemMeta(p.getId(), p.getTitle(), p.getTopic(), p.getDifficulty(), p.getStage(),
                        str(p.getDueDate()), str(p.getCreatedAt())))
                .toList();
    }

    @GetMapping("/stats")
    public Stats stats() {
        List<AppUser> all = users.findAll();
        Map<String, Long> topics = new LinkedHashMap<>();
        for (Object[] r : problems.countByTopic()) topics.put(String.valueOf(r[0]), ((Number) r[1]).longValue());
        return new Stats(all.size(),
                all.stream().filter(u -> "ADMIN".equals(u.effectiveRole())).count(),
                all.stream().filter(u -> !u.active()).count(),
                problems.count(), topics);
    }

    @PutMapping("/users/{id}/role")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeRole(@AuthenticationPrincipal Long me, @PathVariable Long id, @RequestBody RoleChange body) {
        if (!"USER".equals(body.role()) && !"ADMIN".equals(body.role())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role must be USER or ADMIN");
        }
        if (id.equals(me)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You can’t change your own role");
        AppUser u = find(id);
        u.setRole(body.role());
        users.save(u);
    }

    @PutMapping("/users/{id}/enabled")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setEnabled(@AuthenticationPrincipal Long me, @PathVariable Long id, @RequestBody EnabledChange body) {
        if (id.equals(me)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You can’t disable your own account");
        AppUser u = find(id);
        u.setEnabled(body.enabled());
        users.save(u);
    }

    private AppUser find(Long id) {
        return users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private static String str(Object o) { return o == null ? null : o.toString(); }
}
