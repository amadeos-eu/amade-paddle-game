package eu.amadeos.games.paddle.domain;

import java.util.Locale;
import java.util.Optional;

/**
 * How well the computer plays: three numbers. A perfect opponent is easy to write and no fun to play
 * against; these make it human.
 */
// --8<-- [start:difficulty]
public enum Difficulty {
    //      speed  reaction  error
    EASY(0.6, 0.25, 1.7),
    NORMAL(0.8, 0.15, 1.4),
    HARD(0.95, 0.08, 1.25);

    private final double speed;
    private final double reaction;
    private final double error;

    /**
     * @param speed    share of the paddle's top speed the computer uses
     * @param reaction seconds between two looks at the ball
     * @param error    how far off it aims, at most, as a share of half a paddle. Above about 1.17 the error
     *                 can exceed the paddle's reach (half a paddle plus half a ball), and only then does the
     *                 computer miss a ball it could reach. The values were tuned by letting the computer
     *                 play itself: against Easy a point takes about 7 seconds, against Hard about 20.
     */
    Difficulty(double speed, double reaction, double error) {
        this.speed = speed;
        this.reaction = reaction;
        this.error = error;
    }
    // --8<-- [end:difficulty]

    public double speed() {
        return speed;
    }

    public double reaction() {
        return reaction;
    }

    public double error() {
        return error;
    }

    /** Name in settings files and of the high-score table, e.g. {@code normal}. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<Difficulty> ofId(String id) {
        for (Difficulty d : values()) {
            if (d.id().equals(id)) return Optional.of(d);
        }
        return Optional.empty();
    }
}
