package eu.amadeos.games.paddle.ui.game;

import eu.amadeos.games.paddle.domain.Ball;
import eu.amadeos.games.paddle.domain.Court;
import eu.amadeos.games.paddle.domain.Match;
import eu.amadeos.games.paddle.domain.Paddle;
import eu.amadeos.games.paddle.domain.Side;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

/**
 * Draws the court in the game's logical coordinates (640 × 400): black ground, a dashed net, the score in
 * big block digits as on the arcade original, paddles and ball. Moving things are drawn between their last
 * two positions, by {@code alpha}, so the picture moves smoothly although the rules step at their own pace.
 */
final class CourtPainter {
    static final Color GROUND = Color.BLACK;
    static final Color CHALK = Color.WHITE;
    /** A veil over the court behind texts. */
    static final Color VEIL = new Color(0, 0, 0, 150);

    /** The digits 0 to 9 on a grid of 3 × 5 blocks, one string per row, {@code #} a block. */
    private static final String[][] DIGITS = {
            {"###", "#.#", "#.#", "#.#", "###"}, {"..#", "..#", "..#", "..#", "..#"},
            {"###", "..#", "###", "#..", "###"}, {"###", "..#", "###", "..#", "###"},
            {"#.#", "#.#", "###", "..#", "..#"}, {"###", "#..", "###", "..#", "###"},
            {"###", "#..", "###", "#.#", "###"}, {"###", "..#", "..#", "..#", "..#"},
            {"###", "#.#", "###", "#.#", "###"}, {"###", "#.#", "###", "..#", "###"},
    };
    private static final double BLOCK = 8;

    private CourtPainter() {
    }

    // --8<-- [start:paint]
    static void paint(Graphics2D g, Match match, double alpha) {
        g.setColor(GROUND);
        g.fill(new Rectangle2D.Double(0, 0, Court.WIDTH, Court.HEIGHT));
        g.setColor(CHALK);
        for (double y = 4; y < Court.HEIGHT; y += 20) {
            g.fill(new Rectangle2D.Double(Court.WIDTH / 2 - 2, y, 4, 10));
        }
        paintNumber(g, match.score(Side.LEFT), Court.WIDTH / 2 - 60, true);
        paintNumber(g, match.score(Side.RIGHT), Court.WIDTH / 2 + 60, false);
        for (Side side : Side.values()) {
            Paddle p = match.paddle(side);
            double y = p.previousY() + (p.y() - p.previousY()) * alpha;
            g.fill(new Rectangle2D.Double(p.x(), y, Court.PADDLE_WIDTH, Court.PADDLE_HEIGHT));
        }
        Ball ball = match.ball();
        if (match.winner() == null) {
            double x = ball.previousX() + (ball.x() - ball.previousX()) * alpha;
            double y = ball.previousY() + (ball.y() - ball.previousY()) * alpha;
            g.fill(new Rectangle2D.Double(x, y, Court.BALL_SIZE, Court.BALL_SIZE));
        }
    }
    // --8<-- [end:paint]

    /** A number in block digits, its right edge at {@code edge} (left score) or its left edge there (right score). */
    private static void paintNumber(Graphics2D g, int number, double edge, boolean alignRight) {
        String text = Integer.toString(number);
        double digitWidth = 4 * BLOCK;
        double width = text.length() * digitWidth - BLOCK;
        double x = alignRight ? edge - width : edge;
        for (char c : text.toCharArray()) {
            String[] rows = DIGITS[c - '0'];
            for (int r = 0; r < rows.length; r++) {
                for (int col = 0; col < 3; col++) {
                    if (rows[r].charAt(col) == '#') {
                        g.fill(new Rectangle2D.Double(x + col * BLOCK, 20 + r * BLOCK, BLOCK, BLOCK));
                    }
                }
            }
            x += digitWidth;
        }
    }

    /** Darkens the court so text over it stays readable. */
    static void veil(Graphics2D g) {
        g.setColor(VEIL);
        g.fill(new Rectangle2D.Double(0, 0, Court.WIDTH, Court.HEIGHT));
    }

    /** A line of text centred horizontally, its baseline at {@code y}. */
    static void text(Graphics2D g, String text, double y, float size, boolean bold) {
        g.setFont(new Font(Font.SANS_SERIF, bold ? Font.BOLD : Font.PLAIN, 1).deriveFont(size));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(CHALK);
        g.drawString(text, (float) (Court.WIDTH - fm.stringWidth(text)) / 2, (float) y);
    }
}
