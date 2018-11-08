package dartagnan.wmm.utils;

import dartagnan.wmm.filter.FilterAbstract;
import dartagnan.wmm.filter.FilterBasic;
import dartagnan.wmm.relation.RecursiveRelation;
import dartagnan.wmm.relation.Relation;
import dartagnan.wmm.relation.basic.*;
import dartagnan.wmm.relation.binary.BinaryRelation;
import dartagnan.wmm.relation.binary.RelComposition;
import dartagnan.wmm.relation.binary.RelIntersection;
import dartagnan.wmm.relation.binary.RelUnion;
import dartagnan.wmm.relation.unary.RelInverse;
import dartagnan.wmm.relation.unary.RelTrans;
import dartagnan.wmm.relation.unary.UnaryRelation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RelationRepository {

    private Map<String, Relation> relationMap = new HashMap<>();
    private boolean includePoToCtrl;

    public RelationRepository(boolean includePoToCtrl){
        this.includePoToCtrl = includePoToCtrl;
    }

    public Set<Relation> getRelations(){
        Set<Relation> set = new HashSet<>();
        for(Map.Entry<String, Relation> entry : relationMap.entrySet()){
            if(!entry.getValue().getIsNamed() || entry.getValue().getName().equals(entry.getKey())){
                set.add(entry.getValue());
            }
        }
        return set;
    }

    public Relation getRelation(String name){
        Relation relation = relationMap.get(name);
        if(relation == null){
            relation = getBasicRelation(name);
            if(relation != null){
                addRelation(relation);
            }
        }
        return relation;
    }

    public Relation getRelation(Class<?> cls, Object... args){
        Class<?> argClasses[] = getArgsForClass(cls);
        try{
            Method method = cls.getMethod("makeTerm", argClasses);
            String term = (String)method.invoke(null, args);
            Relation relation = relationMap.get(term);

            if(relation == null){
                Constructor constructor = cls.getConstructor(argClasses);
                relation = (Relation)constructor.newInstance(args);
                addRelation(relation);
            }
            return relation;

        } catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public void updateRelation(Relation relation){
        if(relation.getIsNamed()){
            if(relationMap.get(relation.getName()) != null){
                throw new RuntimeException("Relation " + relation.getName() + " is already declared");
            }
            relationMap.put(relation.getName(), relation);
        }
    }

    private void addRelation(Relation relation) {
        relationMap.put(relation.getTerm(), relation);
        if(relation.getIsNamed()){
            relationMap.put(relation.getName(), relation);
        }
    }

    private Class<?>[] getArgsForClass(Class<?> cls){
        if(BinaryRelation.class.isAssignableFrom(cls)){
            return new Class<?>[]{Relation.class, Relation.class};

        } else if(UnaryRelation.class.isAssignableFrom(cls)){
            return new Class<?>[]{Relation.class};

        } else if(RelCartesian.class.isAssignableFrom(cls)){
            return new Class<?>[]{FilterAbstract.class, FilterAbstract.class};

        } else if(RelSetIdentity.class.isAssignableFrom(cls)){
            return new Class<?>[]{FilterAbstract.class};

        } else if(RelFencerel.class.isAssignableFrom(cls) || RecursiveRelation.class.isAssignableFrom(cls)) {
            return new Class<?>[]{String.class};
        }

        throw new RuntimeException("Method getArgsForClass is not implemented for " + cls.getName());
    }

    private Relation getBasicRelation(String name){
        switch (name){
            case "_po":
                return new RelPo(true);
            case "po":
                return new RelPo();
            case "loc":
                return new RelLoc();
            case "id":
                return new RelId();
            case "int":
                return new RelInt();
            case "ext":
                return new RelExt();
            case "co":
                return new RelCo();
            case "rf":
                return new RelRf();
            case "rmw":
                return new RelRMW();
            case "crit":
                return new RelCrit();
            case "idd":
                return new RelIdd();
            case "ctrlDirect":
                return new RelCtrlDirect();
            case "addr":
                return new RelEmpty("addr");
            case "0":
                return new RelEmpty("0");
            case "rf^-1":
                return getRelation(RelInverse.class, getRelation("rf"));
            case "fr":
                return getRelation(RelComposition.class, getRelation("rf^-1"), getRelation("co")).setName("fr");
            case "(R*W)":
                return getRelation(RelCartesian.class, new FilterBasic("R"), new FilterBasic("W"));
            case "idd^+":
                return getRelation(RelTrans.class, getRelation("idd"));
            case "data":
                return getRelation(RelIntersection.class, getRelation("idd^+"), getRelation("(R*W)")).setName("data");
            case "ctrl":
                if(includePoToCtrl){
                    return getRelation(RelComposition.class,
                            getRelation(RelComposition.class, getRelation("idd^+"), getRelation("ctrlDirect")),
                            getRelation(RelUnion.class, getRelation("id"), getRelation("_po"))
                    ).setName("ctrl");
                } else {
                    return getRelation(RelComposition.class, getRelation("idd^+"), getRelation("ctrlDirect")).setName("ctrl");
                }
            case "po-loc":
                return getRelation(RelIntersection.class, getRelation("po"), getRelation("loc")).setName("po-loc");
            case "rfe":
                return getRelation(RelIntersection.class, getRelation("rf"), getRelation("ext")).setName("rfe");
            case "rfi":
                return getRelation(RelIntersection.class, getRelation("rf"), getRelation("int")).setName("rfi");
            case "coe":
                return getRelation(RelIntersection.class, getRelation("co"), getRelation("ext")).setName("coe");
            case "coi":
                return getRelation(RelIntersection.class, getRelation("co"), getRelation("int")).setName("coi");
            case "fre":
                return getRelation(RelIntersection.class, getRelation("fr"), getRelation("ext")).setName("fre");
            case "fri":
                return getRelation(RelIntersection.class, getRelation("fr"), getRelation("int")).setName("fri");
            case "mfence":
                return getRelation(RelFencerel.class,"Mfence");
            case "ish":
                return getRelation(RelFencerel.class,"Ish");
            case "isb":
                return getRelation(RelFencerel.class,"Isb");
            case "sync":
                return getRelation(RelFencerel.class,"Sync");
            case "isync":
                return getRelation(RelFencerel.class,"Isync");
            case "lwsync":
                return getRelation(RelFencerel.class,"Lwsync");
            case "ctrlisync":
                return getRelation(RelIntersection.class, getRelation("ctrl"), getRelation("isync")).setName("ctrlisync");
            case "ctrlisb":
                return getRelation(RelIntersection.class, getRelation("ctrl"), getRelation("isb")).setName("ctrlisb");
            default:
                return null;
        }
    }
}