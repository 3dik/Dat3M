package dartagnan.tests;

import com.microsoft.z3.Context;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Z3Exception;
import dartagnan.Dartagnan;
import dartagnan.parsers.cat.ParserCat;
import dartagnan.program.Program;
import dartagnan.wmm.Wmm;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Stream;

public class DartagnanTest {

    public static void main(String[] args) throws Z3Exception, IOException {

        Runner runner = new Runner("src/dartagnan/tests/resources/dart-expected.csv", ",");

        HashSet<String> blacklist = runner.loadList("src/dartagnan/tests/resources/blacklist.txt");
        HashSet<String> linuxFPBlacklist = runner.loadList("litmus/dart-fixpoint-long-exec-time.txt");
        linuxFPBlacklist.addAll(blacklist);

        runner.runGroup("litmus/X86", "tso", "cat/tso.cat", 2, false, false, blacklist);
        runner.runGroup("litmus/X86", "tso", "cat/tso.cat", 2, false, true, blacklist);
        runner.runGroup("litmus/X86", "tso", "cat/tso.cat", 2, true, false, blacklist);

        runner.runGroup("litmus/PPC", "power", "cat/power.cat", 2, false, false, blacklist);
        runner.runGroup("litmus/PPC", "power", "cat/power.cat", 2, false, true, blacklist);
        runner.runGroup("litmus/PPC", "power", "cat/power.cat", 2, true, false, blacklist);

        runner.runGroup("litmus/AARCH64", "arm", "cat/aarch64.cat", 2, false, false, blacklist);
        runner.runGroup("litmus/AARCH64", "arm", "cat/aarch64.cat", 2, true, true, blacklist);
        runner.runGroup("litmus/AARCH64", "arm", "cat/aarch64.cat", 2, true, false, blacklist);

        runner.runGroup("litmus/C/manual", "sc", "cat/linux-kernel.cat", 2, false, false, linuxFPBlacklist);
        runner.runGroup("litmus/C/luc", "sc", "cat/linux-kernel.cat", 2, false, false, linuxFPBlacklist);
        runner.runGroup("litmus/C/auto", "sc", "cat/linux-kernel.cat", 2, false, false, linuxFPBlacklist);
        runner.runGroup("litmus/C/dart", "sc", "cat/linux-kernel.cat", 2, false, false, linuxFPBlacklist);

        runner.runGroup("litmus/C/manual", "sc", "cat/linux-kernel.cat", 2, false, true, blacklist);
        runner.runGroup("litmus/C/luc", "sc", "cat/linux-kernel.cat", 2, false, true, blacklist);
        runner.runGroup("litmus/C/auto", "sc", "cat/linux-kernel.cat", 2, false, true, blacklist);
        runner.runGroup("litmus/C/dart", "sc", "cat/linux-kernel.cat", 2, false, true, blacklist);

        runner.runGroup("litmus/C/manual", "sc", "cat/linux-kernel.cat", 2, true, false, blacklist);
        runner.runGroup("litmus/C/luc", "sc", "cat/linux-kernel.cat", 2, true, false, blacklist);
        runner.runGroup("litmus/C/auto", "sc", "cat/linux-kernel.cat", 2, true, false, blacklist);
        runner.runGroup("litmus/C/dart", "sc", "cat/linux-kernel.cat", 2, true, false, blacklist);
    }
}

class Runner {

    private static final int ALLOWED = 1;
    private static final int FORBIDDEN = 0;
    private static final int UNDEFINED = -1;
    private static final int ERROR = -2;

    private boolean printFailedOnly = true;
    private boolean printStackTrace = true;

    private HashMap<String, Integer> expectedData;

    Runner(String expectedDataPath, String separator) throws IOException{
        expectedData = loadExpectedData(expectedDataPath, separator);
    }

    void runGroup(String path, String target, String catPath, int steps, boolean relax, boolean idl, HashSet<String> blacklist){
        try{
            long startTime = System.currentTimeMillis();
            Wmm wmm = new ParserCat().parse(catPath, target);
            Stream<Path> paths = Files.walk(Paths.get(path));
            paths.filter(Files::isRegularFile)
                    .filter(f -> (f.toString().endsWith("litmus") || f.toString().endsWith("pts")))
                    .forEach(f -> {
                        String test = f.toString();
                        if(blacklist == null || !blacklist.contains(test)){
                            try {
                                Program program = Dartagnan.parseProgram(test);
                                if (program.getAss() != null) {
                                    Context ctx = new Context();
                                    Solver solver = ctx.mkSolver(ctx.mkTactic(Dartagnan.TACTIC));
                                    boolean result = Dartagnan.testProgram(solver, ctx, program, wmm, target, steps, relax, idl);
                                    int expected = expectedData.getOrDefault(test, UNDEFINED);
                                    printResult(f.toString(), result ? ALLOWED : FORBIDDEN, expected);
                                }

                            } catch (Exception e) {
                                printResult(test, ERROR, ERROR);
                                if(printStackTrace){
                                    e.printStackTrace();
                                }
                            }
                        }
                    });

            long stopTime = System.currentTimeMillis();
            System.out.println(String.format("Group test %-18s target=%-10s cat=%-15s steps=%-3d relax=%-6s idl=%-6s : finished in %d ms",
                    path, target, catPath, steps, relax, idl, stopTime - startTime));

        } catch(IOException e){
            e.printStackTrace();
        }
    }

    HashSet<String> loadList(String path) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            HashSet<String> data = new HashSet<>();
            String str;
            while((str = reader.readLine()) != null){
                data.add(str);
            }
            return data;
        }
    }

    private void printResult(String test, int result, int expected){
        if(result == ERROR){
            System.out.println(String.format("%-120s%-6s%-6s%-6s", test, "Error", "", "!!!"));
        } else if(!printFailedOnly || result != expected){
            String warning = result == expected ? "" : "!!!";
            System.out.println(String.format("%-120s%-6s%-6s%-6s", test, result, expected, warning));
        }
    }

    private HashMap<String, Integer> loadExpectedData(String path, String separator) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            HashMap<String, Integer> data = new HashMap<>();
            String str;
            while((str = reader.readLine()) != null){
                String[] line = str.split(separator);
                if(line.length == 2){
                    data.put(line[0], Integer.parseInt(line[1]));
                }
            }
            return data;
        }
    }
}
