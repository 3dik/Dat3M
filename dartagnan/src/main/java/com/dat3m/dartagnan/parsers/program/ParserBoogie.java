package com.dat3m.dartagnan.parsers.program;

import com.dat3m.dartagnan.parsers.BoogieLexer;
import com.dat3m.dartagnan.parsers.BoogieParser;
import com.dat3m.dartagnan.parsers.program.utils.ParserErrorListener;
import com.dat3m.dartagnan.parsers.program.utils.ProgramBuilder;
import com.dat3m.dartagnan.parsers.program.visitors.VisitorBoogie;
import com.dat3m.dartagnan.program.Program;
import com.dat3m.dartagnan.wmm.utils.Arch;

import org.antlr.v4.runtime.*;

class ParserBoogie implements ParserInterface{

    @Override
    public Program parse(CharStream charStream) {
        BoogieLexer lexer = new BoogieLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);

        BoogieParser parser = new BoogieParser(tokenStream);
        parser.addErrorListener(new ParserErrorListener());
        ProgramBuilder pb = new ProgramBuilder();
        ParserRuleContext parserEntryPoint = parser.main();
        VisitorBoogie visitor = new VisitorBoogie(pb);

        Program program = (Program) parserEntryPoint.accept(visitor);
        program.setArch(Arch.NONE);
        return program;
    }
}
