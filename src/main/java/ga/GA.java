package ga;

import application.appconfig.ApplicationContext;
import chromosome.TestCase;
import chromosome.TestSuite;
import chromosome.action.Action;
import chromosome.action.ConstructorAction;
import chromosome.action.MethodAction;
import evaluation.TestSuiteExecutor;
import parser.ASTParser;
import spoon.reflect.declaration.CtMethod;
import util.RandomHelper;
import writer.ResultWriter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;

public class GA {
    private final ResultWriter resultWriter;
    private final TestSuiteExecutor executor = new TestSuiteExecutor();
    private RandomHelper randomHelper;
    private ASTParser ast;
    private ArrayList<TestSuite> population;
    private int maxSuiteLength;
    private int maxStringLength;
    private int minNr;
    private int maxNr;
    private int stringType;
    private boolean onlyFirst = false;
    private final int varNameLength = Integer.parseInt(ApplicationContext.getProperties().getProperty("data.varNameLength"));
    private final int populationSize = Integer.parseInt(ApplicationContext.getProperties().getProperty("data.populationSize"));
    private final int generations = Integer.parseInt(ApplicationContext.getProperties().getProperty("data.generations"));
    private final double crossoverProb = Double.parseDouble(ApplicationContext.getProperties().getProperty("data.crossoverProb"));
    private final double initialAdditionProb = Double.parseDouble(ApplicationContext.getProperties().getProperty("data.initialAdditionProb"));

    private final Comparator<TestSuite> comp = (o1, o2) -> {
        if (o1.getFitness() > o2.getFitness())
            return -1;
        else if (o1.getFitness() < o2.getFitness())
            return 1;
        else
            return Integer.compare(o1.getTestCases().size(), o2.getTestCases().size());
    };

    public GA(ResultWriter resultWriter) {
        this.resultWriter = resultWriter;
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

    private void checkMaxSuiteSize() {
        if (this.maxSuiteLength < 5 * this.ast.getMethods().size())
            this.maxSuiteLength = 5 * this.ast.getMethods().size();
    }

    public void start(String classPath) {
        this.randomHelper = new RandomHelper(this.minNr, this.maxNr, this.maxStringLength, this.stringType);
        this.ast = new ASTParser(classPath);
        checkMaxSuiteSize();
        generateRandomPopulation();
        evaluatePopulation();
        population.sort(comp);
        for (int gen = 0; gen < generations; gen++) {
            if (onlyFirst && population.get(0).getFitness() == 1.0)
                break;
            ArrayList<TestSuite> newPop = new ArrayList<>();
            newPop.add(population.get(0));
            while (newPop.size() < populationSize) {
                ArrayList<TestSuite> offsprings;
                ArrayList<TestSuite> parents = select();
                if (randomHelper.generateRandomDouble(0, 1) <= crossoverProb)
                    offsprings = crossover(parents.get(0), parents.get(1));
                else
                    offsprings = parents;
                offsprings.set(0, mutate(offsprings.get(0)));
                offsprings.set(1, mutate(offsprings.get(1)));
                offsprings.get(0).setFitness(evaluateChromosome(offsprings.get(0)));
                offsprings.get(1).setFitness(evaluateChromosome(offsprings.get(1)));
                double bestParentFitness = parents.get(0).getFitness() >= parents.get(1).getFitness() ? parents.get(0).getFitness() : parents.get(1).getFitness();
                double bestOffspringFitness = offsprings.get(0).getFitness() >= offsprings.get(1).getFitness() ? offsprings.get(0).getFitness() : offsprings.get(1).getFitness();
                int lengthParents = parents.get(0).getTestCases().size() + parents.get(1).getTestCases().size();
                int lengthOffsprings = offsprings.get(0).getTestCases().size() + offsprings.get(1).getTestCases().size();
                int bestLength = population.get(0).getTestCases().size();
                if (bestOffspringFitness > bestParentFitness || (bestOffspringFitness == bestParentFitness && lengthOffsprings <= lengthParents)) {
                    for (int i = 0; i < offsprings.size(); ++i)
                        if (offsprings.get(i).getTestCases().size() <= 2 * bestLength)
                            newPop.add(offsprings.get(i));
                        else
                            newPop.add(parents.get(i));
                }
                else {
                    newPop.add(parents.get(0));
                    newPop.add(parents.get(1));
                }
            }
            population = newPop;
            population.sort(comp);
            if (population.size() > populationSize) {
                population.subList(populationSize, population.size()).clear();
            }
        }
        try {
            resultWriter.write(population.get(0));
        }
        catch (IOException ignored) {

        }
        File file = new File("./spooned/" + population.get(0).getClassName() + ".java");
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }

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

        if (selected.isEmpty()) {
            selected.add(population.get(0));
            selected.add(population.get(1));
            return selected;
        }

        randomRank = randomHelper.generateRandomDouble(0, 1);
        cumulativeSum = 0.0;
        for (int i = 0; i < populationSize; ++i) {
            cumulativeSum += (populationSize - i) / sumRanks;
            if (cumulativeSum >= randomRank && !selected.contains(population.get(i))) {
                selected.add(population.get(i));
                break;
            }
        }

        if (selected.size() == 1)
            selected.add(population.get(0));
        return selected;
    }

    private ArrayList<TestSuite> crossover(TestSuite c1, TestSuite c2) {
        double a = randomHelper.generateRandomDouble(0, 1);
        ArrayList<TestCase> tc1 = c1.getTestCases();
        ArrayList<TestCase> tc2 = c2.getTestCases();
        int mid1 = (int) (a * tc1.size());
        int mid2 = (int) (a * tc2.size());
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

    private TestSuite mutate(TestSuite testSuite) {
        ArrayList<TestCase> testCases = testSuite.getTestCases();
        ArrayList<TestCase> toDelete = new ArrayList<>();
        double mutationProb = 1.0 / testCases.size();
        double prob, mutationType;
        for (TestCase currentTestCase : testCases) {
            prob = randomHelper.generateRandomDouble(0, 1);
            if (prob <= mutationProb) {
                mutationType = randomHelper.generateRandomDouble(0, 1);
                if (mutationType <= 1.0 / 3.0)
                    toDelete.add(currentTestCase);
                else if (mutationType > 1.0 / 3.0 && mutationType <= 2.0 / 3.0)
                    currentTestCase.setValues(randomHelper.getRandomValues(currentTestCase.getActions()));
                else {
                    ArrayList<Action> actions = currentTestCase.getActions();
                    ArrayList<String> values = currentTestCase.getValues();
                    int nr_params = actions.get(1).getParamTypes().size();
                    values.subList(values.size() - nr_params, values.size()).clear();
                    MethodAction oldMethod = (MethodAction) actions.remove(1);
                    CtMethod<?> method = this.ast.getRandomMethod();
                    if (this.ast.getMethods().size() > 1)
                        while (oldMethod.getMethodName().equals(method.getSimpleName()))
                            method = this.ast.getRandomMethod();
                    String varn = randomHelper.generateRandomVarName(varNameLength);
                    String calln = actions.get(0).getVarName();
                    while (varn.equals(calln))
                        varn = randomHelper.generateRandomVarName(varNameLength);
                    MethodAction newMethod = new MethodAction(method, calln, varn);
                    values.addAll(randomHelper.getRandomValues(newMethod));
                    actions.add(newMethod);
                    currentTestCase.setTestedMethodName(method.getSimpleName());
                    currentTestCase.setActions(actions);
                    currentTestCase.setValues(values);
                }
            }
        }
        for (TestCase testCase : toDelete)
            testCases.remove(testCase);

        double additionProb = initialAdditionProb;
        double additionRandom = randomHelper.generateRandomDouble(0, 1);
        while (additionProb >= additionRandom && testCases.size() < maxSuiteLength) {
            testCases.add(generateRandomTestCase());
            additionProb = additionProb * additionProb;
            additionRandom = randomHelper.generateRandomDouble(0, 1);
        }
        testSuite.setTestCases(testCases);
        return validateChromosome(testSuite);
    }

    private TestSuite validateChromosome(TestSuite testSuite) {
        Set<TestCase> setTC = new LinkedHashSet<>(testSuite.getTestCases());
        testSuite.setTestCases(new ArrayList<>(setTC));
        ArrayList<TestCase> testCases = testSuite.getTestCases();
        for (TestCase testCase : testCases) {
            for (TestCase other : testCases) {
                if (testCase != other) {
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

    private void generateRandomPopulation() {
        this.population = new ArrayList<>();
        for (int i = 0; i < populationSize; ++i) {
            TestSuite testSuite = new TestSuite(this.ast.getClassName());
            ArrayList<TestCase> testCases = new ArrayList<>();
            int max = randomHelper.generateRandomInteger(maxSuiteLength / 2, maxSuiteLength);
            for (int j = 0; j < max; ++j) {
                TestCase testCase = generateRandomTestCase();
                testCases.add(testCase);
            }
            testSuite.setTestCases(testCases);
            population.add(testSuite);
        }
    }

    private TestCase generateRandomTestCase() {
        ArrayList<Action> actions = new ArrayList<>();
        String calln = randomHelper.generateRandomVarName(varNameLength);
        ConstructorAction constructorAction = new ConstructorAction(this.ast.getConstructor(), calln);
        String varn = randomHelper.generateRandomVarName(varNameLength);
        while (varn.equals(calln))
            varn = randomHelper.generateRandomVarName(varNameLength);
        MethodAction methodAction = new MethodAction(this.ast.getRandomMethod(), calln, varn);
        actions.add(constructorAction);
        actions.add(methodAction);
        ArrayList<String> values = randomHelper.getRandomValues(actions);
        return new TestCase(methodAction.getMethodName(), actions, values);
    }

    private void evaluatePopulation() {
        for (TestSuite testSuite : population)
            testSuite.setFitness(evaluateChromosome(testSuite));
    }

    private double evaluateChromosome(TestSuite testSuite) {
        try {
            return executor.execute(testSuite);
        } catch (Exception e) {
            return randomHelper.generateRandomDouble(0, 1);
        }
    }
}
