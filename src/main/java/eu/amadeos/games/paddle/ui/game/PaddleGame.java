package eu.amadeos.games.paddle.ui.game;

import com.google.inject.Inject;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.commons.swing.EdtExecutor;
import eu.amadeos.gameengine.input.Controls;
import eu.amadeos.gameengine.input.Keyboard;
import eu.amadeos.gameengine.loop.GameLoop;
import eu.amadeos.gameengine.scene.Scene;
import eu.amadeos.gameengine.scene.SceneStack;
import eu.amadeos.gameengine.swing.GameCanvas;
import eu.amadeos.games.paddle.domain.ComputerPlayer;
import eu.amadeos.games.paddle.domain.Difficulty;
import eu.amadeos.games.paddle.domain.Match;
import eu.amadeos.games.paddle.domain.MatchResult;
import eu.amadeos.games.paddle.domain.Mode;
import eu.amadeos.games.paddle.domain.Side;
import eu.amadeos.games.paddle.repository.PaddleSettings;

import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;

/**
 * The running game: the loop, the controls and the scenes (title with the computer playing itself, the
 * match, the pause screen, the end of a match). Everything here runs on the game loop thread, except the
 * {@link Arena} methods, which only post commands to it.
 */
public final class PaddleGame implements Arena {
    /** The actions of the game; which keys trigger them comes from the settings. */
    public enum Action { LEFT_UP, LEFT_DOWN, RIGHT_UP, RIGHT_DOWN, PAUSE, ONE_PLAYER, TWO_PLAYERS, CONFIRM }

    /** Steps of the rules per second. */
    static final int UPDATES_PER_SECOND = 120;
    static final int FRAMES_PER_SECOND = 60;

    private final Keyboard keyboard;
    private final Controls<Action> controls;
    private final Messages msg;
    private final RandomGenerator random;
    private final Executor ui;
    private final SceneStack scenes = new SceneStack();
    private volatile Consumer<MatchResult> onMatchOver = result -> { };
    private volatile Difficulty difficulty = Difficulty.NORMAL;
    private GameLoop loop;

    @Inject
    public PaddleGame(Keyboard keyboard, Messages msg, PaddleSettings settings, RandomGenerator random,
                      EdtExecutor ui) {
        this(keyboard, msg, settings, random, (Executor) ui);
    }

    PaddleGame(Keyboard keyboard, Messages msg, PaddleSettings settings, RandomGenerator random, Executor ui) {
        this.keyboard = keyboard;
        this.msg = msg;
        this.random = random;
        this.ui = ui;
        // --8<-- [start:controls]
        controls = new Controls<>(Action.class, keyboard)
                .bind(Action.LEFT_UP, settings.keys("left-up", KeyEvent.VK_W))
                .bind(Action.LEFT_DOWN, settings.keys("left-down", KeyEvent.VK_S))
                .bind(Action.RIGHT_UP, settings.keys("right-up", KeyEvent.VK_UP))
                .bind(Action.RIGHT_DOWN, settings.keys("right-down", KeyEvent.VK_DOWN))
                .bind(Action.PAUSE, settings.keys("pause", KeyEvent.VK_P, KeyEvent.VK_ESCAPE))
                .bind(Action.ONE_PLAYER, KeyEvent.VK_1, KeyEvent.VK_NUMPAD1, KeyEvent.VK_ENTER)
                .bind(Action.TWO_PLAYERS, KeyEvent.VK_2, KeyEvent.VK_NUMPAD2)
                .bind(Action.CONFIRM, KeyEvent.VK_ENTER, KeyEvent.VK_SPACE);
        // --8<-- [end:controls]
        scenes.push(new TitleScene());
    }

    // --8<-- [start:start]
    /** Starts the loop: it steps the scenes 120 times a second and draws them on the canvas about 60 times. */
    public void start(GameCanvas canvas) {
        loop = new GameLoop(UPDATES_PER_SECOND, FRAMES_PER_SECOND, this::update, canvas::render);
        loop.start();
    }

    public void stop() {
        if (loop != null) loop.stop();
    }

    /** One step: read the keys, then let the top scene act. */
    void update(double seconds) {
        keyboard.poll();
        scenes.update(seconds);
    }

    /** Draws all scenes, bottom first, in logical coordinates. */
    public void render(Graphics2D g, double alpha) {
        scenes.render(g, alpha);
    }
    // --8<-- [end:start]

    // ---- Arena: called on the Swing thread, carried out on the loop thread -----------------------

    // --8<-- [start:post]
    @Override
    public void showTitle() {
        run(() -> scenes.replace(new TitleScene()));
    }

    @Override
    public void startMatch(Mode mode, Difficulty newDifficulty) {
        run(() -> scenes.replace(new MatchScene(mode, newDifficulty)));
    }

    @Override
    public void setDifficulty(Difficulty newDifficulty) {
        difficulty = newDifficulty;
    }

    @Override
    public void pause() {
        run(() -> {
            if (scenes.top() instanceof MatchScene) scenes.push(new PauseScene());
        });
    }

    @Override
    public void onMatchOver(Consumer<MatchResult> listener) {
        onMatchOver = listener;
    }

    /** On the loop thread when it runs; directly before the loop starts and in tests. */
    private void run(Runnable command) {
        if (loop != null && loop.running()) loop.post(command);
        else command.run();
    }
    // --8<-- [end:post]

    // ---- for tests -------------------------------------------------------------------------------

    Scene top() {
        return scenes.top();
    }

    int sceneCount() {
        return scenes.size();
    }

    Controls<Action> controls() {
        return controls;
    }

    // ---- the scenes ------------------------------------------------------------------------------

    // --8<-- [start:title]
    /** The computer plays itself; 1 or Enter starts a game against it, 2 a game for two. */
    final class TitleScene implements Scene {
        private Match demo = new Match(random);
        private final ComputerPlayer left = new ComputerPlayer(Side.LEFT, Difficulty.NORMAL, random);
        private final ComputerPlayer right = new ComputerPlayer(Side.RIGHT, Difficulty.NORMAL, random);

        @Override
        public void update(double seconds) {
            if (controls.wasPressed(Action.ONE_PLAYER)) {
                scenes.replace(new MatchScene(Mode.ONE_PLAYER, difficulty));
                return;
            }
            if (controls.wasPressed(Action.TWO_PLAYERS)) {
                scenes.replace(new MatchScene(Mode.TWO_PLAYERS, difficulty));
                return;
            }
            demo.update(seconds, left.move(demo, seconds), right.move(demo, seconds));
            if (demo.winner() != null) demo = new Match(random);
        }

        @Override
        public void render(Graphics2D g, double alpha) {
            CourtPainter.paint(g, demo, alpha);
            CourtPainter.veil(g);
            CourtPainter.text(g, msg.get("window.title"), 150, 64, true);
            CourtPainter.text(g, msg.get("title.one-player"), 230, 16, false);
            CourtPainter.text(g, msg.get("title.two-players"), 260, 16, false);
            CourtPainter.text(g, msg.get("title.pause"), 330, 14, false);
        }
    }
    // --8<-- [end:title]

    // --8<-- [start:match]
    /** A match: the player on the right, the left paddle played by the computer or a second player. */
    final class MatchScene implements Scene {
        private final Mode mode;
        private final Difficulty matchDifficulty;
        private final Match match = new Match(random);
        private final ComputerPlayer computer;

        MatchScene(Mode mode, Difficulty matchDifficulty) {
            this.mode = mode;
            this.matchDifficulty = matchDifficulty;
            this.computer = new ComputerPlayer(Side.LEFT, matchDifficulty, random);
        }

        @Override
        public void update(double seconds) {
            if (controls.wasPressed(Action.PAUSE)) {
                scenes.push(new PauseScene());
                return;
            }
            double leftMove = mode == Mode.TWO_PLAYERS
                    ? controls.axis(Action.LEFT_UP, Action.LEFT_DOWN)
                    : computer.move(match, seconds);
            double rightMove = controls.axis(Action.RIGHT_UP, Action.RIGHT_DOWN);
            match.update(seconds, leftMove, rightMove);
            if (match.winner() != null) {
                MatchResult result = new MatchResult(mode, matchDifficulty, match.winner(),
                        match.score(Side.LEFT), match.score(Side.RIGHT));
                scenes.replace(new OverScene(match, result));
                // The presenter lives on the Swing thread: hand the result over there.
                Consumer<MatchResult> listener = onMatchOver;
                ui.execute(() -> listener.accept(result));
            }
        }

        @Override
        public void render(Graphics2D g, double alpha) {
            CourtPainter.paint(g, match, alpha);
        }

        Match match() {
            return match;
        }
    }
    // --8<-- [end:match]

    /** Drawn over the frozen match until P, Esc, Enter or Space. */
    final class PauseScene implements Scene {
        @Override
        public void update(double seconds) {
            if (controls.wasPressed(Action.PAUSE) || controls.wasPressed(Action.CONFIRM)) scenes.pop();
        }

        @Override
        public void render(Graphics2D g, double alpha) {
            CourtPainter.veil(g);
            CourtPainter.text(g, msg.get("pause.title"), 190, 40, true);
            CourtPainter.text(g, msg.get("pause.continue"), 240, 18, false);
        }
    }

    /** The final score and who won, until Enter or Space leads back to the title. */
    final class OverScene implements Scene {
        private final Match match;
        private final MatchResult result;

        OverScene(Match match, MatchResult result) {
            this.match = match;
            this.result = result;
        }

        @Override
        public void update(double seconds) {
            if (controls.wasPressed(Action.CONFIRM)) scenes.replace(new TitleScene());
        }

        @Override
        public void render(Graphics2D g, double alpha) {
            CourtPainter.paint(g, match, alpha);
            CourtPainter.veil(g);
            String headline = switch (result.mode()) {
                case ONE_PLAYER -> msg.get(result.winner() == Side.RIGHT ? "over.you-win" : "over.computer-wins");
                default -> msg.get(result.winner() == Side.RIGHT ? "over.right-wins" : "over.left-wins");
            };
            CourtPainter.text(g, headline, 190, 40, true);
            CourtPainter.text(g, msg.get("over.continue"), 240, 18, false);
        }
    }
}
