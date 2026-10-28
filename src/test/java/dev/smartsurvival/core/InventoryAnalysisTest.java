package dev.smartsurvival.core;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class InventoryAnalysisTest {
    @Test void countsAllStacksAndIgnoresEmpty(){
        assertEquals(Map.of("minecraft:stone",70),InventoryAnalysis.count(List.of(
            new InventoryAnalysis.Stack("minecraft:stone",64,64,false,false),
            new InventoryAnalysis.Stack("minecraft:stone",6,64,false,false),
            new InventoryAnalysis.Stack("minecraft:dirt",0,64,false,false))));
    }
    @Test void missingNeverGoesNegative(){
        Goal g=new Goal("minecraft:stone",200);assertEquals(0,g.missing(220));assertEquals(136,g.missing(64));assertEquals(200,g.missing(-1));
    }
    @Test void rejectsInvalidGoals(){
        assertThrows(IllegalArgumentException.class,()->new Goal("bad id",1));
        assertThrows(IllegalArgumentException.class,()->new Goal("minecraft:stone",0));
        assertThrows(IllegalArgumentException.class,()->new Goal("minecraft:stone",1_000_001));
    }
    @Test void favoritesAndNeededToolsAreNeverSuggested(){
        var stacks=List.of(new InventoryAnalysis.Stack("minecraft:iron_pickaxe",1,1,true,false),new InventoryAnalysis.Stack("minecraft:iron_pickaxe",1,1,true,false));
        assertEquals(1,InventoryAnalysis.suggest(stacks,Set.of(),Set.of()).size());
        assertTrue(InventoryAnalysis.suggest(stacks,Set.of("minecraft:iron_pickaxe"),Set.of()).isEmpty());
        assertTrue(InventoryAnalysis.suggest(stacks,Set.of(),Set.of("minecraft:iron_pickaxe")).isEmpty());
    }
    @Test void suggestsOnlyActuallyMergeablePlainStacks(){
        var stacks=List.of(new InventoryAnalysis.Stack("minecraft:stone",16,64,false,false),new InventoryAnalysis.Stack("minecraft:stone",16,64,false,false),new InventoryAnalysis.Stack("minecraft:stone",64,64,false,true));
        assertEquals(1,InventoryAnalysis.suggest(stacks,Set.of(),Set.of()).size());
        assertTrue(InventoryAnalysis.suggest(stacks,Set.of(),Set.of()).getFirst().reason().contains("1 slot"));
    }
}
