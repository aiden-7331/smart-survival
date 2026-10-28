package dev.smartsurvival.client.ui;
import dev.smartsurvival.client.SmartSurvivalClient;
import dev.smartsurvival.core.Settings;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import org.lwjgl.sdl.SDLMouse;

public final class HudEditorScreen extends PanelScreen {
    private final SmartSurvivalClient app;private final Screen parent;
    private String selected="Resources";private boolean dragging;private double dx,dy;
    public HudEditorScreen(SmartSurvivalClient app,Screen parent){super("Arrange HUD");this.app=app;this.parent=parent;}
    @Override protected void init(){
        layout();int x=left+5;int w=Math.max(35,(panelWidth-10)/6);
        button("Scale -",x,height-27,w-2,()->adjust(-0.1,0));
        button("Scale +",x+w,height-27,w-2,()->adjust(0.1,0));
        button("Fade -",x+2*w,height-27,w-2,()->adjust(0,-0.1));
        button("Fade +",x+3*w,height-27,w-2,()->adjust(0,0.1));
        button("Show/hide",x+4*w,height-27,w-2,()->{var s=app.settings.widgets.get(selected);s.enabled=!s.enabled;app.changed();});
        button("Done",x+5*w,height-27,w-2,this::onClose);
    }
    private void adjust(double scale,double opacity){var s=app.settings.widgets.get(selected);s.scale+=scale;s.opacity+=opacity;s.normalize();app.changed();}
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean twice){
        if(e.y()>=height-30)return super.mouseClicked(e,twice);
        if(e.button()!=SDLMouse.SDL_BUTTON_LEFT)return false;
        for(String name:Settings.WIDGET_NAMES.reversed()){
            var s=app.settings.widgets.get(name);var b=HudRenderer.bounds(name,s,width,height,app.lines(name).size());
            if(b.contains(e.x(),e.y())){selected=name;dragging=true;dx=e.x()-b.x();dy=e.y()-b.y();return true;}
        }
        return super.mouseClicked(e,twice);
    }
    @Override public boolean mouseDragged(MouseButtonEvent e,double x,double y){
        if(!dragging)return super.mouseDragged(e,x,y);
        var s=app.settings.widgets.get(selected);var b=HudRenderer.bounds(selected,s,width,height,app.lines(selected).size());
        s.x=Math.max(0,Math.min(1,(e.x()-dx)/Math.max(1,width-b.width())));
        s.y=Math.max(0,Math.min(1,(e.y()-dy)/Math.max(1,height-b.height())));
        return true;
    }
    @Override public boolean mouseReleased(MouseButtonEvent e){if(dragging){dragging=false;app.changed();return true;}return super.mouseReleased(e);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float d){
        g.fill(0,0,width,height,0x880C100D);HudRenderer.drawAll(g,app,true,selected);
        g.fill(0,height-48,width,height,0xEF141B16);
        var s=app.settings.widgets.get(selected);
        g.centeredText(font,selected+" • drag to move • scale "+Math.round(s.scale*100)+"% • opacity "+Math.round(s.opacity*100)+"%",width/2,height-42,HudRenderer.WHITE);
        super.extractRenderState(g,x,y,d);
    }
    @Override public void onClose(){app.changed();minecraft.gui.setScreen(parent);}
}
