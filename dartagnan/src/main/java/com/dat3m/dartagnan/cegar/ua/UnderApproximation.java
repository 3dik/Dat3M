package com.dat3m.dartagnan.cegar.ua;

import com.dat3m.dartagnan.cegar.MixedApproximation;
import com.microsoft.z3.Model;

public abstract class UnderApproximation implements MixedApproximation {
    public boolean trueModel(Model model){
        return true;
    }
}
