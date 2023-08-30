package co.purevanilla.mcplugins.taxes;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import it.unimi.dsi.fastutil.Hash;
import net.ess3.api.MaxMoneyException;
import net.essentialsx.api.v2.services.BalanceTop;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;


public class API {

    Taxes taxes;
    Essentials essentials;
    FileConfiguration data;
    File dataFile;
    boolean decimal;
    long lastExecution;
    float percent;
    float timesPerDay;
    double minBalance;

    API(Taxes taxes){
        this.taxes=taxes;
        this.decimal=taxes.getConfig().getBoolean("decimals");
        this.percent= (float) taxes.getConfig().getDouble("percent");
        this.minBalance=taxes.getConfig().getDouble("min-balance");
        this.timesPerDay=(float) taxes.getConfig().getDouble("timesPerDay");
        this.essentials = (Essentials) this.taxes.getServer().getPluginManager().getPlugin("Essentials");
        this.dataFile = new File(this.taxes.getDataFolder(), "data.yml");
        if (!this.dataFile.exists()) {
            this.dataFile.getParentFile().mkdirs();
            this.taxes.saveResource("data.yml", false);
        }
        this.data = YamlConfiguration.loadConfiguration(this.dataFile);
        this.lastExecution=data.getLong("time.lastExecution");
    }

    public void addPoints(Player player, BigDecimal points){
        final String key = "points."+player.getUniqueId();
        if(this.data.contains(key)){
            this.data.set(key, this.data.getDouble(key) + points.doubleValue());
        } else {
            this.data.set(key, points.doubleValue());
        }
    }

    public HashMap<String, Double> showTop(Player player) throws ExecutionException, InterruptedException {
        HashMap<String, Double> top = new HashMap<>();

        // Iterate through the keys in the "points" configuration section
        for (String key : Objects.requireNonNull(this.data.getConfigurationSection("points")).getKeys(false)) {
            Double value = this.data.getDouble("points." + key);

            // Check if the current value is among the top 10
            if (top.size() < 10 || value > Collections.min(top.values())) {
                // If the top map has less than 10 entries or the value is greater than the smallest value in the top map
                if (top.size() == 10) {
                    // If there are already 10 entries, remove the smallest value entry
                    String smallestKey = null;
                    for (String topKey : top.keySet()) {
                        if (smallestKey == null || top.get(topKey) < top.get(smallestKey)) {
                            smallestKey = topKey;
                        }
                    }
                    top.remove(smallestKey);
                }
                top.put(key, value);
            }
        }

        List<Map.Entry<String,Double>> sortedEntries = new ArrayList<Map.Entry<String,Double>>(top.entrySet());
        sortedEntries.sort(new Comparator<>() {
            @Override
            public int compare(Map.Entry<String, Double> e1, Map.Entry<String, Double> e2) {
                return e2.getValue().compareTo(e1.getValue());
            }
        });


        BalanceTop balanceTop = this.essentials.getBalanceTop();
        balanceTop.calculateBalanceTopMapAsync().get();
        Map<UUID, BalanceTop.Entry> balanceTopCache = balanceTop.getBalanceTopCache();
        MiniMessage formatter = MiniMessage.miniMessage();

        player.sendMessage(formatter.deserialize(Objects.requireNonNull(this.taxes.getConfig().getString("format.economy_header"))));
        int i = 0;
        for (BalanceTop.Entry btopEntry:balanceTopCache.values()) {
            player.sendMessage(formatter.deserialize(
                    Objects.requireNonNull(this.taxes.getConfig().getString("format.entry")),
                    Placeholder.unparsed("player", btopEntry.getDisplayName()),
                    Placeholder.unparsed("value", btopEntry.getBalance().toString())
            ));
            i++;
            if(i>=10) break;
        }

        player.sendMessage(formatter.deserialize(Objects.requireNonNull(this.taxes.getConfig().getString("format.points_header"))));
        for (Map.Entry<String,Double> entry: sortedEntries) {
            player.sendMessage(formatter.deserialize(
                    Objects.requireNonNull(this.taxes.getConfig().getString("format.entry")),
                    Placeholder.unparsed("player", Objects.requireNonNull(this.taxes.getServer().getOfflinePlayer(UUID.fromString(entry.getKey())).getName())),
                    Placeholder.unparsed("value", String.valueOf(entry.getValue()))
            ));
        }

        return top;
    }

    public void setLastExecution(){
        this.setLastExecution(System.currentTimeMillis());
    }

    public void setLastExecution(long epoch){
        this.lastExecution=epoch;
        this.data.set("time.lastExecution",epoch);
        try {
            this.save();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean shouldExecute(){
        long current = System.currentTimeMillis();
        long diff = current-this.getLastExecution();
        return diff > 3600f * 24f * 1000f / this.getTimesPerDay();
    }

    public void takeTax() {
        List<UUID> uuids = new ArrayList<>();
        for (OfflinePlayer player:Bukkit.getServer().getOfflinePlayers()) {
            uuids.add(player.getUniqueId());
        }
        for (Player player:Bukkit.getServer().getOnlinePlayers()) {
            if(!uuids.contains(player.getUniqueId())) uuids.add(player.getUniqueId());
        }

        for (UUID uuid:uuids) {
            User user = this.essentials.getUser(uuid);
            BigDecimal decimal = user.getMoney();
            if(decimal.doubleValue()>this.getMinBalance()){
                double newBalance = decimal.doubleValue()*(1f-this.getPercent()/100);
                if(newBalance<this.getMinBalance()){
                    newBalance=this.getMinBalance();
                }
                if(!this.isDecimal()){
                    newBalance=Math.floor(newBalance);
                }
                try {
                    user.setMoney(BigDecimal.valueOf(newBalance));
                } catch (MaxMoneyException e) {
                    // ignore
                }
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

    public void save() throws IOException {
        this.data.save(this.dataFile);
    }
}
