package com.dat3m.dartagnan.cegar;

import com.dat3m.dartagnan.cegar.MixedApproximation;
import com.dat3m.dartagnan.cegar.oa.*;
import com.dat3m.dartagnan.cegar.ua.*;
import com.dat3m.dartagnan.utils.Settings;
import com.dat3m.dartagnan.wmm.Wmm;
import com.dat3m.dartagnan.wmm.axiom.Axiom;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Model;

public class ApproxAll implements MixedApproximation {
    private ApproxAxioms appAxioms;

    public ApproxAll(Wmm wmm, Settings settings){
        checkSupport(wmm, settings);

        if (settings.getApproxAxioms()){
            appAxioms = new ApproxAxioms(wmm);
        }
    }

    public void initialApprox(){
        if (null != appAxioms){
            appAxioms.initialApprox();
        }
    }

    public boolean trueModel(Model model){
        if (null != appAxioms){
            return appAxioms.trueModel(model);
        }
        return true;
    }

    public boolean trueCore(BoolExpr[] core){
        return true;
    }

    private void checkSupport(Wmm wmm, Settings settings){
        if (!settings.getApproxAxioms()){
            return;
        }

        for (Axiom axiom : wmm.getAxioms()){
            if (axiom.isNegated()){
                String msg = "negated axioms cannot be approximated";
                throw new RuntimeException(msg);
            }
        }
    }
}
