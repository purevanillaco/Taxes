package co.purevanilla.mcplugins.taxes;

import net.tnemc.core.TNE;
import net.tnemc.core.common.account.TNEAccount;
import net.tnemc.core.common.api.TNEAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;


public class API {

    Taxes taxes;

    boolean decimal;
    long lastExecution;
    float percent;
    float timesPerDay;
    double minBalance;

    API(Taxes taxes){
        this.taxes=taxes;
        this.decimal=taxes.getConfig().getBoolean("decimals");
        this.lastExecution=taxes.getConfig().getLong("time.lastExecution");
        this.percent= (float) taxes.getConfig().getDouble("percent");
        this.minBalance=taxes.getConfig().getDouble("min-balance");
        this.timesPerDay=(float) taxes.getConfig().getDouble("timesPerDay");
    }

    public void setLastExecution(){
        this.setLastExecution(System.currentTimeMillis());
    }

    public void setLastExecution(long epoch){
        this.lastExecution=epoch;
        this.taxes.getConfig().set("time.lastExecution",epoch);
        this.taxes.saveConfig();
    }

    public boolean shouldExecute(){
        long current = System.currentTimeMillis();
        long diff = current-this.getLastExecution();
        return diff > 3600f * 24f * 1000f / this.getTimesPerDay();
    }

    public void takeTax(){
        TNEAPI tne = TNE.instance().api();
        List<UUID> uuids = new ArrayList<>();
        for (OfflinePlayer player:Bukkit.getServer().getOfflinePlayers()) {
            uuids.add(player.getUniqueId());
        }
        for (Player player:Bukkit.getServer().getOnlinePlayers()) {
            if(!uuids.contains(player.getUniqueId())) uuids.add(player.getUniqueId());
        }

        for (UUID uuid:uuids) {
            TNEAccount account = tne.getAccount(uuid);
            BigDecimal decimal = account.getHoldings();
            if(decimal.doubleValue()>this.getMinBalance()){
                double newBalance = decimal.doubleValue()*(1f-this.getPercent()/100);
                if(newBalance<this.getMinBalance()){
                    newBalance=this.getMinBalance();
                }
                if(!this.isDecimal()){
                    newBalance=Math.floor(newBalance);
                }
                account.setHoldings(BigDecimal.valueOf(newBalance));
            }
        }
    }

    Runnable getRunnable(){
        return new Runnable() {
            @Override
            public void run() {
                if(shouldExecute()){
                    taxes.getLogger().log(Level.INFO,"Saving last execution to the current time");
                    setLastExecution();
                    taxes.getLogger().log(Level.INFO,"Processing taxes");
                    takeTax();
                    taxes.getLogger().log(Level.INFO,"Finished processing taxes");
                }
            }
        };
    }

    public boolean isDecimal() {
        return decimal;
    }

    public double getMinBalance() {
        return minBalance;
    }

    public float getPercent() {
        return percent;
    }

    public float getTimesPerDay() {
        return timesPerDay;
    }

    public long getLastExecution() {
        return lastExecution;
    }
}
