package eu.amadeos.games.paddle.domain;

/** Left or right half of the court. In a game against the computer the player is always right. */
public enum Side {
    LEFT, RIGHT;

    public Side other() {
        return this == LEFT ? RIGHT : LEFT;
    }
}
