
package com.berkan.lonca.player.repository;

import com.berkan.lonca.database.connection.DatabaseExecutor;

import java.sql.PreparedStatement;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import com.berkan.lonca.player.model.PlayerData;
import java.sql.ResultSet;
import java.util.Optional;

public class PlayerRepository {

    private final DatabaseExecutor databaseExecutor;

    public PlayerRepository(DatabaseExecutor databaseExecutor) {
        this.databaseExecutor = databaseExecutor;
    }

    public CompletableFuture<Void> saveOrUpdate(
            UUID playerUuid,
            String playerName
    ) {

        return databaseExecutor.submit(connection -> {

            String sql = """
                    INSERT INTO players (uuid, last_known_name)
                    VALUES (?, ?)
                    ON CONFLICT(uuid)
                    DO UPDATE SET last_known_name = excluded.last_known_name
                    """;

            try (PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                statement.setString(1, playerUuid.toString());
                statement.setString(2, playerName);

                statement.executeUpdate();
            }

            return null;
        });
    }

    public CompletableFuture<Optional<PlayerData>> findByUuid(UUID playerUuid) {

        return databaseExecutor.submit(connection -> {

            String sql = """
                SELECT uuid, last_known_name,
                       total_kills, total_deaths,
                       war_wins, war_losses
                FROM players
                WHERE uuid = ?
                """;

            try (PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setString(1, playerUuid.toString());

                try (ResultSet result = statement.executeQuery()) {

                    if (result.next()) {

                        PlayerData playerData = new PlayerData(
                                UUID.fromString(result.getString("uuid")),
                                result.getString("last_known_name"),
                                result.getInt("total_kills"),
                                result.getInt("total_deaths"),
                                result.getInt("war_wins"),
                                result.getInt("war_losses")
                        );

                        return Optional.of(playerData);
                    }
                }
            }

            return Optional.empty();
        });
    }

}
