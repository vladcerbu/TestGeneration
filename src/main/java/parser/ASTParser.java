package parser;

import spoon.Launcher;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.factory.Factory;

import java.util.Iterator;
import java.util.Random;
import java.util.Set;

public class ASTParser {

    private final Launcher launcher = new Launcher();
    private final String classPath;
    private String className;
    private Set<CtMethod<?>> methods;
    private Set<CtConstructor<Object>> constructors;

    public ASTParser(String classPath) {
        this.classPath = classPath;
        this.initialization();
    }

    private void initialization() {
        launcher.addInputResource(classPath);
        launcher.run();

        String[] splitedPath = classPath.split("\\\\");
        this.className = splitedPath[splitedPath.length - 1].split("\\.")[0];
        Factory factory = launcher.getFactory();
        this.methods = factory.Class().get(className).getMethods();
        this.constructors = factory.Class().get(className).getConstructors();
    }

    public String getClassPath() {
        return classPath;
    }

    public String getClassName() {
        return className;
    }

    public Set<CtMethod<?>> getMethods() {
        return methods;
    }

    public Set<CtConstructor<Object>> getConstructors() {
        return constructors;
    }

    public CtMethod<?> getRandomMethod() {
        Random rand = new Random();
        int index = rand.nextInt(methods.size());
        Iterator<CtMethod<?>> iter = methods.iterator();
        for (int i = 0; i < index; i++) {
            iter.next();
        }
        return iter.next();
    }

    public CtConstructor<Object> getDefaultConstructor() {
        for (CtConstructor<Object> cons : constructors) {
            if (cons.getParameters().size() == 0)
                return cons;
        }
        return null;
    }

    public CtConstructor<Object> getConstructor() {
        int max = 0;
        CtConstructor<Object> constructor = null;
        for (CtConstructor<Object> cons : constructors) {
            if (cons.isPublic() && cons.getParameters().size() >= max) {
                max = cons.getParameters().size();
                constructor = cons;
            }
        }
        return constructor;
    }
}