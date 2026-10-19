package eu.amadeos.games.paddle.ui.main;

import eu.amadeos.commons.i18n.Language;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.commons.i18n.Mnemonics;
import eu.amadeos.commons.i18n.TranslationCheck;
import eu.amadeos.gameengine.swing.GameTexts;
import eu.amadeos.games.paddle.bootstrap.PaddleModule;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TranslationTest {
    @Test
    void everyLanguageHasEveryText() {
        assertEquals(Map.of(), TranslationCheck.problems(PaddleModule.class, PaddleModule.MESSAGES));
    }

    @Test
    void textsAreFormattedInTheLanguage() {
        Messages german = Messages.load(PaddleModule.class, PaddleModule.MESSAGES, Locale.GERMANY);
        assertEquals("Sie haben den Computer auf Schwer geschlagen, 11 zu 7.",
                german.format("scores.won", "Schwer", 11, 7));
    }

    /** The Game menu mixes the engine's entries with the game's own; no two may share a letter. */
    @Test
    void mnemonicsAreUniqueInTheGameMenu() {
        Map<String, String> clashes = new HashMap<>();
        for (Language language : Language.values()) {
            Messages engine = GameTexts.load(language.locale());
            Messages game = Messages.load(PaddleModule.class, PaddleModule.MESSAGES, language.locale());
            List<String> texts = List.of(game.get("menu.game.one-player"), game.get("menu.game.two-players"),
                    game.get("menu.game.easy"), game.get("menu.game.normal"), game.get("menu.game.hard"),
                    game.get("menu.game.sound"), game.get("menu.game.pause"), engine.get("menu.game.scores"), engine.get("menu.game.exit"));
            Map<Character, String> seen = new HashMap<>();
            for (String text : texts) {
                Mnemonics.Text t = Mnemonics.parse(text);
                if (t.index() < 0) {
                    clashes.put(language.code() + " " + text, "no mnemonic");
                    continue;
                }
                String other = seen.put(Character.toLowerCase(t.text().charAt(t.index())), text);
                if (other != null) clashes.put(language.code() + " " + text, other);
            }
        }
        assertEquals(Map.of(), clashes);
    }
}
