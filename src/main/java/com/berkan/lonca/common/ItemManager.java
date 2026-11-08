
package com.berkan.lonca.common;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class ItemManager {

    private final NamespacedKey guildNameChangeKey;
    private final NamespacedKey guildTagChangeKey;
    private final NamespacedKey guildCapacityKey;

    public ItemManager(JavaPlugin plugin) {

        // Özel lonca eşyalarının benzersiz kimlikleri.
        guildNameChangeKey = new NamespacedKey(
                plugin, "guild_name_change"
        );

        guildTagChangeKey = new NamespacedKey(
                plugin, "guild_tag_change"
        );

        guildCapacityKey = new NamespacedKey(
                plugin, "guild_capacity"
        );
    }

    // Lonca tam adı değiştirme nesnesi.
    public ItemStack createNameChangeItem() {

        return createItem(
                Material.NAME_TAG,
                "§6Lonca İsim Değiştirme Nesnesi",
                List.of(
                        "§7Loncanın tam adını değiştirmek için kullanılır.",
                        "§eSağ tıklayarak kullanabilirsiniz."
                ),
                guildNameChangeKey
        );
    }

    // Lonca TAG değiştirme nesnesi.
    public ItemStack createTagChangeItem() {

        return createItem(
                Material.NAME_TAG,
                "§bLonca TAG Değiştirme Nesnesi",
                List.of(
                        "§7Loncanın TAG bilgisini değiştirmek için kullanılır.",
                        "§eSağ tıklayarak kullanabilirsiniz."
                ),
                guildTagChangeKey
        );
    }

    // Lonca kapasitesini kalıcı olarak +1 artıran nesne.
    public ItemStack createCapacityItem() {

        return createItem(
                Material.EMERALD,
                "§aLonca Kapasite Arttırıcı",
                List.of(
                        "§7Loncanın üye kapasitesini kalıcı olarak artırır.",
                        "§a+1 Üye Kapasitesi",
                        "§eYalnızca lonca lideri kullanabilir."
                ),
                guildCapacityKey
        );
    }

    // Ortak özel eşya oluşturma metodu.
    private ItemStack createItem(
            Material material,
            String displayName,
            List<String> lore,
            NamespacedKey key
    ) {

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.setDisplayName(displayName);
        meta.setLore(lore);

        // Eşyaya gizli ve benzersiz kimlik ekle.
        meta.getPersistentDataContainer().set(
                key,
                PersistentDataType.BYTE,
                (byte) 1
        );

        item.setItemMeta(meta);

        return item;
    }

    // İsim değiştirme nesnesi mi?
    public boolean isNameChangeItem(ItemStack item) {
        return hasItemKey(item, guildNameChangeKey);
    }

    // TAG değiştirme nesnesi mi?
    public boolean isTagChangeItem(ItemStack item) {
        return hasItemKey(item, guildTagChangeKey);
    }

    // Kapasite artırıcı mı?
    public boolean isCapacityItem(ItemStack item) {
        return hasItemKey(item, guildCapacityKey);
    }

    // Özel eşya kimliğini kontrol et.
    private boolean hasItemKey(
            ItemStack item,
            NamespacedKey key
    ) {

        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        return item.getItemMeta()
                .getPersistentDataContainer()
                .has(key, PersistentDataType.BYTE);
    }
}
