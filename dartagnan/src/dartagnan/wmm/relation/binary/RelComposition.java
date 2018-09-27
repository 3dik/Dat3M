package dartagnan.wmm.relation.binary;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Z3Exception;
import dartagnan.program.Program;
import dartagnan.program.event.Event;
import dartagnan.utils.Utils;
import dartagnan.wmm.relation.Relation;
import dartagnan.wmm.utils.Tuple;
import dartagnan.wmm.utils.TupleSet;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Florian Furbach
 */
public class RelComposition extends BinaryRelation {

    public static String makeTerm(Relation r1, Relation r2){
        return "(" + r1.getName() + ";" + r2.getName() + ")";
    }

    private int lastEncodedIteration = -1;

    public RelComposition(Relation r1, Relation r2) {
        super(r1, r2);
        term = makeTerm(r1, r2);
    }

    public RelComposition(Relation r1, Relation r2, String name) {
        super(r1, r2, name);
        term = makeTerm(r1, r2);
    }

    @Override
    public void initialise(Program program){
        super.initialise(program);
        lastEncodedIteration = -1;
    }

    @Override
    public TupleSet getMaxTupleSet(){
        if(maxTupleSet == null){
            maxTupleSet = new TupleSet();
            TupleSet set1 = r1.getMaxTupleSet();
            TupleSet set2 = r2.getMaxTupleSet();
            for(Tuple rel1 : set1){
                for(Tuple rel2 : set2.getByFirst(rel1.getSecond())){
                    maxTupleSet.add(new Tuple(rel1.getFirst(), rel2.getSecond()));
                }
            }
        }
        return maxTupleSet;
    }

    @Override
    public TupleSet getMaxTupleSetRecursive(){
        if(recursiveGroupId > 0 && maxTupleSet != null){
            TupleSet set1 = r1.getMaxTupleSetRecursive();
            TupleSet set2 = r2.getMaxTupleSetRecursive();
            for(Tuple rel1 : set1){
                for(Tuple rel2 : set2.getByFirst(rel1.getSecond())){
                    maxTupleSet.add(new Tuple(rel1.getFirst(), rel2.getSecond()));
                }
            }
            return maxTupleSet;
        }
        return getMaxTupleSet();
    }

    @Override
    public void addEncodeTupleSet(TupleSet tuples){
        encodeTupleSet.addAll(tuples);

        Set<Tuple> activeSet = new HashSet<>(tuples);
        activeSet.retainAll(maxTupleSet);

        if(!activeSet.isEmpty()){

            Set<Event> startEvents = new HashSet<>();
            Set<Event> endEvents = new HashSet<>();
            for(Tuple tuple : activeSet){
                startEvents.add(tuple.getFirst());
                endEvents.add(tuple.getSecond());
            }

            TupleSet r1Candidates = new TupleSet();
            for(Tuple tuple : r1.getMaxTupleSet()){
                if(startEvents.contains(tuple.getFirst())){
                    r1Candidates.add(tuple);
                }
            }

            TupleSet r2Candidates = new TupleSet();
            for(Tuple tuple : r2.getMaxTupleSet()){
                if(endEvents.contains(tuple.getSecond())){
                    r2Candidates.add(tuple);
                }
            }

            TupleSet r1NewSet = new TupleSet();
            TupleSet r2NewSet = new TupleSet();

            for(Tuple tuple : activeSet){
                for(Tuple tuple1 : r1Candidates.getByFirst(tuple.getFirst())){
                    for(Tuple tuple2 : r2Candidates.getByFirst(tuple1.getSecond())){
                        if(tuple.getSecond().getEId().equals(tuple2.getSecond().getEId())){
                            r1NewSet.add(tuple1);
                            r2NewSet.add(tuple2);
                        }
                    }
                }
            }
            r1.addEncodeTupleSet(r1NewSet);
            r2.addEncodeTupleSet(r2NewSet);
        }
    }

    @Override
    protected BoolExpr encodeIdl(Context ctx) throws Z3Exception {
        BoolExpr enc = ctx.mkTrue();

        boolean recurseInR1 = (r1.getRecursiveGroupId() & recursiveGroupId) > 0;
        boolean recurseInR2 = (r2.getRecursiveGroupId() & recursiveGroupId) > 0;

        // TODO: A new attribute for this type of set
        TupleSet r1Set = new TupleSet();
        r1Set.addAll(r1.getEncodeTupleSet());
        r1Set.retainAll(r1.getMaxTupleSet());

        TupleSet r2Set = new TupleSet();
        r2Set.addAll(r2.getEncodeTupleSet());
        r2Set.retainAll(r2.getMaxTupleSet());

        for(Tuple tuple : encodeTupleSet){
            Event e1 = tuple.getFirst();
            Event e2 = tuple.getSecond();

            BoolExpr orClause = ctx.mkFalse();
            for(Tuple tuple1 : r1Set.getByFirst(e1)){
                Event e3 = tuple1.getSecond();
                for(Tuple tuple2 : r2Set.getByFirst(e3)){
                    if(tuple2.getSecond().getEId().equals(e2.getEId())){
                        BoolExpr opt1 = Utils.edge(r1.getName(), e1, e3, ctx);
                        BoolExpr opt2 = Utils.edge(r2.getName(), e3, e2, ctx);
                        orClause = ctx.mkOr(orClause, ctx.mkAnd(opt1, opt2));
                    }
                }
            }
            enc = ctx.mkAnd(enc, ctx.mkEq(Utils.edge(this.getName(), e1, e2, ctx), orClause));

            if(recurseInR1 || recurseInR2){
                orClause = ctx.mkFalse();
                for(Tuple tuple1 : r1Set.getByFirst(e1)){
                    Event e3 = tuple1.getSecond();
                    for(Tuple tuple2 : r2Set.getByFirst(e3)){
                        if(tuple2.getSecond().getEId().equals(e2.getEId())){
                            BoolExpr opt1 = Utils.edge(r1.getName(), e1, e3, ctx);
                            if(recurseInR1){
                                opt1 = ctx.mkAnd(opt1, ctx.mkGt(Utils.intCount(this.getName(), e1, e2, ctx), Utils.intCount(r1.getName(), e1, e3, ctx)));
                            }

                            BoolExpr opt2 = Utils.edge(r2.getName(), e3, e2, ctx);
                            if(recurseInR2){
                                opt2 = ctx.mkAnd(opt2, ctx.mkGt(Utils.intCount(this.getName(), e1, e2, ctx), Utils.intCount(r1.getName(), e3, e2, ctx)));
                            }

                            orClause = ctx.mkOr(orClause, ctx.mkAnd(opt1, opt2));
                        }
                    }
                }
                enc = ctx.mkAnd(enc, ctx.mkEq(Utils.edge(this.getName(), e1, e2, ctx), orClause));
            }

        }
        return enc;
    }

    @Override
    protected BoolExpr encodeApprox(Context ctx) throws Z3Exception {
        BoolExpr enc = ctx.mkTrue();

        // TODO: A new attribute for this type of set
        TupleSet r1Set = new TupleSet();
        r1Set.addAll(r1.getEncodeTupleSet());
        r1Set.retainAll(r1.getMaxTupleSet());

        TupleSet r2Set = new TupleSet();
        r2Set.addAll(r2.getEncodeTupleSet());
        r2Set.retainAll(r2.getMaxTupleSet());

        for(Tuple tuple : encodeTupleSet){
            Event e1 = tuple.getFirst();
            Event e2 = tuple.getSecond();

            BoolExpr orClause = ctx.mkFalse();
            for(Tuple tuple1 : r1Set.getByFirst(e1)){
                Event e3 = tuple1.getSecond();
                for(Tuple tuple2 : r2Set.getByFirst(e3)){
                    if(tuple2.getSecond().getEId().equals(e2.getEId())){
                        BoolExpr opt1 = Utils.edge(r1.getName(), e1, e3, ctx);
                        BoolExpr opt2 = Utils.edge(r2.getName(), e3, e2, ctx);
                        orClause = ctx.mkOr(orClause, ctx.mkAnd(opt1, opt2));
                    }
                }
            }

            if (Relation.PostFixApprox) {
                enc = ctx.mkAnd(enc, ctx.mkImplies(orClause, Utils.edge(this.getName(), e1, e2, ctx)));
            } else {
                enc = ctx.mkAnd(enc, ctx.mkEq(Utils.edge(this.getName(), e1, e2, ctx), orClause));
            }
        }
        return enc;
    }

    @Override
    public BoolExpr encodeIteration(int groupId, Context ctx, int iteration){
        BoolExpr enc = ctx.mkTrue();

        if((groupId & recursiveGroupId) > 0 && iteration > lastEncodedIteration) {
            lastEncodedIteration = iteration;
            String name = this.getName() + "_" + iteration;

            if(iteration == 0 && isRecursive){
                for(Tuple tuple : encodeTupleSet){
                    enc = ctx.mkAnd(ctx.mkNot(Utils.edge(name, tuple.getFirst(), tuple.getSecond(), ctx)));
                }

            } else {
                int childIteration = isRecursive ? iteration - 1 : iteration;

                boolean recurseInR1 = (r1.getRecursiveGroupId() & groupId) > 0;
                boolean recurseInR2 = (r2.getRecursiveGroupId() & groupId) > 0;

                String r1Name = recurseInR1 ? r1.getName() + "_" + childIteration : r1.getName();
                String r2Name = recurseInR2 ? r2.getName() + "_" + childIteration : r2.getName();

                TupleSet r1Set = new TupleSet();
                r1Set.addAll(r1.getEncodeTupleSet());
                r1Set.retainAll(r1.getMaxTupleSet());

                TupleSet r2Set = new TupleSet();
                r2Set.addAll(r2.getEncodeTupleSet());
                r2Set.retainAll(r2.getMaxTupleSet());

                for(Tuple tuple : encodeTupleSet){
                    BoolExpr orClause = ctx.mkFalse();

                    Event e1 = tuple.getFirst();
                    Event e2 = tuple.getSecond();

                    for(Tuple tuple1 : r1Set.getByFirst(e1)){
                        Event e3 = tuple1.getSecond();

                        for(Tuple tuple2 : r2Set.getByFirst(e3)){
                            if(tuple2.getSecond().getEId().equals(e2.getEId())){
                                BoolExpr opt1 = Utils.edge(r1Name, e1, e3, ctx);
                                BoolExpr opt2 = Utils.edge(r2Name, e3, e2, ctx);
                                orClause = ctx.mkOr(orClause, ctx.mkAnd(opt1, opt2));
                            }
                        }
                    }

                    BoolExpr edge = Utils.edge(name, tuple.getFirst(), tuple.getSecond(), ctx);
                    enc = ctx.mkAnd(enc, ctx.mkEq(edge, orClause));
                }

                if(recurseInR1){
                    enc = ctx.mkAnd(enc, r1.encodeIteration(groupId, ctx, childIteration));
                }

                if(recurseInR2){
                    enc = ctx.mkAnd(enc, r2.encodeIteration(groupId, ctx, childIteration));
                }
            }
        }

        return enc;
    }
}
