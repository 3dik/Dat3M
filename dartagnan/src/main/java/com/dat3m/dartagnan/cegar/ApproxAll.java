package com.dat3m.dartagnan.cegar;

import com.dat3m.dartagnan.cegar.MixedApproximation;
import com.dat3m.dartagnan.cegar.oa.*;
import com.dat3m.dartagnan.cegar.ua.*;
import com.dat3m.dartagnan.program.Program;
import com.dat3m.dartagnan.utils.Settings;
import com.dat3m.dartagnan.wmm.Wmm;
import com.dat3m.dartagnan.wmm.axiom.Axiom;
import com.dat3m.dartagnan.wmm.relation.base.memory.RelRf;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Model;

public class ApproxAll implements MixedApproximation {
    private ApproxAxioms appAxioms;
    private ApproxRf appRf;

    public ApproxAll(Program program, Wmm wmm, Settings settings){
        checkSupport(wmm, settings);

        if (settings.getApproxAxioms()){
            appAxioms = new ApproxAxioms(wmm);
        }
        if (settings.getApproxRf()){
            RelRf rf = (RelRf) wmm.getRelationRepository().getRelation("rf");
            appRf = new ApproxRf(program, rf);
        }
    }

    public void initialApprox(){
        if (null != appAxioms){
            appAxioms.initialApprox();
        }
        if (null != appRf){
            appRf.initialApprox();
        }
    }

    public boolean trueModel(Model model){
        if (null != appAxioms){
            return appAxioms.trueModel(model);
        }
        return true;
    }

    public boolean trueCore(BoolExpr[] core){
        if (null != appRf){
            return appRf.trueCore(core);
        }
        return true;
    }

    private void checkSupport(Wmm wmm, Settings settings){
        if (!settings.getApprox()){
            return;
        }

        String msg = "approximations mode is incompatible with negated axioms";
        for (Axiom axiom : wmm.getAxioms()){
            if (axiom.isNegated()){
                throw new RuntimeException(msg);
            }
        }
    }
}
