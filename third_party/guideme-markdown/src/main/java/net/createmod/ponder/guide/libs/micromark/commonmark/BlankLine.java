package net.createmod.ponder.guide.libs.micromark.commonmark;

import net.createmod.ponder.guide.libs.micromark.CharUtil;
import net.createmod.ponder.guide.libs.micromark.Construct;
import net.createmod.ponder.guide.libs.micromark.State;
import net.createmod.ponder.guide.libs.micromark.TokenizeContext;
import net.createmod.ponder.guide.libs.micromark.Tokenizer;
import net.createmod.ponder.guide.libs.micromark.Types;
import net.createmod.ponder.guide.libs.micromark.factory.FactorySpace;
import net.createmod.ponder.guide.libs.micromark.symbol.Codes;

public final class BlankLine {
    private BlankLine() {
    }

    public static final Construct blankLine;

    static {
        blankLine = new Construct();
        blankLine.tokenize = (context, effects, ok, nok) -> new StateMachine(context, effects, ok, nok).initial;
        blankLine.partial = true;
    }

    private static class StateMachine {
        private final TokenizeContext context;
        private final Tokenizer.Effects effects;
        private final State ok;
        private final State nok;
        public final State initial;

        public StateMachine(TokenizeContext context, Tokenizer.Effects effects, State ok, State nok) {

            this.context = context;
            this.effects = effects;
            this.ok = ok;
            this.nok = nok;
            this.initial = FactorySpace.create(effects, this::afterWhitespace, Types.linePrefix);
        }

        /**
         * After zero or more spaces or tabs, before a line ending or EOF.
         * <p>
         * 
         * <pre>
         * > | ␠␠␊
         *       ^
         * > | ␊
         *     ^
         * </pre>
         */
        private State afterWhitespace(int code) {
            return code == Codes.eof || CharUtil.markdownLineEnding(code) ? ok.step(code) : nok.step(code);
        }
    }
}
