package eu.amadeos.games.paddle.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * One match: two paddles, a ball, the score. It advances in fixed steps with {@link #update}; nothing in it
 * knows about time, threads, keys or drawing. The first side to {@link #POINTS_TO_WIN} points wins.
 */
public final class Match {
    public static final int POINTS_TO_WIN = 11;

    // --8<-- [start:events]
    /** Something that happened during a step. The match only reports it; what it sounds like is up to the game. */
    public enum Event { PADDLE, WALL, POINT, WON }
    // --8<-- [end:events]

    private final Paddle left = new Paddle(Side.LEFT);
    private final Paddle right = new Paddle(Side.RIGHT);
    private final Ball ball = new Ball();
    private final RandomGenerator random;
    private int leftScore;
    private int rightScore;
    private double serveDelay;
    private Side serveTo;
    private Side winner;
    private int hits;
    private final List<Event> events = new ArrayList<>();

    public Match(RandomGenerator random) {
        this.random = random;
        serveTo = random.nextBoolean() ? Side.LEFT : Side.RIGHT;
        prepareServe();
    }

    public Paddle paddle(Side side) {
        return side == Side.LEFT ? left : right;
    }

    public Ball ball() {
        return ball;
    }

    public int score(Side side) {
        return side == Side.LEFT ? leftScore : rightScore;
    }

    /** The side that won, or {@code null} while the match goes on. */
    public Side winner() {
        return winner;
    }

    /** True while the ball waits in the middle for the serve. */
    public boolean serving() {
        return serveDelay > 0;
    }

    /** What happened during the last {@link #update}, in order. */
    public List<Event> events() {
        return List.copyOf(events);
    }

    /** Paddle hits so far, both sides together. */
    public int hits() {
        return hits;
    }

    // --8<-- [start:update]
    /**
     * Advances the match by one step.
     *
     * @param leftMove  -1 (up) to +1 (down) for the left paddle
     * @param rightMove the same for the right paddle
     */
    public void update(double seconds, double leftMove, double rightMove) {
        events.clear();
        left.move(leftMove, seconds);
        right.move(rightMove, seconds);
        if (winner != null) return;
        if (serveDelay > 0) {
            serveDelay -= seconds;
            if (serveDelay <= 0) serve();
            return;
        }
        ball.previousX = ball.x;
        ball.previousY = ball.y;
        ball.x += ball.vx * seconds;
        ball.y += ball.vy * seconds;
        bounceOffWalls();
        if (ball.vx < 0) hitPaddle(left);
        else hitPaddle(right);
        if (ball.x + Court.BALL_SIZE < 0) point(Side.RIGHT);
        else if (ball.x > Court.WIDTH) point(Side.LEFT);
    }
    // --8<-- [end:update]

    /** Top and bottom walls: the ball is mirrored back into the court. */
    private void bounceOffWalls() {
        double bottom = Court.HEIGHT - Court.BALL_SIZE;
        if (ball.y < 0) {
            ball.y = -ball.y;
            ball.vy = -ball.vy;
            events.add(Event.WALL);
        } else if (ball.y > bottom) {
            ball.y = 2 * bottom - ball.y;
            ball.vy = -ball.vy;
            events.add(Event.WALL);
        }
    }

    // --8<-- [start:hit]
    /**
     * Did the ball cross the paddle's face during this step? Checking the whole way from the previous
     * position, not only where the ball is now, means a fast ball cannot jump through the paddle.
     */
    private void hitPaddle(Paddle paddle) {
        double face = paddle.face();
        boolean towardsLeft = paddle.side() == Side.LEFT;
        // The edge of the ball that meets the face: its left edge on the left side, its right edge on the right.
        double before = towardsLeft ? ball.previousX : ball.previousX + Court.BALL_SIZE;
        double now = towardsLeft ? ball.x : ball.x + Court.BALL_SIZE;
        boolean crossed = towardsLeft ? before >= face && now < face : before <= face && now > face;
        if (!crossed) return;
        // Where was the ball, vertically, at the moment it reached the face?
        double t = (before - face) / (before - now);
        double yAtFace = ball.previousY + (ball.y - ball.previousY) * t;
        if (yAtFace + Court.BALL_SIZE < paddle.y() || yAtFace > paddle.y() + Court.PADDLE_HEIGHT) return;

        // Where it hit decides the angle: the middle sends it straight back, the edges at up to 55°.
        double offset = (yAtFace + Court.BALL_SIZE / 2 - paddle.centreY())
                / (Court.PADDLE_HEIGHT / 2 + Court.BALL_SIZE / 2);
        double angle = Math.clamp(offset, -1.0, 1.0) * Court.MAX_ANGLE;
        double speed = Math.min(Court.MAX_BALL_SPEED, ball.speed() * Court.SPEED_UP);
        double direction = towardsLeft ? 1 : -1;
        ball.vx = direction * speed * Math.cos(angle);
        ball.vy = speed * Math.sin(angle);
        // Mirror the part of the step that went past the face.
        double past = now - face;
        ball.x -= 2 * past;
        hits++;
        events.add(Event.PADDLE);
    }
    // --8<-- [end:hit]

    private void point(Side scorer) {
        if (scorer == Side.LEFT) leftScore++;
        else rightScore++;
        if (score(scorer) >= POINTS_TO_WIN) {
            winner = scorer;
            events.add(Event.WON);
            ball.place((Court.WIDTH - Court.BALL_SIZE) / 2, (Court.HEIGHT - Court.BALL_SIZE) / 2, 0, 0);
            return;
        }
        events.add(Event.POINT);
        // The side that lost the point receives the next serve.
        serveTo = scorer.other();
        prepareServe();
    }

    private void prepareServe() {
        ball.place((Court.WIDTH - Court.BALL_SIZE) / 2, (Court.HEIGHT - Court.BALL_SIZE) / 2, 0, 0);
        serveDelay = Court.SERVE_DELAY;
    }

    private void serve() {
        double angle = (random.nextDouble() * 2 - 1) * Math.toRadians(30);
        double direction = serveTo == Side.LEFT ? -1 : 1;
        ball.vx = direction * Court.SERVE_SPEED * Math.cos(angle);
        ball.vy = Court.SERVE_SPEED * Math.sin(angle);
    }

    // ---- for tests -------------------------------------------------------------------------------

    /** Puts the ball somewhere with some velocity, the serve already done. */
    public void placeBall(double x, double y, double vx, double vy) {
        ball.place(x, y, vx, vy);
        serveDelay = 0;
    }

    /** Puts a paddle's top edge somewhere. */
    public void placePaddle(Side side, double top) {
        paddle(side).moveTo(top);
    }

    /** Sets the score, e.g. to test the end of a match. */
    public void setScore(int left, int right) {
        leftScore = left;
        rightScore = right;
    }
}
