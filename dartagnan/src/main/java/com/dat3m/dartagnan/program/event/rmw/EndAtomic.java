package com.dat3m.dartagnan.program.event.rmw;

import java.util.ArrayList;
import java.util.List;

import com.dat3m.dartagnan.program.event.Event;
import com.dat3m.dartagnan.program.utils.EType;

public class EndAtomic extends Event {

	protected final BeginAtomic begin;

    public EndAtomic(BeginAtomic begin) {
        this.begin = begin;
        addFilters(EType.RMW, EType.ATOMIC);
    	Event next = begin.getSuccessor();
    	while(next != null && next != this) {
    		next.addFilters(EType.RMW);
    		next = next.getSuccessor();
    	}
    }

    protected EndAtomic(EndAtomic other){
		super(other);
		this.begin = other.begin;
	}

    public BeginAtomic getBegin(){
    	return begin;
    }
    
    public List<Event> getBlock(){
    	List<Event> block = new ArrayList<Event>();
    	Event next = begin;
    	while(next != null && next != this) {
    		block.add(next);
    		next = next.getSuccessor();	
    	}
        return block;
    }

    @Override
    public String toString() {
    	return "end_atomic()";
    }
    
	@Override
	public EndAtomic getCopy(){
		return new EndAtomic(this);
	}
}
