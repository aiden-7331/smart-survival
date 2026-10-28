package dev.smartsurvival.core;

import com.google.gson.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import static java.nio.file.StandardCopyOption.*;

public final class SettingsStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    private String warning = "";
    private boolean readOnly;
    public SettingsStore(Path file) { this.file = file; }
    public String warning() { return warning; }
    public synchronized Settings load() {
        if (!Files.exists(file)) return new Settings();
        try {
            if (Files.size(file) > 2_000_000) throw new IOException("Settings file exceeds 2 MB");
            String text = Files.readString(file);
            JsonObject root = JsonParser.parseString(text).getAsJsonObject();
            if (root.has("schema") && root.get("schema").getAsInt() != 1) {
                readOnly = true;
                warning = "Settings were made by a different version. Saving is disabled to preserve them.";
                return new Settings();
            }
            Settings result = GSON.fromJson(root, Settings.class);
            result.normalize();
            return result;
        } catch (IOException | RuntimeException ex) {
            try {
                Path backup = file.resolveSibling(file.getFileName() + ".corrupt-" + Instant.now().toEpochMilli());
                Files.copy(file, backup);
                warning = "Could not read settings. Defaults loaded; the original was backed up.";
            } catch (IOException backupError) {
                readOnly = true;
                warning = "Could not read or back up settings. Saving is disabled.";
            }
            return new Settings();
        }
    }
    public synchronized boolean save(Settings value) {
        if (readOnly) return false;
        Path temp = null;
        try {
            value.normalize();
            Files.createDirectories(file.toAbsolutePath().getParent());
            temp = Files.createTempFile(file.toAbsolutePath().getParent(), "smart-survival-", ".tmp");
            byte[] bytes = GSON.toJson(value).getBytes(StandardCharsets.UTF_8);
            try (FileChannel channel = FileChannel.open(temp, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            if (Files.exists(file)) Files.copy(file, file.resolveSibling(file.getFileName() + ".bak"), REPLACE_EXISTING);
            try { Files.move(temp, file, ATOMIC_MOVE, REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temp, file, REPLACE_EXISTING); }
            return true;
        } catch (IOException | RuntimeException ex) {
            warning = "Settings could not be saved. Check config folder permissions and disk space.";
            return false;
        } finally {
            if (temp != null) try { Files.deleteIfExists(temp); } catch (IOException ignored) {}
        }
    }
}
