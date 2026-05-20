package com.testrank.hcf.core.menu;

import com.testrank.hcf.core.api.HCFService;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MenuService implements HCFService {
    private final Map<UUID, MenuState> states = new ConcurrentHashMap<>();

    public void track(Player player, Inventory inventory, Map<Integer, Button> buttons, Menu menu) {
        states.put(player.getUniqueId(), new MenuState(inventory, buttons, menu));
    }

    public void track(Player player, Inventory inventory, Map<Integer, Button> buttons) {
        states.put(player.getUniqueId(), new MenuState(inventory, buttons, null));
    }

    public void refresh(Player player) {
        MenuState state = state(player);
        if (state == null || state.menu() == null) {
            return;
        }
        Map<Integer, Button> buttons = state.menu().buttons(player);
        state.inventory().clear();
        buttons.forEach((slot, button) -> state.inventory().setItem(slot, button.icon(player)));
        states.put(player.getUniqueId(), new MenuState(state.inventory(), buttons, state.menu()));
        player.updateInventory();
    }

    public MenuState state(Player player) {
        return states.get(player.getUniqueId());
    }

    public void clear(Player player) {
        states.remove(player.getUniqueId());
    }

    public record MenuState(Inventory inventory, Map<Integer, Button> buttons, Menu menu) {}
}
