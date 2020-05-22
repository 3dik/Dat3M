package com.dat3m.dartagnan.utils.options;

import org.apache.commons.cli.*;

public class DartagnanOptions extends BaseOptions {

    public DartagnanOptions(){
        super();
        Option catOption = new Option("cat", true,
                "Path to the CAT file");
        catOption.setRequired(true);
        addOption(catOption);

        addOption(new Option("encstat", "print encodings sizes"));
        addOption(new Option("timestat", "print runtimes"));

        addOption(new Option("appaxioms", "approximate axioms"));
        addOption(new Option("apprf", "approximate rf relation"));
    }

    @Override
    protected void parseSettings(CommandLine cmd){
        super.parseSettings(cmd);

        settings.setShowEncStat(cmd.hasOption("encstat"));
        settings.setShowTimeStat(cmd.hasOption("timestat"));

        settings.setApproxAxioms(cmd.hasOption("appaxioms"));
        settings.setApproxRf(cmd.hasOption("apprf"));
    }
}
