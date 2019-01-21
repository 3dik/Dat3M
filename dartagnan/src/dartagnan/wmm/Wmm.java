package dartagnan.wmm;

import com.google.common.collect.ImmutableSet;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import dartagnan.program.Program;
import dartagnan.wmm.axiom.Axiom;
import dartagnan.wmm.filter.FilterAbstract;
import dartagnan.wmm.filter.FilterBasic;
import dartagnan.wmm.relation.RecursiveRelation;
import dartagnan.wmm.relation.Relation;
import dartagnan.wmm.utils.Arch;
import dartagnan.wmm.utils.RecursiveGroup;
import dartagnan.wmm.utils.RelationRepository;

import java.util.*;

/**
 *
 * @author Florian Furbach
 */
public class Wmm {

    private final static ImmutableSet<String> baseRelations = ImmutableSet.of("co", "rf", "idd", "addrDirect");

    private List<Axiom> axioms = new ArrayList<>();
    private Map<String, FilterAbstract> filters = new HashMap<>();
    private RelationRepository relationRepository;
    private List<RecursiveGroup> recursiveGroups = new ArrayList<>();

    private Program program;
    private boolean drawExecutionGraph = false;
    private Set<String> drawRelations = new HashSet<>();

    public Wmm(String target) {
        relationRepository = new RelationRepository(Arch.encodeCtrlPo(target));
    }

    public void setDrawExecutionGraph(){
        drawExecutionGraph = true;
    }

    public void addDrawRelations(Collection<String> relNames){
        drawRelations.addAll(relNames);
    }

    public void addAxiom(Axiom ax) {
        axioms.add(ax);
    }

    public void addFilter(FilterAbstract filter) {
        filters.put(filter.getName(), filter);
    }

    public FilterAbstract getFilter(String name){
        FilterAbstract filter = filters.get(name);
        if(filter == null){
            filter = new FilterBasic(name);
        }
        return filter;
    }

    public RelationRepository getRelationRepository(){
        return relationRepository;
    }

    public void addRecursiveGroup(Set<RecursiveRelation> recursiveGroup){
        int id = 1 << recursiveGroups.size();
        if(id < 0){
            throw new RuntimeException("Exceeded maximum number of recursive relations");
        }
        recursiveGroups.add(new RecursiveGroup(id, recursiveGroup));
    }

    public BoolExpr encode(Program program, Context ctx, boolean approx, boolean idl) {
        this.program = program;

        for(String relName : baseRelations){
            relationRepository.getRelation(relName);
        }

        for (Axiom ax : axioms) {
            ax.getRel().updateRecursiveGroupId(ax.getRel().getRecursiveGroupId());
        }

        approx = approx & !drawExecutionGraph;
        int encodingMode = approx ? Relation.APPROX : idl ? Relation.IDL : Relation.LFP;

        for(RecursiveGroup recursiveGroup : recursiveGroups){
            recursiveGroup.setDoRecurse();
        }

        for(FilterAbstract filter : filters.values()){
            filter.initialise();
        }

        for(Relation relation : relationRepository.getRelations()){
            relation.initialise(program, ctx, encodingMode);
        }

        for(RecursiveGroup recursiveGroup : recursiveGroups){
            recursiveGroup.initMaxTupleSets();
        }

        for (Axiom ax : axioms) {
            ax.getRel().getMaxTupleSet();
        }

        for(String relName : baseRelations){
            relationRepository.getRelation(relName).getMaxTupleSet();
        }

        if(drawExecutionGraph){
            for(String relName : drawRelations){
                Relation relation = relationRepository.getRelation(relName);
                if(relation != null){
                    relation.addEncodeTupleSet(relation.getMaxTupleSet());
                }
            }
        }

        for (Axiom ax : axioms) {
            ax.getRel().addEncodeTupleSet(ax.getEncodeTupleSet());
        }

        Collections.reverse(recursiveGroups);
        for(RecursiveGroup recursiveGroup : recursiveGroups){
            recursiveGroup.updateEncodeTupleSets();
        }

        BoolExpr enc = ctx.mkTrue();
        for (Axiom ax : axioms) {
            enc = ctx.mkAnd(enc, ax.getRel().encode());
        }

        for(String relName : baseRelations){
            enc = ctx.mkAnd(enc, relationRepository.getRelation(relName).encode());
        }

        if(encodingMode == Relation.LFP){
            for(RecursiveGroup group : recursiveGroups){
                enc = ctx.mkAnd(enc, group.encode(ctx));
            }
        }

        return enc;
    }

    public BoolExpr consistent(Program program, Context ctx) {
        if(this.program != program){
            throw new RuntimeException("Wmm relations must be encoded before consistency predicate");
        }
        BoolExpr expr = ctx.mkTrue();
        for (Axiom ax : axioms) {
            expr = ctx.mkAnd(expr, ax.consistent(ctx));
        }
        return expr;
    }

    public BoolExpr inconsistent(Program program, Context ctx) {
        if(this.program != program){
            throw new RuntimeException("Wmm relations must be encoded before inconsistency predicate");
        }
        BoolExpr expr = ctx.mkFalse();
        for (Axiom ax : axioms) {
            expr = ctx.mkOr(expr, ax.inconsistent(ctx));
        }
        return expr;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (Axiom axiom : axioms) {
            sb.append(axiom).append("\n");
        }

        for (Relation relation : relationRepository.getRelations()) {
            if(relation.getIsNamed()){
                sb.append(relation).append("\n");
            }
        }

        for (Map.Entry<String, FilterAbstract> filter : filters.entrySet()){
            sb.append(filter.getValue()).append("\n");
        }

        return sb.toString();
    }
}
