package com.dat3m.ui.icon;

import com.dat3m.ui.Dat3M;

import java.net.URL;

public enum IconCode {

    DAT3M, DARTAGNAN, PORTHOS, ARMIS;

    @Override
    public String toString(){
        switch(this){
            case DAT3M:
                return "Dat3M";
            case DARTAGNAN:
                return "Dartagnan";
            case ARMIS:
                return "Armis";
            case PORTHOS:
                return "Porthos";
        }
        return super.toString();
    }

    public URL getPath(){
        switch(this){
            case DAT3M:
                return getResource("/dat3m.png");
            case DARTAGNAN:
                return getResource("/dartagnan.jpg");
            case ARMIS:
                return getResource("/aramis.jpg");
            case PORTHOS:
                return getResource("/porthos.jpg");
        }
        throw new RuntimeException("Illegal IconCode option");
    }

    private URL getResource(String filename){
        return Dat3M.class.getResource(filename);
    }
}
