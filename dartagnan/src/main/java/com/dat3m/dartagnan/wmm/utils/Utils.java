package com.dat3m.dartagnan.wmm.utils;

import com.dat3m.dartagnan.program.Register;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;
import com.microsoft.z3.Model;
import com.dat3m.dartagnan.program.event.Event;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import com.dat3m.dartagnan.wmm.utils.TupleSet;

public class Utils {

	public static BoolExpr edge(String relName, Event e1, Event e2, Context ctx) {
		return ctx.mkBoolConst(relName + "(" + e1.repr() + "," + e2.repr() + ")");
	}

	public static BoolExpr bindRegVal(Register register, Event w, Event r, Context ctx){
		return ctx.mkBoolConst("BindRegVal(E" + w.getCId() + ",E" + r.getCId() + "," + register.getName() + ")");
	}

	public static IntExpr intVar(String relName, Event e, Context ctx) {
		return ctx.mkIntConst(relName + "(" + e.repr() + ")");
	}
	
	public static IntExpr intCount(String relName, Event e1, Event e2, Context ctx) {
		return ctx.mkIntConst(relName + "(" + e1.repr() + "," + e2.repr() + ")");
	}

    public static TupleSet enabledTuples(String relName, TupleSet candidates,
            Context ctx, Model model){
        TupleSet result = new TupleSet();
        for (Tuple c : candidates){
            BoolExpr edge = edge(relName, c.getFirst(), c.getSecond(), ctx);
            Expr value = model.getConstInterp(edge);
            if (value != null && value.isTrue()){
                result.add(c);
            }
        }
        return result;
    }

    public static TupleSet executedTuples(Model model, TupleSet tuples){
        TupleSet result = new TupleSet();
        for (Tuple t : tuples){
            boolean lE = model.getConstInterp(t.getFirst().exec()).isTrue();
            boolean rE = model.getConstInterp(t.getSecond().exec()).isTrue();
            if (lE && rE){
                result.add(t);
            }
        }
        return result;
    }
}
