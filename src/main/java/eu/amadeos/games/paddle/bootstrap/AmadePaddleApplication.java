package eu.amadeos.games.paddle.bootstrap;

import eu.amadeos.commons.app.AmadeApplication;
import eu.amadeos.commons.app.AppInfo;
import eu.amadeos.games.paddle.ui.game.PaddleGame;
import eu.amadeos.games.paddle.ui.main.PaddlePresenter;
import eu.amadeos.games.paddle.ui.main.PaddleWindow;

public final class AmadePaddleApplication {
    public static final AppInfo APP = new AppInfo("amade-paddle-game", "Paddle", "paddle");

    private AmadePaddleApplication() {
    }

    /** The version from the jar's manifest; "dev" when run from the IDE. */
    public static String version() {
        String version = AmadePaddleApplication.class.getPackage().getImplementationVersion();
        return version == null ? "dev" : version;
    }

    // --8<-- [start:main]
    public static void main(String[] args) {
        AmadeApplication.launch(APP, injector -> {
            PaddleWindow window = injector.getInstance(PaddleWindow.class);
            PaddlePresenter presenter = injector.getInstance(PaddlePresenter.class);
            PaddleGame game = injector.getInstance(PaddleGame.class);
            window.bind(presenter);
            presenter.start();
            // Not a windowClosed listener: switching to full screen closes and reopens the window.
            window.whenClosed(() -> {
                game.stop();
                System.exit(0);
            });
            window.setLocationByPlatform(true);
            window.setVisible(true);
            // The loop starts once the canvas is on the screen and can get its buffers.
            game.start(window.canvas());
        }, new PaddleModule());
    }
    // --8<-- [end:main]
}
