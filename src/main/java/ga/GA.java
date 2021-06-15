package ga;

import application.appconfig.ApplicationContext;
import chromosome.TestCase;
import chromosome.TestSuite;
import chromosome.action.Action;
import chromosome.action.ConstructorAction;
import chromosome.action.MethodAction;
import executor.TestSuiteExecutor;
import parser.SpoonParser;
import spoon.reflect.declaration.CtMethod;
import util.RandomHelper;
import writer.ResultWriter;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;

public class GA {
    private String classPath;
    private String resultFile;
    private ResultWriter resultWriter;
    private TestSuiteExecutor executor;
    private RandomHelper randomHelper;
    private SpoonParser spoonParser;
    private ArrayList<TestSuite> population;
    private int maxSuiteLength;
    private int maxStringLength;
    private int minNr;
    private int maxNr;
    private int stringType;
    private boolean onlyFirst;
    private int populationSize;
    private int generations;
    private double crossoverProb;
    private double initialAdditionProb;
    private final int varNameLength = Integer.parseInt(ApplicationContext.getProperties().getProperty("data.varNameLength"));
    private double executionTime = 0.0;

    // Comparator for sorting the chromosomes. Biggest fitness first,
    // if equal then we choose the smallest one in terms of test case number
    private final Comparator<TestSuite> comp = (o1, o2) -> {
        if (o1.getFitness() > o2.getFitness())
            return -1;
        else if (o1.getFitness() < o2.getFitness())
            return 1;
        else
            return Integer.compare(o1.getTestCases().size(), o2.getTestCases().size());
    };

    // Constructor
    public GA() { }

    public double getWorstFitness() {
        return population.get(population.size() - 1).getFitness();
    }

    public double getAverageFitness() {
        double total = 0.0;
        for (TestSuite chromosome : population)
            total += chromosome.getFitness();
        return total / population.size();
    }

    // Getters and Setters section
    public double getExecutionTime() {
        return executionTime;
    }

    public double getBestFitness() {
        return population.get(0).getFitness();
    }

    public void setStringType(int stringType) {
        if (stringType > 4 || stringType < 1)
            this.stringType = 1;
        else
            this.stringType = stringType;
    }

    public void setClassPath(String classPath) {
        this.classPath = classPath;
    }

    public void setResultFile(String resultFile) {
        this.resultFile = resultFile;
    }

    public void setPopulationSize(int populationSize) {
        this.populationSize = populationSize;
    }

    public void setGenerations(int generations) {
        this.generations = generations;
    }

    public void setCrossoverProb(double crossoverProb) {
        this.crossoverProb = crossoverProb;
    }

    public void setInitialAdditionProb(double initialAdditionProb) {
        this.initialAdditionProb = initialAdditionProb;
    }

    public void setOnlyFirst(boolean onlyFirst) {
        this.onlyFirst = onlyFirst;
    }

    public void setMaxNr(int maxNr) {
        this.maxNr = maxNr;
    }

    public void setMinNr(int minNr) {
        this.minNr = minNr;
    }

    public void setMaxStringLength(int maxStringLength) {
        this.maxStringLength = maxStringLength;
    }

    public void setMaxSuiteLength(int maxSuiteLength) {
        this.maxSuiteLength = maxSuiteLength;
    }

    // Checking maximum suite size and adjusting it if it is too small
    private void checkMaxSuiteSize() {
        if (this.maxSuiteLength < 5 * this.spoonParser.getMethods().size())
            this.maxSuiteLength = 5 * this.spoonParser.getMethods().size();
    }

    // Initializing the algorithm. Create necessary classes and generate random population
    private void initialize() {
        this.executor = new TestSuiteExecutor();
        this.resultWriter = new ResultWriter(resultFile);
        this.randomHelper = new RandomHelper(minNr, maxNr, maxStringLength, stringType);
        this.spoonParser = new SpoonParser(classPath);
        // Checking the maximum suite size and adjusting it accordingly
        this.checkMaxSuiteSize();
        // Generating random population to start with
        this.generateRandomPopulation();
        // Evaluating the population
        this.evaluatePopulation();
        // Sorting the population to have the best chromosome first
        population.sort(comp);
    }

    // Genetic Algorithm main function
    public void startAlgorithm() throws Exception {
        long start = System.nanoTime();
        this.initialize();
        for (int gen = 0; gen < generations; gen++) {
            // If the user checked the CheckBox for getting only the first found perfect solution, then we return it.
            if (onlyFirst && population.get(0).getFitness() == 1.0)
                break;
            ArrayList<TestSuite> newPop = new ArrayList<>();
            // Elitism: The best suite from the previous generation is kept in the new population
            newPop.add(population.get(0));
            while (newPop.size() < populationSize) {
                ArrayList<TestSuite> offsprings;
                ArrayList<TestSuite> parents = this.select(); // Selecting parents
                if (randomHelper.generateRandomDouble(0, 1) < crossoverProb) // Crossover if probability is met
                    offsprings = this.crossover(parents.get(0), parents.get(1));
                else // Else the offsprings will be the parents
                    offsprings = parents;
                // Mutating the offsprings
                offsprings.set(0, this.mutate(offsprings.get(0)));
                offsprings.set(1, this.mutate(offsprings.get(1)));
                // Evaluating the offsprings
                offsprings.get(0).setFitness(this.evaluateChromosome(offsprings.get(0)));
                offsprings.get(1).setFitness(this.evaluateChromosome(offsprings.get(1)));

                double bestParentFitness = parents.get(0).getFitness() >= parents.get(1).getFitness() ? parents.get(0).getFitness() : parents.get(1).getFitness();
                double bestOffspringFitness = offsprings.get(0).getFitness() >= offsprings.get(1).getFitness() ? offsprings.get(0).getFitness() : offsprings.get(1).getFitness();
                int lengthParents = parents.get(0).getTestCases().size() + parents.get(1).getTestCases().size();
                int lengthOffsprings = offsprings.get(0).getTestCases().size() + offsprings.get(1).getTestCases().size();
                int bestLength = population.get(0).getTestCases().size();
                // Checking to see if the new chromosomes are better than the parents and then decide which ones to add to the new population
                if (bestOffspringFitness > bestParentFitness || (bestOffspringFitness == bestParentFitness && lengthOffsprings <= lengthParents))
                    for (int i = 0; i < offsprings.size(); ++i)
                        if (offsprings.get(i).getTestCases().size() <= 2 * bestLength)
                            newPop.add(offsprings.get(i));
                        else
                            newPop.add(parents.get(i));
                else
                    newPop.addAll(parents);
            }
            population = newPop;
            population.sort(comp);
            // If population is too big, we reduce it to the maximum size by eliminating the worst chromosomes
            if (population.size() > populationSize) {
                population.subList(populationSize, population.size()).clear();
            }
        }

        // Optimize found test suite by potentially reducing its length
        population.set(0, this.optimizeSolution(population.get(0)));
        // Write the resulted test suite (the best one)
        try {
            resultWriter.writeSuite(population.get(0));
        } catch (IOException e) {
            throw new Exception("Unable to write the result");
        }

        // Delete spooned file
        File file = new File("./src/main/resources/spooned/" + population.get(0).getClassName() + ".java");
        //noinspection ResultOfMethodCallIgnored
        file.delete();

        // Calculate execution time
        long end = System.nanoTime();
        executionTime = (double) (end - start) / 1E9;
    }

    // Selection operator - Rank-Based Selection
    private ArrayList<TestSuite> select() {
        ArrayList<TestSuite> selected = new ArrayList<>();
        double sumRanks = populationSize * (populationSize + 1) / 2.0;
        double randomRank = randomHelper.generateRandomDouble(0, 1);
        double cumulativeSum = 0.0;

        for (int i = 0; i < populationSize; ++i) {
            cumulativeSum += (populationSize - i) / sumRanks;
            if (cumulativeSum >= randomRank) {
                selected.add(population.get(i));
                break;
            }
        }

        randomRank = randomHelper.generateRandomDouble(0, 1);
        cumulativeSum = 0.0;
        for (int i = 0; i < populationSize; ++i) {
            cumulativeSum += (populationSize - i) / sumRanks;
            if (cumulativeSum >= randomRank) {
                selected.add(population.get(i));
                break;
            }
        }

        return selected;
    }

    // Crossover operator - One-Point Crossover
    private ArrayList<TestSuite> crossover(TestSuite c1, TestSuite c2) {
        // Cutting point
        double p = randomHelper.generateRandomDouble(0, 1);
        ArrayList<TestCase> tc1 = c1.getTestCases();
        ArrayList<TestCase> tc2 = c2.getTestCases();
        int mid1 = (int) (p * tc1.size());
        int mid2 = (int) (p * tc2.size());
        ArrayList<TestCase> tco1 = new ArrayList<>();
        ArrayList<TestCase> tco2 = new ArrayList<>();
        for (int i = 0; i < mid1; ++i)
            tco1.add(tc1.get(i));
        for (int i = 0; i < mid2; ++i)
            tco2.add(tc2.get(i));
        for (int i = mid1; i < tc1.size(); ++i)
            tco2.add(tc1.get(i));
        for (int i = mid2; i < tc2.size(); ++i)
            tco1.add(tc2.get(i));
        ArrayList<TestSuite> offsprings = new ArrayList<>();
        offsprings.add(new TestSuite(c1.getClassName(), tco1));
        offsprings.add(new TestSuite(c2.getClassName(), tco2));
        return offsprings;
    }

    // Mutation operator - Custom Mutation
    private TestSuite mutate(TestSuite testSuite) {
        ArrayList<TestCase> testCases = testSuite.getTestCases();
        ArrayList<TestCase> toDelete = new ArrayList<>();
        // Mutation probability is always 1/n
        double mutationProb = 1.0 / testCases.size();
        double prob, mutationType;
        for (TestCase currentTestCase : testCases) {
            // Checking which test cases to mutate based on the probability
            prob = randomHelper.generateRandomDouble(0, 1);
            if (prob <= mutationProb) {
                // Checking which type of mutation to apply to the test case
                mutationType = randomHelper.generateRandomDouble(0, 1);
                if (mutationType <= 1.0 / 3.0) // Delete test case
                    toDelete.add(currentTestCase); // We delete all of them at the end of the loop
                else if (mutationType > 1.0 / 3.0 && mutationType <= 2.0 / 3.0) // New Values
                    currentTestCase.setValues(randomHelper.getRandomValues(currentTestCase.getActions()));
                else { // New method
                    ArrayList<Action> actions = currentTestCase.getActions();
                    ArrayList<String> values = currentTestCase.getValues();
                    int nr_params = actions.get(1).getParamTypes().size();
                    // Clear the current values and choose a new method different from the current one
                    values.subList(values.size() - nr_params, values.size()).clear();
                    MethodAction oldMethod = (MethodAction) actions.remove(1); // Remove the old method
                    CtMethod<?> method = spoonParser.getRandomMethod();
                    if (spoonParser.getMethods().size() > 1)
                        while (oldMethod.getMethodName().equals(method.getSimpleName()))
                            method = spoonParser.getRandomMethod();

                    // Make sure that the variable name in the method is different from the one in the constructor
                    String varn = randomHelper.generateRandomVarName(varNameLength);
                    String calln = actions.get(0).getVarName();
                    while (varn.equals(calln))
                        varn = randomHelper.generateRandomVarName(varNameLength);

                    // Initialize the new method and add the new values
                    MethodAction newMethod = new MethodAction(method, calln, varn);
                    values.addAll(randomHelper.getRandomValues(newMethod));
                    actions.add(newMethod); // Add the new method to the test case actions
                    currentTestCase.setTestedMethodName(method.getSimpleName());
                    currentTestCase.setActions(actions);
                    currentTestCase.setValues(values);
                }
            }
        }
        // Delete the test cases that were mutated to be deleted
        for (TestCase testCase : toDelete)
            testCases.remove(testCase);

        // Add new test cases to the suite
        double additionProb = initialAdditionProb;
        double additionRandom = randomHelper.generateRandomDouble(0, 1);
        while (additionProb >= additionRandom && testCases.size() < maxSuiteLength) {
            testCases.add(this.generateRandomTestCase());
            additionProb = additionProb * additionProb;
            additionRandom = randomHelper.generateRandomDouble(0, 1);
        }
        testSuite.setTestCases(testCases);
        // Validate the outcome and return the mutated chromosome
        return this.validateChromosome(testSuite);
    }

    // Validation of a test suite (it needs to have different variable names)
    private TestSuite validateChromosome(TestSuite testSuite) {
        Set<TestCase> setTC = new LinkedHashSet<>(testSuite.getTestCases());
        testSuite.setTestCases(new ArrayList<>(setTC));
        ArrayList<TestCase> testCases = testSuite.getTestCases();
        for (TestCase testCase : testCases) {
            for (TestCase other : testCases) {
                if (!testCase.equals(other)) {
                    // Checking for the constructor's variable name to be different than the other's variable names
                    if (testCase.getActions().get(0).getVarName().equals(other.getActions().get(0).getVarName()) ||
                        testCase.getActions().get(0).getVarName().equals(other.getActions().get(1).getVarName())) {
                        String varn = randomHelper.generateRandomVarName(varNameLength);
                        while (varn.equals(other.getActions().get(0).getVarName()) || varn.equals(other.getActions().get(1).getVarName()))
                            varn = randomHelper.generateRandomVarName(varNameLength);
                        testCase.getActions().get(0).setVarName(varn);
                        MethodAction methodAction = (MethodAction) testCase.getActions().get(1);
                        methodAction.setCallName(varn);
                        testCase.getActions().set(1, methodAction);
                    }
                    // Checking for the method's variable name to be different from the other's variable names
                    if (testCase.getActions().get(1).getVarName().equals(other.getActions().get(0).getVarName()) ||
                            testCase.getActions().get(1).getVarName().equals(other.getActions().get(1).getVarName())) {
                        String varn = randomHelper.generateRandomVarName(varNameLength);
                        while (varn.equals(other.getActions().get(0).getVarName()) || varn.equals(other.getActions().get(1).getVarName()))
                            varn = randomHelper.generateRandomVarName(varNameLength);
                        testCase.getActions().get(1).setVarName(varn);
                    }
                }
            }
        }
        testSuite.setTestCases(testCases);
        return testSuite;
    }

    // Generating a random population of the given size
    private void generateRandomPopulation() {
        population = new ArrayList<>();
        for (int i = 0; i < populationSize; ++i) {
            TestSuite testSuite = new TestSuite(spoonParser.getClassName());
            ArrayList<TestCase> testCases = new ArrayList<>();
            // Generate a random suite with random length
            int max = randomHelper.generateRandomInteger(maxSuiteLength / 2, maxSuiteLength);
            for (int j = 0; j < max; ++j) {
                TestCase testCase = this.generateRandomTestCase();
                testCases.add(testCase);
            }
            testSuite.setTestCases(testCases);
            this.validateChromosome(testSuite);
            population.add(testSuite);
        }
    }

    // Generating a random test case
    private TestCase generateRandomTestCase() {
        ArrayList<Action> actions = new ArrayList<>();
        String calln = randomHelper.generateRandomVarName(varNameLength);
        ConstructorAction constructorAction = new ConstructorAction(spoonParser.getConstructor(), calln);
        String varn = randomHelper.generateRandomVarName(varNameLength);
        while (varn.equals(calln))
            varn = randomHelper.generateRandomVarName(varNameLength);
        MethodAction methodAction = new MethodAction(spoonParser.getRandomMethod(), calln, varn);
        actions.add(constructorAction);
        actions.add(methodAction);
        ArrayList<String> values = randomHelper.getRandomValues(actions);
        return new TestCase(methodAction.getMethodName(), actions, values);
    }

    // Evaluating the population
    private void evaluatePopulation() {
        for (TestSuite testSuite : population)
            testSuite.setFitness(this.evaluateChromosome(testSuite));
    }

    // Evaluating a chromosome
    private double evaluateChromosome(TestSuite testSuite) {
        try {
            return executor.calculateFitness(testSuite);
        } catch (Exception e) {
            return 0.0;
        }
    }

    // Optimizing a test suite by trying to delete some test cases and see if it impacts the fitness
    private TestSuite optimizeSolution(TestSuite testSuite) {
        TestSuite newSuite = new TestSuite(testSuite.getClassName(), testSuite.getTestCases());
        newSuite.getTestCases().remove(randomHelper.generateRandomInteger(0, newSuite.getTestCases().size() - 1));
        newSuite.setFitness(this.evaluateChromosome(newSuite));
        while(testSuite.getFitness().equals(newSuite.getFitness())) {
            testSuite = newSuite;
            newSuite = new TestSuite(testSuite.getClassName(), testSuite.getTestCases());
            newSuite.getTestCases().remove(randomHelper.generateRandomInteger(0, newSuite.getTestCases().size() - 1));
            newSuite.setFitness(this.evaluateChromosome(newSuite));
        }
        return testSuite;
    }
}
