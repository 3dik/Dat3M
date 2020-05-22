package com.dat3m.dartagnan.cegar.ua;

import com.dat3m.dartagnan.cegar.ua.UnderApproximation;
import com.dat3m.dartagnan.program.event.Event;
import com.dat3m.dartagnan.program.event.MemEvent;
import com.dat3m.dartagnan.program.Program;
import com.dat3m.dartagnan.program.Thread;
import com.dat3m.dartagnan.program.utils.EType;
import com.dat3m.dartagnan.wmm.filter.FilterBasic;
import com.dat3m.dartagnan.wmm.relation.base.memory.RelRf;
import com.microsoft.z3.BoolExpr;

import java.util.List;
import java.util.HashSet;

public class ApproxRf extends UnderApproximation {
    private Program program;
    private RelRf rf;

    public ApproxRf(Program program, RelRf rf){
        this.program = program;
        this.rf = rf;
    }

    public void initialApprox(){
        List<Event> inits = program.getCache().getEvents(FilterBasic.get(EType.INIT));
        for (Thread t : program.getThreads()){
            HashSet<Event> writes = new HashSet<>(inits);
            writes.addAll(t.getCache().getEvents(FilterBasic.get(EType.WRITE)));

            HashSet<MemEvent> whitelist = new HashSet<>();
            for (Event w : writes){
                whitelist.add((MemEvent)w);
            }

            for (Event r : t.getCache().getEvents(FilterBasic.get(EType.READ))){
                rf.setApproxWhitelist((MemEvent)r, whitelist);
            }
        }
    }

    public boolean trueCore(BoolExpr[] core){
        for (BoolExpr assumption : core){
            rf.disableApproxWhitelist(rf.getApproxedRead(assumption));
        }
        return 0 == core.length;
    }
}
