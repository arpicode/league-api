package io.arpicode.leagueapi.tournament;

import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TournamentRepository extends JpaRepository<Tournament, Long> {

    // Every response embeds a BoardGameSummary, so the association is fetched up front.
    // Without these, mapping a page of tournaments issues one extra select per row.
    @Override
    @NonNull
    @EntityGraph(attributePaths = "boardGame")
    Page<Tournament> findAll(@NonNull Pageable pageable);

    @Override
    @NonNull
    @EntityGraph(attributePaths = "boardGame")
    Optional<Tournament> findById(@NonNull Long id);

}
