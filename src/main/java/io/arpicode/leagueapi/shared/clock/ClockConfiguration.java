package io.arpicode.leagueapi.shared.clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;


@Configuration
public class ClockConfiguration {

    // The league's zone, and so the clock's: tournament dates are calendar days in it. Services read
    // it back through Clock.getZone() rather than naming a zone themselves, so it is defined here only.
    public static final ZoneId EUROPE_PARIS = ZoneId.of("Europe/Paris");

    @Bean
    public Clock clock() {
        return Clock.system(EUROPE_PARIS);
    }

}
