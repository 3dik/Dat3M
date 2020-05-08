package com.dat3m.dartagnan.wmm.relation.base.stat;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Model;
import com.dat3m.dartagnan.wmm.relation.Relation;
import com.dat3m.dartagnan.wmm.utils.TupleSet;

import java.util.Map;

public class RelEmpty extends Relation {

    public RelEmpty(String name) {
        super(name);
        term = name;
    }

    @Override
    protected void _fillEnabledTuples(Map<Relation, TupleSet> map,
            Model model, int groupId){
    }

    @Override
    public TupleSet getMaxTupleSet(){
        if(maxTupleSet == null){
            maxTupleSet = new TupleSet();
        }
        return maxTupleSet;
    }

    @Override
    protected BoolExpr encodeApprox() {
        return ctx.mkTrue();
    }
}
