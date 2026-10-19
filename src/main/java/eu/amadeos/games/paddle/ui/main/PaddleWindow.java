package eu.amadeos.games.paddle.ui.main;

import com.google.inject.Inject;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.commons.i18n.Mnemonics;
import eu.amadeos.commons.swing.EdtExecutor;
import eu.amadeos.gameengine.input.Keyboard;
import eu.amadeos.gameengine.score.HighScores;
import eu.amadeos.gameengine.score.PlayerName;
import eu.amadeos.gameengine.swing.FullScreen;
import eu.amadeos.gameengine.swing.GameCanvas;
import eu.amadeos.gameengine.swing.GameMenus;
import eu.amadeos.gameengine.swing.GameTexts;
import eu.amadeos.gameengine.swing.HighScoreDialog;
import eu.amadeos.games.paddle.bootstrap.AmadePaddleApplication;
import eu.amadeos.games.paddle.domain.Court;
import eu.amadeos.games.paddle.domain.Difficulty;
import eu.amadeos.games.paddle.domain.Mode;
import eu.amadeos.games.paddle.ui.game.PaddleGame;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The game window: a menu and the court, a {@link GameCanvas} the game loop draws on. The window can be
 * resized or switched to full screen with F11; the court scales with it.
 */
public final class PaddleWindow extends JFrame implements PaddleView {
    private final Messages msg;
    private final GameMenus menus = new GameMenus(GameTexts.load());
    private final HighScores scores;
    private final PlayerName playerName;
    private final GameCanvas canvas;
    private final FullScreen fullScreen;
    private final Map<Difficulty, JRadioButtonMenuItem> difficultyItems = new EnumMap<>(Difficulty.class);
    private final JCheckBoxMenuItem sound;
    private PaddlePresenter presenter;

    @Inject
    public PaddleWindow(Messages msg, HighScores scores, PlayerName playerName, PaddleGame game, Keyboard keyboard) {
        super(msg.get("window.title"));
        EdtExecutor.requireEdt();
        this.msg = msg;
        this.scores = scores;
        this.playerName = playerName;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        java.net.URL icon = getClass().getResource("/icons/amade-paddle-game.png");
        if (icon != null) setIconImage(new ImageIcon(icon).getImage());

        // --8<-- [start:canvas]
        // The canvas is a heavyweight AWT component: lightweight menus would open behind it.
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
        canvas = new GameCanvas((int) Court.WIDTH, (int) Court.HEIGHT, game::render);
        keyboard.attach(canvas);
        // --8<-- [end:canvas]
        getContentPane().add(canvas, BorderLayout.CENTER);
        fullScreen = new FullScreen(this, canvas);
        sound = Mnemonics.apply(new JCheckBoxMenuItem(), msg.get("menu.game.sound"));
        sound.setName("game-sound");
        sound.addActionListener(e -> {
            presenter.setSound(sound.isSelected());
            canvas.requestFocusInWindow();
        });
        setJMenuBar(menuBar());
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowDeactivated(WindowEvent e) {
                if (presenter != null) presenter.focusLost();
            }

            @Override
            public void windowActivated(WindowEvent e) {
                canvas.requestFocusInWindow();
            }
        });
        pack();
    }

    /** Connects the window with the presenter; call once before {@link PaddlePresenter#start()}. */
    public void bind(PaddlePresenter presenter) {
        this.presenter = presenter;
    }

    public GameCanvas canvas() {
        return canvas;
    }

    public FullScreen fullScreen() {
        return fullScreen;
    }

    /** Runs the action when the window closes, but not when it only switches to or from full screen. */
    public void whenClosed(Runnable action) {
        fullScreen.whenClosed(action);
    }

    private JMenuBar menuBar() {
        JMenu game = menus.game();
        game.add(item("menu.game.one-player", "game-one-player", KeyEvent.VK_F2, () -> presenter.newGame(Mode.ONE_PLAYER)));
        game.add(item("menu.game.two-players", "game-two-players", KeyEvent.VK_F3, () -> presenter.newGame(Mode.TWO_PLAYERS)));
        game.addSeparator();
        ButtonGroup group = new ButtonGroup();
        for (Difficulty d : Difficulty.values()) {
            JRadioButtonMenuItem item = Mnemonics.apply(new JRadioButtonMenuItem(), msg.get(PaddlePresenter.difficultyKey(d)));
            item.setName("game-" + d.id());
            item.addActionListener(e -> {
                presenter.setDifficulty(d);
                canvas.requestFocusInWindow();
            });
            group.add(item);
            game.add(item);
            difficultyItems.put(d, item);
        }
        game.addSeparator();
        game.add(sound);
        // No shortcut: P is the game's own pause key.
        game.add(item("menu.game.pause", "game-pause", 0, () -> presenter.pause()));
        game.add(menus.fullScreen(fullScreen::toggle));
        game.addSeparator();
        game.add(menus.highScores(() -> presenter.showScores()));
        game.addSeparator();
        game.add(menus.exit(this::dispose));
        JMenu help = menus.help();
        help.add(menus.about(msg.get("window.title"), () -> presenter.showAbout()));
        JMenuBar bar = new JMenuBar();
        bar.add(game);
        bar.add(help);
        return bar;
    }

    private JMenuItem item(String key, String name, int shortcut, Runnable action) {
        JMenuItem item = Mnemonics.apply(new JMenuItem(), msg.get(key));
        item.setName(name);
        if (shortcut != 0) item.setAccelerator(KeyStroke.getKeyStroke(shortcut, 0));
        item.addActionListener(e -> {
            action.run();
            canvas.requestFocusInWindow();
        });
        return item;
    }

    // ---- PaddleView ------------------------------------------------------------------------------

    @Override
    public void showOptions(Difficulty difficulty, boolean soundOn) {
        difficultyItems.get(difficulty).setSelected(true);
        sound.setSelected(soundOn);
    }

    @Override
    public Optional<String> askName(String message) {
        Optional<String> name = HighScoreDialog.askName(this, message, playerName);
        canvas.requestFocusInWindow();
        return name;
    }

    @Override
    public void showScores(Difficulty selected, int highlight) {
        List<HighScoreDialog.Table> tables = new ArrayList<>();
        for (Difficulty d : Difficulty.values()) {
            tables.add(new HighScoreDialog.Table(d.id(), Mnemonics.strip(msg.get(PaddlePresenter.difficultyKey(d)))));
        }
        HighScoreDialog.show(this, scores, tables, conceded -> msg.format("scores.result", conceded), selected.id(),
                highlight);
        canvas.requestFocusInWindow();
    }

    @Override
    public void showAbout() {
        // Swing's HTML only wraps lines with a fixed body width.
        JOptionPane.showMessageDialog(this,
                "<html><body style='width: 320px'><center><h2>"
                        + msg.get("window.title") + " " + AmadePaddleApplication.version() + "</h2>"
                        + "<p>" + msg.get("dialog.about.text") + "</p><br>"
                        + "<p>" + msg.get("dialog.about.rules") + "</p><br>"
                        + "<small>amade-paddle-game &bull; &copy; 2026 datazuul.com &bull; EUPL-1.2</small>"
                        + "</center></html>",
                Mnemonics.strip(menus.texts().format("menu.help.about", msg.get("window.title"))),
                JOptionPane.INFORMATION_MESSAGE);
        canvas.requestFocusInWindow();
    }
}
