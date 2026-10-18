package eu.amadeos.games.paddle.ui.game;

import eu.amadeos.games.paddle.domain.Difficulty;
import eu.amadeos.games.paddle.domain.MatchResult;
import eu.amadeos.games.paddle.domain.Mode;

import java.util.function.Consumer;

// --8<-- [start:arena]
/**
 * What the presenter may ask of the running game. The game runs on its own thread; every method here is
 * safe to call from the Swing thread and takes effect at the start of the game's next step.
 */
public interface Arena {
    /** Back to the title screen, where the computer plays itself. */
    void showTitle();

    void startMatch(Mode mode, Difficulty difficulty);

    /** The difficulty a match started from the title screen gets. */
    void setDifficulty(Difficulty difficulty);

    /** Pauses a running match; does nothing elsewhere. */
    void pause();

    /** Called on the Swing thread when a match ends. */
    void onMatchOver(Consumer<MatchResult> listener);
}
// --8<-- [end:arena]
