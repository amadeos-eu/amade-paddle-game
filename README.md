# amade-paddle-game

**amade-paddle-game** is the classic ball game for two paddles on the [amadeOS](https://github.com/amadeos-eu/amadeos) desktop. Built on [amade-game-engine](https://github.com/amadeos-eu/amade-game-engine); runs on other operating systems as well.

![License](https://img.shields.io/badge/license-EUPL--1.2-blue)
![Java](https://img.shields.io/badge/Java-25-orange)

## Playing

- **One player** (1, Enter or F2): you play the **right** paddle with the **arrow keys**, the computer the left one. Three difficulties: *Easy*, *Normal*, *Hard*.
- **Two players** (2 or F3) on one keyboard: left **W** and **S**, right the **arrow keys**.
- Where the ball meets the paddle decides its angle: the middle sends it straight back, the edges steeply. Every hit makes the ball faster.
- The first to **11** points wins. **P** or **Esc** pauses, and so does leaving the window.
- **Best results** against the computer, ten per difficulty: the fewer points the computer made, the better.
- On the title screen the computer plays itself.

The keys can be changed in `~/.config/amadeos/paddle/paddle.properties`, with the names Java gives them, several separated by spaces:

```properties
keys.left-up=W
keys.left-down=S
keys.right-up=UP NUMPAD8
keys.right-down=DOWN NUMPAD2
keys.pause=P ESCAPE
```

Not every keyboard reports every combination of keys held at the same time ("ghosting"). W/S together with the arrow keys works on practically all of them.

The window can be resized; the court scales with it. Texts in English, Deutsch, Français, Español and Italiano.

## Files

| What | Where |
|---|---|
| Settings (difficulty, keys) | `~/.config/amadeos/paddle/paddle.properties` |
| Name for high scores | `~/.config/amadeos/paddle/player.properties` |
| Best results | `~/.local/share/amadeos/games/paddle/easy.scores`, `normal.scores`, `hard.scores` |

## Building

```bash
./gradlew test          # the window test needs a display (xvfb-run on servers)
./gradlew run
./gradlew deb           # build/distributions/amade-paddle-game_<version>_all.deb
./gradlew installToAmadeOs   # copies the package to ../amadeos/config/packages.chroot/
```

With `../amade-game-engine` (and `../amade-commons`) checked out next to this repository, Gradle builds those checkouts instead of the JitPack versions.

## Structure

A real-time game: the rules are stepped by a game loop, not driven by events.

- `domain`: the rules without user interface or time of their own. `Match` (paddles, ball, walls, collisions checked over the whole step, scoring), `ComputerPlayer` (looks at the ball now and then, predicts where it arrives, aims with an error, moves at a limited speed), `Difficulty`, `Court` (sizes and speeds), `Mode`, `Side`, `MatchResult`. Whole matches are simulated in tests, including ten minutes of the demo.
- `repository`: `PaddleSettings` (difficulty, keys).
- `ui.game`: the running game on the loop thread. `PaddleGame` (loop, controls, scenes: title, match, pause, end), `CourtPainter` (court, net, block digits, paddles and ball drawn between two steps), `Arena` (what the presenter may ask of the game, from the Swing thread).
- `ui.main`: Model-View-Presenter for everything around the court: `PaddlePresenter` (menu commands, difficulty, pausing, best results), `PaddleView`, `PaddleWindow` (menu and the engine's `GameCanvas`).

From the engine: `GameLoop` (fixed steps of 1/120 s, about 60 frames a second), `GameCanvas` (active rendering in logical coordinates, scaled to the window), `Keyboard` and `Controls` (keys mapped to actions, X11 key repeat filtered), `SceneStack`.

Comments of the form `// --8<-- [start:name]` mark the excerpts the [Game Developer's Handbook](https://datazuul.github.io/amade-game-engine/) quotes.

## Licence

[EUPL-1.2](LICENSE)
