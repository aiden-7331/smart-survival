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
                long oldRevision=app.revision;
                float oldHealth=mc.player.getHealth();
                mc.player.setHealth(oldHealth-2);app.refresh(mc);
                check(app.revision>oldRevision,"Survival display refreshes without inventory changes");
                mc.player.setHealth(oldHealth);app.refresh(mc);
            });
            context.setScreen(()->new DashboardScreen(SmartSurvivalClient.INSTANCE));
            context.waitTicks(5);context.takeScreenshot("dashboard-home");
            context.clickScreenButton("Goals");
            context.clickScreenButton("Add goal");
            context.waitForScreen(ItemPickerScreen.class);
            context.getInput().typeChars("cobblestone");
            context.waitTicks(2);
            context.clickScreenButton("Cobblestone");
            context.clickScreenButton("Save");
            context.waitForScreen(DashboardScreen.class);
            context.runOnClient(mc->check(SmartSurvivalClient.INSTANCE.settings.goals.stream().anyMatch(g->g.item().equals("minecraft:cobblestone")&&g.amount()==64),"Item picker saves searched goal"));
            context.clickScreenButton("Pack");context.clickScreenButton("Manage");context.clickScreenButton("New preset");
            context.waitForScreen(NameScreen.class);context.getInput().typeChars("Test expedition");context.clickScreenButton("Save");
            context.waitForScreen(DashboardScreen.class);
            context.runOnClient(mc->check(SmartSurvivalClient.INSTANCE.settings.presets.containsKey("Test expedition"),"Custom preset creation"));
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
            context.waitTicks(3);
            context.runOnClient(mc->{
                var app=SmartSurvivalClient.INSTANCE;
                var editor=(HudEditorScreen)mc.gui.screen();
                var widget=app.settings.widgets.get("Resources");
                var b=HudRenderer.bounds("Resources",widget,editor.width,editor.height,app.lines("Resources").size());
                double oldX=widget.x;
                var info=new net.minecraft.client.input.MouseButtonInfo(org.lwjgl.sdl.SDLMouse.SDL_BUTTON_LEFT,0);
                check(editor.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(b.x()+5,b.y()+5,info),false),"HUD selected by click");
                editor.mouseDragged(new net.minecraft.client.input.MouseButtonEvent(b.x()+35,b.y()+20,info),30,15);
                editor.mouseReleased(new net.minecraft.client.input.MouseButtonEvent(b.x()+35,b.y()+20,info));
                check(widget.x!=oldX,"HUD drag changes saved position");
            });
            context.takeScreenshot("hud-editor-small");
            context.setScreen(()->null);
            world.getConnection().waitForChunksRender();
            context.takeScreenshot("survival-hud");
            context.runOnClient(mc->{
                var app=SmartSurvivalClient.INSTANCE;
                app.settings.favorites.add("minecraft:iron_pickaxe");
                check(app.protectedStack(new ItemStack(Items.IRON_PICKAXE)),"Favourite matching");
                check(!app.protectedStack(new ItemStack(Items.DIRT)),"Unrelated item protection");
                int pickaxe=mc.player.getInventory().findSlotMatchingItem(new ItemStack(Items.IRON_PICKAXE));
                check(pickaxe>=0 && pickaxe<9,"Test pickaxe in hotbar");
                mc.player.getInventory().setSelectedSlot(pickaxe);
                mc.gameMode.dropItem(mc.player,false);
                check(!mc.player.getInventory().getSelectedItem().isEmpty(),"Hotbar drop blocked");
                var menu=mc.player.containerMenu;
                int slot=menu.findSlot(mc.player.getInventory(),pickaxe).orElseThrow();
                mc.gameMode.handleContainerInput(menu.containerId,slot,0,net.minecraft.world.inventory.ContainerInput.THROW,mc.player);
                check(!menu.getSlot(slot).getItem().isEmpty(),"Container throw blocked");
                menu.setCarried(new ItemStack(Items.IRON_PICKAXE));
                mc.gameMode.handleContainerInput(menu.containerId,-999,0,net.minecraft.world.inventory.ContainerInput.PICKUP,mc.player);
                check(!menu.getCarried().isEmpty(),"Cursor drop blocked");
                menu.setCarried(ItemStack.EMPTY);
                check(app.store.save(app.settings),"Settings write");
                check(app.store.load().goals.equals(app.settings.goals),"Settings reload");
                app.settings.plans.clear();app.settings.plans.add(new Goal("minecraft:iron_pickaxe",1));
                app.invalidate();app.refresh(mc);
                check(app.plan.missing().isEmpty()&&app.plan.steps().isEmpty(),"Carried favourite satisfies finished target");
            });
            world.getServer().runCommand("give @p minecraft:oak_log 16");
            world.getServer().runCommand("item replace entity @p weapon.offhand with minecraft:torch 8");
            context.waitFor(mc->{
                var app=SmartSurvivalClient.INSTANCE;
                return app.counts.getOrDefault("minecraft:oak_log",0)==32 && app.counts.getOrDefault("minecraft:torch",0)==8;
            });
            MixinEnvironment.getCurrentEnvironment().audit();
        }
        context.waitTicks(5);
        context.runOnClient(mc->check(SmartSurvivalClient.INSTANCE.counts.isEmpty(),"Disconnect clears inventory"));
        // Separate server connection: production mod has no server entrypoint.
        try(var server=context.worldBuilder().createServer()) {
            try(var connection=server.connect()) {
                server.runCommand("give @p minecraft:oak_log 4");
                server.runCommand("recipe give @p *");
                context.waitFor(mc->SmartSurvivalClient.INSTANCE.counts.getOrDefault("minecraft:oak_log",0)==4);
                context.runOnClient(mc->{
                    var app=SmartSurvivalClient.INSTANCE;
                    app.settings.plans.clear();app.settings.plans.add(new Goal("minecraft:crafting_table",1));
                    app.invalidate();app.refresh(mc);
                    check(app.plan.missing().isEmpty()&&!app.plan.steps().isEmpty(),"Separate server recipe planning");
                });
                connection.waitForChunksRender();
            }
        }
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
