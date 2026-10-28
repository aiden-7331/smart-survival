package dev.smartsurvival.core;

public record Goal(String item, int amount) {
    public static final int MAX_AMOUNT = 1_000_000;
    public Goal {
        if (item == null || !item.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) throw new IllegalArgumentException("Invalid item identifier");
        if (amount < 1 || amount > MAX_AMOUNT) throw new IllegalArgumentException("Quantity must be 1 to " + MAX_AMOUNT);
    }
    public int missing(int have) { return Math.max(0, amount - Math.max(0, have)); }
}
