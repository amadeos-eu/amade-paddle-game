package eu.amadeos.games.paddle.ui.game;

import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.gameengine.input.Keyboard;
import eu.amadeos.games.paddle.bootstrap.PaddleModule;
import eu.amadeos.games.paddle.domain.Court;
import eu.amadeos.games.paddle.domain.Difficulty;
import eu.amadeos.games.paddle.domain.Match;
import eu.amadeos.games.paddle.domain.MatchResult;
import eu.amadeos.games.paddle.domain.Mode;
import eu.amadeos.games.paddle.domain.Side;
import eu.amadeos.games.paddle.repository.PaddleSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The game without a window and without the loop thread: the test presses keys on the keyboard and calls
 * the steps itself, so everything happens in order on the test thread.
 */
class PaddleGameTest {
    private static final double STEP = 1.0 / PaddleGame.UPDATES_PER_SECOND;

    @TempDir
    Path dir;

    /** The keyboard's clock: one step of the game moves it forward by one step's nanoseconds. */
    private final long[] now = {0};
    private final Keyboard keyboard = new Keyboard(() -> now[0]);
    private final List<MatchResult> results = new ArrayList<>();
    private PaddleGame game;

    @BeforeEach
    void create() {
        Messages msg = Messages.load(PaddleModule.class, PaddleModule.MESSAGES, Locale.ENGLISH);
        game = new PaddleGame(keyboard, msg, new PaddleSettings(dir.resolve("paddle.properties")),
                new SplittableRandom(4), new Mixer(), Runnable::run);
        game.onMatchOver(results::add);
    }

    /** A short press: down and up within one step, then a few steps so the release takes effect. */
    private void tap(int key) {
        keyboard.pressed(key, now[0]);
        keyboard.released(key, now[0] + 1_000_000);
        steps(4);
    }

    private void steps(int count) {
        for (int i = 0; i < count; i++) {
            now[0] += (long) (STEP * 1e9);
            game.update(STEP);
        }
    }

    private Match match() {
        return assertInstanceOf(PaddleGame.MatchScene.class, game.top()).match();
    }

    // --8<-- [start:test-keys]
    @Test
    void oneStartsAGameAndTheArrowKeysMoveTheRightPaddle() {
        assertInstanceOf(PaddleGame.TitleScene.class, game.top());
        tap(KeyEvent.VK_1);
        double before = match().paddle(Side.RIGHT).y();
        keyboard.pressed(KeyEvent.VK_UP, now[0]);
        steps(30);                                              // a quarter of a second
        assertEquals(before - Court.PADDLE_SPEED * 0.25, match().paddle(Side.RIGHT).y(), 1e-6);
    }
    // --8<-- [end:test-keys]

    @Test
    void inTwoPlayerModeWAndSMoveTheLeftPaddle() {
        tap(KeyEvent.VK_2);
        double before = match().paddle(Side.LEFT).y();
        keyboard.pressed(KeyEvent.VK_S, now[0]);
        steps(30);
        assertTrue(match().paddle(Side.LEFT).y() > before);
    }

    // --8<-- [start:test-pause]
    @Test
    void pauseFreezesTheMatchAndShowsItUnderneath() {
        game.startMatch(Mode.ONE_PLAYER, Difficulty.NORMAL);
        steps(200);                                             // the serve is done, the ball flies
        tap(KeyEvent.VK_P);
        assertInstanceOf(PaddleGame.PauseScene.class, game.top());
        assertEquals(2, game.sceneCount(), "the match lies underneath");
        tap(KeyEvent.VK_P);
        Match match = match();
        double x = match.ball().x();
        steps(1);
        assertTrue(match.ball().x() != x, "moving again");
    }
    // --8<-- [end:test-pause]

    @Test
    void thePresenterCanPauseOnlyAMatch() {
        game.pause();
        assertInstanceOf(PaddleGame.TitleScene.class, game.top());
        game.startMatch(Mode.TWO_PLAYERS, Difficulty.EASY);
        game.pause();
        assertInstanceOf(PaddleGame.PauseScene.class, game.top());
    }

    @Test
    void theEndOfAMatchIsReportedAndEnterLeadsBack() {
        game.startMatch(Mode.ONE_PLAYER, Difficulty.HARD);
        Match match = match();
        match.setScore(3, 10);
        match.placePaddle(Side.LEFT, 0);
        match.placeBall(40, 300, -600, 0);                      // past the computer
        steps(20);
        assertEquals(List.of(new MatchResult(Mode.ONE_PLAYER, Difficulty.HARD, Side.RIGHT, 3, 11)), results);
        assertInstanceOf(PaddleGame.OverScene.class, game.top());
        tap(KeyEvent.VK_ENTER);
        assertInstanceOf(PaddleGame.TitleScene.class, game.top());
    }

    @Test
    void theTitleKeepsPlayingItself() {
        steps(120 * 20);
        assertInstanceOf(PaddleGame.TitleScene.class, game.top());
    }

    @Test
    void everySceneDrawsInLogicalCoordinates() {
        BufferedImage image = new BufferedImage((int) Court.WIDTH, (int) Court.HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        game.render(g, 0.5);                                    // title
        game.startMatch(Mode.ONE_PLAYER, Difficulty.EASY);
        steps(200);
        game.pause();
        game.render(g, 0.5);                                    // match with the pause screen over it
        g.dispose();
        assertTrue((image.getRGB(320, 10) & 0xFFFFFF) != 0, "something was drawn");
    }

    @Test
    void keysCanBeChangedInTheSettings() throws Exception {
        java.nio.file.Files.writeString(dir.resolve("paddle.properties"), "keys.right-up=I\n");
        Messages msg = Messages.load(PaddleModule.class, PaddleModule.MESSAGES, Locale.ENGLISH);
        PaddleGame custom = new PaddleGame(keyboard, msg, new PaddleSettings(dir.resolve("paddle.properties")),
                new SplittableRandom(4), new Mixer(), Runnable::run);
        keyboard.pressed(KeyEvent.VK_I, now[0]);
        keyboard.poll();
        assertTrue(custom.controls().isDown(PaddleGame.Action.RIGHT_UP));
    }
}
