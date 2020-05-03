package com.dat3m.dartagnan.cegar;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Model;

public interface MixedApproximation {
    // Set the initial approximation
    public void initialApprox();

    // Verify the model and reduce over-approximation if the model would not
    // satisfy the non-approximated problem.
    public boolean trueModel(Model model);

    // Verify the UNSAT core and reduce under-approximation if those
    // assumptions would not make the non-approximated encoding unsatisfiable
    // too.
    public boolean trueCore(BoolExpr[] core);
}
