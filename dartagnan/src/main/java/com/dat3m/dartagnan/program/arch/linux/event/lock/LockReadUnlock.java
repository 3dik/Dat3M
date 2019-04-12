package com.dat3m.dartagnan.program.arch.linux.event.lock;

import com.dat3m.dartagnan.expression.ExprInterface;
import com.dat3m.dartagnan.expression.IConst;
import com.dat3m.dartagnan.expression.IExpr;
import com.dat3m.dartagnan.program.Register;
import com.dat3m.dartagnan.program.arch.linux.event.lock.utils.State;
import com.dat3m.dartagnan.program.arch.linux.utils.EType;
import com.dat3m.dartagnan.program.arch.linux.utils.Mo;
import com.dat3m.dartagnan.program.event.Event;
import com.dat3m.dartagnan.program.event.MemEvent;
import com.dat3m.dartagnan.program.event.utils.EventWithPartner;
import com.dat3m.dartagnan.program.event.utils.RegWriter;
import com.dat3m.dartagnan.wmm.utils.Arch;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.IntExpr;

public class LockReadUnlock extends MemEvent implements RegWriter, EventWithPartner {

    private final LockReadFailed failedEvent;
    private final Register resultRegister;
    private IntExpr regResultExpr;

    LockReadUnlock(LockReadFailed failedEvent, Register register, IExpr address) {
        super(address, Mo.RELAXED);
        this.failedEvent = failedEvent;
        this.resultRegister = register;
        addFilters(EType.ANY, EType.VISIBLE, EType.MEMORY, EType.READ, EType.RU, EType.REG_WRITER);
    }

    @Override
    public LockReadFailed getPartner(){
        return failedEvent;
    }

    @Override
    public String toString() {
        return "lock_read_unlock(*" + address + ")";
    }

    @Override
    public String label(){
        return "LU_*" + address;
    }

    @Override
    public ExprInterface getMemValue(){
        return new IConst(State.FREE);
    }

    @Override
    public void initialise(Context ctx) {
        memValueExpr = new IConst(State.FREE).toZ3Int(this, ctx);
        memAddressExpr = address.toZ3Int(this, ctx);
        regResultExpr = new IConst(0).toZ3Int(ctx);
    }

    @Override
    public Register getResultRegister(){
        return resultRegister;
    }

    @Override
    public IntExpr getResultRegisterExpr(){
        return regResultExpr;
    }


    // Unrolling
    // -----------------------------------------------------------------------------------------------------------------

    @Override
    public int unroll(int bound, int nextId, Event predecessor) {
        throw new RuntimeException("LockReadUnlock cannot be unrolled: event must be generated during compilation");
    }


    // Compilation
    // -----------------------------------------------------------------------------------------------------------------

    @Override
    public int compile(Arch target, int nextId, Event predecessor) {
        throw new RuntimeException("LockReadUnlock cannot be compiled: event must be generated during compilation");
    }


    // Encoding
    // -----------------------------------------------------------------------------------------------------------------

    @Override
    protected BoolExpr encodeExec(Context ctx){
        return ctx.mkAnd(
                ctx.mkImplies(executes(ctx), ctx.mkBoolConst(cfVar())),
                ctx.mkImplies(ctx.mkBoolConst(cfVar()), ctx.mkEq(executes(ctx), ctx.mkNot(failedEvent.executes(ctx))))
        );
    }
}