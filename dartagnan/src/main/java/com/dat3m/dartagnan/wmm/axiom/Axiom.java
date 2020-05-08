package com.dat3m.dartagnan.wmm.axiom;

import static com.dat3m.dartagnan.utils.Smt.exprLen;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.dat3m.dartagnan.utils.Settings;
import com.dat3m.dartagnan.wmm.relation.Relation;
import com.dat3m.dartagnan.wmm.utils.TupleSet;

/**
 *
 * @author Florian Furbach
 */
public abstract class Axiom {

    protected Relation rel;

    private boolean negate = false;
    private TupleSet approxWhitelist;

    Axiom(Relation rel) {
        this.rel = rel;
    }

    Axiom(Relation rel, boolean negate) {
        this.rel = rel;
        this.negate = negate;
    }

    public Relation getRel() {
        return rel;
    }

    public boolean isNegated(){
        return negate;
    }

    public BoolExpr consistent(Context ctx, Settings settings) {
        BoolExpr enc;
        if(negate){
            enc = _inconsistent(ctx);
        } else{
            enc = _consistent(ctx);
        }
        logEnc(enc, settings);
        return enc;
    }

    public BoolExpr inconsistent(Context ctx, Settings settings) {
        BoolExpr enc;
        if(negate){
            enc = _consistent(ctx);
        } else{
            enc = _inconsistent(ctx);
        }
        logEnc(enc, settings);
        return enc;
    }

    @Override
    public String toString(){
        if(negate){
            return "~" + _toString();
        }
        return _toString();
    }

    public TupleSet getEncodeTupleSet(){
        if (null == approxWhitelist){
            return _getEncodeTupleSet();
        }
        TupleSet result = new TupleSet();
        result.addAll(_getEncodeTupleSet());
        result.retainAll(approxWhitelist);
        return result;
    }

    public void setApproxWhitelist(TupleSet whitelist){
        approxWhitelist = whitelist;
    }

    public void addToApproxWhitelist(TupleSet tuples){
        assert approxWhitelist.addAll(tuples);
    }

    protected abstract TupleSet _getEncodeTupleSet();

    protected abstract BoolExpr _consistent(Context ctx);

    protected abstract BoolExpr _inconsistent(Context ctx);

    protected abstract String _toString();

    private void logEnc(BoolExpr enc, Settings settings){
        if (settings.getShowEncStat()){
            System.out.println(this + ": " + exprLen(enc));
        }
    }
}
