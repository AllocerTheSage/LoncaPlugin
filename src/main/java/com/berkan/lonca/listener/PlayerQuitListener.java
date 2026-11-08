
package com.berkan.lonca.listener;

import com.berkan.lonca.bootstrap.LoncaPlugin;
import com.berkan.lonca.guild.service.GuildItemSessionManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    private final LoncaPlugin plugin;

    public PlayerQuitListener(LoncaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {

        var player = event.getPlayer();
        var playerUuid = player.getUniqueId();

        // Oyuncunun aktif isim veya TAG değiştirme oturumunu al.
        var session = plugin.getGuildItemSessionManager()
                .takeSession(playerUuid);

        // Aktif oturum varsa rezerve edilen eşyayı iade listesine ekle.
        if (session != null) {

            plugin.getGuildItemSessionManager().queueItemReturn(
                    playerUuid,
                    session.getReservedItem()
            );
        }

        // Oyuncunun çevrimiçi verisini bellekten kaldır.
        plugin.getPlayerService().unloadPlayer(playerUuid);
    }
}
