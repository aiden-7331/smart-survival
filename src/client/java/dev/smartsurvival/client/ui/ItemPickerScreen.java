package dev.smartsurvival.client.ui;

import dev.smartsurvival.client.*;
import dev.smartsurvival.core.Goal;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.*;

public final class ItemPickerScreen extends PanelScreen {
    private final Screen parent;
    private final Predicate<String> allowed;
    private final Consumer<Goal> accept;
    private final String initial;
    private final int initialAmount;
    private EditBox search, amount;
    private Button save;
    private String selected;
    private final List<Button> rows=new ArrayList<>();
    private List<String> items=List.of();
    private int page;
    private String error="";
    public ItemPickerScreen(Screen parent, String title, Predicate<String> allowed, Consumer<Goal> accept, Goal existing) {
        super(title);this.parent=parent;this.allowed=allowed;this.accept=accept;
        initial=existing==null?"":existing.item();initialAmount=existing==null?64:existing.amount();
        selected=existing==null?null:existing.item();
    }
    @Override protected void init() {
        layout();
        String query=search==null?initial:search.getValue(), qty=amount==null?""+initialAmount:amount.getValue();
        search=field("Search name or item ID",left+10,48,panelWidth-20,query);
        amount=field("Quantity",left+80,height-38,80,qty);amount.setMaxLength(7);
        save=button("Save",left+166,height-38,60,this::save);
        button("Cancel",left+panelWidth-70,height-38,60,this::onClose);
        button("<",left+10,height-64,30,()->{page=Math.max(0,page-1);updateRows();});
        button(">",left+panelWidth-40,height-64,30,()->{page=Math.min(Math.max(0,(items.size()-1)/rows.size()),page+1);updateRows();});
        rows.clear();
        int count=Math.max(1,(height-148)/22);
        for(int i=0;i<count;i++) { final int n=i;rows.add(button("",left+30,76+i*22,panelWidth-40,()->select(n))); }
        search.setResponder(value->{page=0;filter();});filter();setInitialFocus(search);
    }
    private void filter() {
        String q=search.getValue().toLowerCase(Locale.ROOT).trim();
        items=BuiltInRegistries.ITEM.stream().map(ItemsView::id).filter(ItemsView::exists).filter(allowed)
            .filter(id->id.contains(q)||ItemsView.name(id).toLowerCase(Locale.ROOT).contains(q))
            .sorted(Comparator.comparing(ItemsView::name)).toList();updateRows();
    }
    private void updateRows() {
        for(int i=0;i<rows.size();i++) {
            int n=page*rows.size()+i;Button b=rows.get(i);b.visible=n<items.size();
            if(b.visible) {
                String id=items.get(n);
                b.setMessage(Component.literal(fit((id.equals(selected)?"✓ ":"")+ItemsView.name(id),panelWidth-52)));
            }
        }
        save.active=selected!=null;
        if(selected!=null)save.setTooltip(Tooltip.create(Component.literal("Save "+ItemsView.name(selected)+" with the entered quantity")));
    }
    private void select(int row) {
        int n=page*rows.size()+row;if(n>=items.size())return;
        selected=items.get(n);error="";updateRows();
    }
    private void save() {
        if(selected==null)return;
        try { accept.accept(new Goal(selected,Integer.parseInt(amount.getValue().trim())));onClose(); }
        catch(IllegalArgumentException ex) { error="Enter a quantity from 1 to 1,000,000."; }
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        frame(g,"Select an item, enter its quantity, then Save.");
        super.extractRenderState(g,mx,my,delta);
        for(int i=0;i<rows.size();i++) {
            int n=page*rows.size()+i;if(n<items.size()) {
                g.item(ItemsView.stack(items.get(n)),left+10,78+i*22);
                if(rows.get(i).isMouseOver(mx,my))g.setTooltipForNextFrame(Component.literal(items.get(n)),mx,my);
            }
        }
        g.text(font,"Quantity",left+10,height-32,HudRenderer.MUTED,false);
        g.centeredText(font,items.isEmpty()?"No matching known items":(page+1)+" / "+Math.max(1,(items.size()+rows.size()-1)/rows.size()),width/2,height-58,HudRenderer.MUTED);
        if(!error.isEmpty())g.text(font,fit(error,panelWidth-20),left+10,height-77,HudRenderer.GOLD);
    }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
}
