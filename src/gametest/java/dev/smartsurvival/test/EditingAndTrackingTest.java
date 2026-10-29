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
            context.clickScreenButton("Acacia Boat");
            context.waitForScreen(DashboardScreen.class);
            assertAmount(context,12);
            System.out.println("REGRESSION_PASS quantity mouse editing and item-row save");

            context.clickScreenButton("Acacia Boat × 12");
            editQuantity(context,"7");
            // Saving another preference while an editor is open must not detach its list.
            context.runOnClient(mc->SmartSurvivalClient.INSTANCE.changed());
            context.clickScreenButton("Acacia Boat");
            context.waitForScreen(DashboardScreen.class);
            assertAmount(context,7);
            System.out.println("REGRESSION_PASS quantity save after another preference save");
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
        context.getInput().holdControl();context.getInput().pressKey(SDLScancode.SDL_SCANCODE_A);context.getInput().releaseControl();
        context.getInput().typeChars(value);context.waitTicks(12);
        context.runOnClient(mc->{
            var box=mc.gui.screen().children().stream().filter(w->w instanceof EditBox e&&e.getMessage().getString().equals("Quantity")).map(w->(EditBox)w).findFirst().orElseThrow();
            check(box.getValue().equals(value),"Typed quantity survives ticks: "+box.getValue());
        });
    }
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
