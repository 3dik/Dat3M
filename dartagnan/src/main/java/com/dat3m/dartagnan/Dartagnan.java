package com.dat3m.dartagnan;

import static com.dat3m.dartagnan.utils.Result.FAIL;
import static com.dat3m.dartagnan.utils.Result.getResult;
import static com.dat3m.dartagnan.utils.Smt.exprLen;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Stack;

import org.apache.commons.cli.HelpFormatter;

import com.dat3m.dartagnan.asserts.AbstractAssert;
import com.dat3m.dartagnan.parsers.cat.ParserCat;
import com.dat3m.dartagnan.parsers.program.ProgramParser;
import com.dat3m.dartagnan.program.Program;
import com.dat3m.dartagnan.utils.CheckClock;
import com.dat3m.dartagnan.utils.Graph;
import com.dat3m.dartagnan.utils.Result;
import com.dat3m.dartagnan.utils.Settings;
import com.dat3m.dartagnan.utils.options.DartagnanOptions;
import com.dat3m.dartagnan.wmm.Wmm;
import com.dat3m.dartagnan.wmm.utils.Arch;
import com.microsoft.z3.Context;
import com.microsoft.z3.Solver;
import com.microsoft.z3.enumerations.Z3_ast_print_mode;
import com.microsoft.z3.BoolExpr;

public class Dartagnan {

    public static void main(String[] args) throws IOException {
        CheckClock.push("outer");

        DartagnanOptions options = new DartagnanOptions();
        try {
            options.parse(args);
        }
        catch (Exception e){
            if(e instanceof UnsupportedOperationException){
                System.out.println(e.getMessage());
            }
            new HelpFormatter().printHelp("DARTAGNAN", options);
            System.exit(1);
            return;
        }

        Wmm mcm = new ParserCat().parse(new File(options.getTargetModelFilePath()));
		Program p = new ProgramParser().parse(new File(options.getProgramFilePath()));
		
        Arch target = p.getArch();
        if(target == null){
            target = options.getTarget();
        }
        if(target == null) {
            System.out.println("Compilation target cannot be inferred");
            System.exit(0);
            return;
        }
        
        Context ctx = new Context();
        Solver s = ctx.mkSolver();
        Settings settings = options.getSettings();

        Result result = testProgram(s, ctx, p, mcm, target, settings);

        if(options.getProgramFilePath().endsWith(".litmus")) {
            System.out.println("Settings: " + options.getSettings());
            if(p.getAssFilter() != null){
                System.out.println("Filter " + (p.getAssFilter()));
            }
            System.out.println("Condition " + p.getAss().toStringWithType());
            System.out.println(result == Result.FAIL ? "Ok" : "No");        	
        } else {
        	System.out.println(result);
        }

        if(settings.getDrawGraph() && canDrawGraph(p.getAss(), result.equals(FAIL))) {
            ctx.setPrintMode(Z3_ast_print_mode.Z3_PRINT_SMTLIB_FULL);
            drawGraph(new Graph(s.getModel(), ctx, p, settings.getGraphRelations()), options.getGraphFilePath());
            System.out.println("Execution graph is written to " + options.getGraphFilePath());
        }

        ctx.close();

        CheckClock.pop(); // outer
        if (settings.getShowTimeStat()){
            System.out.println("start list time");
            CheckClock.print();
            System.out.println("end list time");
        }
    }

    public static Result testProgram(Solver solver, Context ctx, Program program, Wmm wmm, Arch target, Settings settings){
        boolean showEnc = settings.getShowEncStat();

    	program.unroll(settings.getBound(), 0);
        program.compile(target, 0);
        // AssertionInline depends on compiled events (copies)
        // Thus we need to set the assertion after compilation
        if(program.getAss() == null){
        	program.setAss(program.createAssertion());
        }

        CheckClock.push("encoding");

        BoolExpr encNDet = program.encodeUINonDet(ctx);
        solver.add(encNDet);
        BoolExpr encCF = program.encodeCF(ctx);
        solver.add(encCF);
        BoolExpr encRegs = program.encodeFinalRegisterValues(ctx);
        solver.add(encRegs);
        // wmm.encode would print the encoding size of each relation
        if (showEnc) {
            System.out.println("start list enc");
        }
        BoolExpr encRels = wmm.encode(program, ctx, settings);
        solver.add(encRels);
        BoolExpr encAxioms = wmm.consistent(program, ctx);
        solver.add(encAxioms);

        if (showEnc) {
            Stack<String> backup = CheckClock.popAll();
            CheckClock.push("exprLen");
            System.out.println("list delimeter");
            System.out.println("ndet: " + exprLen(encNDet));
            System.out.println("cf: " + exprLen(encCF));
            System.out.println("regs: " + exprLen(encRegs));
            System.out.println("rels: " + exprLen(encRels));
            System.out.println("axis: " + exprLen(encAxioms));
            CheckClock.pop();
            CheckClock.pushAll(backup);
        }

        CheckClock.pop();
        CheckClock.push("solve");
        // Used for getting the UNKNOWN
        // pop() is inside getResult
        solver.push();
        CheckClock.pop();
        CheckClock.push("encoding");
        BoolExpr encAss = program.getAss().encode(ctx);
        solver.add(encAss);
        if (showEnc) {
            System.out.println("ass: " + exprLen(encAss));
        }
        if(program.getAssFilter() != null){
            BoolExpr encAssF = program.getAssFilter().encode(ctx);
            solver.add(encAssF);
            if (showEnc) {
                Stack<String> backup = CheckClock.popAll();
                CheckClock.push("exprLen");
                System.out.println("assf: " + exprLen(encAssF));
                CheckClock.pop();
                CheckClock.pushAll(backup);
            }
        }
        if (showEnc) {
            System.out.println("end list enc");
        }
        CheckClock.pop();

        CheckClock.push("solve");
        Result result = getResult(solver, program, ctx);
        CheckClock.pop();
        return result;
    }

    public static boolean canDrawGraph(AbstractAssert ass, boolean result){
        String type = ass.getType();
        if(type == null){
            return result;
        }

        if(result){
            return type.equals(AbstractAssert.ASSERT_TYPE_EXISTS) || type.equals(AbstractAssert.ASSERT_TYPE_FINAL);
        }
        return type.equals(AbstractAssert.ASSERT_TYPE_NOT_EXISTS) || type.equals(AbstractAssert.ASSERT_TYPE_FORALL);
    }

    public static void drawGraph(Graph graph, String path) throws IOException {
        File newTextFile = new File(path);
        FileWriter fw = new FileWriter(newTextFile);
        fw.write(graph.toString());
        fw.close();
    }
}
