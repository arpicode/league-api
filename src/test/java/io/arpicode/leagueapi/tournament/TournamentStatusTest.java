package io.arpicode.leagueapi.tournament;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class TournamentStatusTest {

    @ParameterizedTest(name = "{0} → {1} = {2}")
    @MethodSource("transitions")
    @DisplayName("should allow valid tournament status transitions")
    void canTransitionTo(TournamentStatus from, TournamentStatus to, boolean expected) {
        assertThat(expected).isEqualTo(from.canTransitionTo(to));
    }

    @ParameterizedTest(name = "{0} terminal = {1}")
    @MethodSource("terminalStatuses")
    @DisplayName("should report a status as terminal only when a tournament can never leave it")
    void isTerminal(TournamentStatus status, boolean expected) {
        assertThat(status.isTerminal()).isEqualTo(expected);
    }

    static Stream<Arguments> terminalStatuses() {
        return Stream.of(
                Arguments.of(TournamentStatus.DRAFT, false),
                Arguments.of(TournamentStatus.OPEN, false),
                Arguments.of(TournamentStatus.IN_PROGRESS, false),
                Arguments.of(TournamentStatus.CLOSED, true),
                Arguments.of(TournamentStatus.CANCELLED, true)
        );
    }

    static Stream<Arguments> transitions() {
        return Stream.of(
                // DRAFT
                Arguments.of(TournamentStatus.DRAFT, TournamentStatus.DRAFT, true),
                Arguments.of(TournamentStatus.DRAFT, TournamentStatus.OPEN, true),
                Arguments.of(TournamentStatus.DRAFT, TournamentStatus.CANCELLED, true),
                Arguments.of(TournamentStatus.DRAFT, TournamentStatus.IN_PROGRESS, false),
                Arguments.of(TournamentStatus.DRAFT, TournamentStatus.CLOSED, false),

                // OPEN
                Arguments.of(TournamentStatus.OPEN, TournamentStatus.OPEN, true),
                Arguments.of(TournamentStatus.OPEN, TournamentStatus.DRAFT, false),
                Arguments.of(TournamentStatus.OPEN, TournamentStatus.CANCELLED, true),
                Arguments.of(TournamentStatus.OPEN, TournamentStatus.IN_PROGRESS, true),
                Arguments.of(TournamentStatus.OPEN, TournamentStatus.CLOSED, false),

                // IN_PROGRESS
                Arguments.of(TournamentStatus.IN_PROGRESS, TournamentStatus.IN_PROGRESS, true),
                Arguments.of(TournamentStatus.IN_PROGRESS, TournamentStatus.CLOSED, true),
                Arguments.of(TournamentStatus.IN_PROGRESS, TournamentStatus.DRAFT, false),
                Arguments.of(TournamentStatus.IN_PROGRESS, TournamentStatus.OPEN, false),
                Arguments.of(TournamentStatus.IN_PROGRESS, TournamentStatus.CANCELLED, false),

                // CLOSED (terminal)
                Arguments.of(TournamentStatus.CLOSED, TournamentStatus.CLOSED, true),
                Arguments.of(TournamentStatus.CLOSED, TournamentStatus.DRAFT, false),
                Arguments.of(TournamentStatus.CLOSED, TournamentStatus.OPEN, false),
                Arguments.of(TournamentStatus.CLOSED, TournamentStatus.IN_PROGRESS, false),
                Arguments.of(TournamentStatus.CLOSED, TournamentStatus.CANCELLED, false),

                // CANCELLED (terminal)
                Arguments.of(TournamentStatus.CANCELLED, TournamentStatus.CANCELLED, true),
                Arguments.of(TournamentStatus.CANCELLED, TournamentStatus.DRAFT, false),
                Arguments.of(TournamentStatus.CANCELLED, TournamentStatus.OPEN, false),
                Arguments.of(TournamentStatus.CANCELLED, TournamentStatus.IN_PROGRESS, false),
                Arguments.of(TournamentStatus.CANCELLED, TournamentStatus.CLOSED, false)
        );
    }
}