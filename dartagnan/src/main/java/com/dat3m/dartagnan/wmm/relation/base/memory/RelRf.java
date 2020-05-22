package com.dat3m.dartagnan.wmm.relation.base.memory;

import com.dat3m.dartagnan.program.utils.EType;
import com.dat3m.dartagnan.utils.Settings;
import com.dat3m.dartagnan.wmm.filter.FilterBasic;
import com.dat3m.dartagnan.wmm.filter.FilterUnion;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Model;
import com.dat3m.dartagnan.program.event.Event;
import com.dat3m.dartagnan.program.event.MemEvent;
import com.dat3m.dartagnan.program.Program;
import com.dat3m.dartagnan.wmm.relation.Relation;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import com.dat3m.dartagnan.wmm.utils.TupleSet;
import com.dat3m.dartagnan.wmm.utils.Utils;

import java.util.*;

import static com.dat3m.dartagnan.wmm.utils.Utils.edge;

public class RelRf extends Relation {

    private Map<MemEvent, Set<MemEvent>> approxWhitelists;
    private Map<BoolExpr, MemEvent> assumptions;

    public RelRf(){
        term = "rf";
        forceDoEncode = true;
        approxWhitelists = new HashMap<>();
        assumptions = new HashMap<>();
    }

    @Override
    public void initialise(Program program, Context ctx, Settings settings){
        super.initialise(program, ctx, settings);
        assumptions.clear();
    }

    @Override
    protected void _fillEnabledTuples(Map<Relation, TupleSet> map,
            Model model, int groupId){
        TupleSet tuples = map.get(this);
        tuples.addAll(Utils.enabledTuples("rf", getMaxTupleSet(), ctx, model));
    }

    @Override
    public Set<BoolExpr> getAssumptions(){
        return assumptions.keySet();
    }

    public MemEvent getApproxedRead(BoolExpr assumption){
        return assumptions.get(assumption);
    }

    public void setApproxWhitelist(MemEvent read, Set<MemEvent> writes){
        approxWhitelists.put(read, writes);
    }

    public void disableApproxWhitelist(MemEvent read){
        approxWhitelists.remove(read);
    }

    @Override
    public TupleSet getMaxTupleSet(){
        if(maxTupleSet == null){
            maxTupleSet = new TupleSet();

            List<Event> eventsLoad = program.getCache().getEvents(FilterBasic.get(EType.READ));
            List<Event> eventsStore = program.getCache().getEvents(FilterUnion.get(
                    FilterBasic.get(EType.WRITE),
                    FilterBasic.get(EType.INIT)
            ));

            for(Event e1 : eventsStore){
                for(Event e2 : eventsLoad){
                    MemEvent me1 = (MemEvent) e1;
                    MemEvent me2 = (MemEvent) e2;

                    boolean may = !approxWhitelists.containsKey(me2)
                        || approxWhitelists.get(me2).contains(me1);
                    may &= MemEvent.canAddressTheSameLocation(me1, me2);

                    if (may){
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
        Map<MemEvent, List<BoolExpr>> edgeMap = new HashMap<>();
        Map<MemEvent, BoolExpr> memInitMap = new HashMap<>();

        boolean canAccNonInitMem = settings.getFlag(Settings.FLAG_CAN_ACCESS_UNINITIALIZED_MEMORY);
        boolean useSeqEncoding = settings.getFlag(Settings.FLAG_USE_SEQ_ENCODING_REL_RF);

        for(Tuple tuple : maxTupleSet){
            MemEvent w = (MemEvent) tuple.getFirst();
            MemEvent r = (MemEvent) tuple.getSecond();
            BoolExpr edge = edge(term, w, r, ctx);
            BoolExpr sameAddress = ctx.mkEq(w.getMemAddressExpr(), r.getMemAddressExpr());
            BoolExpr sameValue = ctx.mkEq(w.getMemValueExpr(), r.getMemValueExpr());

            edgeMap.putIfAbsent(r, new ArrayList<>());
            edgeMap.get(r).add(edge);
            if(canAccNonInitMem && w.is(EType.INIT)){
                memInitMap.put(r, ctx.mkOr(memInitMap.getOrDefault(r, ctx.mkFalse()), sameAddress));
            }
            enc = ctx.mkAnd(enc, ctx.mkImplies(edge, ctx.mkAnd(w.exec(), r.exec(), sameAddress, sameValue)));
        }

        for(MemEvent r : edgeMap.keySet()){
            enc = ctx.mkAnd(enc, useSeqEncoding
                    ? encodeEdgeSeq(r, memInitMap.get(r), edgeMap.get(r))
                    : encodeEdgeNaive(r, memInitMap.get(r), edgeMap.get(r)));
        }
        return enc;
    }

    private BoolExpr encodeEdgeNaive(MemEvent read, BoolExpr isMemInit, List<BoolExpr> edges){
        BoolExpr atMostOne = ctx.mkTrue();
        BoolExpr atLeastOne = ctx.mkFalse();
        for(int i = 0; i < edges.size(); i++){
            atLeastOne = ctx.mkOr(atLeastOne, edges.get(i));
            for(int j = i + 1; j < edges.size(); j++){
                atMostOne = ctx.mkAnd(atMostOne, ctx.mkNot(ctx.mkAnd(edges.get(i), edges.get(j))));
            }
        }

        atLeastOne = processAtLeastOne(atLeastOne, read, isMemInit);
        return ctx.mkAnd(atMostOne, atLeastOne);
    }

    private BoolExpr encodeEdgeSeq(MemEvent read, BoolExpr isMemInit, List<BoolExpr> edges){
        int num = edges.size();
        int readId = read.getCId();
        BoolExpr lastSeqVar = mkSeqVar(readId, 0);
        BoolExpr newSeqVar = lastSeqVar;
        BoolExpr atMostOne = ctx.mkEq(lastSeqVar, edges.get(0));

        for(int i = 1; i < num; i++){
            newSeqVar = mkSeqVar(readId, i);
            atMostOne = ctx.mkAnd(atMostOne, ctx.mkEq(newSeqVar, ctx.mkOr(lastSeqVar, edges.get(i))));
            atMostOne = ctx.mkAnd(atMostOne, ctx.mkNot(ctx.mkAnd(edges.get(i), lastSeqVar)));
            lastSeqVar = newSeqVar;
        }
        BoolExpr atLeastOne = ctx.mkOr(newSeqVar, edges.get(edges.size() - 1));

        atLeastOne = processAtLeastOne(atLeastOne, read, isMemInit);
        return ctx.mkAnd(atMostOne, atLeastOne);
    }

    private BoolExpr processAtLeastOne(BoolExpr enc, MemEvent read,
            BoolExpr isMemInit){
        if(settings.getFlag(Settings.FLAG_CAN_ACCESS_UNINITIALIZED_MEMORY)) {
            enc = ctx.mkImplies(ctx.mkAnd(read.exec(), isMemInit), enc);
        } else {
            enc = ctx.mkImplies(read.exec(), enc);
        }

        if (approxWhitelists.containsKey(read)){
            String assumptionName = "rf_assumption(" + read.repr() + ")";
            BoolExpr assumption = ctx.mkBoolConst(assumptionName);
            assumptions.put(assumption, read);

            enc = ctx.mkImplies(assumption, enc);
        }

        return enc;
    }

    private BoolExpr mkSeqVar(int readId, int i) {
        return (BoolExpr) ctx.mkConst("s(" + term + ",E" + readId + "," + i + ")", ctx.mkBoolSort());
    }
}
