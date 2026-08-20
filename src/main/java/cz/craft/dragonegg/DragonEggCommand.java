package cz.craft.dragonegg;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DragonEggCommand extends Command {

    private final DragonEggPlugin plugin;

    public DragonEggCommand(DragonEggPlugin plugin) {
        super("dragonegg");
        this.plugin = plugin;
        this.setDescription("Hlavni prikaz pro spravu a sledovani Draciho Vajicka");
        this.setPermission("dragonegg.use");
        this.setAliases(List.of("de", "egg"));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("dragonegg.reload")) {
                sender.sendMessage(ColorUtils.parse(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.no-permission")));
                return true;
            }
            plugin.reloadConfig();
            sender.sendMessage(ColorUtils.parse(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.reload-success")));
            return true;
        }

        if (args.length > 0 && (args[0].equalsIgnoreCase("where") || args[0].equalsIgnoreCase("track") || args[0].equalsIgnoreCase("kde"))) {
            if (!sender.hasPermission("dragonegg.where")) {
                sender.sendMessage(ColorUtils.parse(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.no-permission")));
                return true;
            }

            findEggLocation(sender);
            return true;
        }

        sender.sendMessage(ColorUtils.parse(plugin.getConfig().getString("messages.prefix") + "<yellow>Příkazy: <gold>/dragonegg where <gray>- Zjistí polohu vajíčka, <gold>/dragonegg reload <gray>- Znovu načte config."));
        return true;
    }

    private void findEggLocation(CommandSender sender) {
        String prefix = plugin.getConfig().getString("messages.prefix");

        // 1. Kontrola u hráčů
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getInventory().contains(Material.DRAGON_EGG)) {
                Location loc = player.getLocation();
                String msg = plugin.getConfig().getString("messages.tracking-player", "")
                        .replace("{player}", player.getName())
                        .replace("{world}", loc.getWorld().getName())
                        .replace("{x}", String.valueOf(loc.getBlockX()))
                        .replace("{y}", String.valueOf(loc.getBlockY()))
                        .replace("{z}", String.valueOf(loc.getBlockZ()));
                sender.sendMessage(ColorUtils.parse(prefix + msg));
                return;
            }
        }

        // 2. Kontrola na zemi ve všech světech
        for (World world : Bukkit.getWorlds()) {
            for (Item item : world.getEntitiesByClass(Item.class)) {
                if (item.getItemStack().getType() == Material.DRAGON_EGG) {
                    Location loc = item.getLocation();
                    String msg = plugin.getConfig().getString("messages.tracking-item", "")
                            .replace("{world}", world.getName())
                            .replace("{x}", String.valueOf(loc.getBlockX()))
                            .replace("{y}", String.valueOf(loc.getBlockY()))
                            .replace("{z}", String.valueOf(loc.getBlockZ()));
                    sender.sendMessage(ColorUtils.parse(prefix + msg));
                    return;
                }
            }
        }

        // 3. Kontrola v načtených truhlách (správné prohledávání načtených chunků)
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                for (BlockState tile : chunk.getTileEntities()) {
                    if (tile instanceof Container container) {
                        if (container.getInventory().contains(Material.DRAGON_EGG)) {
                            Location loc = container.getLocation();
                            String msg = plugin.getConfig().getString("messages.tracking-chest", "")
                                    .replace("{world}", world.getName())
                                    .replace("{x}", String.valueOf(loc.getBlockX()))
                                    .replace("{y}", String.valueOf(loc.getBlockY()))
                                    .replace("{z}", String.valueOf(loc.getBlockZ()));
                            sender.sendMessage(ColorUtils.parse(prefix + msg));
                            return;
                        }
                    }
                }
            }
        }

        sender.sendMessage(ColorUtils.parse(prefix + plugin.getConfig().getString("messages.tracking-not-found")));
    }
}
