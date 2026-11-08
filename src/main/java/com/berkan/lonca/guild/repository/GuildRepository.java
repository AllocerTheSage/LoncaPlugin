
package com.berkan.lonca.guild.repository;

import com.berkan.lonca.database.connection.DatabaseExecutor;
import com.berkan.lonca.guild.model.Guild;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import com.berkan.lonca.guild.model.GuildCapacityResult;



public class GuildRepository {

    private final DatabaseExecutor databaseExecutor;

    public GuildRepository(DatabaseExecutor databaseExecutor) {
        this.databaseExecutor = databaseExecutor;
    }

    // Yeni lonca oluşturur ve kurucuyu lider olarak kaydeder.
    public CompletableFuture<Guild> create(
            String name,
            UUID leaderUuid,
            int capacity
    ) {

        return databaseExecutor.submit(connection -> {

            connection.setAutoCommit(false);

            try {
                String sql = """
                    INSERT INTO guilds (name, capacity)
                    VALUES (?, ?)
                    """;

                long guildId;

                try (PreparedStatement statement =
                             connection.prepareStatement(sql)) {

                    statement.setString(1, name);
                    statement.setInt(2, capacity);

                    statement.executeUpdate();
                }

                // Oluşturulan loncanın ID numarasını al.
                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     "SELECT last_insert_rowid()"
                             );
                     ResultSet result = statement.executeQuery()) {

                    if (!result.next()) {
                        throw new SQLException("Lonca ID alinamadi!");
                    }

                    guildId = result.getLong(1);
                }

                // Kurucuyu loncaya lider olarak ekle.
                String memberSql = """
                    INSERT INTO guild_members
                    (guild_id, player_uuid, rank)
                    VALUES (?, ?, 'LEADER')
                    """;

                try (PreparedStatement statement =
                             connection.prepareStatement(memberSql)) {

                    statement.setLong(1, guildId);
                    statement.setString(2, leaderUuid.toString());

                    statement.executeUpdate();
                }

                connection.commit();

                return new Guild(
                        guildId,
                        name,
                        null,
                        1,
                        0,
                        capacity,
                        null,
                        false,
                        null
                );

            } catch (SQLException exception) {

                connection.rollback();
                throw exception;

            } finally {

                connection.setAutoCommit(true);
            }
        });
    }

    // Oyuncunun herhangi bir loncaya üye olup olmadığını kontrol eder.
    public CompletableFuture<Boolean> isPlayerInGuild(UUID playerUuid) {

        return databaseExecutor.submit(connection -> {

            String sql = """
                    SELECT 1 FROM guild_members
                    WHERE player_uuid = ?
                    """;

            try (PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setString(1, playerUuid.toString());

                try (ResultSet result = statement.executeQuery()) {
                    return result.next();
                }
            }
        });
    }

    // Lonca adının daha önce kullanılıp kullanılmadığını kontrol eder.
    public CompletableFuture<Boolean> existsByName(String name) {

        return databaseExecutor.submit(connection -> {

            String sql = """
                    SELECT 1 FROM guilds
                    WHERE name = ? COLLATE NOCASE
                    """;

            try (PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setString(1, name);

                try (ResultSet result = statement.executeQuery()) {
                    return result.next();
                }
            }
        });
    }

    // Lonca TAG'inin daha önce kullanılıp kullanılmadığını kontrol eder.
    public CompletableFuture<Boolean> existsByTag(String tag) {

        return databaseExecutor.submit(connection -> {

            String sql = """
                    SELECT 1 FROM guilds
                    WHERE tag = ? COLLATE NOCASE
                    """;

            try (PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setString(1, tag);

                try (ResultSet result = statement.executeQuery()) {
                    return result.next();
                }
            }
        });
    }
    // Oyuncunun herhangi bir loncanın lideri olup olmadığını kontrol eder.
    public CompletableFuture<Boolean> isGuildLeader(UUID playerUuid) {

        return databaseExecutor.submit(connection -> {

            String sql = """
                SELECT 1
                FROM guild_members
                WHERE player_uuid = ?
                AND rank = 'LEADER'
                LIMIT 1
                """;

            try (PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setString(1, playerUuid.toString());

                try (ResultSet result = statement.executeQuery()) {
                    return result.next();
                }
            }
        });
    }
    // Loncanın adını lider üzerinden günceller.
    public CompletableFuture<Boolean> updateGuildName(UUID leaderUuid, String newName) {

        return databaseExecutor.submit(connection -> {

            String sql = """
                UPDATE guilds
                SET name = ?
                WHERE id = (
                    SELECT guild_id
                    FROM guild_members
                    WHERE player_uuid = ?
                    AND rank = 'LEADER'
                )
                """;

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, newName);
                statement.setString(2, leaderUuid.toString());

                return statement.executeUpdate() > 0;
            }
        });
    }

    // Loncanın TAG bilgisini lider üzerinden günceller.
    public CompletableFuture<Boolean> updateGuildTag(UUID leaderUuid, String newTag) {

        return databaseExecutor.submit(connection -> {

            String sql = """
                UPDATE guilds
                SET tag = ?
                WHERE id = (
                    SELECT guild_id
                    FROM guild_members
                    WHERE player_uuid = ?
                    AND rank = 'LEADER'
                )
                """;

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, newTag);
                statement.setString(2, leaderUuid.toString());

                return statement.executeUpdate() > 0;
            }
        });
    }
    // Lonca kapasitesini kalıcı olarak 1 artırır.
// Liderin loncasının kapasitesini artırır ve lonca ID'sini döndürür.

    // Lonca kapasitesini güvenli bir transaction ile 1 artırır.
    public CompletableFuture<GuildCapacityResult> increaseGuildCapacity(UUID leaderUuid) {

        return databaseExecutor.submit(connection -> {

            // Önce bağlantının mevcut durumunu sakla.
            boolean previousAutoCommit = connection.getAutoCommit();

            connection.setAutoCommit(false);

            try {

                long guildId;

                // 1. Oyuncunun lider olduğu loncayı bul.
                String findSql = """
                        SELECT guild_id
                        FROM guild_members
                        WHERE player_uuid = ?
                        AND rank = 'LEADER'
                        """;

                try (PreparedStatement statement =
                             connection.prepareStatement(findSql)) {

                    statement.setString(1, leaderUuid.toString());

                    try (ResultSet result = statement.executeQuery()) {

                        if (!result.next()) {
                            connection.rollback();
                            return null;
                        }

                        guildId = result.getLong("guild_id");
                    }
                }

                // 2. Bulunan loncanın kapasitesini 1 artır.
                String updateSql = """
                        UPDATE guilds
                        SET capacity = capacity + 1
                        WHERE id = ?
                        """;

                try (PreparedStatement statement =
                             connection.prepareStatement(updateSql)) {

                    statement.setLong(1, guildId);

                    if (statement.executeUpdate() == 0) {
                        connection.rollback();
                        return null;
                    }
                }

                // 3. Yeni kapasiteyi veritabanından oku.
                String selectSql = """
                        SELECT capacity
                        FROM guilds
                        WHERE id = ?
                        """;

                int newCapacity;

                try (PreparedStatement statement =
                             connection.prepareStatement(selectSql)) {

                    statement.setLong(1, guildId);

                    try (ResultSet result = statement.executeQuery()) {

                        if (!result.next()) {
                            connection.rollback();
                            return null;
                        }

                        newCapacity = result.getInt("capacity");
                    }
                }

                // 4. Bütün işlemler başarılıysa değişiklikleri kaydet.
                connection.commit();

                return new GuildCapacityResult(guildId, newCapacity);

            } catch (SQLException exception) {

                // Herhangi bir SQL hatasında bütün değişiklikleri geri al.
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    exception.addSuppressed(rollbackException);
                }

                throw exception;

            } finally {

                // Bağlantıyı önceki otomatik kayıt ayarına döndür.
                connection.setAutoCommit(previousAutoCommit);
            }
        });
    }

    // Başarısız eşya kullanımında kapasite artışını geri alır.
// Belirtilen loncanın kapasitesini bir azaltır.
    public CompletableFuture<Boolean> decreaseGuildCapacity(long guildId) {

        return databaseExecutor.submit(connection -> {

            String sql = """
                UPDATE guilds
                SET capacity = capacity - 1
                WHERE id = ?
                AND capacity > 1
                """;

            try (PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setLong(1, guildId);

                return statement.executeUpdate() > 0;
            }
        });
    }
}
