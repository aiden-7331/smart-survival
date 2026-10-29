package dev.smartsurvival.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.smartsurvival.core.*;
import dev.smartsurvival.client.ui.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.sdl.SDLScancode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

public final class SmartSurvivalClient implements ClientModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("Smart Survival");
    public static SmartSurvivalClient INSTANCE;
    public Settings settings;
    public SettingsStore store;
    public final RecipeCatalog catalog = new RecipeCatalog();
    public Map<String,Integer> counts = Map.of(), craftingCounts = Map.of();
    public List<InventoryAnalysis.Advice> advice = List.of();
    public Planner.Result plan = new Planner.Result(Map.of(), List.of(), Set.of(), false);
    public int freeSlots = 36;
    public long revision;
    private List<ItemStack> previous = List.of();
    private Object previousPlayer;
    private boolean dirty = true;
    private int ticks;
    private KeyMapping dashboard, toggleHud, pinHeld, favoriteHeld;
    private final Map<String,List<HudRenderer.Line>> hudLines = new LinkedHashMap<>();

    @Override public void onInitializeClient() {
        INSTANCE = this;
        store = new SettingsStore(FabricLoader.getInstance().getConfigDir().resolve("smart-survival.json"));
        settings = store.load();
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("smart-survival", "general"));
        dashboard = key("dashboard", SDLScancode.SDL_SCANCODE_K, category);
        toggleHud = key("toggle_hud", SDLScancode.SDL_SCANCODE_F8, category);
        pinHeld = key("pin_held", SDLScancode.SDL_SCANCODE_UNKNOWN, category);
        favoriteHeld = key("favorite_held", SDLScancode.SDL_SCANCODE_UNKNOWN, category);
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> store.save(settings));
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
            Identifier.fromNamespaceAndPath("smart-survival", "widgets"),
            (graphics, delta) -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null && settings.hud && mc.gui.screen() == null)
                    HudRenderer.drawAll(graphics, this, false, null);
            });
        LOG.info("Smart Survival initialized");
    }
    private static KeyMapping key(String name, int code, KeyMapping.Category category) {
        return KeyMappingHelper.registerKeyMapping(new KeyMapping("key.smart-survival."+name, InputConstants.Type.KEYBOARD, code, category));
    }
    private void tick(Minecraft client) {
        if (client.player == null) {
            if (previousPlayer != null) {
                previousPlayer = null; previous = List.of(); counts = Map.of(); craftingCounts = Map.of();
                advice = List.of(); plan = new Planner.Result(Map.of(),List.of(),Set.of(),false);
                catalog.clear(); hudLines.clear(); dirty = true;
            }
            return;
        }
        if (previousPlayer != client.player) {
            previousPlayer = client.player; previous = List.of(); catalog.invalidate(); dirty = true;
        }
        while (dashboard.consumeClick()) {
            if (client.gui.screen() == null) client.gui.setScreen(new DashboardScreen(this));
        }
        while (toggleHud.consumeClick()) { settings.hud = !settings.hud; changed(); }
        while (pinHeld.consumeClick()) if (client.gui.screen() == null) {
            ItemStack held = client.player.getMainHandItem();
            if (!held.isEmpty()) { upsert(settings.goals, new Goal(ItemsView.id(held),64)); changed(); }
        }
        while (favoriteHeld.consumeClick()) if (client.gui.screen() == null) {
            ItemStack held = client.player.getMainHandItem();
            if (!held.isEmpty()) toggleFavorite(ItemsView.id(held));
        }
        if (++ticks % 10 == 0 || dirty) refresh(client);
    }
    public void refresh(Minecraft client) {
        if (client.player == null) return;
        Inventory inv = client.player.getInventory();
        List<ItemStack> snapshot = new ArrayList<>();
        boolean inventoryChanged = previous.size() != inv.getContainerSize();
        for (int i=0;i<inv.getContainerSize();i++) {
            ItemStack stack = inv.getItem(i);
            if (!inventoryChanged && !ItemStack.matches(previous.get(i),stack)) inventoryChanged = true;
            snapshot.add(stack.copy());
        }
        // Recount the small carried inventory every sample. Change detection only
        // controls expensive planning/advice, never whether displayed counts refresh.
        Map<String,Integer> all = new TreeMap<>(), craft = new TreeMap<>();
        List<InventoryAnalysis.Stack> main = new ArrayList<>();
        int availableSlots=0;
        for (int i=0;i<snapshot.size();i++) {
            ItemStack stack=snapshot.get(i);
            if(i<36 && stack.isEmpty())availableSlots++;
            if(stack.isEmpty())continue;
            String id=ItemsView.id(stack);
            all.merge(id,stack.getCount(),Integer::sum);
            if(i<36 && Inventory.isUsableForCrafting(stack) && !settings.favorites.contains(id))
                craft.merge(id,stack.getCount(),Integer::sum);
            if(i<36)main.add(new InventoryAnalysis.Stack(id,stack.getCount(),stack.getMaxStackSize(),stack.isDamageableItem(),!stack.getComponentsPatch().isEmpty()));
        }
        boolean totalsChanged=!all.equals(counts)||!craft.equals(craftingCounts)||availableSlots!=freeSlots;
        counts=Map.copyOf(all);craftingCounts=Map.copyOf(craft);freeSlots=availableSlots;
        previous=snapshot;
        boolean recipesChanged = catalog.refresh(client);
        if (inventoryChanged || totalsChanged || dirty || recipesChanged) {
            Set<String> needed = new HashSet<>();
            settings.goals.forEach(g -> needed.add(g.item()));
            settings.presets.getOrDefault(settings.activePreset,List.of()).forEach(g -> needed.add(g.item()));
            settings.plans.forEach(g -> needed.add(g.item()));
            try {
                // Special, equipped and favourite finished outputs still satisfy targets;
                // they simply cannot be spent as ingredients.
                List<Goal> outstanding = new ArrayList<>();
                for (Goal target : settings.plans) {
                    int protectedHave = Math.max(0, counts.getOrDefault(target.item(),0) - craftingCounts.getOrDefault(target.item(),0));
                    int left = target.missing(protectedHave);
                    if (left > 0) outstanding.add(new Goal(target.item(),left));
                }
                plan = catalog.planner().plan(outstanding, craftingCounts, settings.recipeChoices);
                needed.addAll(plan.missing().keySet());
            } catch (RuntimeException ex) {
                LOG.error("Could not calculate crafting plan",ex);
                plan = new Planner.Result(Map.of(),List.of(),Set.of("Planning failed; see the game log."),true);
            }
            advice = InventoryAnalysis.suggest(main,settings.favorites,needed);
            dirty = false; revision++;
        }
        rebuildHud(client);
    }
    private void rebuildHud(Minecraft client) {
        List<HudRenderer.Line> goals = pendingGoalLines(settings.goals);
        List<HudRenderer.Line> pack = pendingGoalLines(settings.presets.getOrDefault(settings.activePreset,List.of()));
        List<HudRenderer.Line> craft = new ArrayList<>();
        plan.missing().forEach((id,n) -> craft.add(new HudRenderer.Line(id,ItemsView.name(id),"Need "+n, HudRenderer.GOLD)));
        if (!settings.plans.isEmpty() && craft.isEmpty())
            craft.add(new HudRenderer.Line(null,plan.limited() ? "Planner notice • open dashboard" : plan.steps().isEmpty() ? "Goals complete" : "Materials ready • craft next",plan.limited()?HudRenderer.GOLD:HudRenderer.GREEN));
        hudLines.put("Resources",goals); hudLines.put("Preparation",pack); hudLines.put("Crafting",craft);
        List<HudRenderer.Line> bag = new ArrayList<>();
        if (freeSlots <= settings.nearFullSlots) bag.add(new HudRenderer.Line(null,freeSlots+" inventory slots free",HudRenderer.GOLD));
        if (!advice.isEmpty()) bag.add(new HudRenderer.Line(null,advice.size()+" suggestions • open dashboard",HudRenderer.MUTED));
        hudLines.put("Inventory",bag);
        var survival=List.of(new HudRenderer.Line(null,
            "Health "+(int)Math.ceil(client.player.getHealth())+" • Food "+client.player.getFoodData().getFoodLevel()+" • XP "+client.player.experienceLevel,HudRenderer.WHITE));
        if(!survival.equals(hudLines.put("Survival",survival))) revision++;
    }
    private List<HudRenderer.Line> pendingGoalLines(List<Goal> savedGoals) {
        List<HudRenderer.Line> lines=new ArrayList<>();
        for(Goal goal:savedGoals)
            if(goal.missing(counts.getOrDefault(goal.item(),0))>0)lines.add(progress(goal));
        if(!savedGoals.isEmpty() && lines.isEmpty())
            lines.add(new HudRenderer.Line(null,"All goals complete",HudRenderer.GREEN));
        return lines;
    }
    private HudRenderer.Line progress(Goal g) {
        int have = counts.getOrDefault(g.item(),0), missing = g.missing(have);
        return new HudRenderer.Line(g.item(),ItemsView.name(g.item()),have+" / "+g.amount()+(missing>0?" • "+missing+" left":" ✓"),missing==0?HudRenderer.GREEN:HudRenderer.WHITE);
    }
    public List<HudRenderer.Line> lines(String widget) { return hudLines.getOrDefault(widget,List.of()); }
    public void changed() { dirty = true; store.save(settings); revision++; }
    public void invalidate() { dirty = true; }
    public void toggleFavorite(String id) {
        if (!settings.favorites.remove(id)) settings.favorites.add(id);
        changed();
    }
    public boolean protectedStack(ItemStack stack) {
        return settings.protectFavorites && !stack.isEmpty() && settings.favorites.contains(ItemsView.id(stack));
    }
    public void blockedDrop() {
        var p = Minecraft.getInstance().player;
        if (p != null) p.sendOverlayMessage(Component.literal("Favourite protected. Unfavourite it in Smart Survival to drop it."));
    }
    public static void upsert(List<Goal> list, Goal goal) {
        for (int i=0;i<list.size();i++) if (list.get(i).item().equals(goal.item())) { list.set(i,goal); return; }
        if (list.size()<128) list.add(goal);
    }
}
