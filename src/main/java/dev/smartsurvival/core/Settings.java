package dev.smartsurvival.core;

import java.util.*;

public final class Settings {
    public int schema = 1;
    public boolean hud = true;
    public boolean protectFavorites = true;
    public int nearFullSlots = 3;
    public List<Goal> goals = new ArrayList<>();
    public List<Goal> plans = new ArrayList<>();
    public Map<String, String> recipeChoices = new TreeMap<>();
    public Set<String> favorites = new TreeSet<>();
    public Map<String, List<Goal>> presets = defaults();
    public String activePreset = "Mining";
    public Map<String, Widget> widgets = defaultWidgets();
    public static final List<String> WIDGET_NAMES = List.of("Resources", "Preparation", "Crafting", "Inventory", "Survival");

    public static final class Widget {
        public boolean enabled = true;
        public double x, y;
        public double scale = 1.0, opacity = 0.78;
        public Widget(double x, double y) { this.x = x; this.y = y; }
        public void normalize() {
            x = bounded(x, 0, 1, 0.02); y = bounded(y, 0, 1, 0.02);
            scale = bounded(scale, 0.65, 1.75, 1);
            opacity = bounded(opacity, 0.15, 1, 0.78);
        }
    }
    static double bounded(double n, double min, double max, double fallback) {
        return Double.isFinite(n) ? Math.max(min, Math.min(max, n)) : fallback;
    }
    public void normalize() {
        if (schema != 1) throw new IllegalArgumentException("Unsupported settings version " + schema);
        nearFullSlots = Math.max(0, Math.min(12, nearFullSlots));
        goals = clean(goals); plans = clean(plans);
        if (recipeChoices == null) recipeChoices = new TreeMap<>();
        recipeChoices.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null);
        if (favorites == null) favorites = new TreeSet<>();
        favorites.removeIf(s -> s == null || !s.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"));
        favorites = new TreeSet<>(favorites.stream().limit(1024).toList());
        if (presets == null) presets = defaults();
        Map<String, List<Goal>> safe = new LinkedHashMap<>();
        presets.forEach((name, entries) -> {
            if (name != null && !name.isBlank() && safe.size() < 32)
                safe.put(name.substring(0, Math.min(32, name.length())), clean(entries));
        });
        presets = safe.isEmpty() ? defaults() : safe;
        if (!presets.containsKey(activePreset)) activePreset = presets.keySet().iterator().next();
        if (widgets == null) widgets = defaultWidgets();
        var fallback = defaultWidgets();
        for (String name : WIDGET_NAMES) {
            if (widgets.get(name) == null) widgets.put(name, fallback.get(name));
            widgets.get(name).normalize();
        }
        widgets.keySet().retainAll(WIDGET_NAMES);
    }
    private static List<Goal> clean(List<Goal> values) {
        List<Goal> out = new ArrayList<>();
        if (values != null) for (Goal g : values) {
            if (g == null || out.size() >= 128) continue;
            try { out.add(new Goal(g.item(), g.amount())); } catch (IllegalArgumentException ignored) {}
        }
        return out;
    }
    public static Map<String, Widget> defaultWidgets() {
        Map<String, Widget> map = new LinkedHashMap<>();
        map.put("Resources", new Widget(0.02, 0.05));
        map.put("Preparation", new Widget(0.98, 0.05));
        map.put("Crafting", new Widget(0.02, 0.42));
        map.put("Inventory", new Widget(0.98, 0.50));
        map.put("Survival", new Widget(0.02, 0.94));
        map.get("Preparation").enabled = false;
        map.get("Crafting").enabled = false;
        map.get("Survival").enabled = false;
        return map;
    }
    private static Goal g(String name, int n) { return new Goal("minecraft:" + name, n); }
    public static Map<String, List<Goal>> defaults() {
        Map<String, List<Goal>> map = new LinkedHashMap<>();
        map.put("Mining", new ArrayList<>(List.of(g("iron_pickaxe",1), g("torch",32), g("bread",16), g("water_bucket",1))));
        map.put("Exploring", new ArrayList<>(List.of(g("compass",1), g("white_bed",1), g("bread",24), g("torch",32))));
        map.put("Building", new ArrayList<>(List.of(g("scaffolding",32), g("iron_axe",1), g("iron_pickaxe",1), g("bread",16))));
        map.put("Fighting", new ArrayList<>(List.of(g("iron_sword",1), g("shield",1), g("bow",1), g("arrow",32), g("cooked_beef",16))));
        return map;
    }
}
