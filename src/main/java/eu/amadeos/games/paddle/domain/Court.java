package eu.amadeos.games.paddle.domain;

/**
 * Sizes of the court in logical units, the same as the pixels of the 640 × 400 picture the game draws
 * (the window scales it). Speeds are in units per second.
 */
public final class Court {
    public static final double WIDTH = 640;
    public static final double HEIGHT = 400;
    public static final double PADDLE_WIDTH = 10;
    public static final double PADDLE_HEIGHT = 60;
    /** Distance of each paddle from its side of the court. */
    public static final double PADDLE_MARGIN = 24;
    public static final double PADDLE_SPEED = 380;
    public static final double BALL_SIZE = 10;
    public static final double SERVE_SPEED = 300;
    public static final double MAX_BALL_SPEED = 900;
    /** Every hit makes the ball this much faster. */
    public static final double SPEED_UP = 1.06;
    /** The steepest angle off a paddle, hit at its very edge. */
    public static final double MAX_ANGLE = Math.toRadians(55);
    /** The pause before each serve. */
    public static final double SERVE_DELAY = 1.0;

    private Court() {
    }
}
