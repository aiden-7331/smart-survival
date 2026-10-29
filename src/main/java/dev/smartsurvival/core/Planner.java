package dev.smartsurvival.core;

import java.util.*;

/**
 * Deterministic, bounded planner. Tries recipes and ingredient alternatives against
 * independent stock snapshots, then commits only the selected branch. Not a global
 * minimum-cost solver: the chosen route is a practical local estimate.
 */
public final class Planner {
    public record Ingredient(List<String> alternatives, int count) {
        public Ingredient {
            alternatives = alternatives.stream().distinct().sorted().toList();
            if (alternatives.isEmpty() || count < 1 || count > 64) throw new IllegalArgumentException("Invalid ingredient");
        }
    }
    public record Recipe(String id, String output, int count, List<Ingredient> ingredients, String station) {
        public Recipe {
            ingredients = List.copyOf(ingredients);
            if (count < 1 || count > 64 || ingredients.isEmpty() || ingredients.size() > 9) throw new IllegalArgumentException("Invalid recipe");
        }
    }
    public record Step(String recipe, String item, long crafts, String station) {}
    public record Result(Map<String, Long> missing, List<Step> steps, Set<String> notes, boolean limited) {}
    private final Map<String, List<Recipe>> index = new TreeMap<>();
    public Planner(Collection<Recipe> recipes) {
        for (Recipe r : recipes) index.computeIfAbsent(r.output, key -> new ArrayList<>()).add(r);
        index.values().forEach(list -> list.sort(Comparator.comparing(Recipe::id)));
    }
    public List<Recipe> recipes(String item) { return List.copyOf(index.getOrDefault(item, List.of())); }
    private static final class Budget {
        int visits = 12_000;
        void spend() { if (--visits < 0) throw new Limit(); }
    }
    private static final class Limit extends RuntimeException {}
    private static final class State {
        Map<String, Long> stock = new TreeMap<>(), missing = new TreeMap<>();
        List<Step> steps = new ArrayList<>();
        Set<String> notes = new LinkedHashSet<>();
        State copy() {
            State s = new State(); s.stock.putAll(stock); s.missing.putAll(missing); s.steps.addAll(steps); s.notes.addAll(notes); return s;
        }
        void take(State s) { stock = s.stock; missing = s.missing; steps = s.steps; notes = s.notes; }
        long cost() { return missing.values().stream().mapToLong(Long::longValue).sum(); }
        long consume(String item, long quantity) {
            long use = Math.min(quantity, stock.getOrDefault(item, 0L));
            stock.put(item, stock.getOrDefault(item, 0L) - use);
            return quantity - use;
        }
    }
    public Result plan(List<Goal> goals, Map<String, Integer> inventory, Map<String, String> preferredRecipes) {
        State state = new State();
        inventory.forEach((k,v) -> { if (v > 0) state.stock.put(k, v.longValue()); });
        Budget budget = new Budget();
        boolean limited = false;
        // Reserve every finished goal before expanding ingredients; avoids consuming another goal's finished goods.
        List<Goal> remaining = new ArrayList<>();
        for (Goal goal : goals) {
            long need = state.consume(goal.item(), goal.amount());
            if (need > 0) remaining.add(new Goal(goal.item(), (int) need));
        }
        for (Goal goal : remaining) {
            State trial = state.copy();
            try { need(goal.item(), goal.amount(), trial, new LinkedHashSet<>(), budget, preferredRecipes, 0); state.take(trial); }
            catch (Limit ex) {
                limited = true;
                state.missing.merge(goal.item(), (long) goal.amount(), Long::sum);
                state.notes.add("Planning limit reached: acquire remaining finished items directly.");
            }
        }
        return new Result(Collections.unmodifiableMap(state.missing), List.copyOf(state.steps), Set.copyOf(state.notes), limited);
    }
    private void need(String item, long amount, State state, Set<String> path, Budget budget, Map<String,String> preferred, int depth) {
        budget.spend();
        if (amount < 0 || amount > 1_000_000_000L) throw new Limit();
        amount = state.consume(item, amount);
        if (amount == 0) return;
        if (depth > 24) throw new Limit();
        if (path.contains(item)) {
            state.missing.merge(item, amount, Long::sum);
            state.notes.add("A recipe cycle was stopped; collect the listed material directly.");
            return;
        }
        Set<String> nextPath = new LinkedHashSet<>(path); nextPath.add(item);
        List<Recipe> choices = index.getOrDefault(item, List.of());
        String chosen = preferred.get(item);
        if (chosen != null && choices.stream().anyMatch(r -> r.id.equals(chosen)))
            choices = choices.stream().filter(r -> r.id.equals(chosen)).toList();
        State best = null;
        for (Recipe recipe : choices) {
            budget.spend();
            // Reversible unpacking is useful when its packed input is already carried.
            // Otherwise prefer acquiring the primitive instead of planning conversion loops.
            if (isUnpacking(recipe) && recipe.ingredients.stream().anyMatch(in ->
                in.alternatives.stream().noneMatch(a -> state.stock.getOrDefault(a,0L) >= in.count))) continue;
            State trial = state.copy();
            long crafts = (amount + recipe.count - 1) / recipe.count;
            for (Ingredient in : recipe.ingredients) {
                long required = Math.multiplyExact(crafts, in.count);
                // Mix interchangeable items already on hand, e.g. oak + birch planks.
                for (String alternative : in.alternatives) {
                    required = trial.consume(alternative, required);
                    if (required == 0) break;
                }
                if (required > 0) {
                    State ingredientBest = null;
                    for (String alternative : in.alternatives) {
                        State candidate = trial.copy();
                        need(alternative, required, candidate, nextPath, budget, preferred, depth + 1);
                        if (better(candidate, ingredientBest)) ingredientBest = candidate;
                    }
                    trial.take(ingredientBest);
                }
            }
            trial.stock.merge(item, crafts * recipe.count - amount, Long::sum);
            trial.steps.add(new Step(recipe.id, item, crafts, recipe.station));
            if (better(trial, best)) best = trial;
        }
        if (best == null) state.missing.merge(item, amount, Long::sum);
        else state.take(best);
    }
    private boolean isUnpacking(Recipe r) {
        if (r.ingredients.size() != 1 || r.count <= r.ingredients.getFirst().count) return false;
        for (String input : r.ingredients.getFirst().alternatives) {
            for (Recipe back : index.getOrDefault(input, List.of())) {
                if (back.ingredients.stream().allMatch(i -> i.alternatives.size() == 1 && i.alternatives.contains(r.output)))
                    return true;
            }
        }
        return false;
    }
    private static boolean better(State candidate, State best) {
        return best == null || candidate.notes.size() < best.notes.size()
            || (candidate.notes.size() == best.notes.size() && (candidate.cost() < best.cost()
            || (candidate.cost() == best.cost() && candidate.steps.size() < best.steps.size())));
    }
}
