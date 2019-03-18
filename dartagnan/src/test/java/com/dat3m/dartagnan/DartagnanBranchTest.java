package com.dat3m.dartagnan;

import com.dat3m.dartagnan.parsers.cat.ParserCat;
import com.dat3m.dartagnan.parsers.program.ProgramParser;
import com.dat3m.dartagnan.program.Program;
import com.dat3m.dartagnan.wmm.utils.alias.Alias;
import com.dat3m.dartagnan.utils.ResourceHelper;
import com.dat3m.dartagnan.wmm.Wmm;
import com.dat3m.dartagnan.wmm.utils.Arch;
import com.dat3m.dartagnan.wmm.utils.Mode;
import com.google.common.collect.ImmutableMap;
import com.microsoft.z3.Context;
import com.microsoft.z3.Solver;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class DartagnanBranchTest {

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Iterable<Object[]> data() throws IOException {
        ImmutableMap<String, Boolean> expected = readExpectedResults();

        Wmm linuxWmm = new ParserCat().parse(ResourceHelper.CAT_RESOURCE_PATH + "cat/linux-kernel.cat");
        Wmm aarch64Wmm = new ParserCat().parse(ResourceHelper.CAT_RESOURCE_PATH + "cat/aarch64.cat");

        List<Object[]> data = Files.walk(Paths.get(ResourceHelper.TEST_RESOURCE_PATH + "branch/C/"))
                .filter(Files::isRegularFile)
                .filter(f -> (f.toString().endsWith("litmus")))
                .map(f -> new Object[]{f.toString(), expected.get(f.getFileName().toString()), linuxWmm})
                .collect(Collectors.toList());

        data.addAll(Files.walk(Paths.get(ResourceHelper.TEST_RESOURCE_PATH + "branch/AARCH64/"))
                .filter(Files::isRegularFile)
                .filter(f -> (f.toString().endsWith("litmus")))
                .map(f -> new Object[]{f.toString(), expected.get(f.getFileName().toString()), aarch64Wmm})
                .collect(Collectors.toList()));

        return data;
    }

    private static ImmutableMap<String, Boolean> readExpectedResults() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(ResourceHelper.TEST_RESOURCE_PATH + "branch/expected.csv"));
        ImmutableMap.Builder<String, Boolean> builder = new ImmutableMap.Builder<>();
        String str;
        while((str = reader.readLine()) != null){
            String[] line = str.split(",");
            if(line.length == 2){
                builder.put(line[0], Integer.parseInt(line[1]) == 1);
            }
        }
        reader.close();
        return builder.build();
    }

    private String input;
    private Wmm wmm;
    private boolean expected;

    public DartagnanBranchTest(String input, boolean expected, Wmm wmm) {
        this.input = input;
        this.expected = expected;
        this.wmm = wmm;
    }

    @Test
    public void test() {
        try{
            Program program = new ProgramParser().parse(input);
            Context ctx = new Context();
            Solver solver = ctx.mkSolver(ctx.mkTactic(Dartagnan.TACTIC));
            assertEquals(expected, Dartagnan.testProgram(solver, ctx, program, wmm, Arch.NONE, 1, Mode.KNASTER, Alias.CFIS));
            ctx.close();
        } catch (IOException e){
            fail("Missing resource file");
        }
    }
}
