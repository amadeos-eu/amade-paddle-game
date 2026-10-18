package eu.amadeos.games.paddle.ui.main;

import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.input.Keyboard;
import eu.amadeos.gameengine.score.HighScores;
import eu.amadeos.gameengine.score.PlayerName;
import eu.amadeos.gameengine.score.Ranking;
import eu.amadeos.games.paddle.bootstrap.PaddleModule;
import eu.amadeos.games.paddle.domain.Difficulty;
import eu.amadeos.games.paddle.domain.MatchResult;
import eu.amadeos.games.paddle.domain.Mode;
import eu.amadeos.games.paddle.repository.PaddleSettings;
import eu.amadeos.games.paddle.ui.game.Arena;
import eu.amadeos.games.paddle.ui.game.PaddleGame;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.awt.event.KeyEvent;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.SplittableRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/** The real window, game and loop: keys on the canvas reach the game, F2 reaches the menu. */
class PaddleWindowTest {
    @TempDir
    Path dir;

    private PaddleWindow window;
    private PaddleGame game;
    private final AtomicReference<String> started = new AtomicReference<>();

    @BeforeEach
    void open() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        Messages msg = Messages.load(PaddleModule.class, PaddleModule.MESSAGES, Locale.ENGLISH);
        PaddleSettings settings = new PaddleSettings(dir.resolve("paddle.properties"));
        HighScores scores = new HighScores(dir.resolve("scores"), Ranking.LOWER_WINS);
        Keyboard keyboard = new Keyboard();
        onEdt(() -> {
            game = new PaddleGame(keyboard, msg, settings, new SplittableRandom(1),
                    new eu.amadeos.commons.swing.EdtExecutor());
            window = new PaddleWindow(msg, scores, new PlayerName(dir, "tester"), game, keyboard);
            // The real game, recording which match the presenter starts.
            Arena recorder = new Arena() {
                @Override
                public void showTitle() {
                    game.showTitle();
                }

                @Override
                public void startMatch(Mode mode, Difficulty d) {
                    started.set(mode + " " + d);
                    game.startMatch(mode, d);
                }

                @Override
                public void setDifficulty(Difficulty d) {
                    game.setDifficulty(d);
                }

                @Override
                public void pause() {
                    game.pause();
                }

                @Override
                public void onMatchOver(Consumer<MatchResult> listener) {
                    game.onMatchOver(listener);
                }
            };
            PaddlePresenter presenter = new PaddlePresenter(window, recorder, msg, settings, scores);
            window.bind(presenter);
            presenter.start();
            window.setVisible(true);
            game.start(window.canvas());
        });
    }

    @AfterEach
    void close() throws Exception {
        if (game != null) game.stop();
        if (window != null) onEdt(window::dispose);
    }

    private static void onEdt(Runnable code) throws Exception {
        try {
            SwingUtilities.invokeAndWait(code);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Error error) throw error;
            throw (Exception) e.getCause();
        }
    }

    private void key(int id, int code) throws Exception {
        onEdt(() -> window.canvas().dispatchEvent(new KeyEvent(window.canvas(), id, System.currentTimeMillis(), 0,
                code, KeyEvent.CHAR_UNDEFINED)));
    }

    @Test
    void f2OnTheCanvasStartsAGameThroughTheMenu() throws Exception {
        key(KeyEvent.KEY_PRESSED, KeyEvent.VK_F2);
        key(KeyEvent.KEY_RELEASED, KeyEvent.VK_F2);
        long deadline = System.currentTimeMillis() + 3000;
        while (started.get() == null && System.currentTimeMillis() < deadline) Thread.sleep(20);
        assertTrue("ONE_PLAYER NORMAL".equals(started.get()), "started: " + started.get());
    }
}
