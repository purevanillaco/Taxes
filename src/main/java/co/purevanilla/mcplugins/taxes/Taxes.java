package co.purevanilla.mcplugins.taxes;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

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
    }

    public static API getAPI(){
        return Taxes.api;
    }

}
