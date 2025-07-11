package com.shane.raspberryjuicereload.util;

import org.apache.commons.lang3.EnumUtils;
import org.bukkit.Material;

import java.util.HashSet;
import java.util.Set;

public class MaterialUtil {
    public static Set<Material> stringToSet(String materials) {
        Set<Material> set = new HashSet<Material>();
        String[] strings = materials.split(",");
        for (String material : strings) {
            if (!EnumUtils.isValidEnum(Material.class, material)) continue;
            set.add(Material.getMaterial(material));
        }
        return set;
    }
}
