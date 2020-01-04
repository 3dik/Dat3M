package com.dat3m.dartagnan.wmm.relation.unary;

import com.dat3m.dartagnan.utils.Settings;
import com.dat3m.dartagnan.program.Program;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.dat3m.dartagnan.program.event.Event;
import com.dat3m.dartagnan.wmm.utils.Utils;
import com.dat3m.dartagnan.wmm.relation.Relation;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import com.dat3m.dartagnan.wmm.utils.TupleSet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.dat3m.dartagnan.wmm.utils.Utils.edge;
import static com.dat3m.dartagnan.wmm.utils.Utils.intCount;

/**
 *
 * @author Florian Furbach
 */
public class RelTransitive extends UnaryRelation {

    private Map<Event, Set<Event>> map;
    private TupleSet encodeSet;

    public static String makeTerm(Relation r1){
        return r1.getName() + "^+";
    }

    public RelTransitive(Relation r1) {
        super(r1);
        term = makeTerm(r1);
    }

    public RelTransitive(Relation r1, String name) {
        super(r1, name);
        term = makeTerm(r1);
    }

    @Override
    public void initialise(Program program, Context ctx, Settings settings){
        super.initialise(program, ctx, settings);
        encodeSet = new TupleSet();
        map = null;
    }

    @Override
    public TupleSet getMaySet(){
        if(maySet == null){
            map = r1.getMaySet().transMap();
            maySet = new TupleSet();
            for(Event e1 : map.keySet()){
                for(Event e2 : map.get(e1)){
                    maySet.add(new Tuple(e1, e2));
                }
            }
        }
        return maySet;
    }

    @Override
    public void addToActiveSet(TupleSet tuples){
        TupleSet activeSet = new TupleSet();
        activeSet.addAll(tuples);
        activeSet.removeAll(this.activeSet);
        this.activeSet.addAll(activeSet);
        activeSet.retainAll(maySet);

        TupleSet fullActiveSet = getFullEncodeTupleSet(activeSet);
        if(encodeSet.addAll(fullActiveSet)){
            fullActiveSet.retainAll(r1.getMaySet());
            r1.addToActiveSet(fullActiveSet);
        }
    }

    @Override
    protected BoolExpr encodeKnaster() {
        BoolExpr enc = ctx.mkTrue();

        for(Tuple tuple : encodeSet){
            BoolExpr orClause = ctx.mkFalse();

            Event e1 = tuple.getFirst();
            Event e2 = tuple.getSecond();

            if(r1.getMaySet().contains(new Tuple(e1, e2))){
                orClause = ctx.mkOr(orClause, Utils.edge(r1.getName(), e1, e2, ctx));
            }

            for(Event e3 : map.get(e1)){
                if(e3.getCId() != e1.getCId() && e3.getCId() != e2.getCId() && map.get(e3).contains(e2)){
                    orClause = ctx.mkOr(orClause, ctx.mkAnd(Utils.edge(this.getName(), e1, e3, ctx), Utils.edge(this.getName(), e3, e2, ctx)));
                }
            }

            enc = ctx.mkAnd(enc, ctx.mkEq(Utils.edge(this.getName(), e1, e2, ctx), orClause));
        }

        return enc;
    }

    @Override
    protected BoolExpr encodeIDL() {
        BoolExpr enc = ctx.mkTrue();

        for(Tuple tuple : encodeSet){
            Event e1 = tuple.getFirst();
            Event e2 = tuple.getSecond();

            BoolExpr orClause = ctx.mkFalse();
            for(Tuple tuple2 : encodeSet.getByFirst(e1)){
                if (!tuple2.equals(tuple)) {
                    Event e3 = tuple2.getSecond();
                    if (map.get(e3).contains(e2)) {
                        orClause = ctx.mkOr(orClause, ctx.mkAnd(
                                edge(this.getName(), e1, e3, ctx),
                                edge(this.getName(), e3, e2, ctx),
                                ctx.mkGt(intCount(this.idlConcatName(), e1, e2, ctx), intCount(this.getName(), e1, e3, ctx)),
                                ctx.mkGt(intCount(this.idlConcatName(), e1, e2, ctx), intCount(this.getName(), e3, e2, ctx))));
                    }
                }
            }

            enc = ctx.mkAnd(enc, ctx.mkEq(edge(this.idlConcatName(), e1, e2, ctx), orClause));

            orClause = ctx.mkFalse();
            for(Tuple tuple2 : encodeSet.getByFirst(e1)){
                if (!tuple2.equals(tuple)) {
                    Event e3 = tuple2.getSecond();
                    if (map.get(e3).contains(e2)) {
                        orClause = ctx.mkOr(orClause, ctx.mkAnd(
                                edge(this.getName(), e1, e3, ctx),
                                edge(this.getName(), e3, e2, ctx)));
                    }
                }
            }

            enc = ctx.mkAnd(enc, ctx.mkEq(edge(this.idlConcatName(), e1, e2, ctx), orClause));

            enc = ctx.mkAnd(enc, ctx.mkEq(edge(this.getName(), e1, e2, ctx), ctx.mkOr(
                    edge(r1.getName(), e1,e2, ctx),
                    ctx.mkAnd(edge(this.idlConcatName(), e1, e2, ctx), ctx.mkGt(intCount(this.getName(), e1, e2, ctx), intCount(this.idlConcatName(), e1, e2, ctx)))
            )));

            enc = ctx.mkAnd(enc, ctx.mkEq(edge(this.getName(),e1, e2, ctx), ctx.mkOr(
                    edge(r1.getName(), e1, e2, ctx),
                    edge(this.idlConcatName(), e1, e2, ctx)
            )));
        }

        return enc;
    }

    @Override
    protected BoolExpr encodeKleene() {
        BoolExpr enc = ctx.mkTrue();
        int iteration = 0;

        // Encode initial iteration
        Set<Tuple> currentTupleSet = new HashSet<>(r1.getActiveSet());
        for(Tuple tuple : currentTupleSet){
            enc = ctx.mkAnd(enc, ctx.mkEq(
                    Utils.edge(r1.getName() + "_" + iteration, tuple, ctx),
                    Utils.edge(r1.getName(), tuple, ctx)
            ));
        }

        while(true){
            Map<Tuple, Set<BoolExpr>> currentTupleMap = new HashMap<>();
            Set<Tuple> newTupleSet = new HashSet<>();

            // Original tuples from the previous iteration
            for(Tuple tuple : currentTupleSet){
                currentTupleMap.putIfAbsent(tuple, new HashSet<>());
                currentTupleMap.get(tuple).add(
                        Utils.edge(r1.getName() + "_" + iteration, tuple, ctx)
                );
            }

            // Combine tuples from the previous iteration
            for(Tuple tuple1 : currentTupleSet){
                Event e1 = tuple1.getFirst();
                Event e3 = tuple1.getSecond();
                for(Tuple tuple2 : currentTupleSet){
                    if(e3.getCId() == tuple2.getFirst().getCId()){
                        Event e2 = tuple2.getSecond();
                        Tuple newTuple = new Tuple(e1, e2);
                        currentTupleMap.putIfAbsent(newTuple, new HashSet<>());
                        currentTupleMap.get(newTuple).add(ctx.mkAnd(
                                Utils.edge(r1.getName() + "_" + iteration, e1, e3, ctx),
                                Utils.edge(r1.getName() + "_" + iteration, e3, e2, ctx)
                        ));

                        if(newTuple.getFirst().getCId() != newTuple.getSecond().getCId()){
                            newTupleSet.add(newTuple);
                        }
                    }
                }
            }

            iteration++;

            // Encode this iteration
            for(Tuple tuple : currentTupleMap.keySet()){
                BoolExpr orClause = ctx.mkFalse();
                for(BoolExpr expr : currentTupleMap.get(tuple)){
                    orClause = ctx.mkOr(orClause, expr);
                }

                BoolExpr edge = Utils.edge(r1.getName() + "_" + iteration, tuple, ctx);
                enc = ctx.mkAnd(enc, ctx.mkEq(edge, orClause));
            }

            if(!currentTupleSet.addAll(newTupleSet)){
                break;
            }
        }

        // Encode that transitive relation equals the relation at the last iteration
        for(Tuple tuple : activeSet){
            enc = ctx.mkAnd(enc, ctx.mkEq(
                    Utils.edge(getName(), tuple, ctx),
                    Utils.edge(r1.getName() + "_" + iteration, tuple, ctx)
            ));
        }

        return enc;
    }

    private TupleSet getFullEncodeTupleSet(TupleSet tuples){
        TupleSet processNow = new TupleSet();
        processNow.addAll(tuples);
        processNow.retainAll(getMaySet());

        TupleSet result = new TupleSet();

        while(!processNow.isEmpty()) {
            TupleSet processNext = new TupleSet();
            result.addAll(processNow);

            for (Tuple tuple : processNow) {
                Event e1 = tuple.getFirst();
                Event e2 = tuple.getSecond();
                for (Event e3 : map.get(e1)) {
                    if (e3.getCId() != e1.getCId() && e3.getCId() != e2.getCId()
                            && map.get(e3).contains(e2)) {
                        processNext.add(new Tuple(e1, e3));
                        processNext.add(new Tuple(e3, e2));
                    }
                }
            }
            processNext.removeAll(result);
            processNow = processNext;
        }
        return result;
    }

    private String idlConcatName(){
        return "(" + getName() + ";" + getName() + ")";
    }
}