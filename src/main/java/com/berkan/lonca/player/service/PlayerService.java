
package com.berkan.lonca.player.service;

import com.berkan.lonca.player.model.PlayerData;
import com.berkan.lonca.player.repository.PlayerRepository;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerService {

    private final PlayerRepository playerRepository;

    private final Map<UUID, PlayerData> onlinePlayers =
            new ConcurrentHashMap<>();

    public PlayerService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    public CompletableFuture<PlayerData> loadPlayer(
            UUID uuid,
            String playerName
    ) {

        return playerRepository.saveOrUpdate(uuid, playerName)
                .thenCompose(result -> playerRepository.findByUuid(uuid))
                .thenApply(optional -> {

                    PlayerData playerData = optional.orElseThrow(
                            () -> new IllegalStateException(
                                    "Oyuncu verisi bulunamadi: " + uuid
                            )
                    );

                    onlinePlayers.put(uuid, playerData);

                    return playerData;
                });
    }

    public PlayerData getPlayer(UUID uuid) {
        return onlinePlayers.get(uuid);
    }

    public void unloadPlayer(UUID uuid) {
        onlinePlayers.remove(uuid);
    }
}
