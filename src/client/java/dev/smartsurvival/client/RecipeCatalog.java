package dev.smartsurvival.client;

import dev.smartsurvival.core.Planner;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.*;
import net.minecraft.util.context.ContextMap;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Reads only recipe displays the server has sent to the local recipe book. */
public final class RecipeCatalog {
    private Object collections;
    private boolean dirty = true;
    private Planner planner = new Planner(List.of());
    private int supported, skipped;
    private final Set<String> cooking = new HashSet<>();
    public void invalidate() { dirty = true; }
    public void clear() { collections=null; dirty=true; planner=new Planner(List.of()); supported=0; skipped=0; cooking.clear(); }
    public Planner planner() { return planner; }
    public int supported() { return supported; }
    public int skipped() { return skipped; }
    public boolean cooking(String recipe) { return cooking.contains(recipe); }
    public boolean refresh(Minecraft client) {
        if(client.player==null || client.level==null) return false;
        var current=client.player.getRecipeBook().getCollections();
        if(!dirty && collections==current) return false;
        collections=current; dirty=false; skipped=0; cooking.clear();
        ContextMap context=SlotDisplayContext.fromLevel(client.level);
        Map<String,Planner.Recipe> recipes=new TreeMap<>();
        for(var collection:current) for(var entry:collection.getRecipes()) {
            try {
                RecipeDisplay display=entry.display();
                List<SlotDisplay> slots;
                boolean furnace=false;
                if(display instanceof ShapedCraftingRecipeDisplay shaped) slots=shaped.ingredients();
                else if(display instanceof ShapelessCraftingRecipeDisplay shapeless) slots=shapeless.ingredients();
                else if(display instanceof FurnaceRecipeDisplay heat) { slots=List.of(heat.ingredient()); furnace=true; }
                else { skipped++; continue; }
                var outputs=display.result().resolveForStacks(context);
                if(outputs.size()!=1 || !plain(outputs.getFirst())) { skipped++; continue; }
                ItemStack out=outputs.getFirst();
                List<Planner.Ingredient> ingredients=new ArrayList<>();
                boolean valid=true;
                for(SlotDisplay slot:slots) {
                    if(slot instanceof SlotDisplay.Empty) continue;
                    if(slot instanceof SlotDisplay.WithRemainder remainder) slot=remainder.input();
                    List<ItemStack> values=slot.resolveForStacks(context);
                    if(values.isEmpty() || values.stream().anyMatch(s->!plain(s))) { valid=false; break; }
                    // Each crafting grid slot consumes one item; result stacks preserve actual output count.
                    ingredients.add(new Planner.Ingredient(values.stream().map(ItemsView::id).toList(),1));
                }
                if(!valid || ingredients.isEmpty()) { skipped++; continue; }
                ItemStack station=display.craftingStation().resolveForFirstStack(context);
                String stationName=station.isEmpty()?"Crafting":station.getHoverName().getString();
                String output=ItemsView.id(out);
                String stationId=station.isEmpty()?"crafting":ItemsView.id(station);
                String id=signature(output+"|"+out.getCount()+"|"+stationId+"|"+ingredients);
                recipes.put(id,new Planner.Recipe(id,output,out.getCount(),ingredients,stationName));
                if(furnace) cooking.add(id);
            } catch(RuntimeException ex) {
                skipped++;
                SmartSurvivalClient.LOG.debug("Unsupported synced recipe display",ex);
            }
        }
        planner=new Planner(recipes.values()); supported=recipes.size();
        return true;
    }
    private static boolean plain(ItemStack stack) {
        return !stack.isEmpty() && stack.getComponentsPatch().isEmpty();
    }
    private static String signature(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))).substring(0,24); }
        catch(NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
}
