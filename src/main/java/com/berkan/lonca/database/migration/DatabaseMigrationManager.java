
package com.berkan.lonca.database.migration;

import com.berkan.lonca.database.connection.DatabaseManager;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseMigrationManager {

    private final DatabaseManager databaseManager;

    public DatabaseMigrationManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void migrate() throws SQLException {

        Connection connection = databaseManager.getConnection();

        try (Statement statement = connection.createStatement()) {

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS schema_version (
                        version INTEGER PRIMARY KEY,
                        applied_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
        }

        int currentVersion = getCurrentVersion(connection);

        if (currentVersion < 2) {

            connection.setAutoCommit(false);

            try (Statement migration = connection.createStatement()) {

                migration.execute("""
                        CREATE TABLE players (
                            uuid TEXT PRIMARY KEY,
                            last_known_name TEXT NOT NULL,
                            total_kills INTEGER NOT NULL DEFAULT 0,
                            total_deaths INTEGER NOT NULL DEFAULT 0,
                            war_wins INTEGER NOT NULL DEFAULT 0,
                            war_losses INTEGER NOT NULL DEFAULT 0
                        )
                        """);

                migration.execute("""
                        CREATE TABLE guilds (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            name TEXT NOT NULL COLLATE NOCASE UNIQUE,
                            tag TEXT NOT NULL COLLATE NOCASE UNIQUE,
                            level INTEGER NOT NULL DEFAULT 1,
                            xp INTEGER NOT NULL DEFAULT 0,
                            capacity INTEGER NOT NULL,
                            description TEXT,
                            applications_open INTEGER NOT NULL DEFAULT 0,
                            created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,

                            CHECK (level BETWEEN 1 AND 30),
                            CHECK (xp >= 0),
                            CHECK (capacity >= 1),
                            CHECK (applications_open IN (0, 1))
                        )
                        """);

                migration.execute("""
                        CREATE TABLE guild_members (
                            guild_id INTEGER NOT NULL,
                            player_uuid TEXT NOT NULL,
                            rank TEXT NOT NULL DEFAULT 'MEMBER',
                            contribution_xp INTEGER NOT NULL DEFAULT 0,
                            joined_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,

                            PRIMARY KEY (player_uuid),

                            FOREIGN KEY (guild_id)
                                REFERENCES guilds(id) ON DELETE CASCADE,

                            FOREIGN KEY (player_uuid)
                                REFERENCES players(uuid) ON DELETE CASCADE,

                            CHECK (rank IN ('LEADER', 'MANAGER', 'MEMBER')),
                            CHECK (contribution_xp >= 0)
                        )
                        """);

                migration.execute("""
                        CREATE UNIQUE INDEX idx_one_leader_per_guild
                        ON guild_members(guild_id)
                        WHERE rank = 'LEADER'
                        """);

                migration.execute("""
                        CREATE TABLE guild_applications (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            guild_id INTEGER NOT NULL,
                            player_uuid TEXT NOT NULL,
                            status TEXT NOT NULL DEFAULT 'PENDING',
                            created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            expires_at TEXT NOT NULL,

                            FOREIGN KEY (guild_id)
                                REFERENCES guilds(id) ON DELETE CASCADE,

                            FOREIGN KEY (player_uuid)
                                REFERENCES players(uuid) ON DELETE CASCADE,

                            CHECK (status IN (
                                'PENDING', 'ACCEPTED', 'REJECTED', 'EXPIRED'
                            ))
                        )
                        """);

                migration.execute("""
                        CREATE UNIQUE INDEX idx_unique_pending_application
                        ON guild_applications(guild_id, player_uuid)
                        WHERE status = 'PENDING'
                        """);

                migration.execute("""
                        CREATE TABLE guild_invitations (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            guild_id INTEGER NOT NULL,
                            inviter_uuid TEXT NOT NULL,
                            target_uuid TEXT NOT NULL,
                            created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            expires_at TEXT NOT NULL,

                            FOREIGN KEY (guild_id)
                                REFERENCES guilds(id) ON DELETE CASCADE,

                            FOREIGN KEY (inviter_uuid)
                                REFERENCES players(uuid),

                            FOREIGN KEY (target_uuid)
                                REFERENCES players(uuid),

                            CHECK (inviter_uuid != target_uuid)
                        )
                        """);

                migration.execute("""
                        CREATE UNIQUE INDEX idx_unique_pending_invitation
                        ON guild_invitations(guild_id, target_uuid)
                        """);

                migration.execute("""
                        CREATE INDEX idx_guild_members_guild
                        ON guild_members(guild_id)
                        """);

                migration.execute("""
                        CREATE INDEX idx_applications_guild_status
                        ON guild_applications(guild_id, status)
                        """);

                migration.execute("""
                        CREATE INDEX idx_invitations_target
                        ON guild_invitations(target_uuid)
                        """);

                migration.execute("""
                        INSERT INTO schema_version (version)
                        VALUES (2)
                        """);

                connection.commit();

            } catch (SQLException exception) {

                connection.rollback();
                throw exception;

            } finally {

                connection.setAutoCommit(true);
            }
        }
    }

    private int getCurrentVersion(Connection connection)
            throws SQLException {

        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT COALESCE(MAX(version), 0) FROM schema_version"
             )) {

            return result.next() ? result.getInt(1) : 0;
        }
    }
}
