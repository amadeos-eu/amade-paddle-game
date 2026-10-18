package eu.amadeos.games.paddle.domain;

import java.util.random.RandomGenerator;

/**
 * The computer opponent. It looks at the ball now and then, works out where the ball will reach its side,
 * aims there with a small error and moves at a limited speed. Each of the three limits comes from the
 * {@link Difficulty}; together they make it beatable.
 */
public final class ComputerPlayer {
    /** Closer than this to its target, the paddle stops, so it does not tremble. */
    private static final double DEAD_ZONE = 4;
    /** Within this distance the paddle slows down instead of overshooting. */
    private static final double SLOW_ZONE = 24;

    private final Side side;
    private final Difficulty difficulty;
    private final RandomGenerator random;
    private double nextLook;
    private double target = Court.HEIGHT / 2;
    /** How far off it aims at the ball now coming; chosen once per ball, as a person decides once. */
    private double miss;
    private boolean wasComing;

    public ComputerPlayer(Side side, Difficulty difficulty, RandomGenerator random) {
        this.side = side;
        this.difficulty = difficulty;
        this.random = random;
    }

    public Side side() {
        return side;
    }

    // --8<-- [start:move]
    /** How to move this step: -1 (up) to +1 (down), at most the difficulty's share of full speed. */
    public double move(Match match, double seconds) {
        nextLook -= seconds;
        if (nextLook <= 0) {
            nextLook = difficulty.reaction();
            target = chooseTarget(match);
        }
        double distance = target - match.paddle(side).centreY();
        if (Math.abs(distance) < DEAD_ZONE) return 0;
        double urgency = Math.min(1, Math.abs(distance) / SLOW_ZONE);
        return Math.signum(distance) * urgency * difficulty.speed();
    }

    private double chooseTarget(Match match) {
        Ball ball = match.ball();
        boolean coming = side == Side.LEFT ? ball.vx() < 0 : ball.vx() > 0;
        if (coming && !wasComing) miss = (random.nextDouble() * 2 - 1) * difficulty.error() * Court.PADDLE_HEIGHT / 2;
        wasComing = coming;
        if (!coming) return Court.HEIGHT / 2;               // wait in the middle
        return predictY(ball, match.paddle(side).face()) + miss;
    }
    // --8<-- [end:move]

    // --8<-- [start:predict]
    /**
     * Where the ball's centre will be when it reaches {@code faceX}, bounces off the walls included.
     *
     * <p>The trick: pretend there are no walls and let the ball fly on in a straight line. Then fold the
     * result back into the court, as if the court were mirrored at every wall, the way light behaves
     * between two mirrors.
     */
    static double predictY(Ball ball, double faceX) {
        double ballEdge = ball.vx() < 0 ? ball.x() : ball.x() + Court.BALL_SIZE;
        double time = Math.max(0, (faceX - ballEdge) / ball.vx());
        double free = ball.y() + ball.vy() * time;                 // as if there were no walls
        double room = Court.HEIGHT - Court.BALL_SIZE;              // where the ball's top edge can be
        double period = 2 * room;                                  // down and up again
        double folded = ((free % period) + period) % period;       // 0 to period, also for negative values
        if (folded > room) folded = period - folded;               // the mirrored half comes back
        return folded + Court.BALL_SIZE / 2;
    }
    // --8<-- [end:predict]
}
