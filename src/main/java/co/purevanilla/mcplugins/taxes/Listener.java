package co.purevanilla.mcplugins.taxes;

import net.ess3.api.events.UserBalanceUpdateEvent;
import org.bukkit.event.EventHandler;

import java.math.BigDecimal;
import java.util.EventListener;
import java.util.List;

public class Listener implements org.bukkit.event.Listener {

    API api;

    public Listener(API api){
        this.api=api;
    }

    @EventHandler()
    public void onMoney(UserBalanceUpdateEvent event){
        if(event.getCause() == UserBalanceUpdateEvent.Cause.API){
            BigDecimal delta = event.getNewBalance().subtract(event.getOldBalance());
            if(delta.compareTo(BigDecimal.ZERO)>0){
                this.api.addPoints(event.getPlayer(), delta);
            }
        }
    }

}
