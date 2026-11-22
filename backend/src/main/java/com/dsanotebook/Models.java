package com.dsanotebook;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

@Entity
@Table(name = "app_users")
@Getter @Setter @NoArgsConstructor
class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(unique = true, nullable = false) private String email;
    @Column(nullable = false) private String passwordHash;
    // Nullable on purpose: rows created before roles existed still load (null means USER / enabled).
    private String role;
    private Boolean enabled;
    private LocalDate createdAt;
    private Instant lastLoginAt;

    String effectiveRole() { return role == null ? "USER" : role; }
    boolean active() { return enabled == null || enabled; }
}

@Entity
@Table(name = "problems")
@Getter @Setter @NoArgsConstructor
class Problem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long userId;
    private String title;
    @Column(length = 500) private String link;
    private String topic;
    private String difficulty;
    @Column(columnDefinition = "text") private String statement;
    @Column(columnDefinition = "text") private String brute;
    @Column(columnDefinition = "text") private String optimal;
    private String timeComplexity;
    private String spaceComplexity;
    @Column(columnDefinition = "text") private String trick;
    @Column(columnDefinition = "text") private String code;
    private int stage;
    private LocalDate dueDate;
    private LocalDate createdAt;
}

interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmail(String email);
}

interface ProblemRepository extends JpaRepository<Problem, Long> {
    List<Problem> findByUserIdOrderByCreatedAtDescIdDesc(Long userId);
    Optional<Problem> findByIdAndUserId(Long id, Long userId);

    /** Per user: [userId, total, mastered, last added]. */
    @Query("select p.userId, count(p), count(case when p.stage >= 5 then 1 end), max(p.createdAt) from Problem p group by p.userId")
    List<Object[]> summaryByUser();

    @Query("select p.topic, count(p) from Problem p group by p.topic order by count(p) desc")
    List<Object[]> countByTopic();
}
