package eu.amadeos.games.paddle.ui.main;

import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.score.HighScores;
import eu.amadeos.gameengine.score.Ranking;
import eu.amadeos.games.paddle.bootstrap.PaddleModule;
import eu.amadeos.games.paddle.domain.Difficulty;
import eu.amadeos.games.paddle.domain.Mode;
import eu.amadeos.games.paddle.repository.PaddleSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaddleCommandsTest {
    @TempDir
    Path dir;

    @Test
    void menuCommandsReachTheGame() {
        PaddlePresenterTest.FakeArena arena = new PaddlePresenterTest.FakeArena();
        PaddlePresenterTest.FakeView view = new PaddlePresenterTest.FakeView();
        PaddleSettings settings = new PaddleSettings(dir.resolve("paddle.properties"));
        PaddlePresenter presenter = new PaddlePresenter(view, arena,
                Messages.load(PaddleModule.class, PaddleModule.MESSAGES, Locale.ENGLISH), settings,
                new HighScores(dir.resolve("scores"), Ranking.LOWER_WINS));
        presenter.start();
        presenter.setDifficulty(Difficulty.HARD);
        presenter.newGame(Mode.ONE_PLAYER);
        presenter.focusLost();
        presenter.newGame(Mode.TWO_PLAYERS);
        assertEquals(List.of("title", "match ONE_PLAYER HARD", "pause", "match TWO_PLAYERS HARD"), arena.commands);
        assertEquals(Difficulty.HARD, settings.difficulty());
        assertEquals(Difficulty.HARD, view.difficulty);
        presenter.showScores();
        assertEquals(Difficulty.HARD, view.scoresShown);
    }
}
