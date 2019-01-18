package dartagnan.utils;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.IntExpr;
import dartagnan.program.event.Event;
import dartagnan.wmm.utils.Tuple;

public class Utils {

	public static BoolExpr edge(String relName, Event e1, Event e2, Context ctx) {
		return ctx.mkBoolConst(relName + "(" + e1.repr() + "," + e2.repr() + ")");
	}

	public static BoolExpr edge(String relName, Tuple tuple, Context ctx) {
		return ctx.mkBoolConst(relName + "(" + tuple.getFirst().repr() + "," + tuple.getSecond().repr() + ")");
	}

	public static IntExpr intVar(String relName, Event e, Context ctx) {
		return ctx.mkIntConst(relName + "(" + e.repr() + ")");
	}
	
	public static IntExpr intCount(String relName, Event e1, Event e2, Context ctx) {
		return ctx.mkIntConst(relName + "(" + e1.repr() + "," + e2.repr() + ")");
	}

	public static IntExpr intCount(String relName, Tuple tuple, Context ctx) {
		return ctx.mkIntConst(relName + "(" + tuple.getFirst().repr() + "," + tuple.getSecond().repr() + ")");
	}
}