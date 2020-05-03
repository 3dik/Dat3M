package com.dat3m.dartagnan.cegar;

import com.dat3m.dartagnan.cegar.MixedApproximation;
import com.dat3m.dartagnan.cegar.oa.*;
import com.dat3m.dartagnan.cegar.ua.*;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Model;

public class ApproxAll implements MixedApproximation {
    public void initialApprox(){
    }

    public boolean trueModel(Model model){
        return true;
    }

    public boolean trueCore(BoolExpr[] core){
        return true;
    }
}
