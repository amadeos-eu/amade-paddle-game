package eu.amadeos.games.paddle.repository;

import eu.amadeos.games.paddle.domain.Difficulty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PaddleSettingsTest {
    @TempDir
    Path dir;

    @Test
    void normalAtFirstThenAsChosen() {
        Path file = dir.resolve("paddle.properties");
        assertEquals(Difficulty.NORMAL, new PaddleSettings(file).difficulty());
        new PaddleSettings(file).setDifficulty(Difficulty.EASY);
        assertEquals(Difficulty.EASY, new PaddleSettings(file).difficulty());
    }

    @Test
    void keysByTheirJavaNames() throws Exception {
        Path file = dir.resolve("paddle.properties");
        Files.writeString(file, "keys.right-up=up numpad8\nkeys.pause=NO_SUCH_KEY\n");
        PaddleSettings settings = new PaddleSettings(file);
        assertArrayEquals(new int[]{KeyEvent.VK_UP, KeyEvent.VK_NUMPAD8}, settings.keys("right-up", KeyEvent.VK_I));
        assertArrayEquals(new int[]{KeyEvent.VK_P}, settings.keys("pause", KeyEvent.VK_P), "unknown names: the default");
        assertArrayEquals(new int[]{KeyEvent.VK_W}, settings.keys("left-up", KeyEvent.VK_W));
    }
}
