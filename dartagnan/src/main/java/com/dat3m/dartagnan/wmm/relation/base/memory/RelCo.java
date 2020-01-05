package com.dat3m.dartagnan.wmm.relation.base.memory;

import com.dat3m.dartagnan.program.memory.Location;
import com.dat3m.dartagnan.program.memory.Memory;
import com.dat3m.dartagnan.program.utils.EType;
import com.dat3m.dartagnan.wmm.filter.FilterBasic;
import com.dat3m.dartagnan.wmm.filter.FilterMinus;
import com.dat3m.dartagnan.wmm.filter.FilterUnion;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.IntExpr;
import com.dat3m.dartagnan.program.event.Event;
import com.dat3m.dartagnan.program.event.MemEvent;
import com.dat3m.dartagnan.program.memory.Address;
import com.dat3m.dartagnan.wmm.utils.Utils;
import com.dat3m.dartagnan.wmm.relation.Relation;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import com.dat3m.dartagnan.wmm.utils.TupleSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.dat3m.dartagnan.wmm.utils.Utils.edge;
import static com.dat3m.dartagnan.wmm.utils.Utils.intVar;

public class RelCo extends Relation {

    public RelCo(){
        term = "co";
        forceDoEncode = true;
    }

    @Override
    public TupleSet getMaySet(){
        if(maySet == null){
            maySet = new TupleSet();
            List<Event> storeNonInit = program.getCache().getEvents(FilterMinus.get(
                    FilterBasic.get(EType.WRITE),
                    FilterUnion.get(FilterBasic.get(EType.UNINIT), FilterBasic.get(EType.INIT))
            ));

            for(Event e1 : program.getCache().getEvents(FilterBasic.get(EType.UNINIT))){
                for(Event e2 : program.getCache().getEvents(FilterBasic.get(EType.INIT))){
                    if(MemEvent.canAddressTheSameLocation((MemEvent) e1, (MemEvent)e2)){
                        maySet.add(new Tuple(e1, e2));
                    }
                }
            }
            for(Event e1 : program.getCache().getEvents(FilterBasic.get(EType.WRITE))){
                for(Event e2 : storeNonInit){
                    if(e1.getCId() != e2.getCId() && MemEvent.canAddressTheSameLocation((MemEvent) e1, (MemEvent)e2)){
                        maySet.add(new Tuple(e1, e2));
                    }
                }
            }
        }
        return maySet;
    }

    @Override
    protected BoolExpr encodeKnaster() {
        BoolExpr enc = encodeUninitializedMemory();

        Set<Location> locations = program.getLocations();
        List<Event> eventsInit = program.getCache().getEvents(FilterBasic.get(EType.INIT));
        List<Event> eventsStore = program.getCache().getEvents(FilterMinus.get(
                FilterBasic.get(EType.WRITE),
                FilterUnion.get(FilterBasic.get(EType.UNINIT), FilterBasic.get(EType.INIT))
        ));

        for(Event e : eventsInit) {
            enc = ctx.mkAnd(enc, ctx.mkEq(intVar("co", e, ctx), ctx.mkInt(1)));
        }

        List<IntExpr> intVars = new ArrayList<>();
        for(Event w : eventsStore) {
            IntExpr coVar = intVar("co", w, ctx);
            enc = ctx.mkAnd(enc, ctx.mkGt(coVar, ctx.mkInt(1)));
            intVars.add(coVar);
        }
        enc = ctx.mkAnd(enc, ctx.mkDistinct(intVars.toArray(new IntExpr[0])));

        for(Event w :  program.getCache().getEvents(FilterBasic.get(EType.WRITE))){
            MemEvent w1 = (MemEvent)w;
            BoolExpr lastCo = w1.exec();

            for(Tuple t : maySet.getByFirst(w1)){
                MemEvent w2 = (MemEvent)t.getSecond();
                BoolExpr relation = edge("co", w1, w2, ctx);
                lastCo = ctx.mkAnd(lastCo, ctx.mkNot(edge("co", w1, w2, ctx)));

                enc = ctx.mkAnd(enc, ctx.mkEq(relation, ctx.mkAnd(
                        ctx.mkAnd(ctx.mkAnd(w1.exec(), w2.exec()), ctx.mkEq(w1.getMemAddressExpr(), w2.getMemAddressExpr())),
                        ctx.mkLt(Utils.intVar("co", w1, ctx), Utils.intVar("co", w2, ctx))
                )));
            }

            BoolExpr lastCoExpr = ctx.mkBoolConst("co_last(" + w1.repr() + ")");
            enc = ctx.mkAnd(enc, ctx.mkEq(lastCoExpr, lastCo));

            for(Location location : locations){
                Address address = location.getAddress();
                if(w1.getMaxAddressSet().contains(address) || w1.getMaxAddressSet().contains(Memory.MEMORY_ADDRESS_ANY)){
                    enc = ctx.mkAnd(enc, ctx.mkImplies(
                            ctx.mkAnd(lastCoExpr, ctx.mkEq(w1.getMemAddressExpr(), address.toZ3Int(ctx))),
                            ctx.mkEq(address.getLastMemValueExpr(ctx), w1.getMemValueExpr())
                    ));
                }
            }
        }
        return enc;
    }

    private BoolExpr encodeUninitializedMemory(){
        List<Event> eventsRead = program.getCache().getEvents(FilterBasic.get(EType.READ));
        List<Event> eventsUnInit = program.getCache().getEvents(FilterBasic.get(EType.UNINIT));
        List<Event> eventsInit = program.getCache().getEvents(FilterBasic.get(EType.INIT));
        BoolExpr enc = ctx.mkTrue();

        for(int i = 0; i < eventsUnInit.size(); i++){
            MemEvent e1 = (MemEvent)eventsUnInit.get(i);
            // Enforce position in coherence order
            enc = ctx.mkAnd(enc, ctx.mkEq(intVar("co", e1, ctx), ctx.mkInt(0)));
            // Enforce equivalent values for two equivalent addresses of uninitialized memory
            for(int j = i + 1; j < eventsUnInit.size(); j++){
                MemEvent e2 = (MemEvent)eventsUnInit.get(j);
                enc = ctx.mkAnd(enc, ctx.mkImplies(
                        ctx.mkEq(e1.getMemAddressExpr(), e2.getMemAddressExpr()),
                        ctx.mkEq(e1.getMemValueExpr(), e2.getMemValueExpr())
                ));
            }

            // Forbid reads from uninitialized memory if there is initial write to the same location
            BoolExpr noRF = ctx.mkTrue();
            BoolExpr noRfUnInit = ctx.mkBoolConst("noRF" + "(" + e1.getCId() + ")");
            for(Event e2 : eventsRead){
                noRF = ctx.mkAnd(noRF, ctx.mkNot(Utils.edge("rf", e1, e2, ctx)));
            }
            enc = ctx.mkAnd(enc, ctx.mkEq(noRfUnInit, noRF));
            for(Event e : eventsInit){
                MemEvent e2 = (MemEvent)e;
                enc = ctx.mkAnd(enc, ctx.mkImplies(ctx.mkEq(e1.getMemAddressExpr(), e2.getMemAddressExpr()), noRfUnInit));
            }
        }
        return enc;
    }
}
