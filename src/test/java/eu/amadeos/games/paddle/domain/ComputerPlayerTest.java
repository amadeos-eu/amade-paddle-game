package eu.amadeos.games.paddle.domain;

import org.junit.jupiter.api.Test;

import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComputerPlayerTest {
    private static final double STEP = 1.0 / 120;

    @Test
    void predictsAStraightBall() {
        Ball ball = new Ball();
        ball.place(300, 195, 300, 0);
        assertEquals(200, ComputerPlayer.predictY(ball, 600), 1e-9);
    }

    // --8<-- [start:test-predict]
    @Test
    void predictsTheBounceOffAWall() {
        Ball ball = new Ball();
        // Top edge at 50, rising 1 unit per unit to the right; the face is 200 units away.
        ball.place(390, 50, 300, -300);
        // Without walls the top edge would end at 50 - 200 = -150; mirrored at the top wall: +150.
        assertEquals(150 + Court.BALL_SIZE / 2, ComputerPlayer.predictY(ball, 600), 1e-9);
    }
    // --8<-- [end:test-predict]

    @Test
    void waitsInTheMiddleWhileTheBallFliesAway() {
        Match match = new Match(new SplittableRandom(1));
        match.placePaddle(Side.LEFT, 0);
        match.placeBall(300, 100, 300, 0);       // away from the left player
        ComputerPlayer computer = new ComputerPlayer(Side.LEFT, Difficulty.HARD, new SplittableRandom(1));
        assertTrue(computer.move(match, STEP) > 0, "moves down towards the middle");
    }

    /** Plays a whole match computer against computer and returns it. */
    private static Match play(Difficulty left, Difficulty right, long seed) {
        Match match = new Match(new SplittableRandom(seed));
        ComputerPlayer l = new ComputerPlayer(Side.LEFT, left, new SplittableRandom(seed + 1));
        ComputerPlayer r = new ComputerPlayer(Side.RIGHT, right, new SplittableRandom(seed + 2));
        for (int step = 0; step < 120 * 60 * 30 && match.winner() == null; step++) {
            match.update(STEP, l.move(match, STEP), r.move(match, STEP));
        }
        return match;
    }

    // --8<-- [start:test-strength]
    @Test
    void harderOpponentsWinMoreOften() {
        int hardWins = 0;
        for (long seed = 0; seed < 20; seed++) {
            Match match = play(Difficulty.EASY, Difficulty.HARD, seed);
            assertNotNull(match.winner(), "every match ends");
            if (match.winner() == Side.RIGHT) hardWins++;
        }
        assertTrue(hardWins >= 16, "hard beat easy only " + hardWins + " of 20 times");
    }
    // --8<-- [end:test-strength]

    // --8<-- [start:test-demo]
    @Test
    void tenMinutesOfDemoNeverGetStuck() {
        Match match = new Match(new SplittableRandom(7));
        ComputerPlayer l = new ComputerPlayer(Side.LEFT, Difficulty.NORMAL, new SplittableRandom(8));
        ComputerPlayer r = new ComputerPlayer(Side.RIGHT, Difficulty.NORMAL, new SplittableRandom(9));
        int points = 0;
        int lastScore = 0;
        int rally = 0;
        int longestRally = 0;
        int lastHits = 0;
        for (int step = 0; step < 120 * 60 * 10; step++) {
            match.update(STEP, l.move(match, STEP), r.move(match, STEP));
            Ball ball = match.ball();
            assertTrue(ball.y() >= 0 && ball.y() <= Court.HEIGHT - Court.BALL_SIZE, "the ball left the court");
            if (match.hits() > lastHits) {
                lastHits = match.hits();
                longestRally = Math.max(longestRally, ++rally);
            }
            int score = match.score(Side.LEFT) + match.score(Side.RIGHT);
            if (score > lastScore) {
                points++;
                rally = 0;
            }
            lastScore = score;
            if (match.winner() != null) {                       // the demo starts a new match
                match = new Match(new SplittableRandom(step));
                lastScore = 0;
                lastHits = 0;
            }
        }
        assertTrue(points >= 20, "points keep coming: " + points + " in ten minutes");
        assertTrue(longestRally < 200, "no endless rally: " + longestRally);
    }
    // --8<-- [end:test-demo]
}
