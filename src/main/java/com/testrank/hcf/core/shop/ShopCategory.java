package com.testrank.hcf.core.shop;

import org.bukkit.Material;

import java.util.List;

public record ShopCategory(
        String id,
        String displayName,
        Material icon,
        short iconData,
        int slot,
        List<ShopItem> items
) {}
