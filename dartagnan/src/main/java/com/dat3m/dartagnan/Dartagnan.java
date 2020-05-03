package com.dat3m.dartagnan;

import static com.dat3m.dartagnan.utils.Result.FAIL;
import static com.dat3m.dartagnan.utils.Result.interpretCheckResult;
import static com.dat3m.dartagnan.utils.Smt.exprLen;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Stack;

import org.apache.commons.cli.HelpFormatter;

import com.dat3m.dartagnan.asserts.AbstractAssert;
import com.dat3m.dartagnan.cegar.ApproxAll;
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
import com.microsoft.z3.Status;

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
        if (settings.getShowEncStat()){
            System.out.println("start list enc");
        }
        CheckClock.push("encoding");
        prepare(solver, ctx, program, wmm, target, settings);
        ApproxAll approx = new ApproxAll();
        approx.initialApprox();

        Status status = null;
        while (true){
            push(solver); // the step encoding and the assertion come next

            encodeStep(solver, ctx, program, wmm, settings);

            Stack<String> backup = CheckClock.popAll();
            CheckClock.push("solve");
            status = solver.check();
            CheckClock.pop();
            CheckClock.pushAll(backup);

            if (Status.SATISFIABLE == status){
                if (approx.trueModel(solver.getModel())){
                    break;
                }
            } else if (approx.trueCore(solver.getUnsatCore())){
                break;
            }
            solver.pop(); // assertion
            solver.pop(); // step encoding
        }
        CheckClock.pop();
        if (settings.getShowEncStat()){
            System.out.println("end list enc");
        }

        CheckClock.push("solve");
        Result res = interpretCheckResult(status, solver, program, ctx);
        CheckClock.pop();
        return res;
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

    private static void prepare(Solver solver, Context ctx, Program program,
            Wmm wmm, Arch target, Settings settings){
    	program.unroll(settings.getBound(), 0);
        program.compile(target, 0);
        // AssertionInline depends on compiled events (copies)
        // Thus we need to set the assertion after compilation
        if(program.getAss() == null){
        	program.setAss(program.createAssertion());
        }

        BoolExpr encNDet = program.encodeUINonDet(ctx);
        solver.add(encNDet);
        BoolExpr encCF = program.encodeCF(ctx);
        solver.add(encCF);
        BoolExpr encRegs = program.encodeFinalRegisterValues(ctx);
        solver.add(encRegs);
        if (settings.getShowEncStat()){
            System.out.println("ndet: " + exprLen(encNDet));
            System.out.println("cf: " + exprLen(encCF));
            System.out.println("regs: " + exprLen(encRegs));
        }

        wmm.prepare(program, settings);
    }

    private static void encodeStep(Solver solver, Context ctx,
            Program program, Wmm wmm, Settings settings){
        // these encoding functions can print the size of each encoding part
        if (settings.getShowEncStat()){
            System.out.println("list delimeter");
        }
        BoolExpr encRels = wmm.encode(ctx, settings);
        solver.add(encRels);
        if (settings.getShowEncStat()){
            System.out.println("list delimeter");
        }
        BoolExpr encAxioms = wmm.consistent(program, ctx, settings);
        solver.add(encAxioms);

        if (settings.getShowEncStat()){
            System.out.println("list delimeter");
            System.out.println("rels: " + exprLen(encRels));
            System.out.println("axis: " + exprLen(encAxioms));
        }

        // Used for getting the UNKNOWN
        // pop() is inside interpretCheckResult
        push(solver);
        BoolExpr encAss = program.getAss().encode(ctx);
        solver.add(encAss);
        if (settings.getShowEncStat()){
            System.out.println("ass: " + exprLen(encAss));
        }
        if(program.getAssFilter() != null){
            BoolExpr encAssF = program.getAssFilter().encode(ctx);
            solver.add(encAssF);
            if (settings.getShowEncStat()){
                System.out.println("assf: " + exprLen(encAssF));
            }
        }
    }

    private static void push(Solver solver){
        Stack<String> backup = CheckClock.popAll();
        CheckClock.push("solve");
        solver.push();
        CheckClock.pop();
        CheckClock.pushAll(backup);
    }
}
