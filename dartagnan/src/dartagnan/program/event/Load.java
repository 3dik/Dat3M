package dartagnan.program.event;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import dartagnan.program.Location;
import dartagnan.program.Register;
import dartagnan.program.utils.EType;
import dartagnan.utils.MapSSA;
import dartagnan.utils.Pair;

import static dartagnan.utils.Utils.ssaLoc;
import static dartagnan.utils.Utils.ssaReg;

public class Load extends MemEvent {

	protected Register reg;
	protected int ssaRegIndex;
	
	public Load(Register reg, Location loc, String atomic) {
		this.reg = reg;
		this.loc = loc;
		this.condLevel = 0;
		this.atomic = atomic;
		addFilters(EType.ANY, EType.MEMORY, EType.READ);
	}

	@Override
	public Register getReg() {
		return reg;
	}

	@Override
	public String toString() {
		return nTimesCondLevel() + reg + " <- " + loc;
	}

	@Override
	public String label(){
		return "R[" + atomic + "] " + loc;
	}

	@Override
	public Load clone() {
		Register newReg = reg.clone();
		Location newLoc = loc.clone();
		Load newLoad = new Load(newReg, newLoc, atomic);
		newLoad.condLevel = condLevel;
		newLoad.setHLId(getHLId());
		newLoad.setUnfCopy(getUnfCopy());
		return newLoad;
	}

	@Override
	public Pair<BoolExpr, MapSSA> encodeDF(MapSSA map, Context ctx) {
		if(mainThread != null){
            Expr z3Reg = ssaReg(reg, map.getFresh(reg), ctx);
            Expr z3Loc = ssaLoc(loc, mainThread.getTId(), map.getFresh(loc), ctx);
            this.ssaLoc = z3Loc;
            this.ssaRegIndex = map.get(reg);
            return new Pair<>(ctx.mkImplies(executes(ctx), ctx.mkEq(z3Reg, z3Loc)), map);
		}
		throw new RuntimeException("Main thread is not set for " + toString());
	}

	@Override
	public int getSsaRegIndex() {
		return ssaRegIndex;
	}
}