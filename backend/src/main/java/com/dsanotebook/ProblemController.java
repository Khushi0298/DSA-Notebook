package com.dsanotebook;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/problems")
public class ProblemController {
    /** Same field names the React app uses. Dates are ISO strings (yyyy-MM-dd). */
    public record ProblemDto(Long id, String title, String link, String topic, String difficulty,
                             String statement, String brute, String optimal, String time, String space,
                             String trick, String code, Integer stage, String due, String created) {}

    private final ProblemRepository repo;

    ProblemController(ProblemRepository repo) { this.repo = repo; }

    @GetMapping
    public List<ProblemDto> list(@AuthenticationPrincipal Long userId) {
        return repo.findByUserIdOrderByCreatedAtDescIdDesc(userId).stream().map(ProblemController::toDto).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProblemDto create(@AuthenticationPrincipal Long userId, @RequestBody ProblemDto d) {
        requireValid(d);
        Problem p = new Problem();
        p.setUserId(userId);
        apply(p, d);
        LocalDate created = parseDate(d.created(), LocalDate.now());
        p.setCreatedAt(created);
        p.setStage(0);
        p.setDueDate(created.plusDays(1));
        return toDto(repo.save(p));
    }

    @PutMapping("/{id}")
    public ProblemDto update(@AuthenticationPrincipal Long userId, @PathVariable Long id, @RequestBody ProblemDto d) {
        requireValid(d);
        Problem p = owned(userId, id);
        apply(p, d);
        return toDto(repo.save(p));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        repo.delete(owned(userId, id));
    }

    /** The client sends its own local date so "tomorrow" matches the user's time zone. */
    @PostMapping("/{id}/review")
    public ProblemDto review(@AuthenticationPrincipal Long userId, @PathVariable Long id,
                             @RequestParam boolean remembered, @RequestParam(required = false) String today) {
        Problem p = owned(userId, id);
        var r = ReviewScheduler.next(p.getStage(), remembered, parseDate(today, LocalDate.now()));
        p.setStage(r.stage());
        p.setDueDate(r.due());
        return toDto(repo.save(p));
    }

    /** Lets you bring in a JSON backup exported from the browser-only version. */
    @PostMapping("/import")
    public Map<String, Integer> importAll(@AuthenticationPrincipal Long userId, @RequestBody List<ProblemDto> list) {
        if (list.size() > 2000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Too many problems in one import");
        int imported = 0;
        for (ProblemDto d : list) {
            if (!valid(d)) continue;
            Problem p = new Problem();
            p.setUserId(userId);
            apply(p, d);
            LocalDate created = parseDate(d.created(), LocalDate.now());
            p.setCreatedAt(created);
            p.setStage(d.stage() == null ? 0 : Math.max(0, Math.min(5, d.stage())));
            p.setDueDate(parseDate(d.due(), created.plusDays(1)));
            repo.save(p);
            imported++;
        }
        return Map.of("imported", imported);
    }

    private Problem owned(Long userId, Long id) {
        return repo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Problem not found"));
    }

    private static boolean valid(ProblemDto d) {
        return d != null && notBlank(d.title()) && notBlank(d.statement()) && notBlank(d.optimal());
    }

    private static void requireValid(ProblemDto d) {
        if (!valid(d)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title, problem statement and optimal approach are required");
    }

    private static boolean notBlank(String s) { return s != null && !s.isBlank(); }

    private static String cut(String s, int n) { return s == null || s.length() <= n ? s : s.substring(0, n); }

    private static LocalDate parseDate(String s, LocalDate fallback) {
        try { return s == null || s.isBlank() ? fallback : LocalDate.parse(s); } catch (Exception e) { return fallback; }
    }

    private static void apply(Problem p, ProblemDto d) {
        p.setTitle(cut(d.title(), 255));
        p.setLink(cut(d.link(), 500));
        p.setTopic(cut(d.topic(), 100));
        p.setDifficulty(cut(d.difficulty(), 50));
        p.setStatement(d.statement());
        p.setBrute(d.brute());
        p.setOptimal(d.optimal());
        p.setTimeComplexity(cut(d.time(), 100));
        p.setSpaceComplexity(cut(d.space(), 100));
        p.setTrick(d.trick());
        p.setCode(d.code());
    }

    private static ProblemDto toDto(Problem p) {
        return new ProblemDto(p.getId(), p.getTitle(), p.getLink(), p.getTopic(), p.getDifficulty(), p.getStatement(),
                p.getBrute(), p.getOptimal(), p.getTimeComplexity(), p.getSpaceComplexity(), p.getTrick(), p.getCode(),
                p.getStage(), p.getDueDate().toString(), p.getCreatedAt().toString());
    }
}
