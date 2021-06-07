package util;

import chromosome.action.Action;

import java.util.ArrayList;
import java.util.Random;

public class RandomHelper {
    private final Random random = new Random();
    private final int minNr;
    private final int maxNr;
    private final int maxStringLength;
    private final int stringType;

    public RandomHelper(int minNr, int maxNr, int maxStringLength, int stringType) {
        this.minNr = minNr;
        this.maxNr = maxNr;
        this.maxStringLength = maxStringLength;
        this.stringType = stringType;
    }

    public Integer generateRandomInteger(int minNr, int maxNr) {
        return random.nextInt(maxNr + 1 - minNr) + minNr;
    }

    public String generateRandomIntegerString(int minNr, int maxNr) {
        return Integer.toString(generateRandomInteger(minNr, maxNr));
    }

    public Double generateRandomDouble(int minNr, int maxNr) {
        return minNr + (maxNr - minNr) * random.nextDouble();
    }

    public String generateRandomDoubleString(int minNr, int maxNr) {
        return Double.toString(generateRandomDouble(minNr, maxNr));
    }

    public Boolean generateRandomBoolean() {
        return random.nextBoolean();
    }

    public String generateRandomBooleanString() {
        return generateRandomBoolean() ? "true" : "false";
    }

    public String generateRandomString(int maxStringLength, int stringType) {
        switch (stringType) {
            case 1 -> {
                return generateRandomStringSmall(maxStringLength);
            }
            case 2 -> {
                return generateRandomStringBig(maxStringLength);
            }
            case 3 -> {
                return generateRandomStringNumbers(maxStringLength);
            }
            case 4 -> {
                return generateRandomStringAll(maxStringLength);
            }
            default -> {
                return "";
            }
        }
    }

    public ArrayList<String> getRandomValues(ArrayList<Action> actions) {
        ArrayList<String> values = new ArrayList<>();
        for (Action action : actions) {
            ArrayList<String> params = action.getParamTypes();
            for (String p : params) {
                switch (p) {
                    case "int", "Integer" -> values.add(generateRandomIntegerString(minNr, maxNr));
                    case "double", "Double" -> values.add(generateRandomDoubleString(minNr, maxNr));
                    case "boolean", "Boolean" -> values.add(generateRandomBooleanString());
                    case "String" -> values.add(generateRandomString(maxStringLength, stringType));
                    default -> values.add("null");
                }
            }
        }
        return values;
    }

    public ArrayList<String> getRandomValues(Action action) {
        ArrayList<String> values = new ArrayList<>();
        for (String p : action.getParamTypes()) {
            switch (p) {
                case "int", "Integer" -> values.add(generateRandomIntegerString(minNr, maxNr));
                case "double", "Double" -> values.add(generateRandomDoubleString(minNr, maxNr));
                case "boolean", "Boolean" -> values.add(generateRandomBooleanString());
                case "String" -> values.add(generateRandomString(maxStringLength, stringType));
                default -> values.add("null");
            }
        }
        return values;
    }

    public String generateRandomVarName(int maxLength) {
        int leftLimit = 97; // letter 'a'
        int rightLimit = 122; // letter 'z'
        return random.ints(leftLimit, rightLimit + 1)
                .limit(maxLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    public String generateRandomStringSmall(int maxStringLength) {
        int leftLimit = 97; // letter 'a'
        int rightLimit = 122; // letter 'z'
        maxStringLength = generateRandomInteger(1, maxStringLength + 1);
        return random.ints(leftLimit, rightLimit + 1)
                .limit(maxStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    public String generateRandomStringBig(int maxStringLength) {
        int leftLimit = 65; // letter 'A'
        int rightLimit = 122; // letter 'z'
        maxStringLength = generateRandomInteger(1, maxStringLength + 1);
        return random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i >= 65 && i <= 90) || i >= 97)
                .limit(maxStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    public String generateRandomStringNumbers(int maxStringLength) {
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        maxStringLength = generateRandomInteger(1, maxStringLength + 1);
        return random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(maxStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    public String generateRandomStringAll(int maxStringLength) {
        int leftLimit = 35; // '#'
        int rightLimit = 126; // '~'
        maxStringLength = generateRandomInteger(1, maxStringLength + 1);
        return random.ints(leftLimit, rightLimit + 1)
                .filter(i -> i != 92) // no '\'
                .limit(maxStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }
}
