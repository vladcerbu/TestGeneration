package chromosome.action;

import spoon.reflect.declaration.CtConstructor;

public class ConstructorAction extends Action {

    public ConstructorAction(CtConstructor<Object> constructor, String varName) {
        super(constructor, varName);
    }

    @Override
    public String toString() {
        return super.getType() + " " + super.getVarName() + " = new " + super.getType() + "(";
    }
}
