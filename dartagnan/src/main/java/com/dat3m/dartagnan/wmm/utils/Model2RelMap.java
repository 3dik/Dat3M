package com.dat3m.dartagnan.wmm.utils;

import com.dat3m.dartagnan.wmm.relation.RecursiveRelation;
import com.dat3m.dartagnan.wmm.relation.Relation;
import com.microsoft.z3.Model;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class Model2RelMap {
    public static Map<Relation, TupleSet> convert(Model model,
            Set<Relation> rels, List<RecursiveGroup> groups){
        Map<Relation, TupleSet> map = new HashMap<>();

        // important: groups must be in catfile order
        for (RecursiveGroup group : groups){
            kleene(map, model, group);
        }

        for (Relation rel : rels){
            if (!map.keySet().contains(rel)){
                rel.fillEnabledTuples(map, model, 0);
            }
        }

        return map;
    }

    private static void kleene(Map<Relation, TupleSet> map,
            Model model, RecursiveGroup group){
        boolean changed = true;
        while (changed){
            changed = false;

            for (RecursiveRelation rel : group.getRelations()){
                changed |= rel.fillEnabledTuplesRec(map, model, group.getId());
            }
        }
    }
}
