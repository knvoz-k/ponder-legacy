package net.createmod.ponder.guide.libs.micromark.commonmark;

import net.createmod.ponder.guide.libs.micromark.Assert;
import net.createmod.ponder.guide.libs.micromark.CharUtil;
import net.createmod.ponder.guide.libs.micromark.Construct;
import net.createmod.ponder.guide.libs.micromark.State;
import net.createmod.ponder.guide.libs.micromark.TokenizeContext;
import net.createmod.ponder.guide.libs.micromark.Tokenizer;
import net.createmod.ponder.guide.libs.micromark.Types;
import net.createmod.ponder.guide.libs.micromark.factory.FactorySpace;

public final class LineEnding {
    private LineEnding() {
    }

    public static final Construct lineEnding;

    static {
        lineEnding = new Construct();
        lineEnding.name = "lineEnding";
        lineEnding.tokenize = (context, effects, ok, nok) -> new StateMachine(context, effects, ok, nok)::start;
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

        private State start(int code) {
            Assert.check(CharUtil.markdownLineEnding(code), "expected eol");
            effects.enter(Types.lineEnding);
            effects.consume(code);
            effects.exit(Types.lineEnding);
            return FactorySpace.create(effects, ok, Types.linePrefix);
        }
    }
}
