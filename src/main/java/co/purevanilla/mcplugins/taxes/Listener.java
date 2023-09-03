package co.purevanilla.mcplugins.taxes;

import co.purevanilla.mcplugins.gemmy.event.Death;
import co.purevanilla.mcplugins.gemmy.event.Pickup;
import org.bukkit.event.EventHandler;

import java.math.BigDecimal;

public class Listener implements org.bukkit.event.Listener {

    API api;

    public Listener(API api){
        this.api=api;
    }

    @EventHandler()
    public void onMoney(Pickup event){
        this.api.addPoints(event.getPlayer(), BigDecimal.valueOf(event.getAmount()));
    }

    @EventHandler()
    public void onDeath(Death event){
        this.api.addPoints(event.getPlayer(), BigDecimal.valueOf(-event.getAmount()));
    }

}
