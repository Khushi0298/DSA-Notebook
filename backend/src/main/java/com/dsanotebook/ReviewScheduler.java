package com.dsanotebook;

import java.time.LocalDate;

/** Spaced repetition: a remembered problem comes back after a longer gap each time. */
public final class ReviewScheduler {
    static final int[] STEPS = {1, 3, 7, 14, 30, 60};

    public record Result(int stage, LocalDate due) {}

    private ReviewScheduler() {}

    public static Result next(int stage, boolean remembered, LocalDate today) {
        int s = remembered ? Math.min(stage + 1, STEPS.length - 1) : 0;
        return new Result(s, today.plusDays(STEPS[s]));
    }
}
