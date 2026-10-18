package eu.amadeos.games.paddle.domain;

/** The ball: a square, its position (top left) and velocity in units per second. */
public final class Ball {
    double x;
    double y;
    double vx;
    double vy;
    double previousX;
    double previousY;

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double vx() {
        return vx;
    }

    public double vy() {
        return vy;
    }

    public double previousX() {
        return previousX;
    }

    public double previousY() {
        return previousY;
    }

    public double speed() {
        return Math.hypot(vx, vy);
    }

    public double centreY() {
        return y + Court.BALL_SIZE / 2;
    }

    void place(double newX, double newY, double newVx, double newVy) {
        x = newX;
        y = newY;
        previousX = newX;
        previousY = newY;
        vx = newVx;
        vy = newVy;
    }
}
