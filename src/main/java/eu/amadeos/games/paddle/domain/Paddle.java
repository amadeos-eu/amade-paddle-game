package eu.amadeos.games.paddle.domain;

/** A paddle: it only moves up and down, at most at {@link Court#PADDLE_SPEED}, and never leaves the court. */
public final class Paddle {
    private final Side side;
    private double y;
    private double previousY;

    Paddle(Side side) {
        this.side = side;
        centre();
    }

    public Side side() {
        return side;
    }

    /** Left edge. */
    public double x() {
        return side == Side.LEFT ? Court.PADDLE_MARGIN : Court.WIDTH - Court.PADDLE_MARGIN - Court.PADDLE_WIDTH;
    }

    /** Top edge. */
    public double y() {
        return y;
    }

    /** Top edge before the last step, for drawing between steps. */
    public double previousY() {
        return previousY;
    }

    public double centreY() {
        return y + Court.PADDLE_HEIGHT / 2;
    }

    /** The edge the ball bounces off: the right edge of the left paddle, the left edge of the right one. */
    public double face() {
        return side == Side.LEFT ? x() + Court.PADDLE_WIDTH : x();
    }

    /**
     * Moves for one step.
     *
     * @param direction -1 up to +1 down; fractions move slower, as the computer does
     */
    void move(double direction, double seconds) {
        previousY = y;
        double d = Math.clamp(direction, -1.0, 1.0);
        y = Math.clamp(y + d * Court.PADDLE_SPEED * seconds, 0.0, Court.HEIGHT - Court.PADDLE_HEIGHT);
    }

    void centre() {
        y = (Court.HEIGHT - Court.PADDLE_HEIGHT) / 2;
        previousY = y;
    }

    /** For tests: puts the paddle's top edge at {@code top}. */
    void moveTo(double top) {
        y = top;
        previousY = top;
    }
}
