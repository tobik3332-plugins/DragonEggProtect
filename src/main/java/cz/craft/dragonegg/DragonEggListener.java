package cz.craft.dragonegg;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class DragonEggListener implements Listener {

    private final DragonEggPlugin plugin;

    public DragonEggListener(DragonEggPlugin plugin) {
        this.plugin = plugin;
    }

    // ========== SLEDOVÁNÍ A TELEPORTACE BLOKU ==========

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getBlockPlaced().getType() == Material.DRAGON_EGG) {
            plugin.addPlacedEgg(event.getBlockPlaced().getLocation());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.DRAGON_EGG) {
            plugin.removePlacedEgg(event.getBlock().getLocation());
        }
    }

    // Zachytí odskočení (teleport) vajíčka po kliknutí hráče
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event) {
        if (event.getBlock().getType() == Material.DRAGON_EGG) {
            plugin.removePlacedEgg(event.getBlock().getLocation());
            plugin.addPlacedEgg(event.getToBlock().getLocation());
        }
    }

    // Zachytí změnu na FallingBlock (když vajíčko padá) nebo dopad bloku
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockChange(EntityChangeBlockEvent event) {
        if (event.getBlock().getType() == Material.DRAGON_EGG && event.getTo() == Material.AIR) {
            plugin.removePlacedEgg(event.getBlock().getLocation());
        }
        if (event.getTo() == Material.DRAGON_EGG) {
            plugin.addPlacedEgg(event.getToBlock().getLocation());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.DRAGON_EGG) {
                if (plugin.getConfig().getBoolean("block-protection.prevent-teleport", false)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    // ========== OCHRANA ITEMŮ A TRUHEL ==========

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Item item) {
            if (item.getItemStack().getType() == Material.DRAGON_EGG) {
                if (plugin.getConfig().getBoolean("item-protection.prevent-fire", true) ||
                    plugin.getConfig().getBoolean("item-protection.prevent-cactus", true) ||
                    plugin.getConfig().getBoolean("item-protection.prevent-explosion", true)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemCombust(EntityCombustEvent event) {
        if (event.getEntity() instanceof Item item) {
            if (item.getItemStack().getType() == Material.DRAGON_EGG) {
                if (plugin.getConfig().getBoolean("item-protection.prevent-fire", true)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemDespawn(ItemDespawnEvent event) {
        if (event.getEntity().getItemStack().getType() == Material.DRAGON_EGG) {
            if (plugin.getConfig().getBoolean("item-protection.prevent-despawn", true)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!plugin.getConfig().getBoolean("container-protection.enabled", true)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (player.hasPermission("dragonegg.bypass")) return;

        ItemStack currentItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();

        boolean isEggInvolved = (currentItem != null && currentItem.getType() == Material.DRAGON_EGG)
                             || (cursorItem != null && cursorItem.getType() == Material.DRAGON_EGG);

        if (!isEggInvolved) return;

        InventoryType topType = event.getView().getTopInventory().getType();

        if (topType != InventoryType.CRAFTING && topType != InventoryType.PLAYER) {
            boolean isAllowed = (topType == InventoryType.CHEST || topType == InventoryType.BARREL);
            boolean clickedTop = event.getRawSlot() < event.getView().getTopInventory().getSize();

            if (clickedTop && !isAllowed) {
                event.setCancelled(true);
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                String msg = plugin.getConfig().getString("container-protection.deny-message");
                player.sendMessage(ColorUtils.parse(plugin.getConfig().getString("messages.prefix") + msg));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!plugin.getConfig().getBoolean("container-protection.enabled", true)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (player.hasPermission("dragonegg.bypass")) return;

        if (event.getOldCursor().getType() == Material.DRAGON_EGG) {
            InventoryType topType = event.getView().getTopInventory().getType();
            if (topType != InventoryType.CRAFTING && topType != InventoryType.PLAYER && topType != InventoryType.CHEST && topType != InventoryType.BARREL) {
                event.setCancelled(true);
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        }
    }
}
