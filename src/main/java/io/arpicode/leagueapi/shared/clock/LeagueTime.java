package io.arpicode.leagueapi.shared.clock;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.ZoneId;

// The league's calendar: tournament dates are days in this zone, so an instant is converted to it
// before it is compared with them. Kept apart from the Clock bean on purpose, so a test that fixes
// the clock at a UTC instant does not also move the date rules to UTC days.
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LeagueTime {

    public static final ZoneId ZONE = ZoneId.of("Europe/Paris");

}
