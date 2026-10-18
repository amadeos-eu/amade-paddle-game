package eu.amadeos.games.paddle.ui.main;

import com.google.inject.Inject;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.commons.i18n.Mnemonics;
import eu.amadeos.gameengine.score.HighScores;
import eu.amadeos.gameengine.score.Score;
import eu.amadeos.games.paddle.domain.Difficulty;
import eu.amadeos.games.paddle.domain.MatchResult;
import eu.amadeos.games.paddle.domain.Mode;
import eu.amadeos.games.paddle.domain.Side;
import eu.amadeos.games.paddle.repository.PaddleSettings;
import eu.amadeos.games.paddle.ui.game.Arena;

import java.time.Instant;
import java.util.function.Supplier;

/**
 * Everything around the court, without Swing: the menu commands, the difficulty, pausing when the window
 * loses the focus, and the best results. The court is the {@link Arena}'s; the presenter only tells it what
 * to do. All methods run on the Swing thread.
 */
public final class PaddlePresenter {
    private final PaddleView view;
    private final Arena arena;
    private final Messages msg;
    private final PaddleSettings settings;
    private final HighScores scores;
    private final Supplier<Instant> now;
    private Difficulty difficulty;

    @Inject
    public PaddlePresenter(PaddleView view, Arena arena, Messages msg, PaddleSettings settings, HighScores scores) {
        this(view, arena, msg, settings, scores, Instant::now);
    }

    PaddlePresenter(PaddleView view, Arena arena, Messages msg, PaddleSettings settings, HighScores scores,
                    Supplier<Instant> now) {
        this.view = view;
        this.arena = arena;
        this.msg = msg;
        this.settings = settings;
        this.scores = scores;
        this.now = now;
    }

    public void start() {
        difficulty = settings.difficulty();
        view.showOptions(difficulty);
        arena.setDifficulty(difficulty);
        arena.onMatchOver(this::matchOver);
        arena.showTitle();
    }

    public void newGame(Mode mode) {
        arena.startMatch(mode, difficulty);
    }

    public void setDifficulty(Difficulty d) {
        difficulty = d;
        settings.setDifficulty(d);
        view.showOptions(d);
        arena.setDifficulty(d);
    }

    public void pause() {
        arena.pause();
    }

    /** The window lost the focus: nobody is at the keys, so a running match waits. */
    public void focusLost() {
        arena.pause();
    }

    public void showScores() {
        view.showScores(difficulty, -1);
    }

    public void showAbout() {
        view.showAbout();
    }

    // --8<-- [start:match-over]
    /**
     * A match ended. A win against the computer may enter the table of its difficulty; the fewer points the
     * computer made, the better.
     */
    void matchOver(MatchResult result) {
        if (result.mode() != Mode.ONE_PLAYER || result.winner() != Side.RIGHT) return;
        int conceded = result.score(Side.LEFT);
        String table = result.difficulty().id();
        if (!scores.qualifies(table, conceded)) return;
        String level = Mnemonics.strip(msg.get(difficultyKey(result.difficulty())));
        view.askName(msg.format("scores.won", level, result.score(Side.RIGHT), conceded)).ifPresent(name -> {
            int place = scores.add(table, new Score(name, conceded, now.get()));
            view.showScores(result.difficulty(), place);
        });
    }
    // --8<-- [end:match-over]

    /** The text key of a difficulty in the menu, e.g. {@code menu.game.easy}. */
    static String difficultyKey(Difficulty d) {
        return "menu.game." + d.id();
    }
}
