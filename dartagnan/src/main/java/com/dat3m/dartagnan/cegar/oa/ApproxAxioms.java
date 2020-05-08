package com.dat3m.dartagnan.cegar.oa;

import com.dat3m.dartagnan.cegar.oa.OverApproximation;
import com.dat3m.dartagnan.program.event.Event;
import com.dat3m.dartagnan.wmm.axiom.Axiom;
import com.dat3m.dartagnan.wmm.axiom.Empty;
import com.dat3m.dartagnan.wmm.axiom.Irreflexive;
import com.dat3m.dartagnan.wmm.axiom.Acyclic;
import com.dat3m.dartagnan.wmm.relation.Relation;
import com.dat3m.dartagnan.wmm.utils.Model2RelMap;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import com.dat3m.dartagnan.wmm.utils.TupleSet;
import com.dat3m.dartagnan.wmm.Wmm;
import com.microsoft.z3.Model;

import java.util.Map;
import java.util.Set;

public class ApproxAxioms extends OverApproximation {
    private Wmm wmm;

    public ApproxAxioms(Wmm wmm){
        this.wmm = wmm;
    }

    public void initialApprox(){
        for (Axiom axiom : wmm.getAxioms()){
            axiom.setApproxWhitelist(new TupleSet());
        }
    }

    public boolean trueModel(Model model){
        Map<Relation, TupleSet> map = Model2RelMap.convert(model,
                wmm.getRelationRepository().getRelations(),
                wmm.getRecursiveGroups());
        for (Axiom axiom : wmm.getAxioms()){
            TupleSet culprits = checkAxiom(axiom, map.get(axiom.getRel()));
            if (!culprits.isEmpty()){
                axiom.addToApproxWhitelist(culprits);
                return false;
            }
        }
        return true;
    }

    private TupleSet checkAxiom(Axiom axiom, TupleSet tuples){
        if (axiom instanceof Empty){
            return checkEmpty(tuples);
        } else if (axiom instanceof Irreflexive){
            return checkIrreflexive(tuples);
        } else {
            return checkAcyclic(tuples);
        }
    }

    private TupleSet checkEmpty(TupleSet tuples){
        return tuples;
    }

    private TupleSet checkIrreflexive(TupleSet tuples){
        TupleSet culprits = new TupleSet();
        for (Tuple tuple : tuples){
            if (tuple.getFirst().equals(tuple.getSecond())){
                culprits.add(tuple);
            }
        }
        return culprits;
    }

    private TupleSet checkAcyclic(TupleSet tuples){
        TupleSet culprits = new TupleSet();

        Map<Event, Set<Event>> trans = tuples.transMap();
        for (Event e1 : trans.keySet()){
            if (trans.get(e1).contains(e1)){
                for (Event e2 : trans.get(e1)){
                    if (!e1.equals(e2) && trans.get(e2).contains(e1)){
                        culprits.add(new Tuple(e1, e2));
                    }
                }
            }
        }

        for (Tuple t : tuples){
            if (t.getFirst().equals(t.getSecond())){
                culprits.add(t);
            }
        }

        culprits.retainAll(tuples);
        return culprits;
    }
}
