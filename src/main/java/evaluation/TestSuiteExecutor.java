package evaluation;

import chromosome.TestCase;
import chromosome.TestSuite;
import chromosome.action.Action;
import org.apache.commons.io.FileUtils;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfoStore;
import org.jacoco.core.instr.Instrumenter;
import org.jacoco.core.runtime.IRuntime;
import org.jacoco.core.runtime.LoggerRuntime;
import org.jacoco.core.runtime.RuntimeData;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public final class TestSuiteExecutor {

    public static class MemoryClassLoader extends ClassLoader {

        private final Map<String, byte[]> definitions = new HashMap<>();

        public void addDefinition(final String name, final byte[] bytes) {
            definitions.put(name, bytes);
        }

        @Override
        protected Class<?> loadClass(final String name, final boolean resolve)
                throws ClassNotFoundException {
            final byte[] bytes = definitions.get(name);
            if (bytes != null) {
                return defineClass(name, bytes, 0, bytes.length);
            }
            return super.loadClass(name, resolve);
        }

    }

    public TestSuiteExecutor() { }

    private void prepareEvaluation(TestSuite testSuite) {
        String fileName = "./spooned/" + testSuite.getClassName() + ".java";
        try {
            String text = Files.readString(Paths.get(fileName));
            for (int i = 0; i < text.length(); ++i) {
                if (text.charAt(i) == '{') {
                    text = insertString(text, "implements Runnable ", i-1);
                    break;
                }
            }
            StringBuilder newCode = new StringBuilder("\n\t@Override\n\tpublic void run() {\n");
            for (TestCase testCase : testSuite.getTestCases()) {
                Queue<String> valueQ = new LinkedList<>(testCase.getValues());
                for (Action action : testCase.getActions()) {
                    newCode.append("\t\t");
                    newCode.append(action.toString());
                    int commas = action.getParamTypes().size() - 1;
                    for (String parType : action.getParamTypes()) {
                        String value = valueQ.remove();
                        switch (parType) {
                            case "int", "Integer" -> newCode.append(Integer.valueOf(value));
                            case "double", "Double" -> newCode.append(Double.valueOf(value));
                            case "boolean", "Boolean" -> newCode.append(Boolean.valueOf(value));
                            case "String" -> newCode.append("\"").append(value).append("\"");
                        }
                        if (commas > 0) {
                            newCode.append(", ");
                            commas--;
                        }
                    }
                    newCode.append(");\n");
                }
            }
            newCode.append("\t}");
            for (int i = text.length() - 1; i >= 0; --i) {
                if (text.charAt(i) == '}') {
                    text = insertString(text, newCode.toString(), i-2);
                    break;
                }
            }
            String temp = "./run/" + testSuite.getClassName() + ".java";
            FileWriter myWriter = new FileWriter(temp);
            myWriter.write(text);
            myWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String insertString(String originalString, String stringToBeInserted, int index) {
        StringBuilder newString = new StringBuilder();
        for (int i = 0; i < originalString.length(); i++) {
            newString.append(originalString.charAt(i));
            if (i == index) {
                newString.append(stringToBeInserted);
            }
        }
        return newString.toString();
    }

    public double execute(TestSuite testSuite) throws Exception {
        prepareEvaluation(testSuite);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        compiler.run(null, null, null, "./run/" + testSuite.getClassName() + ".java");
        File resource = new File("./run/" + testSuite.getClassName() + ".class");
        final String targetName = testSuite.getClassName();

        // For instrumentation and runtime we need a IRuntime instance
        // to collect execution data:
        final IRuntime runtime = new LoggerRuntime();

        // The Instrumenter creates a modified version of our test target class
        // that contains additional probes for execution data recording:
        final Instrumenter instr = new Instrumenter(runtime);
        InputStream original = FileUtils.openInputStream(resource);
        final byte[] instrumented = instr.instrument(original, targetName);
        original.close();

        // Now we're ready to run our instrumented class and need to startup the
        // runtime first:
        final RuntimeData data = new RuntimeData();
        runtime.startup(data);

        // In this tutorial we use a special class loader to directly load the
        // instrumented class definition from a byte[] instances.
        final MemoryClassLoader memoryClassLoader = new MemoryClassLoader();
        memoryClassLoader.addDefinition(targetName, instrumented);
        final Class<?> targetClass = memoryClassLoader.loadClass(targetName);

        // Here we execute our test target class through its Runnable interface:
        final Constructor<?> constructor = targetClass.getConstructors()[0];
        final Runnable targetInstance = (Runnable) constructor.newInstance();
        targetInstance.run();

        // At the end of test execution we collect execution data and shutdown
        // the runtime:
        final ExecutionDataStore executionData = new ExecutionDataStore();
        final SessionInfoStore sessionInfos = new SessionInfoStore();
        data.collect(executionData, sessionInfos, false);
        runtime.shutdown();

        // Together with the original class definition we can calculate coverage
        // information:
        final CoverageBuilder coverageBuilder = new CoverageBuilder();
        final Analyzer analyzer = new Analyzer(executionData, coverageBuilder);
        original = FileUtils.openInputStream(resource);
        analyzer.analyzeClass(original, targetName);
        original.close();

        IClassCoverage cc = (IClassCoverage) coverageBuilder.getClasses().toArray()[0];
        ICounter branchCounter = cc.getBranchCounter();
        int coveredBranches = branchCounter.getCoveredCount();
        int totalBranches = branchCounter.getTotalCount();
        //noinspection ResultOfMethodCallIgnored
        resource.delete();
        resource = new File("./run/" + testSuite.getClassName() + ".java");
        //noinspection ResultOfMethodCallIgnored
        resource.delete();
        return (double) coveredBranches / totalBranches;
    }
}