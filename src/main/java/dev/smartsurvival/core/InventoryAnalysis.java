package dev.smartsurvival.core;

import java.util.*;

public final class InventoryAnalysis {
    private InventoryAnalysis() {}
    public record Stack(String item, int count, int maxCount, boolean tool, boolean special) {}
    public record Advice(String item, String reason) {}
    public static Map<String, Integer> count(List<Stack> stacks) {
        Map<String, Integer> result = new TreeMap<>();
        for (Stack stack : stacks) if (stack.count > 0) result.merge(stack.item, stack.count, Integer::sum);
        return Map.copyOf(result);
    }
    public static List<Advice> suggest(List<Stack> stacks, Set<String> favorites, Set<String> needed) {
        List<Advice> result = new ArrayList<>();
        Map<String, Integer> tools = new TreeMap<>();
        Map<String, Integer> slots = new TreeMap<>(), counts = new TreeMap<>(), maximums = new TreeMap<>();
        for (Stack s : stacks) {
            if (s.count <= 0 || favorites.contains(s.item) || s.special) continue;
            if (s.tool) tools.merge(s.item, 1, Integer::sum);
            if (s.maxCount > 1) {
                slots.merge(s.item, 1, Integer::sum); counts.merge(s.item, s.count, Integer::sum);
                maximums.put(s.item, s.maxCount);
            }
        }
        tools.forEach((item, n) -> { if (n > 1 && !needed.contains(item)) result.add(new Advice(item, n + " matching tools; consider storing a spare")); });
        slots.forEach((item, n) -> {
            int required = (counts.get(item) + maximums.get(item) - 1) / maximums.get(item);
            if (n > required) result.add(new Advice(item, "Partial stacks could free " + (n - required) + " slot(s)"));
        });
        // Context matters: only flag common overflow, never label all unpinned items as junk.
        Set<String> bulk = Set.of("minecraft:dirt", "minecraft:cobblestone", "minecraft:cobbled_deepslate", "minecraft:netherrack", "minecraft:gravel");
        counts.forEach((item, n) -> {
            if (bulk.contains(item) && n > 128 && !needed.contains(item))
                result.add(new Advice(item, "Over two stacks; consider storing surplus if you do not need it"));
        });
        return List.copyOf(result);
    }
}
