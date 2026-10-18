package eu.amadeos.games.paddle.ui.main;

import eu.amadeos.games.paddle.domain.Difficulty;

import java.util.Optional;

/** What {@link PaddlePresenter} needs from the window; tests replace it. The court itself is the game's. */
public interface PaddleView {
    /** Selects the difficulty in the menu. */
    void showOptions(Difficulty difficulty);

    /** Asks for the player's name after a good result; empty if cancelled. */
    Optional<String> askName(String message);

    /** Shows the best results, the table of {@code selected} first; {@code highlight} -1 for no new entry. */
    void showScores(Difficulty selected, int highlight);

    void showAbout();
}
