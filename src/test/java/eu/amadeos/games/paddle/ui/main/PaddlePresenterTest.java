package eu.amadeos.games.paddle.ui.main;

import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.score.HighScores;
import eu.amadeos.gameengine.score.Ranking;
import eu.amadeos.gameengine.score.Score;
import eu.amadeos.games.paddle.bootstrap.PaddleModule;
import eu.amadeos.games.paddle.domain.Difficulty;
import eu.amadeos.games.paddle.domain.MatchResult;
import eu.amadeos.games.paddle.domain.Mode;
import eu.amadeos.games.paddle.domain.Side;
import eu.amadeos.games.paddle.repository.PaddleSettings;
import eu.amadeos.games.paddle.ui.game.Arena;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PaddlePresenterTest {
    private static final Instant WON_AT = Instant.parse("2026-10-18T20:00:00Z");

    /** Records the commands for the game. */
    static final class FakeArena implements Arena {
        final List<String> commands = new ArrayList<>();
        Difficulty difficulty;
        Consumer<MatchResult> listener;

        @Override
        public void showTitle() {
            commands.add("title");
        }

        @Override
        public void startMatch(Mode mode, Difficulty d) {
            commands.add("match " + mode + " " + d);
        }

        @Override
        public void setDifficulty(Difficulty d) {
            difficulty = d;
        }

        @Override
        public void pause() {
            commands.add("pause");
        }

        @Override
        public void onMatchOver(Consumer<MatchResult> l) {
            listener = l;
        }
    }

    /** Records what the presenter shows and answers its questions from a queue. */
    static final class FakeView implements PaddleView {
        Difficulty difficulty;
        final Deque<Optional<String>> names = new ArrayDeque<>();
        String askedWith;
        Difficulty scoresShown;
        int highlight = -2;

        @Override
        public void showOptions(Difficulty d) {
            difficulty = d;
        }

        @Override
        public Optional<String> askName(String message) {
            askedWith = message;
            return names.isEmpty() ? Optional.empty() : names.pop();
        }

        @Override
        public void showScores(Difficulty selected, int place) {
            scoresShown = selected;
            highlight = place;
        }

        @Override
        public void showAbout() {
        }
    }

    @TempDir
    Path dir;

    private final FakeArena arena = new FakeArena();
    private final FakeView view = new FakeView();
    private PaddleSettings settings;
    private HighScores scores;

    @BeforeEach
    void start() {
        settings = new PaddleSettings(dir.resolve("paddle.properties"));
        scores = new HighScores(dir.resolve("scores"), Ranking.LOWER_WINS);
        Messages msg = Messages.load(PaddleModule.class, PaddleModule.MESSAGES, Locale.ENGLISH);
        new PaddlePresenter(view, arena, msg, settings, scores, () -> WON_AT).start();
    }

    @Test
    void startsOnTheTitleWithTheSavedDifficulty() {
        assertEquals(List.of("title"), arena.commands);
        assertEquals(Difficulty.NORMAL, view.difficulty);
        assertEquals(Difficulty.NORMAL, arena.difficulty);
    }

    // --8<-- [start:test-win]
    @Test
    void aWinAgainstTheComputerEntersTheTable() {
        view.names.push(Optional.of("Anna"));
        arena.listener.accept(new MatchResult(Mode.ONE_PLAYER, Difficulty.NORMAL, Side.RIGHT, 4, 11));
        assertEquals("You beat the computer on Normal, 11 to 4.", view.askedWith);
        assertEquals(List.of(new Score("Anna", 4, WON_AT)), scores.list("normal"));
        assertEquals(Difficulty.NORMAL, view.scoresShown);
        assertEquals(0, view.highlight);
    }

    @Test
    void lossesAndTwoPlayerGamesDoNotCount() {
        arena.listener.accept(new MatchResult(Mode.ONE_PLAYER, Difficulty.NORMAL, Side.LEFT, 11, 9));
        arena.listener.accept(new MatchResult(Mode.TWO_PLAYERS, Difficulty.NORMAL, Side.RIGHT, 2, 11));
        assertNull(view.askedWith);
    }
    // --8<-- [end:test-win]
}
