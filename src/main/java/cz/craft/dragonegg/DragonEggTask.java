package cz.craft.dragonegg;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DragonEggTask extends BukkitRunnable {

    private final DragonEggPlugin plugin;
    private final Set<UUID> glowingPlayers = new HashSet<>();
    private Team glowTeam;

    public DragonEggTask(DragonEggPlugin plugin) {
        this.plugin = plugin;
        setupScoreboardTeam();
    }

    private void setupScoreboardTeam() {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = board.getTeam("DragonEggGlow");
        if (team == null) {
            team = board.registerNewTeam("DragonEggGlow");
        }

        String colorStr = plugin.getConfig().getString("glow-effect.color", "LIGHT_PURPLE");
        try {
            ChatColor color = ChatColor.valueOf(colorStr.toUpperCase());
            team.setColor(color);
        } catch (Exception e) {
            team.setColor(ChatColor.LIGHT_PURPLE);
        }

        this.glowTeam = team;
    }

    @Override
    public void run() {
        // 1. Kontrola Void Teleportace ze všech světů
        if (plugin.getConfig().getBoolean("void-teleport.enabled", true)) {
            double configTriggerY = plugin.getConfig().getDouble("void-teleport.trigger-y", -70.0);
            String targetWorldName = plugin.getConfig().getString("void-teleport.target-world", "spawn");
            double tx = plugin.getConfig().getDouble("void-teleport.target-x", 0.5);
            double ty = plugin.getConfig().getDouble("void-teleport.target-y", 100.0);
            double tz = plugin.getConfig().getDouble("void-teleport.target-z", 0.5);

            World targetWorld = Bukkit.getWorld(targetWorldName);

            for (World world : Bukkit.getWorlds()) {
                double safeVoidY = world.getMinHeight() - 20; 
                double triggerY = Math.max(configTriggerY, safeVoidY);

                // Kontrola Itemů ve voidu
                for (Item item : new HashSet<>(world.getEntitiesByClass(Item.class))) {
                    if (item.isValid() && item.getItemStack().getType() == Material.DRAGON_EGG) {
                        if (item.getLocation().getY() <= triggerY) {
                            if (targetWorld != null) {
                                Location targetLoc = new Location(targetWorld, tx, ty, tz);
                                ItemStack stack = item.getItemStack().clone();
                                item.remove();

                                Item newItem = targetWorld.dropItem(targetLoc, stack);
                                newItem.setVelocity(new Vector(0, 0, 0));

                                if (plugin.getConfig().getBoolean("void-teleport.broadcast", true)) {
                                    String msg = plugin.getConfig().getString("void-teleport.broadcast-message");
                                    Bukkit.broadcast(ColorUtils.parse(msg));
                                }
                            }
                        }
                    }
                }

                // Kontrola Padajících bloků (FallingBlock) ve voidu
                for (FallingBlock fb : new HashSet<>(world.getEntitiesByClass(FallingBlock.class))) {
                    if (fb.isValid() && fb.getBlockData().getMaterial() == Material.DRAGON_EGG) {
                        if (fb.getLocation().getY() <= triggerY) {
                            if (targetWorld != null) {
                                Location targetLoc = new Location(targetWorld, tx, ty, tz);
                                fb.remove();

                                Item newItem = targetWorld.dropItem(targetLoc, new ItemStack(Material.DRAGON_EGG));
                                newItem.setVelocity(new Vector(0, 0, 0));

                                if (plugin.getConfig().getBoolean("void-teleport.broadcast", true)) {
                                    String msg = plugin.getConfig().getString("void-teleport.broadcast-message");
                                    Bukkit.broadcast(ColorUtils.parse(msg));
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Kontrola Glow a Částic
        boolean glowEnabled = plugin.getConfig().getBoolean("glow-effect.enabled", true);
        String particleStr = plugin.getConfig().getString("glow-effect.particles", "END_ROD");
        Particle particle = null;
        if (!particleStr.equalsIgnoreCase("NONE")) {
            try {
                particle = Particle.valueOf(particleStr.toUpperCase());
            } catch (Exception ignored) {}
        }

        Set<UUID> currentHolders = new HashSet<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getInventory().contains(Material.DRAGON_EGG)) {
                currentHolders.add(player.getUniqueId());

                if (glowEnabled && player.hasPermission("dragonegg.effects")) {
                    if (!player.isGlowing()) {
                        player.setGlowing(true);
                        if (glowTeam != null) {
                            glowTeam.addEntry(player.getName());
                        }
                    }
                }

                if (particle != null) {
                    player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1.0, 0), 3, 0.3, 0.5, 0.3, 0.01);
                }
            }
        }

        for (UUID uuid : new HashSet<>(glowingPlayers)) {
            if (!currentHolders.contains(uuid)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    player.setGlowing(false);
                    if (glowTeam != null) {
                        glowTeam.removeEntry(player.getName());
                    }
                }
            }
        }

        glowingPlayers.clear();
        glowingPlayers.addAll(currentHolders);
    }

    public void clearAllGlow() {
        for (UUID uuid : glowingPlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.setGlowing(false);
                if (glowTeam != null) {
                    glowTeam.removeEntry(player.getName());
                }
            }
        }
        glowingPlayers.clear();
    }
}
