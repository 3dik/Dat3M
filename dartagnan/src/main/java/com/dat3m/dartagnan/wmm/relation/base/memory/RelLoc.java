package com.dat3m.dartagnan.wmm.relation.base.memory;

import com.dat3m.dartagnan.program.utils.EType;
import com.dat3m.dartagnan.wmm.filter.FilterBasic;
import com.dat3m.dartagnan.wmm.relation.Relation;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Model;
import com.dat3m.dartagnan.program.event.Event;
import com.dat3m.dartagnan.program.event.MemEvent;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import com.dat3m.dartagnan.wmm.utils.TupleSet;
import com.dat3m.dartagnan.wmm.utils.Utils;

import java.util.Collection;
import java.util.Map;

import static com.dat3m.dartagnan.wmm.utils.Utils.edge;

public class RelLoc extends Relation {

    public RelLoc(){
        term = "loc";
    }

    @Override
    protected void _fillEnabledTuples(Map<Relation, TupleSet> map,
            Model model, int groupId){
        TupleSet tuples = map.get(this);
        for (Tuple t : Utils.executedTuples(model, getMaxTupleSet())){
            MemEvent e1 = (MemEvent)t.getFirst();
            MemEvent e2 = (MemEvent)t.getSecond();
            int mem1 = e1.getAddress().getIntValue(e1, ctx, model);
            int mem2 = e2.getAddress().getIntValue(e2, ctx, model);
            if (mem1 == mem2){
                tuples.add(t);
            }
        }
    }

    @Override
    public TupleSet getMaxTupleSet(){
        if(maxTupleSet == null){
            maxTupleSet = new TupleSet();
            Collection<Event> events = program.getCache().getEvents(FilterBasic.get(EType.MEMORY));
            for(Event e1 : events){
                for(Event e2 : events){
                    if(e1.getCId() != e2.getCId() && MemEvent.canAddressTheSameLocation((MemEvent) e1, (MemEvent)e2)){
                        maxTupleSet.add(new Tuple(e1, e2));
                    }
                }
            }
        }
        return maxTupleSet;
    }

    @Override
    protected BoolExpr encodeApprox() {
        BoolExpr enc = ctx.mkTrue();
        for(Tuple tuple : encodeTupleSet) {
            BoolExpr rel = edge(this.getName(), tuple.getFirst(), tuple.getSecond(), ctx);
            enc = ctx.mkAnd(enc, ctx.mkEq(rel, ctx.mkAnd(
                    ctx.mkAnd(tuple.getFirst().exec(), tuple.getSecond().exec()),
                    ctx.mkEq(((MemEvent)tuple.getFirst()).getMemAddressExpr(), ((MemEvent)tuple.getSecond()).getMemAddressExpr())
            )));
        }
        return enc;
    }
}
