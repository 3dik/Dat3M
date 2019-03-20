package com.dat3m.ui.editor;

public enum EditorCode {

    PROGRAM, SOURCE_MM, TARGET_MM;

    @Override
    public String toString(){
        switch(this){
            case PROGRAM:
                return "Program";
            case SOURCE_MM:
                return "Source Memory Model";
            case TARGET_MM:
                return "Target Memory Model";
        }
        return super.toString();
    }

    public String editorMenuActionCommand(){
        switch(this){
            case PROGRAM:
                return "editor_menu_action_program";
            case SOURCE_MM:
                return "editor_menu_action_source_mm";
            case TARGET_MM:
                return "editor_menu_action_target_mm";
        }
        throw new RuntimeException("Illegal EditorCode");
    }

    public String editorActionCommand(){
        switch(this){
            case PROGRAM:
                return "editor_action_program";
            case SOURCE_MM:
                return "editor_action_source_mm";
            case TARGET_MM:
                return "editor_action_target_mm";
        }
        throw new RuntimeException("Illegal EditorCode");
    }
}