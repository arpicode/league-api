package io.arpicode.leagueapi.player;

import io.arpicode.leagueapi.player.dto.PlayerRequest;
import io.arpicode.leagueapi.player.dto.PlayerResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PlayerService {

    private final PlayerRepository playerRepository;

    public PlayerService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Transactional
    public PlayerResponse create(PlayerRequest playerRequest) {
        Player player = new Player(playerRequest.username(), playerRequest.email());
        Player savedPlayer = playerRepository.save(player);

        return toPlayerResponse(savedPlayer);
    }

    @Transactional(readOnly = true)
    public List<PlayerResponse> list() {
        List<Player> players = playerRepository.findAll();
        return players.stream()
                .map(this::toPlayerResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PlayerResponse getById(long id) {
        Player player = playerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Player not found"));

        return toPlayerResponse(player);
    }

    @Transactional
    public PlayerResponse update(long id, PlayerRequest playerRequest) {
        Player player = playerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Player not found"));

        player.setUsername(playerRequest.username());
        player.setEmail(playerRequest.email());

        return toPlayerResponse(player);
    }

    @Transactional
    public void delete(long id) {
        Player player = playerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Player not found"));

        playerRepository.delete(player);
    }

    private PlayerResponse toPlayerResponse(Player player) {
        return new PlayerResponse(
                player.getId(),
                player.getUsername(),
                player.getEmail(),
                player.getCreatedAt(),
                player.getUpdatedAt());
    }

}
