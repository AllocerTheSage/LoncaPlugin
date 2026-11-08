
package com.berkan.lonca.listener;

import com.berkan.lonca.bootstrap.LoncaPlugin;
import com.berkan.lonca.guild.service.GuildItemSessionManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

public class GuildItemChatListener implements Listener {

    private final LoncaPlugin plugin;

    public GuildItemChatListener(LoncaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {

        Player player = event.getPlayer();

        var session = plugin.getGuildItemSessionManager()
                .getSession(player.getUniqueId());

        if (session == null) {
            return;
        }

        event.setCancelled(true);

        String value = PlainTextComponentSerializer.plainText()
                .serialize(event.message()).trim();

        plugin.getServer().getScheduler().runTask(plugin, () -> {

            if (!player.isOnline()) {
                return;
            }

            var currentSession = plugin.getGuildItemSessionManager()
                    .getSession(player.getUniqueId());

            if (currentSession == null) {
                return;
            }

            boolean nameChange = currentSession.getType()
                    == GuildItemSessionManager.SessionType.CHANGE_NAME;

            boolean valid = nameChange
                    ? value.matches("[A-Za-z0-9 _.-]{4,16}")
                    : value.matches("[A-Za-z0-9_]{2,6}");

            if (!valid) {
                player.sendMessage(
                        "§cGeçersiz değer! Kurallara uygun giriş yap."
                );
                return;
            }

            // İlk giriş kaydedilir.
            if (currentSession.getFirstValue() == null) {
                currentSession.setFirstValue(value);
                player.sendMessage(
                        "§eOnaylamak için aynı değeri tekrar yaz."
                );
                return;
            }

            // İki giriş eşleşmiyorsa oturum kapatılır ve eşya iade edilir.
            if (!currentSession.getFirstValue().equals(value)) {

                var removedSession = plugin.getGuildItemSessionManager()
                        .takeSession(player.getUniqueId());

                returnReservedItem(player, removedSession);

                player.sendMessage(
                        "§cGirişler eşleşmedi. İşlem iptal edildi."
                );
                return;
            }

            currentSession.setConfirmedValue(value);

            var completedSession = plugin.getGuildItemSessionManager()
                    .takeSession(player.getUniqueId());

            updateGuild(player, completedSession, value);
        });
    }

    private void updateGuild(
            Player player,
            GuildItemSessionManager.Session session,
            String value
    ) {

        boolean nameChange = session.getType()
                == GuildItemSessionManager.SessionType.CHANGE_NAME;

        var future = nameChange
                ? plugin.getGuildRepository()
                .updateGuildName(player.getUniqueId(), value)
                : plugin.getGuildRepository()
                .updateGuildTag(player.getUniqueId(), value);

        future.whenComplete((success, error) -> {

            plugin.getServer().getScheduler().runTask(plugin, () -> {

                if (error != null || !Boolean.TRUE.equals(success)) {

                    if (player.isOnline()) {
                        returnReservedItem(player, session);

                        player.sendMessage(
                                "§cİşlem başarısız oldu. Eşyan iade edildi."
                        );
                    } else {
                        // Oyuncu çevrimdışıysa eşyayı kaybetmemek için
                        // bekleyen iade listesine kaydediyoruz.
                        plugin.getGuildItemSessionManager().queueItemReturn(
                                player.getUniqueId(),
                                session.getReservedItem()
                        );

                        plugin.getLogger().info(
                                "Oyuncunun eşyası iade listesine eklendi: "
                                        + player.getUniqueId()
                        );
                    }

                    if (error != null) {
                        plugin.getLogger().severe(error.getMessage());
                    }

                    return;
                }

                if (!player.isOnline()) {
                    return;
                }

                player.sendMessage(
                        nameChange
                                ? "§aLonca adı başarıyla değiştirildi: §f" + value
                                : "§aLonca TAG bilgisi başarıyla değiştirildi: §f" + value
                );
            });
        });
    }

    // Rezerve edilen eşyayı oyuncuya geri verir.
    private void returnReservedItem(
            Player player,
            GuildItemSessionManager.Session session
    ) {

        if (session == null || !player.isOnline()) {
            return;
        }

        ItemStack item = session.getReservedItem();
        var inventory = player.getInventory();

        int originalSlot = session.getOriginalSlot();

        // Önce eşyanın alındığı slotu kontrol et.
        if (originalSlot >= 0
                && originalSlot < inventory.getSize()
                && inventory.getItem(originalSlot) == null) {

            inventory.setItem(originalSlot, item);
            return;
        }

        // Slot doluysa envanterde uygun yer ara.
        var leftovers = inventory.addItem(item);

        // Envanter tamamen doluysa eşyayı yere bırak.
        for (ItemStack leftover : leftovers.values()) {
            player.getWorld().dropItemNaturally(
                    player.getLocation(),
                    leftover
            );
        }
    }
}
