package dev.smartsurvival.core;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class SettingsStoreTest {
    @TempDir Path dir;
    @Test void roundTripsAllUserData(){
        Path path=dir.resolve("settings.json");Settings s=new Settings();
        s.goals.add(new Goal("minecraft:stone",4096));s.plans.add(new Goal("minecraft:torch",128));
        s.favorites.add("minecraft:diamond_pickaxe");s.presets.put("Expedition",new ArrayList<>(List.of(new Goal("minecraft:bread",24))));
        s.activePreset="Expedition";s.widgets.get("Resources").x=.75;s.widgets.get("Resources").scale=1.4;
        s.recipeChoices.put("minecraft:torch","routeA");
        SettingsStore store=new SettingsStore(path);assertTrue(store.save(s));Settings loaded=store.load();
        assertEquals(s.goals,loaded.goals);assertEquals(s.plans,loaded.plans);assertEquals(s.presets,loaded.presets);
        assertEquals(s.favorites,loaded.favorites);assertEquals("Expedition",loaded.activePreset);
        assertEquals(.75,loaded.widgets.get("Resources").x);assertEquals(1.4,loaded.widgets.get("Resources").scale);
        assertEquals(s.recipeChoices,loaded.recipeChoices);
    }
    @Test void backsUpCorruptSettingsBeforeSavingDefaults()throws Exception{
        Path path=dir.resolve("settings.json");Files.writeString(path,"{broken");
        SettingsStore store=new SettingsStore(path);Settings defaults=store.load();
        assertFalse(store.warning().isEmpty());assertEquals("Mining",defaults.activePreset);
        try(var files=Files.list(dir)){assertTrue(files.anyMatch(p->p.getFileName().toString().contains(".corrupt-")));}
        assertEquals("{broken",Files.readString(path));assertTrue(store.save(defaults));
    }
    @Test void neverOverwritesFutureSchema()throws Exception{
        Path path=dir.resolve("settings.json");String data="{\"schema\":999,\"future\":true}";Files.writeString(path,data);
        SettingsStore store=new SettingsStore(path);assertFalse(store.save(store.load()));assertEquals(data,Files.readString(path));
    }
    @Test void normalizesInvalidWidgetSettings()throws Exception{
        Path path=dir.resolve("settings.json");
        Files.writeString(path,"{\"schema\":1,\"widgets\":{\"Resources\":{\"x\":-10,\"y\":50,\"scale\":20,\"opacity\":-2}},\"goals\":null,\"presets\":null}");
        Settings s=new SettingsStore(path).load();
        assertEquals(0,s.widgets.get("Resources").x);assertEquals(1,s.widgets.get("Resources").y);
        assertEquals(1.75,s.widgets.get("Resources").scale);assertEquals(.15,s.widgets.get("Resources").opacity);
        assertEquals(5,s.widgets.size());assertTrue(s.goals.isEmpty());assertFalse(s.presets.isEmpty());
    }
    @Test void keepsLastGoodBackup()throws Exception{
        Path path=dir.resolve("settings.json");SettingsStore store=new SettingsStore(path);Settings s=new Settings();
        assertTrue(store.save(s));String original=Files.readString(path);s.hud=false;assertTrue(store.save(s));
        assertEquals(original,Files.readString(dir.resolve("settings.json.bak")));
    }
    @Test void reportsUnwritableDestinationWithoutThrowing()throws Exception{
        Path parent=dir.resolve("not-directory");Files.writeString(parent,"x");
        SettingsStore store=new SettingsStore(parent.resolve("settings.json"));assertFalse(store.save(new Settings()));assertFalse(store.warning().isBlank());
    }
}
