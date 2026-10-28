package dev.smartsurvival.core;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PlannerTest {
    private static String id(String n){return "minecraft:"+n;}
    private static Goal goal(String n,int count){return new Goal(id(n),count);}
    private static Planner.Ingredient in(int count,String...names){return new Planner.Ingredient(Arrays.stream(names).map(PlannerTest::id).toList(),count);}
    private static Planner.Recipe recipe(String name,String output,int n,Planner.Ingredient...ins){return new Planner.Recipe(name,id(output),n,List.of(ins),"Crafting table");}
    private static Planner.Result plan(List<Planner.Recipe> recipes,List<Goal> goals,Map<String,Integer> inv){return new Planner(recipes).plan(goals,inv,Map.of());}
    @Test void recursivelyUsesOutputQuantities() {
        var r=plan(List.of(recipe("planks","planks",4,in(1,"log")),recipe("sticks","stick",4,in(2,"planks"))),List.of(goal("stick",9)),Map.of());
        assertEquals(Map.of(id("log"),2L),r.missing());
        assertEquals(3,r.steps().getLast().crafts());
    }
    @Test void consumesIntermediateStockBeforeRawMaterials(){
        var r=plan(List.of(recipe("planks","planks",4,in(1,"log")),recipe("sticks","stick",4,in(2,"planks"))),List.of(goal("stick",8)),Map.of(id("planks"),3));
        assertEquals(Map.of(id("log"),1L),r.missing());
    }
    @Test void consumesFinishedOutputBeforeCrafting(){
        var r=plan(List.of(recipe("planks","planks",4,in(1,"log"))),List.of(goal("planks",12)),Map.of(id("planks"),8,id("log"),1));
        assertTrue(r.missing().isEmpty());assertEquals(1,r.steps().getLast().crafts());
    }
    @Test void sharesStockAcrossGoals(){
        var r=plan(List.of(recipe("a","a",1,in(2,"planks")),recipe("b","b",1,in(2,"planks"))),List.of(goal("a",1),goal("b",1)),Map.of(id("planks"),3));
        assertEquals(Map.of(id("planks"),1L),r.missing());
    }
    @Test void reservesFinishedGoalsBeforeConsumingIngredients(){
        var r=plan(List.of(recipe("sticks","stick",4,in(2,"planks"))),List.of(goal("stick",4),goal("planks",2)),Map.of(id("planks"),2));
        assertEquals(Map.of(id("planks"),2L),r.missing());
    }
    @Test void reusesSurplusFromEarlierCrafts(){
        var r=plan(List.of(recipe("planks","planks",4,in(1,"log")),recipe("a","a",1,in(2,"planks")),recipe("b","b",1,in(2,"planks"))),List.of(goal("a",1),goal("b",1)),Map.of());
        assertEquals(Map.of(id("log"),1L),r.missing());
    }
    @Test void mixesInventoryAlternatives(){
        var r=plan(List.of(recipe("table","table",1,in(4,"oak_planks","birch_planks"))),List.of(goal("table",1)),Map.of(id("oak_planks"),2,id("birch_planks"),2));
        assertTrue(r.missing().isEmpty());
    }
    @Test void choosesRecipeWithAvailableIngredients(){
        var recipes=List.of(recipe("a","torch",4,in(1,"coal"),in(1,"stick")),recipe("b","torch",4,in(1,"charcoal"),in(1,"stick")));
        var r=plan(recipes,List.of(goal("torch",4)),Map.of(id("charcoal"),1,id("stick"),1));
        assertTrue(r.missing().isEmpty());assertEquals("b",r.steps().getLast().recipe());
    }
    @Test void respectsExplicitRecipeChoice(){
        var recipes=List.of(recipe("a","torch",4,in(1,"coal")),recipe("b","torch",4,in(1,"charcoal")));
        var r=new Planner(recipes).plan(List.of(goal("torch",4)),Map.of(id("charcoal"),1),Map.of(id("torch"),"a"));
        assertEquals(Map.of(id("coal"),1L),r.missing());
    }
    @Test void cyclesTerminateWithoutCreatingFreeResources(){
        var r=plan(List.of(recipe("a","a",1,in(1,"b")),recipe("b","b",1,in(1,"a"))),List.of(goal("a",2)),Map.of());
        assertFalse(r.missing().isEmpty());assertFalse(r.notes().isEmpty());
    }
    @Test void packingUsesPrimitiveInsteadOfCircularUnpacking(){
        var r=plan(List.of(recipe("pack","block",1,in(9,"ingot")),recipe("unpack","ingot",9,in(1,"block"))),List.of(goal("block",1)),Map.of());
        assertEquals(Map.of(id("ingot"),9L),r.missing());
    }
    @Test void unpacksCarriedBlocks(){
        var r=plan(List.of(recipe("pack","block",1,in(9,"ingot")),recipe("unpack","ingot",9,in(1,"block"))),List.of(goal("ingot",3)),Map.of(id("block"),1));
        assertTrue(r.missing().isEmpty());
    }
    @Test void largeQuantitiesAreBatched(){
        var r=plan(List.of(recipe("planks","planks",4,in(1,"log"))),List.of(goal("planks",1_000_000)),Map.of());
        assertEquals(Map.of(id("log"),250_000L),r.missing());assertFalse(r.limited());
    }
    @Test void unknownRecipeIsAnExplicitMaterial(){
        assertEquals(Map.of(id("unknown"),4L),plan(List.of(),List.of(goal("unknown",4)),Map.of()).missing());
    }
}
