package com.berkan.lonca.listener;

import com.berkan.lonca.bootstrap.LoncaPlugin;
import com.berkan.lonca.guild.service.GuildItemSessionManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

public class GuildItemListener implements Listener {

    private final LoncaPlugin plugin;
    private final Set<UUID> pendingCapacityPlayers =
            ConcurrentHashMap.newKeySet();

    public GuildItemListener(LoncaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onItemUse(PlayerInteractEvent event) {

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        var item = event.getItem();

        boolean nameItem = plugin.getItemManager().isNameChangeItem(item);
        boolean tagItem = plugin.getItemManager().isTagChangeItem(item);
        boolean capacityItem = plugin.getItemManager().isCapacityItem(item);

        if (!nameItem && !tagItem && !capacityItem) {
            return;
        }

        event.setCancelled(true);

        // Kapasite eşyası kullanımı.
        if (capacityItem) {
            increaseCapacity(player);
            return;
        }

        // İsim ve TAG değiştirme işlemleri yalnızca liderlere açıktır.
        plugin.getGuildRepository()
                .isGuildLeader(player.getUniqueId())
                .whenComplete((isLeader, error) -> {

                    plugin.getServer().getScheduler().runTask(plugin, () -> {

                        if (!player.isOnline()) {
                            return;
                        }

                        if (error != null) {
                            player.sendMessage("§cLonca bilgisi kontrol edilemedi.");
                            plugin.getLogger().severe(error.getMessage());
                            return;
                        }

                        if (!Boolean.TRUE.equals(isLeader)) {
                            player.sendMessage("§cBu eşyayı yalnızca lonca lideri kullanabilir.");
                            return;
                        }

                        GuildItemSessionManager.SessionType type;

                        if (nameItem) {
                            type = GuildItemSessionManager.SessionType.CHANGE_NAME;
                            player.sendMessage("§6Yeni lonca adını sohbete yaz.");
                        } else {
                            type = GuildItemSessionManager.SessionType.CHANGE_TAG;
                            player.sendMessage("§bYeni lonca TAG bilgisini sohbete yaz.");
                        }

                        // Oyuncunun elindeki eşyayı kontrol et.
                        var inventory = player.getInventory();
                        int originalSlot = inventory.getHeldItemSlot();

                        var reservedItem = inventory.getItemInMainHand();

                        boolean correctItem = nameItem
                                ? plugin.getItemManager().isNameChangeItem(reservedItem)
                                : plugin.getItemManager().isTagChangeItem(reservedItem);

                        if (!correctItem) {
                            player.sendMessage("§cKullanılan eşya artık elinde bulunmuyor.");
                            return;
                        }

// Kullanılan eşyadan bir adet ayır.
                        var itemCopy = reservedItem.clone();
                        itemCopy.setAmount(1);

                        if (reservedItem.getAmount() > 1) {
                            reservedItem.setAmount(reservedItem.getAmount() - 1);
                        } else {
                            inventory.setItemInMainHand(null);
                        }

// Eşyayı ve orijinal slotunu oturuma kaydet.
                        plugin.getGuildItemSessionManager().startSession(
                                player.getUniqueId(),
                                type,
                                itemCopy,
                                originalSlot
                        );
                    });
                });
    }

    // Kapasiteyi artırır ve başarılıysa eşyayı tüketir.

    // Kapasite eşyasını güvenli şekilde kullanır.
    private void increaseCapacity(Player player) {

        UUID playerUuid = player.getUniqueId();

        // Aynı oyuncunun birden fazla işlem başlatmasını engelle.
        if (!pendingCapacityPlayers.add(playerUuid)) {
            player.sendMessage("§cKapasite işlemin zaten devam ediyor.");
            return;
        }

        // 1. Envanterdeki kapasite eşyasını bul ve işlem başlamadan ayır.
        ItemStack reservedItem = null;

        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {

            ItemStack item = player.getInventory().getItem(slot);

            if (item == null || !plugin.getItemManager().isCapacityItem(item)) {
                continue;
            }

            reservedItem = item.clone();
            reservedItem.setAmount(1);

            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().setItem(slot, null);
            }

            break;
        }

        // Eşya bulunamadıysa işlemi başlatma.
        if (reservedItem == null) {
            pendingCapacityPlayers.remove(playerUuid);
            player.sendMessage("§cEnvanterinde kapasite eşyası bulunamadı.");
            return;
        }
        // Lambda içerisinde güvenle kullanabilmek için sabit referans oluştur.
        final ItemStack itemToReturn = reservedItem;

        // 2. Veritabanında kapasiteyi artır.
        plugin.getGuildRepository()
                .increaseGuildCapacity(playerUuid)
                .whenComplete((capacity, error) -> {

                    // Bukkit işlemlerini ana oyun thread'inde gerçekleştir.
                    Bukkit.getScheduler().runTask(plugin, () -> {

                        pendingCapacityPlayers.remove(playerUuid);

                        Player onlinePlayer = Bukkit.getPlayer(playerUuid);

                        // 3. Veritabanı işlemi başarısızsa eşyayı iade et.
                        if (error != null || capacity == null) {

                            returnCapacityItem(playerUuid, itemToReturn);

                            if (onlinePlayer != null) {
                                onlinePlayer.sendMessage(
                                        "§cKapasite artırılamadı. Eşyan iade edildi."
                                );
                            }

                            if (error != null) {
                                plugin.getLogger().severe(
                                        "Lonca kapasitesi artırılırken hata oluştu: "
                                                + error.getMessage()
                                );
                            }

                            return;
                        }

                        // 4. İşlem başarılıysa eşya tüketilmiş olur.
                        if (onlinePlayer != null) {
                            onlinePlayer.sendMessage(
                                    "§aLonca kapasiten artırıldı! Yeni kapasite: §e"
                                            + capacity.getCapacity()
                            );
                        }
                    });
                });
    }

    // Kapasite eşyasını oyuncuya iade eder.
    private void returnCapacityItem(UUID playerUuid, ItemStack item) {

        Player onlinePlayer = Bukkit.getPlayer(playerUuid);

        // Oyuncu çevrimdışıysa giriş yaptığında iade edilmek üzere sıraya al.
        if (onlinePlayer == null) {
            plugin.getGuildItemSessionManager().queueItemReturn(
                    playerUuid,
                    item
            );
            return;
        }

        // Oyuncu çevrimiçiyse envanterine ekle.
        var leftovers = onlinePlayer.getInventory().addItem(item.clone());

        // Envanter doluysa kalan eşyaları yere bırak.
        leftovers.values().forEach(leftover ->
                onlinePlayer.getWorld().dropItemNaturally(
                        onlinePlayer.getLocation(),
                        leftover
                )
        );
    }
}