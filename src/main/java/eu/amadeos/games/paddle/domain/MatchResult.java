package eu.amadeos.games.paddle.domain;

/** How a match ended. */
public record MatchResult(Mode mode, Difficulty difficulty, Side winner, int leftScore, int rightScore) {
    public int score(Side side) {
        return side == Side.LEFT ? leftScore : rightScore;
    }
}
