package writer;

import chromosome.TestSuite;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class ResultWriter {

    private final String resultsFile;

    public ResultWriter(String resultsFile) {
        this.resultsFile = resultsFile;
    }

    public void write(TestSuite testSuite) throws IOException {
        String fileName = this.resultsFile + "\\\\" + testSuite.getClassName() + "Test.java";
        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
        writer.write(testSuite.toString());
        writer.close();
    }
}
