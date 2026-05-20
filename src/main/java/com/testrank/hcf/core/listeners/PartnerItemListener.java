package com.testrank.hcf.core.listeners;

import com.testrank.hcf.core.partneritems.PartnerItemService;
import com.testrank.hcf.core.util.Text;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

public final class PartnerItemListener implements Listener {
    private final PartnerItemService partnerItems;

    public PartnerItemListener(PartnerItemService partnerItems) {
        this.partnerItems = partnerItems;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (partnerItems.id(event.getItem()).isEmpty()) {
            return;
        }
        event.setCancelled(true);
        if (!partnerItems.tryUse(event.getPlayer(), event.getItem())) {
            event.getPlayer().sendMessage(Text.color("&cThat partner item is on cooldown."));
        }
    }
}
