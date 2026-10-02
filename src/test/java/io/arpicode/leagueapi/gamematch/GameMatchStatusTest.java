package io.arpicode.leagueapi.gamematch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class GameMatchStatusTest {

    @ParameterizedTest(name = "{0} → {1} = {2}")
    @MethodSource("transitions")
    @DisplayName("should allow valid game match status transitions")
    void canTransitionTo(GameMatchStatus from, GameMatchStatus to, boolean expected) {
        assertThat(expected).isEqualTo(from.canTransitionTo(to));
    }

    @ParameterizedTest(name = "{0} terminal = {1}")
    @MethodSource("terminalStatuses")
    @DisplayName("should report a status as terminal only when a game match can never leave it")
    void isTerminal(GameMatchStatus status, boolean expected) {
        assertThat(status.isTerminal()).isEqualTo(expected);
    }

    static Stream<Arguments> terminalStatuses() {
        return Stream.of(
            Arguments.of(GameMatchStatus.SCHEDULED, false),
            Arguments.of(GameMatchStatus.IN_PROGRESS, false),
            Arguments.of(GameMatchStatus.COMPLETED, true),
            Arguments.of(GameMatchStatus.CANCELLED, true)
        );
    }

    static Stream<Arguments> transitions() {
        return Stream.of(
            // SCHEDULED
            Arguments.of(GameMatchStatus.SCHEDULED, GameMatchStatus.SCHEDULED, false),
            Arguments.of(GameMatchStatus.SCHEDULED, GameMatchStatus.IN_PROGRESS, true),
            Arguments.of(GameMatchStatus.SCHEDULED, GameMatchStatus.COMPLETED, false),
            Arguments.of(GameMatchStatus.SCHEDULED, GameMatchStatus.CANCELLED, true),

            // IN_PROGRESS
            Arguments.of(GameMatchStatus.IN_PROGRESS, GameMatchStatus.SCHEDULED, false),
            Arguments.of(GameMatchStatus.IN_PROGRESS, GameMatchStatus.IN_PROGRESS, false),
            Arguments.of(GameMatchStatus.IN_PROGRESS, GameMatchStatus.CANCELLED, true),
            Arguments.of(GameMatchStatus.IN_PROGRESS, GameMatchStatus.COMPLETED, true),

            // COMPLETED (terminal)
            Arguments.of(GameMatchStatus.COMPLETED, GameMatchStatus.SCHEDULED, false),
            Arguments.of(GameMatchStatus.COMPLETED, GameMatchStatus.IN_PROGRESS, false),
            Arguments.of(GameMatchStatus.COMPLETED, GameMatchStatus.COMPLETED, false),
            Arguments.of(GameMatchStatus.COMPLETED, GameMatchStatus.CANCELLED, false),

            // CANCELLED (terminal)
            Arguments.of(GameMatchStatus.CANCELLED, GameMatchStatus.SCHEDULED, false),
            Arguments.of(GameMatchStatus.CANCELLED, GameMatchStatus.IN_PROGRESS, false),
            Arguments.of(GameMatchStatus.CANCELLED, GameMatchStatus.COMPLETED, false),
            Arguments.of(GameMatchStatus.CANCELLED, GameMatchStatus.CANCELLED, false)
        );
    }

}