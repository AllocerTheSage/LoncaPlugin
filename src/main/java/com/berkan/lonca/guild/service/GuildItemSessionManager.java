
package com.berkan.lonca.guild.service;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GuildItemSessionManager {

    // Oyuncuların aktif isim ve TAG değiştirme işlemlerini tutar.
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    // Oyuncu çevrimdışıyken iade edilmesi gereken eşyaları tutar.
    private final Map<UUID, List<ItemStack>> pendingReturns =
            new ConcurrentHashMap<>();

    // ==================================================
    // OTURUM YÖNETİMİ
    // ==================================================

    // Oyuncu için yeni bir işlem başlatır.
    public void startSession(
            UUID playerUuid,
            SessionType type,
            ItemStack reservedItem,
            int originalSlot
    ) {
        sessions.put(
                playerUuid,
                new Session(type, reservedItem, originalSlot)
        );
    }

    // Oyuncunun aktif işlem oturumunu getirir.
    public Session getSession(UUID playerUuid) {
        return sessions.get(playerUuid);
    }

    // Oyuncunun aktif oturumunu kaldırır.
    public void removeSession(UUID playerUuid) {
        sessions.remove(playerUuid);
    }

    // Oturumu hem getirir hem de aktif oturum listesinden çıkarır.
    public Session takeSession(UUID playerUuid) {
        return sessions.remove(playerUuid);
    }

    // ==================================================
    // BEKLEYEN EŞYA İADE SİSTEMİ
    // ==================================================

    // Oyuncu çevrimdışıyken iade edilmesi gereken eşyayı kaydeder.
    public void queueItemReturn(UUID playerUuid, ItemStack item) {

        // Her oyuncunun birden fazla farklı özel eşyası olabilir.
        List<ItemStack> items = pendingReturns.computeIfAbsent(
                playerUuid,
                uuid -> Collections.synchronizedList(new ArrayList<>())
        );

        // Orijinal eşyanın verisini korumak için kopyasını sakla.
        items.add(item.clone());
    }

    // Oyuncunun bekleyen bütün iadelerini getirir ve listeden çıkarır.
    public List<ItemStack> takePendingReturns(UUID playerUuid) {

        return pendingReturns.remove(playerUuid);
    }

    // ==================================================
    // OTURUM TİPLERİ
    // ==================================================

    public enum SessionType {
        CHANGE_NAME,
        CHANGE_TAG
    }

    // ==================================================
    // OTURUM MODELİ
    // ==================================================

    public static class Session {

        // Yapılan işlemin türü.
        private final SessionType type;

        // Oyuncudan geçici olarak alınan özel eşya.
        private final ItemStack reservedItem;

        // Eşyanın başlangıçta bulunduğu envanter slotu.
        private final int originalSlot;

        // Oyuncunun ilk girdiği değer.
        private String firstValue;

        // Oyuncunun onayladığı ikinci değer.
        private String confirmedValue;

        public Session(
                SessionType type,
                ItemStack reservedItem,
                int originalSlot
        ) {
            this.type = type;

            // Eşyanın bağımsız bir kopyasını sakla.
            this.reservedItem = reservedItem.clone();

            this.originalSlot = originalSlot;
        }

        public SessionType getType() {
            return type;
        }

        // Rezerve edilen eşyanın kopyasını döndürür.
        public ItemStack getReservedItem() {
            return reservedItem.clone();
        }

        public int getOriginalSlot() {
            return originalSlot;
        }

        public String getFirstValue() {
            return firstValue;
        }

        public void setFirstValue(String firstValue) {
            this.firstValue = firstValue;
        }

        public String getConfirmedValue() {
            return confirmedValue;
        }

        public void setConfirmedValue(String confirmedValue) {
            this.confirmedValue = confirmedValue;
        }
    }
}
