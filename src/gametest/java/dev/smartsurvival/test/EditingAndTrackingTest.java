package dev.smartsurvival.test;

import dev.smartsurvival.client.*;
import dev.smartsurvival.client.ui.*;
import dev.smartsurvival.core.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import org.lwjgl.sdl.SDLMouse;
import org.lwjgl.sdl.SDLScancode;

public final class EditingAndTrackingTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        try(var world=context.worldBuilder().adjustSettings(s->s.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL)).create()) {
            context.runOnClient(mc->{
                var app=SmartSurvivalClient.INSTANCE;
                app.settings=new Settings();
                app.settings.goals.add(new Goal("minecraft:acacia_boat",64));
                app.changed();
            });
            world.getServer().runCommand("give @p minecraft:acacia_boat 3");
            context.waitTicks(30);
            assertBoatCount(context,3);
            world.getServer().runCommand("item replace entity @p hotbar.8 with minecraft:acacia_boat");
            context.waitTicks(30);
            assertBoatCount(context,4);
            world.getServer().runCommand("clear @p minecraft:acacia_boat 2");
            context.waitTicks(30);
            assertBoatCount(context,2);
            System.out.println("REGRESSION_PASS acacia boat acquisition, hotbar, removal and HUD");

            context.setScreen(()->new DashboardScreen(SmartSurvivalClient.INSTANCE));
            context.clickScreenButton("Goals");
            context.clickScreenButton("Acacia Boat × 64");
            editQuantity(context,"12");
            context.clickScreenButton("Save");
            context.waitForScreen(DashboardScreen.class);
            assertAmount(context,12);
            System.out.println("REGRESSION_PASS quantity mouse editing and explicit save");

            context.clickScreenButton("Acacia Boat × 12");
            editQuantity(context,"7");
            // Saving another preference while an editor is open must not detach its list.
            context.runOnClient(mc->SmartSurvivalClient.INSTANCE.changed());
            context.clickScreenButton("Save");
            context.waitForScreen(DashboardScreen.class);
            assertAmount(context,7);
            System.out.println("REGRESSION_PASS quantity save after another preference save");
            context.clickScreenButton("Acacia Boat × 7");
            editQuantity(context,"0");
            context.clickScreenButton("Save");
            context.waitForScreen(ItemPickerScreen.class);
            context.clickScreenButton("Cancel");
            context.waitForScreen(DashboardScreen.class);
            assertAmount(context,7);

            // Editing remains valid after a resize at a small GUI size.
            context.clickScreenButton("Acacia Boat × 7");
            editQuantity(context,"9");
            context.getInput().resizeWindow(640,480);
            context.waitTicks(4);context.takeScreenshot("quantity-editor-small");
            context.clickScreenButton("Save");
            context.waitForScreen(DashboardScreen.class);
            assertAmount(context,9);
            context.setScreen(()->null);
            context.waitTicks(15);context.takeScreenshot("acacia-resource-count");
            context.runOnClient(mc->check(SmartSurvivalClient.INSTANCE.lines("Resources").getFirst().detail().startsWith("2 / 9"),"HUD uses edited target"));

            world.getServer().runCommand("recipe give @p *");
            context.waitTicks(30);
            context.setScreen(()->new DashboardScreen(SmartSurvivalClient.INSTANCE));
            context.clickScreenButton("Plan");context.clickScreenButton("Add target");
            context.getInput().typeChars("crafting_table");context.clickScreenButton("Crafting Table");
            editQuantity(context,"5");context.clickScreenButton("Save");
            context.waitForScreen(DashboardScreen.class);
            context.runOnClient(mc->check(SmartSurvivalClient.INSTANCE.settings.plans.getFirst().amount()==5,"Crafting quantity saved"));
            context.clickScreenButton("Crafting Table × 5");editQuantity(context,"11");context.clickScreenButton("Save");
            context.waitForScreen(DashboardScreen.class);
            context.runOnClient(mc->check(SmartSurvivalClient.INSTANCE.store.load().plans.getFirst().amount()==11,"Crafting quantity edit persisted"));

            context.clickScreenButton("Pack");context.clickScreenButton("+ Item");
            context.getInput().typeChars("acacia_boat");context.clickScreenButton("Acacia Boat");
            editQuantity(context,"6");context.clickScreenButton("Save");
            context.waitForScreen(DashboardScreen.class);
            context.runOnClient(mc->{
                var settings=SmartSurvivalClient.INSTANCE.store.load();
                check(settings.presets.get(settings.activePreset).stream().anyMatch(g->g.item().equals("minecraft:acacia_boat")&&g.amount()==6),"Preparation quantity saved");
            });
            System.out.println("REGRESSION_PASS goal, crafting and preparation quantities; invalid values; cancel; resize");
            // Creative inventory changes use the same inventory-only counting path.
            context.setScreen(()->null);
            world.getServer().runCommand("gamemode creative @p");
            world.getServer().runCommand("give @p minecraft:acacia_boat 1");
            context.waitTicks(30);
            context.runOnClient(mc->{
                var app=SmartSurvivalClient.INSTANCE;
                check(app.counts.getOrDefault("minecraft:acacia_boat",0)==3,"Creative inventory count");
                check(app.lines("Resources").getFirst().detail().startsWith("3 / 9"),"Creative HUD count");
            });
            System.out.println("REGRESSION_PASS creative inventory and HUD counts");

            // Completing visible goals promotes the next unfinished items into the HUD.
            world.getServer().runCommand("clear @p");
            context.waitTicks(30);
            context.runOnClient(mc->{
                var app=SmartSurvivalClient.INSTANCE;
                var queue=java.util.List.of(new Goal("minecraft:acacia_boat",1),new Goal("minecraft:cobblestone",2),
                    new Goal("minecraft:dirt",3),new Goal("minecraft:oak_log",4),new Goal("minecraft:torch",5),new Goal("minecraft:bread",6));
                app.settings.goals=new java.util.ArrayList<>(queue);
                app.settings.presets.put(app.settings.activePreset,new java.util.ArrayList<>(queue));
                app.changed();
            });
            context.waitTicks(15);
            context.runOnClient(mc->check(SmartSurvivalClient.INSTANCE.lines("Resources").size()==6,"Initial six-item queue"));
            world.getServer().runCommand("give @p minecraft:acacia_boat 1");
            world.getServer().runCommand("give @p minecraft:cobblestone 2");
            world.getServer().runCommand("give @p minecraft:dirt 3");
            world.getServer().runCommand("give @p minecraft:oak_log 4");
            context.waitTicks(30);
            context.runOnClient(mc->{
                var app=SmartSurvivalClient.INSTANCE;
                for(String widget:java.util.List.of("Resources","Preparation")) {
                    var ids=app.lines(widget).stream().map(HudRenderer.Line::item).toList();
                    check(ids.equals(java.util.List.of("minecraft:torch","minecraft:bread")),"Next unfinished items promoted in "+widget+": "+ids);
                }
                check(app.settings.goals.size()==6 && app.store.load().goals.size()==6,"Completed goals remain saved");
            });
            context.takeScreenshot("next-resource-goals");
            world.getServer().runCommand("give @p minecraft:torch 5");
            world.getServer().runCommand("give @p minecraft:bread 6");
            context.waitTicks(30);
            context.runOnClient(mc->{
                for(String widget:java.util.List.of("Resources","Preparation")) {
                    var lines=SmartSurvivalClient.INSTANCE.lines(widget);
                    check(lines.size()==1 && lines.getFirst().item()==null && lines.getFirst().text().equals("All goals complete"),"All-complete status in "+widget);
                }
            });
            world.getServer().runCommand("clear @p minecraft:torch 1");
            context.waitTicks(30);
            context.runOnClient(mc->{
                var lines=SmartSurvivalClient.INSTANCE.lines("Resources");
                check(lines.size()==1 && "minecraft:torch".equals(lines.getFirst().item()),"Spent resource returns to queue");
                check(lines.getFirst().detail().startsWith("4 / 5"),"Reopened goal uses current inventory");
            });
            System.out.println("REGRESSION_PASS automatic goal promotion, completion status, saved goals and reopening after spending");
        }
    }
    private static void assertBoatCount(ClientGameTestContext context,int expected) {
        context.runOnClient(mc->{
            var app=SmartSurvivalClient.INSTANCE;
            check(app.counts.getOrDefault("minecraft:acacia_boat",0)==expected,"Boat inventory count "+expected+", got "+app.counts);
            var line=app.lines("Resources").stream().filter(l->"minecraft:acacia_boat".equals(l.item())).findFirst().orElseThrow();
            check(line.detail().startsWith(expected+" / 64"),"Boat HUD count "+expected+", got "+line.detail());
        });
    }
    private static void assertAmount(ClientGameTestContext context,int expected) {
        context.runOnClient(mc->{
            var app=SmartSurvivalClient.INSTANCE;
            check(app.settings.goals.getFirst().amount()==expected,"Goal amount should stay "+expected+", got "+app.settings.goals);
            check(app.store.load().goals.getFirst().amount()==expected,"Saved goal quantity");
        });
    }
    static void editQuantity(ClientGameTestContext context,String value) {
        context.waitForScreen(ItemPickerScreen.class);
        double[] pos=context.computeOnClient(mc->{
            var screen=mc.gui.screen();
            var box=screen.children().stream().filter(w->w instanceof EditBox e&&e.getMessage().getString().equals("Quantity")).map(w->(EditBox)w).findFirst().orElseThrow();
            return new double[]{(box.getX()+box.getWidth()/2.0)*mc.getWindow().getScreenWidth()/screen.width,(box.getY()+10.0)*mc.getWindow().getScreenHeight()/screen.height};
        });
        context.getInput().setCursorPos(pos[0],pos[1]);
        context.getInput().pressMouse(SDLMouse.SDL_BUTTON_LEFT);
        context.getInput().pressKey(SDLScancode.SDL_SCANCODE_END);
        for(int i=0;i<7;i++)context.getInput().pressKey(SDLScancode.SDL_SCANCODE_BACKSPACE);
        context.getInput().typeChars(value);context.waitTicks(12);
        context.runOnClient(mc->{
            var box=mc.gui.screen().children().stream().filter(w->w instanceof EditBox e&&e.getMessage().getString().equals("Quantity")).map(w->(EditBox)w).findFirst().orElseThrow();
            check(box.getValue().equals(value),"Typed quantity survives ticks: "+box.getValue());
        });
    }
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
