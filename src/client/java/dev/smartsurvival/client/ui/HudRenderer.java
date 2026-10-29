package dev.smartsurvival.client.ui;

import dev.smartsurvival.client.*;
import dev.smartsurvival.core.Settings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.util.*;

public final class HudRenderer {
    public static final int WHITE=0xFFE8E8DD, MUTED=0xFFAEB6A7, GREEN=0xFF9FD383, GOLD=0xFFE3BB70;
    public record Line(String item, String text, String detail, int color) {
        public Line(String item, String text, int color) { this(item,text,null,color); }
    }
    public record Bounds(int x, int y, int width, int height, float scale) {
        public boolean contains(double mx,double my) { return mx>=x && my>=y && mx<x+width && my<y+height; }
    }
    private HudRenderer() {}
    public static Bounds bounds(String name, Settings.Widget config, int width, int height, int lines) {
        float scale = (float)Math.min(config.scale*Math.min(1.0,width/480.0),Math.min(width/198.0,Math.max(0.5,(height-28)/132.0)));
        int w = Math.round(190*scale), h = Math.round((24+Math.min(4,Math.max(1,lines))*24+(lines>4?12:0))*scale);
        return new Bounds((int)Math.round(config.x*Math.max(0,width-w)),(int)Math.round(config.y*Math.max(0,height-h)),w,h,scale);
    }
    public static void drawAll(GuiGraphicsExtractor g, SmartSurvivalClient app, boolean edit, String selected) {
        var font = Minecraft.getInstance().font;
        var drawOrder = new ArrayList<>(Settings.WIDGET_NAMES);
        if(edit && selected!=null && drawOrder.remove(selected)) drawOrder.add(selected);
        for (String name : drawOrder) {
            var config = app.settings.widgets.get(name);
            var lines = app.lines(name);
            if (!edit && (!config.enabled || lines.isEmpty())) continue;
            if (lines.isEmpty()) lines = List.of(new Line(null,"No active entries",MUTED));
            Bounds b = bounds(name,config,g.guiWidth(),g.guiHeight(),lines.size());
            g.pose().pushMatrix(); g.pose().translate(b.x,b.y); g.pose().scale(b.scale,b.scale);
            int h=(int)Math.ceil(b.height/b.scale);
            int alpha=(int)(config.opacity*255);
            g.fill(0,0,190,h,(alpha<<24)|0x141B16);
            g.fill(0,0,2,h,config.enabled?GREEN:MUTED);
            if (edit && name.equals(selected)) g.outline(0,0,190,h,GOLD);
            g.text(font,name+(edit&&!config.enabled?" (hidden)":""),8,7,GREEN,false);
            int y=23;
            for(int i=0;i<Math.min(4,lines.size());i++) {
                Line line=lines.get(i); int x=8;
                if(line.item!=null) { g.pose().pushMatrix();g.pose().translate(7,y-2);g.pose().scale(0.75f,0.75f);g.item(ItemsView.stack(line.item),0,0);g.pose().popMatrix();x=23; }
                String text=font.plainSubstrByWidth(line.text,182-x);
                if(!text.equals(line.text)) text=font.plainSubstrByWidth(line.text,174-x)+"…";
                g.text(font,text,x,y,line.color,false);
                if(line.detail!=null) {
                    float detailScale=Math.min(1.0f,(182-x)/(float)Math.max(1,font.width(line.detail)));
                    g.pose().pushMatrix();g.pose().translate(x,y+10);g.pose().scale(detailScale,detailScale);
                    g.text(font,line.detail,0,0,line.color,false);g.pose().popMatrix();
                }
                y+=24;
            }
            if(lines.size()>4)g.text(font,"+"+(lines.size()-4)+" more • dashboard",8,y,MUTED,false);
            g.pose().popMatrix();
        }
    }
}
