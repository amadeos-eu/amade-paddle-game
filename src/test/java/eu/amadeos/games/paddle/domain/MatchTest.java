package eu.amadeos.games.paddle.domain;

import org.junit.jupiter.api.Test;

import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchTest {
    private static final double STEP = 1.0 / 120;

    private static Match match() {
        return new Match(new SplittableRandom(1));
    }

    @Test
    void theBallWaitsBeforeTheServe() {
        Match match = match();
        assertTrue(match.serving());
        double x = match.ball().x();
        for (int i = 0; i < 100; i++) match.update(STEP, 0, 0);   // 0.83 s
        assertEquals(x, match.ball().x());
        for (int i = 0; i < 30; i++) match.update(STEP, 0, 0);
        assertTrue(match.ball().x() != x, "served after one second");
        assertEquals(Court.SERVE_SPEED, match.ball().speed(), 1e-9);
    }

    @Test
    void wallsMirrorTheBall() {
        Match match = match();
        match.placeBall(300, 2, 0, -240);       // 2 units below the top, flying up at 2 units per step
        match.update(STEP, 0, 0);
        assertEquals(0, match.ball().y(), 1e-9, "would be at -0, mirrored to 0");
        match.update(STEP, 0, 0);
        assertEquals(2, match.ball().y(), 1e-9);
        assertTrue(match.ball().vy() > 0);
    }

    // --8<-- [start:test-hit]
    @Test
    void theMiddleOfThePaddleSendsTheBallStraightBackAndFaster() {
        Match match = match();
        Paddle right = match.paddle(Side.RIGHT);
        match.placePaddle(Side.RIGHT, 170);                     // centre at 200
        match.placeBall(right.face() - Court.BALL_SIZE - 1, 195, 600, 0);
        match.update(STEP, 0, 0);
        assertTrue(match.ball().vx() < 0, "bounced back");
        assertEquals(0, match.ball().vy(), 1e-9, "straight back");
        assertEquals(600 * Court.SPEED_UP, match.ball().speed(), 1e-9);
        assertEquals(1, match.hits());
    }

    @Test
    void theEdgeOfThePaddleSendsItSteeply() {
        Match match = match();
        Paddle left = match.paddle(Side.LEFT);
        match.placePaddle(Side.LEFT, 170);
        match.placeBall(left.face() + 1, 225, -600, 0);         // ball centre at the paddle's lower edge
        match.update(STEP, 0, 0);
        double angle = Math.toDegrees(Math.atan2(match.ball().vy(), match.ball().vx()));
        assertTrue(angle > 40 && angle <= 55, "downwards at a steep angle, was " + angle);
    }

    @Test
    void aFastBallCannotJumpThroughThePaddle() {
        Match match = match();
        Paddle right = match.paddle(Side.RIGHT);
        match.placePaddle(Side.RIGHT, 170);
        // Fast enough to cross the whole paddle within one step: 3000 units/s is 25 units per step.
        match.placeBall(right.face() - Court.BALL_SIZE - 5, 195, 3000, 0);
        match.update(STEP, 0, 0);
        assertTrue(match.ball().vx() < 0, "the swept check found the crossing");
        assertTrue(match.ball().x() + Court.BALL_SIZE <= right.face());
    }
    // --8<-- [end:test-hit]

    @Test
    void aMissedBallScoresForTheOtherSide() {
        Match match = match();
        match.placePaddle(Side.LEFT, 0);
        match.placeBall(30, 300, -600, 0);
        for (int i = 0; i < 20; i++) match.update(STEP, 0, 0);
        assertEquals(1, match.score(Side.RIGHT));
        assertTrue(match.serving(), "and the next serve waits");
    }

    @Test
    void elevenPointsWin() {
        Match match = match();
        match.setScore(3, 10);
        match.placePaddle(Side.LEFT, 0);
        match.placeBall(30, 300, -600, 0);
        for (int i = 0; i < 20; i++) match.update(STEP, 0, 0);
        assertEquals(Side.RIGHT, match.winner());
        double x = match.ball().x();
        match.update(STEP, 0, 0);
        assertEquals(x, match.ball().x(), "nothing moves any more");
    }

    @Test
    void paddlesStayInTheCourt() {
        Match match = match();
        for (int i = 0; i < 240; i++) match.update(STEP, -1, 1);
        assertEquals(0, match.paddle(Side.LEFT).y());
        assertEquals(Court.HEIGHT - Court.PADDLE_HEIGHT, match.paddle(Side.RIGHT).y());
        assertNull(match.winner());
    }
}
