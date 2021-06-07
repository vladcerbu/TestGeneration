package chromosome.action;

import spoon.reflect.declaration.CtMethod;

public class MethodAction extends Action {
    private String callName;
    private String methodName;

    public MethodAction(CtMethod<?> method, String callName, String varName) {
        super(method, varName);
        this.callName = callName;
        this.methodName = method.getSimpleName();
    }

    public String getCallName() {
        return callName;
    }

    public void setCallName(String callName) {
        this.callName = callName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    @Override
    public String toString() {
        String code;
        if (super.getType().equals("void"))
            code = callName + "." + methodName + "(";
        else
            code = super.getType() + " " + super.getVarName() + " = " + callName + "." + methodName + "(";
        return code;
    }
}
