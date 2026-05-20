package com.testrank.hcf.core.freeze;

import com.testrank.hcf.core.api.HCFService;
import com.testrank.hcf.core.staff.StaffService;
import org.bukkit.entity.Player;

public final class FreezeService implements HCFService {
    private final StaffService staff;

    public FreezeService(StaffService staff) {
        this.staff = staff;
    }

    public void freeze(Player player) {
        staff.freeze(player, true);
    }

    public void thaw(Player player) {
        staff.freeze(player, false);
    }
}
