package dev.smartsurvival.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class PanelScreen extends Screen {
    protected int left, panelWidth;
    protected PanelScreen(String title) { super(Component.literal(title)); }
    protected void layout() { panelWidth=Math.min(660,width-16); left=(width-panelWidth)/2; }
    protected Button button(String label,int x,int y,int w,Runnable action) {
        return addRenderableWidget(Button.builder(Component.literal(label),b->action.run()).bounds(x,y,w,20).build());
    }
    protected EditBox field(String label,int x,int y,int w,String value) {
        EditBox box=new EditBox(font,x,y,w,20,Component.literal(label));box.setMaxLength(128);
        box.setValue(value);box.setHint(Component.literal(label));addRenderableWidget(box);return box;
    }
    protected String fit(String text,int w) {
        return font.width(text)<=w?text:font.plainSubstrByWidth(text,Math.max(1,w-8))+"…";
    }
    protected void frame(GuiGraphicsExtractor g,String subtitle) {
        g.fill(0,0,width,height,0xC00C100D);
        g.fill(left,8,left+panelWidth,height-8,0xF019211B);
        g.fill(left,8,left+panelWidth,10,HudRenderer.GREEN);
        g.text(font,title,left+10,18,HudRenderer.WHITE);
        g.text(font,fit(subtitle,panelWidth-20),left+10,32,HudRenderer.MUTED,false);
    }
    @Override public boolean isPauseScreen() { return false; }
}
