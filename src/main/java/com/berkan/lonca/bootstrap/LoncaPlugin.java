
package com.berkan.lonca.bootstrap;

import com.berkan.lonca.database.connection.DatabaseExecutor;
import com.berkan.lonca.database.connection.DatabaseManager;
import com.berkan.lonca.database.migration.DatabaseMigrationManager;
import com.berkan.lonca.guild.model.Guild;
import com.berkan.lonca.listener.PlayerJoinListener;
import com.berkan.lonca.listener.PlayerQuitListener;
import com.berkan.lonca.player.repository.PlayerRepository;
import com.berkan.lonca.player.service.PlayerService;
import org.bukkit.plugin.java.JavaPlugin;
import com.berkan.lonca.guild.repository.GuildRepository;
import com.berkan.lonca.guild.service.GuildService;
import com.berkan.lonca.common.ItemManager;
import com.berkan.lonca.listener.GuildItemListener;
import com.berkan.lonca.guild.service.GuildItemSessionManager;
import com.berkan.lonca.listener.GuildItemChatListener;

import java.io.IOException;
import java.sql.SQLException;

public final class LoncaPlugin extends JavaPlugin {

    private DatabaseManager databaseManager;
    private DatabaseExecutor databaseExecutor;

    private PlayerRepository playerRepository;
    private PlayerService playerService;

    private GuildRepository guildRepository;
    private GuildService guildService;
    private GuildItemSessionManager guildItemSessionManager;

    private ItemManager itemManager;


    @Override
    public void onEnable() {

        try {
            // Veritabanı bağlantısı
            databaseManager = new DatabaseManager(this);
            databaseManager.connect();

            // Veritabanı tabloları
            DatabaseMigrationManager migrationManager =
                    new DatabaseMigrationManager(databaseManager);

            migrationManager.migrate();

            // Veritabanı işlem kuyruğu
            databaseExecutor = new DatabaseExecutor(databaseManager);

            // Oyuncu sistemi
            playerRepository = new PlayerRepository(databaseExecutor);
            playerService = new PlayerService(playerRepository);

            // Lonca Sistemi
            guildRepository = new GuildRepository(databaseExecutor);
            guildService = new GuildService(guildRepository);
            guildItemSessionManager = new GuildItemSessionManager();
            // ItemManager
            itemManager = new ItemManager(this);

            // Event kayıtları
            getServer().getPluginManager().registerEvents(
                    new PlayerJoinListener(this),
                    this
            );

            getServer().getPluginManager().registerEvents(
                    new PlayerQuitListener(this),
                    this
            );

            getServer().getPluginManager().registerEvents(
                    new GuildItemListener(this), this
            );

            getServer().getPluginManager().registerEvents(
                    new GuildItemChatListener(this), this
            );

            getLogger().info("LoncaPlugin basariyla etkinlestirildi!");

        } catch (SQLException | IOException exception) {

            getLogger().severe("LoncaPlugin baslatilamadi!");
            exception.printStackTrace();

            if (databaseManager != null) {
                try {
                    databaseManager.disconnect();
                } catch (SQLException closeException) {
                    closeException.printStackTrace();
                }
            }

            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {

        boolean canDisconnect = true;

        // Önce veritabanı işlem kuyruğunu kapat.
        if (databaseExecutor != null) {
            canDisconnect = databaseExecutor.shutdown();
        }

        // Ardından SQLite bağlantısını kapat.
        if (canDisconnect && databaseManager != null) {
            try {
                databaseManager.disconnect();

            } catch (SQLException exception) {
                getLogger().severe("Veritabani kapatilamadi!");
                exception.printStackTrace();
            }

        } else if (!canDisconnect) {
            getLogger().severe(
                    "Veritabani islemleri tamamlanamadi. Baglanti acik birakildi!"
            );
        }

        getLogger().info("LoncaPlugin kapatildi.");
    }
    // PUBLİC KISMI

    // DB
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    public DatabaseExecutor getDatabaseExecutor() {
        return databaseExecutor;
    }
    // PLAYER
    public PlayerRepository getPlayerRepository() {
        return playerRepository;
    }
    public PlayerService getPlayerService() {
        return playerService;
    }
    // GUİLD
    public GuildRepository getGuildRepository() {
        return guildRepository;
    }
    public GuildService getGuildService() {
        return guildService;
    }
    public  GuildItemSessionManager getGuildItemSessionManager() {
        return guildItemSessionManager;
    }
    // ItemManager
    public ItemManager getItemManager() {
        return itemManager;
    }
}
