package co.purevanilla.mcplugins.taxes;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;


public class Taxes extends JavaPlugin {

    static API api;

    @Override
    public void onEnable() {
        super.onEnable();
        saveDefaultConfig();
        Taxes.api=new API(this);
        this.getLogger().log(Level.INFO,"starting tax-check task");
        Bukkit.getScheduler().runTaskTimerAsynchronously(this,Taxes.api.getRunnable(),0,20);

        // events
        getServer().getPluginManager().registerEvents(new Listener(Taxes.api), this);

        // cmd
        Objects.requireNonNull(this.getCommand("balancetop")).setExecutor(new CMD(Taxes.api));
    }

    @Override
    public void onDisable() {
        super.onDisable();
        try {
            api.save();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static API getAPI(){
        return Taxes.api;
    }

}
