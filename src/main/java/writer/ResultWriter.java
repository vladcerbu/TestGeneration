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

    public void writeSuite(TestSuite testSuite) throws IOException {
        String fileName = this.resultsFile + "\\\\" + testSuite.getClassName() + "Test.java";
        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, false));
        writer.write(testSuite.toString());
        writer.close();
    }

    public void writePerformance(String className, int generations, int populationSize, double bestFitness, double averageFitness, double worstFitness, double time) throws IOException {
        String fileName = this.resultsFile + "\\\\" + className + "Test_performance_nrGen" + generations + "_popSize" + populationSize + ".csv";
        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true));
        writer.write(bestFitness + "," + averageFitness + "," + worstFitness + "," + time + "\n");
        writer.close();
    }
}
