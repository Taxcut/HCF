package com.testrank.hcf.core.shop;

import org.bukkit.Material;

public record ShopItem(
        String id,
        Material material,
        short data,
        int amount,
        long buyPrice,
        long sellPrice,
        int slot,
        String displayName
) {}
