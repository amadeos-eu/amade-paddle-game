package eu.amadeos.games.paddle.ui.game;

import eu.amadeos.gameengine.audio.Sound;
import eu.amadeos.gameengine.audio.Sound.Wave;

/** The sounds of Paddle, made once at start: short square-wave beeps like the arcade machines, no recordings. */
// --8<-- [start:sounds]
final class SoundBank {
    /** A paddle hits the ball: a short, clear beep. */
    final Sound paddle = Sound.tone(Wave.SQUARE, 460, 460, 0.05, 0.22);
    /** The ball bounces off the top or bottom: an octave lower and shorter. */
    final Sound wall = Sound.tone(Wave.SQUARE, 230, 230, 0.03, 0.15);
    /** A point: a longer tone that falls by an octave. */
    final Sound point = Sound.tone(Wave.SQUARE, 490, 245, 0.3, 0.2);
    /** C, E, G, high C: the match is won. */
    final Sound won = note(523, 0.1).then(note(659, 0.1)).then(note(784, 0.1)).then(note(1047, 0.35));

    private static Sound note(double hz, double seconds) {
        return Sound.tone(Wave.TRIANGLE, hz, hz, seconds, 0.3);
    }
}
// --8<-- [end:sounds]
