package cz.craft.dragonegg;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class DragonEggPlugin extends JavaPlugin {

    private DragonEggTask task;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        Bukkit.getCommandMap().register("dragonegg", new DragonEggCommand(this));
        getServer().getPluginManager().registerEvents(new DragonEggListener(this), this);

        this.task = new DragonEggTask(this);
        this.task.runTaskTimer(this, 10L, 10L);

        getLogger().info("DragonEggProtect byl uspesne nacten!");
    }

    @Override
    public void onDisable() {
        if (task != null) {
            task.clearAllGlow();
        }
    }

    public void addPlacedEgg(Location loc) {
        if (loc == null || loc.getWorld() == null) return;
        List<String> list = getConfig().getStringList("data.placed-eggs");
        String locStr = loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
        if (!list.contains(locStr)) {
            list.add(locStr);
            getConfig().set("data.placed-eggs", list);
            saveConfig();
        }
    }

    public void removePlacedEgg(Location loc) {
        if (loc == null || loc.getWorld() == null) return;
        List<String> list = getConfig().getStringList("data.placed-eggs");
        String locStr = loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
        if (list.contains(locStr)) {
            list.remove(locStr);
            getConfig().set("data.placed-eggs", list);
            saveConfig();
        }
    }

    public List<Location> getPlacedEggs() {
        List<Location> locs = new ArrayList<>();
        List<String> list = getConfig().getStringList("data.placed-eggs");
        boolean changed = false;

        for (String s : new ArrayList<>(list)) {
            String[] parts = s.split(",");
            if (parts.length == 4) {
                World w = Bukkit.getWorld(parts[0]);
                if (w != null) {
                    int x = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);
                    int z = Integer.parseInt(parts[3]);
                    Location loc = new Location(w, x, y, z);

                    int chunkX = x >> 4;
                    int chunkZ = z >> 4;

                    // Pokud je chunk načtený, ověříme blok. Pokud načtený není, lokaci nemažeme!
                    if (w.isChunkLoaded(chunkX, chunkZ)) {
                        if (loc.getBlock().getType() == Material.DRAGON_EGG) {
                            locs.add(loc);
                        } else {
                            list.remove(s);
                            changed = true;
                        }
                    } else {
                        locs.add(loc);
                    }
                }
            }
        }

        if (changed) {
            getConfig().set("data.placed-eggs", list);
            saveConfig();
        }
        return locs;
    }
}
