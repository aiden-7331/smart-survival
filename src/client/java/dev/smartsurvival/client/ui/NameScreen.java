package dev.smartsurvival.client.ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import java.util.function.Predicate;
public final class NameScreen extends PanelScreen {
    private final Screen parent; private final Predicate<String> accept; private final String initial;
    private EditBox name; private String error="";
    public NameScreen(Screen parent,String initial,Predicate<String> accept) {super("Name your checklist");this.parent=parent;this.initial=initial;this.accept=accept;}
    @Override protected void init() {
        layout();name=field("Checklist name",left+10,60,panelWidth-20,initial);name.setMaxLength(32);
        button("Save",left+10,94,90,()->{String v=name.getValue().trim();if(v.isEmpty()||!accept.test(v))error="Use a unique, non-empty name (up to 32 presets).";else onClose();});
        button("Cancel",left+110,94,90,this::onClose);setInitialFocus(name);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float d) {frame(g,"Reusable supplies for your next trip.");super.extractRenderState(g,x,y,d);g.text(font,fit(error,panelWidth-20),left+10,126,HudRenderer.GOLD);}
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
}
