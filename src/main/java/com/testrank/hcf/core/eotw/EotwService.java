package com.testrank.hcf.core.eotw;

import com.testrank.hcf.core.api.HCFService;

public final class EotwService implements HCFService {
    private volatile boolean active;

    public boolean active() {
        return active;
    }

    public void active(boolean active) {
        this.active = active;
    }
}
