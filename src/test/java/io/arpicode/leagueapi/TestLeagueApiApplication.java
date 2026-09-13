package io.arpicode.leagueapi;

import org.springframework.boot.SpringApplication;

public class TestLeagueApiApplication {

    public static void main(String[] args) {
        SpringApplication.from(LeagueApiApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
