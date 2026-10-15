package edu.bsc.exam;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ScoringTest {
    @Test void scoresCorrectAnswersAndIgnoresWrongAnswers() {
        var key = Map.of(1L, new Scoring.Key("A", new BigDecimal("2.00")),
                         2L, new Scoring.Key("C", new BigDecimal("1.50")),
                         3L, new Scoring.Key("B", new BigDecimal("3.00")));
        assertEquals(new BigDecimal("2.00"), Scoring.score(key, Map.of(1L, "A", 2L, "B")));
    }
    @Test void unansweredQuestionGetsZero() {
        var key = Map.of(1L, new Scoring.Key("D", new BigDecimal("4.00")));
        assertEquals(new BigDecimal("0.00"), Scoring.score(key, Map.of()));
    }
    @Test void optionMatchingIsCaseInsensitive() {
        var key = Map.of(1L, new Scoring.Key("B", new BigDecimal("1.25")));
        assertEquals(new BigDecimal("1.25"), Scoring.score(key, Map.of(1L, "b")));
    }
    @Test void emptyExamGetsZero() {
        assertEquals(new BigDecimal("0.00"), Scoring.score(Map.of(), Map.of()));
    }
}
