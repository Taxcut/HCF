package com.testrank.hcf.core.vanish;

import com.testrank.hcf.core.api.HCFService;
import com.testrank.hcf.core.staff.StaffService;
import org.bukkit.entity.Player;

public final class VanishService implements HCFService {
    private final StaffService staff;

    public VanishService(StaffService staff) {
        this.staff = staff;
    }

    public boolean vanished(Player player) {
        return staff.vanished(player);
    }
}
