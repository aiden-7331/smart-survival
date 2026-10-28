package dev.smartsurvival.test;

import dev.smartsurvival.client.*;
import dev.smartsurvival.client.ui.*;
import dev.smartsurvival.core.Goal;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.MixinEnvironment;

public final class SurvivalClientTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        try(var world=context.worldBuilder().adjustSettings(s->s.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL)).create()) {
            world.getServer().runCommand("give @p minecraft:oak_log 16");
            world.getServer().runCommand("give @p minecraft:iron_pickaxe 1");
            world.getServer().runCommand("recipe give @p *");
            context.waitFor(mc->mc.player.getInventory().contains(new ItemStack(Items.OAK_LOG)));
            context.waitTicks(40);
            context.runOnClient(mc->{
                var app=SmartSurvivalClient.INSTANCE;
                app.settings.goals.clear();app.settings.plans.clear();
                app.settings.goals.add(new Goal("minecraft:oak_log",32));
                app.settings.plans.add(new Goal("minecraft:crafting_table",4));
                app.settings.widgets.values().forEach(w->w.enabled=true);
                app.invalidate();app.refresh(mc);
                check(app.counts.getOrDefault("minecraft:oak_log",0)==16,"Inventory count");
                check(!app.catalog.planner().recipes("minecraft:crafting_table").isEmpty(),"Synced recipe book");
                check(app.plan.missing().isEmpty(),"Recursive crafting from carried logs");
                check(!app.plan.steps().isEmpty(),"Crafting steps");
            });
            context.setScreen(()->new DashboardScreen(SmartSurvivalClient.INSTANCE));
            context.waitTicks(5);context.takeScreenshot("dashboard-home");
            for(int tab=1;tab<6;tab++){
                final int n=tab;
                context.runOnClient(mc->((DashboardScreen)mc.gui.screen()).showTab(n));
                context.waitTicks(3);context.takeScreenshot("dashboard-tab-"+tab);
            }
            context.getInput().resizeWindow(640,480);
            context.waitTicks(4);
            context.setScreen(()->new DashboardScreen(SmartSurvivalClient.INSTANCE));
            context.waitTicks(3);context.takeScreenshot("dashboard-small");
            context.setScreen(()->new HudEditorScreen(SmartSurvivalClient.INSTANCE,new DashboardScreen(SmartSurvivalClient.INSTANCE)));
            context.waitTicks(3);context.takeScreenshot("hud-editor-small");
            context.setScreen(()->null);
            world.getConnection().waitForChunksRender();
            context.takeScreenshot("survival-hud");
            context.runOnClient(mc->{
                var app=SmartSurvivalClient.INSTANCE;
                app.settings.favorites.add("minecraft:iron_pickaxe");
                check(app.protectedStack(new ItemStack(Items.IRON_PICKAXE)),"Favourite matching");
                check(!app.protectedStack(new ItemStack(Items.DIRT)),"Unrelated item protection");
                check(app.store.save(app.settings),"Settings write");
                check(app.store.load().goals.equals(app.settings.goals),"Settings reload");
            });
            MixinEnvironment.getCurrentEnvironment().audit();
        }
        context.waitTicks(5);
        context.runOnClient(mc->check(SmartSurvivalClient.INSTANCE.counts.isEmpty(),"Disconnect clears inventory"));
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
