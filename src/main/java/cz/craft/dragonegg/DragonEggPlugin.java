package cz.craft.dragonegg;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class DragonEggPlugin extends JavaPlugin {

    private DragonEggTask task;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Registrace příkazu
        Bukkit.getCommandMap().register("dragonegg", new DragonEggCommand(this));

        // Registrace listeneru
        getServer().getPluginManager().registerEvents(new DragonEggListener(this), this);

        // Spuštění úlohy pro kontroly (glow, void teleportace, částice)
        this.task = new DragonEggTask(this);
        this.task.runTaskTimer(this, 10L, 10L);

        getLogger().info("DragonEggProtect byl úspěšně načten!");
    }

    @Override
    public void onDisable() {
        if (task != null) {
            task.clearAllGlow();
        }
    }
}
