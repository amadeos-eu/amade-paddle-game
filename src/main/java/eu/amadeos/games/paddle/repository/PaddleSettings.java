package eu.amadeos.games.paddle.repository;

import com.google.inject.Inject;
import eu.amadeos.commons.config.AppDirectories;
import eu.amadeos.commons.config.PropertiesFile;
import eu.amadeos.games.paddle.domain.Difficulty;

import javax.swing.KeyStroke;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * The computer's difficulty, the sound and the keys, in {@code ~/.config/amadeos/paddle/paddle.properties}. Keys are
 * written as Java names them, several separated by spaces, e.g. {@code keys.left-up=W} or
 * {@code keys.right-up=UP NUMPAD8}.
 */
public final class PaddleSettings {
    private static final String DIFFICULTY = "difficulty";
    private static final String SOUND = "sound";

    private final Path file;

    @Inject
    public PaddleSettings(AppDirectories directories) {
        this(directories.configFile("paddle.properties"));
    }

    public PaddleSettings(Path file) {
        this.file = file;
    }

    public Difficulty difficulty() {
        return Difficulty.ofId(PropertiesFile.load(file).getProperty(DIFFICULTY, "")).orElse(Difficulty.NORMAL);
    }

    public void setDifficulty(Difficulty difficulty) {
        set(DIFFICULTY, difficulty.id());
    }

    /** Sound effects on, unless switched off. */
    public boolean sound() {
        return !"off".equals(PropertiesFile.load(file).getProperty(SOUND, "on"));
    }

    public void setSound(boolean on) {
        set(SOUND, on ? "on" : "off");
    }

    private void set(String key, String value) {
        Properties properties = PropertiesFile.load(file);
        properties.setProperty(key, value);
        PropertiesFile.store(file, properties, "amade-paddle-game settings");
    }

    /**
     * The keys of an action, e.g. {@code keys("left-up", KeyEvent.VK_W)}: those in the file if it names any
     * it understands, else the defaults.
     */
    public int[] keys(String action, int... defaults) {
        String names = PropertiesFile.load(file).getProperty("keys." + action, "").strip();
        List<Integer> codes = new ArrayList<>();
        for (String name : names.split("\\s+")) {
            if (name.isEmpty()) continue;
            KeyStroke stroke = KeyStroke.getKeyStroke(name.toUpperCase(java.util.Locale.ROOT));
            if (stroke != null && stroke.getKeyCode() != 0) codes.add(stroke.getKeyCode());
        }
        return codes.isEmpty() ? defaults : codes.stream().mapToInt(Integer::intValue).toArray();
    }
}
