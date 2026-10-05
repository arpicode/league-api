package io.arpicode.leagueapi.shared.clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;


@Configuration
public class ClockConfiguration {

    // Instants only, so UTC. The zone that turns an instant into a league day is LeagueTime.ZONE,
    // never this clock's.
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

}
