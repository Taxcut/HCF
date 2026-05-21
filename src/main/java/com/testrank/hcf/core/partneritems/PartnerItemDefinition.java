package com.testrank.hcf.core.partneritems;

import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;

import java.util.List;

public record PartnerItemDefinition(
        String id,
        String displayName,
        Material material,
        List<String> lore,
        long cooldownMillis,
        List<PotionEffect> selfEffects,
        List<PotionEffect> targetEffects
) {}
