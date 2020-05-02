package com.dat3m.dartagnan.utils;

import java.util.LinkedList;
import java.util.Arrays;
import java.util.Stack;

import com.dat3m.dartagnan.utils.CheckClock;
import com.microsoft.z3.Expr;

public class Smt {
    public static int exprLen(Expr e){
        Stack<String> backup = CheckClock.popAll();
        CheckClock.push("exprLen");

        // This is not implemented recursively because some of the encodings
        // are veery deeply nested. For example, my RAM usage explodes on
        // loopv1.bpl.
        LinkedList<Expr> queue = new LinkedList<>();
        queue.add(e);

        int len = 0;
        while (0 < queue.size()) {
            Expr current = queue.removeLast(); //less memory-usage?
            Expr[] children = current.getArgs();
            queue.addAll(Arrays.asList(children));
            if (0 == children.length) {
                len++;
            }
        }

        CheckClock.pop();
        CheckClock.pushAll(backup);
        return len;
    }
}
