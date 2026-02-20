package dev.cosmicmod.client;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.network.chat.Component;

public class SearchManager {
    private static String searchQuery = "";

    public static String getSearchQuery() {
        return searchQuery;
    }

    public static void setSearchQuery(String query) {
        searchQuery = query;
    }

    public static boolean matches(ItemStack stack) {
        if (searchQuery == null || searchQuery.isEmpty()) {
            return false;
        }

        String query = searchQuery.toLowerCase();

        // Check name
        if (stack.getHoverName().getString().toLowerCase().contains(query)) {
            return true;
        }

        // Check lore
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore != null) {
            for (Component line : lore.lines()) {
                if (line.getString().toLowerCase().contains(query)) {
                    return true;
                }
            }
        }

        // Check custom data (metadata)
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            if (customData.toString().toLowerCase().contains(query)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isOnCosmic() {
        if (ChatStateManager.getCurrentServer().equals("localhost")) return true;
        return ChatStateManager.isCosmicSky() || ChatStateManager.isCosmicPrisons();
    }

    public static String getCosmicEnergyAmount(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;

        // Only draw Energy Values on light_blue_dye
        if (!stack.is(net.minecraft.world.item.Items.LIGHT_BLUE_DYE)) return null;

        // Try CustomData first (Cosmic Prisons specific energy item from issue)
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            var tag = customData.copyTag();
            if (tag.contains("PublicBukkitValues")) {
                var bukkit = tag.getCompound("PublicBukkitValues");
                if ("cosmic_energy".equals(bukkit.getString("cosmicprisons:custom_item_id"))) {
                    if (bukkit.contains("cosmicprisons:amount")) {
                        return formatNumber(bukkit.getDouble("cosmicprisons:amount"));
                    }
                }
            }
        }

        // In CosmicSky/Prisons, energy is usually in lore
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore != null) {
            for (Component line : lore.lines()) {
                String text = line.getString();
                if (text.contains("Cosmic Energy")) {
                    // Typical format: "Cosmic Energy: 1,234"
                    if (text.contains(":")) {
                        String[] parts = text.split(":");
                        if (parts.length > 1) {
                            return parts[1].trim();
                        }
                    }
                    // Handle "Contains 83,150 Cosmic Energy"
                    if (text.contains("Contains")) {
                        String energy = text.replace("Contains", "").replace("Cosmic Energy", "").trim();
                        if (!energy.isEmpty()) return energy;
                    }
                    
                    // Fallback: just return the line if it seems to contain a number
                    String numeric = text.replaceAll("[^0-9,]", "").trim();
                    if (!numeric.isEmpty()) return numeric;
                }
            }
        }
        return null;
    }

    public static String getTrinketCharges(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;

        String name = stack.getHoverName().getString();
        // Trinket name format example: "Strength Trinket I (7)" or "Strength Trinket I (1,234)"
        if (name.contains("Trinket") && name.contains("(") && name.endsWith(")")) {
            int lastOpen = name.lastIndexOf('(');
            String charges = name.substring(lastOpen + 1, name.length() - 1);
            if (charges.replace(",", "").matches("\\d+")) {
                return charges;
            }
        }
        return null;
    }

    public static boolean isTrinket(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        String name = stack.getHoverName().getString();
        return name.contains("Trinket");
    }

    /**
     * Maps trinket names to potion effect IDs (as strings or identifier parts).
     * Cosmic trinkets usually correspond to a potion effect that is applied when used.
     * While that effect is active, the trinket is on "cooldown".
     */
    public static String getTrinketEffectId(ItemStack stack) {
        if (!isTrinket(stack)) return null;
        String name = stack.getHoverName().getString().toLowerCase();
        
        if (name.contains("strength")) return "strength";
        if (name.contains("speed")) return "speed";
        if (name.contains("fire resistance")) return "fire_resistance";
        if (name.contains("resistance")) return "resistance"; // Check fire resistance first
        if (name.contains("regeneration")) return "regeneration";
        if (name.contains("haste")) return "haste";
        if (name.contains("night vision")) return "night_vision";
        if (name.contains("jump boost")) return "jump_boost";
        if (name.contains("invisibility")) return "invisibility";
        if (name.contains("water breathing")) return "water_breathing";
        if (name.contains("luck")) return "luck";
        
        return null;
    }

    public static String getCooldownNameForEffect(String effectId) {
        if (effectId == null) return null;
        String lower = effectId.toLowerCase();
        if (lower.contains("strength")) return "Strength";
        if (lower.contains("speed")) return "Speed";
        if (lower.contains("fire_resistance")) return "Fire Resistance";
        if (lower.contains("resistance")) return "Resistance";
        if (lower.contains("regeneration")) return "Regeneration";
        if (lower.contains("haste")) return "Haste";
        if (lower.contains("night_vision")) return "Night Vision";
        if (lower.contains("jump_boost")) return "Jump Boost";
        if (lower.contains("invisibility")) return "Invisibility";
        if (lower.contains("water_breathing")) return "Water Breathing";
        if (lower.contains("luck")) return "Luck";
        return null;
    }

    public static String[] getAllTrinketCooldownNames() {
        return new String[]{"Strength", "Speed", "Regeneration", "Fire Resistance", "Resistance", "Haste", "Night Vision", "Jump Boost", "Invisibility", "Water Breathing", "Luck"};
    }

    public static int getTrinketMaxDurationMillis(ItemStack stack) {
        if (!isTrinket(stack)) return 0;
        String name = stack.getHoverName().getString();
        
        // Cosmic trinkets usually last 45 seconds, 1 minute, etc.
        // We can check the lore for the duration if it's there.
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore != null) {
            for (Component line : lore.lines()) {
                String text = line.getString().toLowerCase();
                if (text.contains("duration")) {
                    String num = text.replaceAll("[^0-9]", "");
                    if (!num.isEmpty()) {
                        int val = Integer.parseInt(num);
                        if (text.contains("minute")) return val * 60 * 1000;
                        if (text.contains("second") || text.contains("s")) return val * 1000;
                    }
                }
            }
        }
        
        // Fallback: common durations
        String lowerName = name.toLowerCase();
        if (lowerName.contains(" i ")) return 45 * 1000; // Tier I often 45s
        if (lowerName.contains(" ii ")) return 60 * 1000; // Tier II often 60s
        if (lowerName.contains(" iii ")) return 90 * 1000; // Tier III often 90s
        
        return 60 * 1000; // Default to 1 minute
    }

    public static String getMoneyNoteValue(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            var tag = customData.copyTag();
            
            // Try standard noteValue
            if (tag.contains("noteValue")) {
                try {
                    double value = tag.getDouble("noteValue");
                    return formatNumber(value);
                } catch (Exception ignored) {}
            }
            
            // Try Cosmic Prisons PublicBukkitValues
            if (tag.contains("PublicBukkitValues")) {
                var bukkit = tag.getCompound("PublicBukkitValues");
                if ("money_note".equals(bukkit.getString("cosmicprisons:custom_item_id"))) {
                    if (bukkit.contains("cosmicprisons:amount")) {
                        return "$" + formatNumber(bukkit.getDouble("cosmicprisons:amount"));
                    }
                }
            }
        }

        // Fallback to lore
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore != null) {
            for (Component line : lore.lines()) {
                String text = line.getString();
                if (text.contains("Value $")) {
                    // text might be "Value $100,000"
                    try {
                        String valStr = text.substring(text.indexOf('$') + 1).replace(",", "").trim();
                        double value = Double.parseDouble(valStr);
                        return formatNumber(value);
                    } catch (Exception ignored) {}
                }
            }
        }

        return null;
    }

    public static String getExpBottleValue(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            var tag = customData.copyTag();
            if (tag.contains("expValue")) {
                try {
                    double value = tag.getDouble("expValue");
                    return formatNumber(value);
                } catch (Exception ignored) {}
            }
        }

        // Fallback to lore
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore != null) {
            for (Component line : lore.lines()) {
                String text = line.getString();
                if (text.contains("Value ") && text.contains(" XP")) {
                    // text might be "Value 30,000 XP"
                    try {
                        String valStr = text.substring(text.indexOf("Value ") + 6, text.indexOf(" XP")).replace(",", "").trim();
                        double value = Double.parseDouble(valStr);
                        return formatNumber(value);
                    } catch (Exception ignored) {}
                }
            }
        }

        return null;
    }


    public static boolean isPet(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            var tag = customData.copyTag();
            return "inventory_pet".equals(tag.getString("persistentItem"));
        }
        return false;
    }

    public static long getPetLastUsed(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            var tag = customData.copyTag();
            if (tag.contains("lastUsed")) {
                return tag.getLong("lastUsed");
            }
        }
        return 0;
    }

    public static int getPetCooldownMillis(ItemStack stack) {
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore != null) {
            for (Component line : lore.lines()) {
                String text = line.getString();
                if (text.contains("Cooldown")) {
                    // Example: "Cooldown 20 minutes" or " 20 minutes" (sometimes Lore lines are separate)
                    // Looking at the issue description, it's often:
                    // '{"text":"","extra":[{"text":"Cooldown","italic":false,"color":"white","bold":true}]}'
                    // '{"text":"","extra":[{"text":" 20 minutes","italic":false,"color":"gray"}]}'
                    // So we might need to check the next line or the current line.
                    // Let's check if the current line has the number.
                    return parseCooldown(text);
                }
            }
            // If "Cooldown" was on one line and the value on another, we might miss it.
            // Let's try searching all lines for "minutes" or "seconds" if Cooldown was found.
            boolean foundCooldownHeader = false;
            for (Component line : lore.lines()) {
                String text = line.getString();
                if (text.contains("Cooldown")) {
                    foundCooldownHeader = true;
                    int val = parseCooldown(text);
                    if (val > 0) return val;
                    continue;
                }
                if (foundCooldownHeader) {
                    int val = parseCooldown(text);
                    if (val > 0) return val;
                }
            }
        }
        return 0;
    }

    private static int parseCooldown(String text) {
        try {
            String lower = text.toLowerCase();
            // Handle "every 60s" which is Passive Ability cooldown
            // Handle "Cooldown 20 minutes" which is Active Ability cooldown
            
            String num = lower.replaceAll("[^0-9]", "");
            if (num.isEmpty()) return 0;
            int val = Integer.parseInt(num);

            if (lower.contains("minute")) {
                return val * 60 * 1000;
            } else if (lower.contains("second") || lower.contains("s")) {
                // Check if 's' is actually part of a unit like '60s'
                // This will also match "seconds"
                return val * 1000;
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private static String formatNumber(double value) {
        if (value < 1000) return String.valueOf((int)value);
        if (value < 1000000) return String.format("%.1fk", value / 1000.0).replace(".0k", "k");
        if (value < 1000000000) return String.format("%.1fm", value / 1000000.0).replace(".0m", "m");
        return String.format("%.1fb", value / 1000000000.0).replace(".0b", "b");
    }
}
