package com.dat3m.dartagnan.program.memory;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.IntExpr;
import com.dat3m.dartagnan.program.memory.utils.IllegalMemoryAccessException;

import java.util.*;

public class Memory {

    public static final Address MEMORY_ADDRESS_ANY = new Address(-1);

    private BiMap<Location, Address> map;
    private Map<String, Location> locationIndex;
    private Map<String, List<Address>> arrays;

    private int nextIndex = 0;

    public Memory(){
        map = HashBiMap.create();
        locationIndex = new HashMap<>();
        arrays = new HashMap<>();
    }

    public Location getLocationForAddress(Address address){
        return map.inverse().get(address);
    }

    public BoolExpr encode(Context ctx){
        BoolExpr enc = ctx.mkTrue();
        Set<IntExpr> expressions = new HashSet<>();

        for(List<Address> array : arrays.values()){
            int size = array.size();
            IntExpr e1 = array.get(0).toZ3Int(ctx);
            expressions.add(e1);

            for(int i = 1; i < size; i++){
                IntExpr e2 = array.get(i).toZ3Int(ctx);
                enc = ctx.mkAnd(enc, ctx.mkEq(ctx.mkAdd(e1, ctx.mkInt(1)), e2));
                expressions.add(e2);
                e1 = e2;
            }
        }
        for(Address address : map.values()){
            expressions.add(address.toZ3Int(ctx));
        }
        for(IntExpr expr : expressions){
            enc = ctx.mkAnd(enc, ctx.mkGe(expr, ctx.mkInt(0)));
        }
        return ctx.mkAnd(enc, ctx.mkDistinct(expressions.toArray(new IntExpr[0])));
    }

    public List<Address> malloc(String name, int size){
        if(!arrays.containsKey(name) && size > 0){
            List<Address> addresses = new ArrayList<>();
            for(int i = 0; i < size; i++){
                addresses.add(new Address(nextIndex++));
            }
            arrays.put(name, addresses);
            return addresses;
        }
        throw new IllegalMemoryAccessException("Illegal malloc for " + name);
    }

    public Location getOrCreateLocation(String name){
        if(!locationIndex.containsKey(name)){
            Location location = new Location(name, new Address(nextIndex++));
            map.put(location, location.getAddress());
            locationIndex.put(name, location);
            return location;
        }
        return locationIndex.get(name);
    }
}