package net.createmod.ponder.guide.libs.micromark.commonmark;

import net.createmod.ponder.guide.libs.micromark.Assert;
import net.createmod.ponder.guide.libs.micromark.CharUtil;
import net.createmod.ponder.guide.libs.micromark.Construct;
import net.createmod.ponder.guide.libs.micromark.State;
import net.createmod.ponder.guide.libs.micromark.TokenizeContext;
import net.createmod.ponder.guide.libs.micromark.Tokenizer;
import net.createmod.ponder.guide.libs.micromark.Types;
import net.createmod.ponder.guide.libs.micromark.symbol.Codes;

public final class CharacterEscape {
    private CharacterEscape() {
    }

    public static final Construct characterEscape;

    static {
        characterEscape = new Construct();
        characterEscape.name = "characterEscape";
        characterEscape.tokenize = (context, effects, ok, nok) -> new StateMachine(context, effects, ok, nok)::start;
    }

    private static class StateMachine {
        private final TokenizeContext context;
        private final Tokenizer.Effects effects;
        private final State ok;
        private final State nok;

        public StateMachine(TokenizeContext context, Tokenizer.Effects effects, State ok, State nok) {
            this.context = context;
            this.effects = effects;
            this.ok = ok;
            this.nok = nok;
        }

        /**
         * Start of a character escape.
         *
         * <pre>
         * > | a\*b
         *      ^
         * </pre>
         */
        private State start(int code) {
            Assert.check(code == Codes.backslash, "expected `\\`");
            effects.enter(Types.characterEscape);
            effects.enter(Types.escapeMarker);
            effects.consume(code);
            effects.exit(Types.escapeMarker);
            return this::open;
        }

        /**
         * Inside a character escape, after `\`.
         *
         * <pre>
         * > | a\*b
         *       ^
         * </pre>
         */
        private State open(int code) {
            if (CharUtil.asciiPunctuation(code)) {
                effects.enter(Types.characterEscapeValue);
                effects.consume(code);
                effects.exit(Types.characterEscapeValue);
                effects.exit(Types.characterEscape);
                return ok;
            }

            return nok.step(code);
        }

    }
}
