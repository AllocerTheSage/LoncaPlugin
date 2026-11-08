
package com.berkan.lonca.database.connection;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {

    private final JavaPlugin plugin;
    private Connection connection;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public synchronized void connect() throws SQLException, IOException {

        if (connection != null && !connection.isClosed()) {
            return;
        }

        File dataFolder = plugin.getDataFolder();

        Files.createDirectories(dataFolder.toPath());

        File databaseFile = new File(dataFolder, "lonca.db");

        String url = "jdbc:sqlite:" + databaseFile.getAbsolutePath();

        connection = DriverManager.getConnection(url);

        try (var statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA journal_mode = WAL");
            statement.execute("PRAGMA busy_timeout = 5000");
        }

        plugin.getLogger().info("SQLite veritabanina baglanildi!");
    }

    public synchronized Connection getConnection() {

        if (connection == null) {
            throw new IllegalStateException(
                    "Veritabani baglantisi henuz kurulmamış!"
            );
        }

        try {
            if (connection.isClosed()) {
                throw new IllegalStateException(
                        "Veritabani baglantisi kapali!"
                );
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Veritabani baglantisi kontrol edilemedi!",
                    exception
            );
        }

        return connection;
    }

    public synchronized void disconnect() throws SQLException {

        if (connection != null && !connection.isClosed()) {
            connection.close();
            connection = null;

            plugin.getLogger().info(
                    "SQLite veritabani baglantisi kapatildi!"
            );
        }
    }
}
