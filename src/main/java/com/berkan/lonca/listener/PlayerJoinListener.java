
package com.berkan.lonca.listener;

import com.berkan.lonca.bootstrap.LoncaPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final LoncaPlugin plugin;

    public PlayerJoinListener(LoncaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {

        var player = event.getPlayer();

        plugin.getPlayerService()
                .loadPlayer(player.getUniqueId(), player.getName())
                .whenComplete((playerData, error) -> {

                    if (error != null) {
                        plugin.getLogger().severe(
                                "Oyuncu verileri yuklenemedi: "
                                        + player.getName()
                        );
                        plugin.getLogger().severe(error.getMessage());
                        return;
                    }

                    plugin.getServer().getScheduler().runTask(
                            plugin,
                            () -> {
                                if (!player.isOnline()) {
                                    plugin.getPlayerService()
                                            .unloadPlayer(player.getUniqueId());
                                }
                            }
                    );
                });
    }
}
