package com.dsanotebook;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ReviewSchedulerTest {
    private final LocalDate today = LocalDate.of(2026, 1, 10);

    @Test
    void rememberedMovesToNextStep() {
        var r = ReviewScheduler.next(0, true, today);
        assertEquals(1, r.stage());
        assertEquals(today.plusDays(3), r.due());
    }

    @Test
    void forgottenResetsToTomorrow() {
        var r = ReviewScheduler.next(4, false, today);
        assertEquals(0, r.stage());
        assertEquals(today.plusDays(1), r.due());
    }

    @Test
    void stageIsCappedAtLastStep() {
        var r = ReviewScheduler.next(5, true, today);
        assertEquals(5, r.stage());
        assertEquals(today.plusDays(60), r.due());
    }
}
