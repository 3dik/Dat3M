package com.dat3m.dartagnan.utils;

import java.lang.System;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class CheckClock {
    public static HashMap<String,Long> tasks = new HashMap();

    private static Stack<Session> sessions = new Stack<>();

    public static void push(String task){
        sessions.push(new Session(task));
    }

    public static String pop(){
        String name = peek();
        Long oldDuration = tasks.getOrDefault(name, new Long(0));
        tasks.put(name, oldDuration + sessions.pop().getDuration());
        return name;
    }

    // backup should be reversed, cf. popAll
    public static void pushAll(Stack<String> backup){
        pop(); // outer
        while (!backup.empty()){
            push(backup.pop());
        }
    }

    // note that the session stack is reversed
    public static Stack<String> popAll(){
        Stack<String> backup = new Stack<>();
        while (!sessions.empty()){
            backup.push(pop());
        }
        push("outer");
        return backup;
    }

    public static String peek(){
        return sessions.peek().getName();
    }

    public static void print(){
        for (Map.Entry<String, Long> pair : tasks.entrySet()){
            System.out.println(pair.getKey() + ": " + pair.getValue());
        }
    }
}

class Session {
    private String name;
    private Long start;

    public Session(String name){
        this.name = name;
        start = time();
    }

    public Long getDuration(){
        return time() - start;
    }

    public String getName(){
        return name;
    }

    private static Long time(){
        return System.currentTimeMillis();
    }
}
