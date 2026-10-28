package dev.smartsurvival.client.ui;

import dev.smartsurvival.client.*;
import dev.smartsurvival.core.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;
import java.util.*;

public final class DashboardScreen extends PanelScreen {
    private final SmartSurvivalClient app;
    private static final String[] TABS={"Home","Goals","Plan","Pack","Bag","Setup"};
    private int tab, page, mode;
    private long revision=-1;
    private final List<Row> entries=new ArrayList<>();
    private List<Row> visible=List.of();
    private String subtitle="";
    private record Row(String item,String title,String detail,int color,Runnable action,String extra,Runnable secondary) {}
    public DashboardScreen(SmartSurvivalClient app) {super("Smart Survival");this.app=app;}
    @Override protected void init() { layout(); rebuild(); }
    public void showTab(int index) {tab=index;page=0;mode=0;rebuild();}
    private void rebuild() {
        clearWidgets(); entries.clear(); revision=app.revision;
        int tw=(panelWidth-20)/TABS.length;
        for(int i=0;i<TABS.length;i++) {final int n=i;var b=button(TABS[i],left+10+i*tw,46,tw-2,()->showTab(n));b.active=tab!=i;}
        switch(tab) {
            case 0 -> home();
            case 1 -> goals(app.settings.goals);
            case 2 -> plans();
            case 3 -> preparation();
            case 4 -> inventory();
            case 5 -> settings();
            default -> {}
        }
        int perPage=Math.max(1,(height-146)/34);
        page=Math.min(page,Math.max(0,(entries.size()-1)/perPage));
        int from=page*perPage,to=Math.min(entries.size(),from+perPage);
        visible=List.copyOf(entries.subList(from,to));
        for(int i=0;i<visible.size();i++) {
            Row row=visible.get(i);int y=100+i*34;int extra=row.secondary==null?0:54;
            if(row.action!=null) {
                var b=button(fit(row.title,panelWidth-48-extra),left+30,y,panelWidth-40-extra,row.action);
                b.setTooltip(Tooltip.create(Component.literal(row.title+"\n"+row.detail)));
            }
            if(row.secondary!=null)button(row.extra,left+panelWidth-60,y,50,row.secondary);
        }
        button("<",left+10,height-35,28,()->{page=Math.max(0,page-1);rebuild();});
        button(">",left+42,height-35,28,()->{page++;rebuild();});
        button("Done",left+panelWidth-70,height-35,60,this::onClose);
    }
    private void home() {
        subtitle="A little preparation. A better adventure.";
        button("Edit HUD",left+10,73,94,()->minecraft.gui.setScreen(new HudEditorScreen(app,this)));
        button(app.settings.hud?"HUD: On":"HUD: Off",left+110,73,94,()->{app.settings.hud=!app.settings.hud;change();});
        add(null,"Resource goals",app.settings.goals.size()+" pinned • inventory updates twice per second",()->showTab(1));
        add(null,"Adventure preparation",app.settings.activePreset+" • "+ready()+" ready",()->showTab(3));
        add(null,"Crafting plan",app.settings.plans.size()+" targets • "+app.plan.missing().size()+" missing material types",()->showTab(2));
        add(null,"Inventory",app.freeSlots+" / 36 slots free • "+app.settings.favorites.size()+" favourites",()->showTab(4));
        if(minecraft.player!=null)add(null,"Survival","Health "+(int)Math.ceil(minecraft.player.getHealth())+" • Food "+minecraft.player.getFoodData().getFoodLevel()+" • XP level "+minecraft.player.experienceLevel,null);
        if(!app.store.warning().isBlank()) add(null,"Settings notice",app.store.warning(),null);
    }
    private String ready() {
        var list=app.settings.presets.getOrDefault(app.settings.activePreset,List.of());
        return list.stream().filter(g->g.missing(app.counts.getOrDefault(g.item(),0))==0).count()+"/"+list.size();
    }
    private void goals(List<Goal> list) {
        subtitle="Track any item or block you can carry.";
        button("Add goal",left+10,73,94,()->pick(list,null,false));
        for(Goal goal:list)goalRow(list,goal,false);
        if(list.isEmpty())add(null,"Start your next project","Choose Add goal, search for an item, then enter a quantity.",null);
    }
    private void plans() {
        subtitle="Known recipes only • favourites and special stacks reserved";
        button("Add target",left+10,73,90,()->pick(app.settings.plans,null,true));
        button(new String[]{"Targets","Materials","Steps"}[mode%3],left+106,73,90,()->{mode=(mode+1)%3;page=0;rebuild();});
        if(mode==0) {
            for(Goal goal:app.settings.plans) {
                goalRow(app.settings.plans,goal,true);
            }
            if(app.settings.plans.isEmpty())add(null,"Plan from your recipe book","Unlock a recipe in-game, then add its output as a target.",null);
            add(null,"Recipe coverage",app.catalog.supported()+" supported displays • "+app.catalog.skipped()+" unsupported",null);
        } else if(mode==1) {
            app.plan.missing().forEach((id,n)->entries.add(new Row(id,ItemsView.name(id)+" • need "+n,"Additional items after allocating carried materials",HudRenderer.GOLD,null,"Pin",()->{SmartSurvivalClient.upsert(app.settings.goals,new Goal(id,(int)Math.min(Goal.MAX_AMOUNT,n+app.counts.getOrDefault(id,0))));change();})));
            if(app.plan.missing().isEmpty())add(null,app.plan.steps().isEmpty()?"Targets satisfied":"Materials ready",app.plan.steps().isEmpty()?"Add a target to start planning.":"Open Steps to see the remaining crafts.",null);
        } else {
            for(var step:app.plan.steps())add(step.item(),ItemsView.name(step.item())+" • "+step.crafts()+" craft(s)",step.station()+(app.catalog.cooking(step.recipe())?" • fuel needed separately":""),null);
            if(app.plan.steps().isEmpty())add(null,"No crafts remaining","Collect the missing materials or add a target.",null);
        }
        for(String note:app.plan.notes()) add(null,"Planner note",note,null);
        if(app.plan.steps().stream().anyMatch(s->app.catalog.cooking(s.recipe())))
            add(null,"Bring fuel","Cooking materials are listed; fuel choice and burn time are not estimated.",null);
    }
    private void preparation() {
        subtitle="Checklist: "+app.settings.activePreset+" • exact item types • "+ready()+" ready";
        button(fit(app.settings.activePreset,88),left+10,73,94,()->{
            var names=new ArrayList<>(app.settings.presets.keySet());
            app.settings.activePreset=names.get((names.indexOf(app.settings.activePreset)+1)%names.size());page=0;change();
        });
        button("+ Item",left+110,73,62,()->pick(app.settings.presets.get(app.settings.activePreset),null,false));
        button("Manage",left+178,73,72,()->{mode=mode==0?1:0;page=0;rebuild();});
        if(mode==0) {
            for(Goal g:app.settings.presets.get(app.settings.activePreset))goalRow(app.settings.presets.get(app.settings.activePreset),g,false);
            if(app.settings.presets.get(app.settings.activePreset).isEmpty())add(null,"Make it your own","Add the exact supplies you prefer to carry.",null);
        } else {
            add(null,"New preset","Create an empty reusable checklist",()->minecraft.gui.setScreen(new NameScreen(this,"",name->{
                if(app.settings.presets.size()>=32||app.settings.presets.containsKey(name))return false;
                app.settings.presets.put(name,new ArrayList<>());app.settings.activePreset=name;app.changed();mode=0;return true;
            })));
            add(null,"Duplicate preset","Start from the current checklist",()->minecraft.gui.setScreen(new NameScreen(this,app.settings.activePreset+" copy",name->{
                if(app.settings.presets.size()>=32||app.settings.presets.containsKey(name))return false;
                app.settings.presets.put(name,new ArrayList<>(app.settings.presets.get(app.settings.activePreset)));app.settings.activePreset=name;app.changed();mode=0;return true;
            })));
            add(null,"Rename preset","Keep its supplies and choose a new name",()->minecraft.gui.setScreen(new NameScreen(this,app.settings.activePreset,name->{
                if(!name.equals(app.settings.activePreset)&&app.settings.presets.containsKey(name))return false;
                var list=app.settings.presets.remove(app.settings.activePreset);app.settings.presets.put(name,list);app.settings.activePreset=name;app.changed();return true;
            })));
            if(app.settings.presets.size()>1)add(null,"Delete current preset","Requires confirmation",()->minecraft.gui.setScreen(new net.minecraft.client.gui.screens.ConfirmScreen(yes->{
                if(yes){app.settings.presets.remove(app.settings.activePreset);app.settings.activePreset=app.settings.presets.keySet().iterator().next();app.changed();}
                minecraft.gui.setScreen(this);
            },Component.literal("Delete "+app.settings.activePreset+"?"),Component.literal("This removes the saved checklist."))));
        }
    }
    private void inventory() {
        subtitle=app.freeSlots+" / 36 slots free • "+(app.freeSlots<=app.settings.nearFullSlots?"Nearly full":"Room to explore");
        button(new String[]{"Carried items","Suggestions","Favourites"}[mode%3],left+10,73,110,()->{mode=(mode+1)%3;page=0;rebuild();});
        button("Favourite item",left+126,73,112,()->minecraft.gui.setScreen(new ItemPickerScreen(this,"Choose a favourite",id->true,g->{app.settings.favorites.add(g.item());app.changed();},new Goal("minecraft:diamond",1))));
        if(mode==1) {
            for(var a:app.advice)add(a.item(),ItemsView.name(a.item()),a.reason(),null);
            if(app.advice.isEmpty())add(null,"Nothing to suggest","Favourites and special items are excluded from surplus advice.",null);
        } else {
            var ids=mode==2?new TreeSet<>(app.settings.favorites):new TreeSet<>(app.counts.keySet());
            for(String id:ids) {
                boolean fav=app.settings.favorites.contains(id);
                entries.add(new Row(id,ItemsView.name(id),"Carried: "+app.counts.getOrDefault(id,0)+(fav?" • protected favourite":" • click star to protect"),fav?HudRenderer.GOLD:HudRenderer.WHITE,null,fav?"★":"☆",()->{app.toggleFavorite(id);rebuild();}));
            }
            if(ids.isEmpty())add(null,"No items here yet","Favourites apply to all stacks of an item type.",null);
        }
    }
    private void settings() {
        subtitle="Saved locally • no server installation required";
        button("Edit HUD",left+10,73,100,()->minecraft.gui.setScreen(new HudEditorScreen(app,this)));
        add(null,"HUD: "+(app.settings.hud?"On":"Off"),"Toggle all widgets; also available as a keybind",()->{app.settings.hud=!app.settings.hud;change();});
        add(null,"Favourite drop protection: "+(app.settings.protectFavorites?"On":"Off"),"Blocks manual drops; does not protect from death or server actions",()->{app.settings.protectFavorites=!app.settings.protectFavorites;change();});
        add(null,"Nearly full at "+app.settings.nearFullSlots+" free slots","Click to cycle 0–12 free slots",()->{app.settings.nearFullSlots=(app.settings.nearFullSlots+1)%13;change();});
        for(String name:Settings.WIDGET_NAMES) {
            var widget=app.settings.widgets.get(name);
            add(null,name+": "+(widget.enabled?"Shown":"Hidden"),"Toggle this HUD widget",()->{widget.enabled=!widget.enabled;change();});
        }
        add(null,"Keybindings","Options → Controls → Key Binds → Smart Survival",null);
        if(!app.store.warning().isEmpty())add(null,"Save status",app.store.warning(),null);
    }
    private void goalRow(List<Goal> list,Goal goal,boolean plan) {
        int have=app.counts.getOrDefault(goal.item(),0);
        String detail=have+" carried / "+goal.amount()+" goal • "+goal.missing(have)+" missing";
        entries.add(new Row(goal.item(),ItemsView.name(goal.item())+" × "+goal.amount(),detail,goal.missing(have)==0?HudRenderer.GREEN:HudRenderer.WHITE,()->pick(list,goal,plan),"Remove",()->{list.remove(goal);change();}));
        if(plan) {
            var routes=app.catalog.planner().recipes(goal.item());
            if(!routes.isEmpty()) {
                String choice=app.settings.recipeChoices.get(goal.item());
                int selected=-1;for(int i=0;i<routes.size();i++)if(routes.get(i).id().equals(choice))selected=i;
                final int next=selected+1;
                String label=selected<0?"Automatic":routes.get(selected).station()+" • output "+routes.get(selected).count()+" • route "+(selected+1);
                String materials=selected<0?"Uses available ingredients; click to choose a recipe":routes.get(selected).ingredients().stream().map(in->ItemsView.name(in.alternatives().getFirst())+(in.alternatives().size()>1?" / alternatives":"")).reduce((a,b)->a+", "+b).orElse("");
                add(null,"Recipe: "+label,materials,()->{if(next>=routes.size())app.settings.recipeChoices.remove(goal.item());else app.settings.recipeChoices.put(goal.item(),routes.get(next).id());change();});
            } else add(null,"Recipe unavailable","This target is shown as a material until its recipe is known.",null);
        }
    }
    private void pick(List<Goal> list,Goal existing,boolean craftable) {
        minecraft.gui.setScreen(new ItemPickerScreen(this,craftable?"Add a crafting target":"Choose an item",
            id->!craftable||!app.catalog.planner().recipes(id).isEmpty(),
            goal->{if(existing!=null)list.remove(existing);SmartSurvivalClient.upsert(list,goal);app.changed();},existing));
    }
    private void add(String item,String title,String detail,Runnable action) {entries.add(new Row(item,title,detail,HudRenderer.WHITE,action,null,null));}
    private void change(){app.changed();app.refresh(minecraft);rebuild();}
    @Override public void tick(){if(revision!=app.revision)rebuild();}
    @Override public boolean mouseScrolled(double x,double y,double hx,double vy) {
        if(y>95&&y<height-40){page=Math.max(0,page+(vy<0?1:-1));rebuild();return true;}return super.mouseScrolled(x,y,hx,vy);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        frame(g,subtitle);super.extractRenderState(g,mx,my,delta);
        for(int i=0;i<visible.size();i++) {
            Row r=visible.get(i);int y=100+i*34;
            if(r.item!=null)g.item(ItemsView.stack(r.item),left+10,y+2);
            if(r.action==null)g.text(font,fit(r.title,panelWidth-(r.secondary==null?40:94)),left+30,y+3,r.color,false);
            g.text(font,fit(r.detail,panelWidth-40),left+30,y+23,HudRenderer.MUTED,false);
            if(my>=y+21&&my<y+33&&mx>=left&&mx<left+panelWidth)g.setTooltipForNextFrame(Component.literal(r.detail),mx,my);
        }
        int perPage=Math.max(1,(height-146)/34);
        g.text(font,(page+1)+" / "+Math.max(1,(entries.size()+perPage-1)/perPage),left+80,height-29,HudRenderer.MUTED,false);
    }
}
