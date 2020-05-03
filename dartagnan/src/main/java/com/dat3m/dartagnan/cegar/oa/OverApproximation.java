package com.dat3m.dartagnan.cegar.oa;

import com.dat3m.dartagnan.cegar.MixedApproximation;
import com.microsoft.z3.BoolExpr;

public abstract class OverApproximation implements MixedApproximation {
    public boolean trueCore(BoolExpr[] core){
        return true;
    }
}
