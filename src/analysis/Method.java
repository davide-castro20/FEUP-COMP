package analysis;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Method {

    private final String name;
    private final Type returnType;
    private final List<Symbol> parameters, localVariables;

    public Method(String name, Type returnType, List<Symbol> getParameters) {
        this.name = name;
        this.returnType = returnType;
        this.parameters = getParameters;
        this.localVariables = new ArrayList<>();
    }

    public Method(Symbol symbol, List<Symbol> getParameters) {
        this(
            symbol.getName(),
            symbol.getType(),
            getParameters
        );
    }

    public void addLocalVar(Symbol s) {
        this.localVariables.add(s);
    }

    public String getName() {
        return name;
    }

    public Type getReturnType() {
        return returnType;
    }

    public List<Symbol> getParameters() {
        return parameters;
    }

    public List<Symbol> getLocalVariables() {
        return localVariables;
    }

    private boolean areParametersEqual(Method m) {
        if (this.parameters.size() != m.parameters.size())
            return false;

        // Check if all types are same
        for (int i = 0; i < this.parameters.size(); i++) {
            if (!this.parameters.get(i).getType().equals(m.parameters.get(i).getType()))
                return false;
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Method)) return false;
        Method method = (Method) o;
        return name.equals(method.name) && areParametersEqual(method);
    }

    @Override
    public int hashCode() {
        List<Type> paramTypes = new ArrayList<>();
        for (Symbol s: parameters)
            paramTypes.add(s.getType());
        return Objects.hash(name, paramTypes);
    }

    @Override
    public String toString() {
        StringBuilder varsString = new StringBuilder();
        for (Symbol s : localVariables) {
            varsString.append("\t\t").append(s).append("\n");
        }
        return "Method{\n" +
                "\tname='" + name + "'\n" +
                "\treturnType=" + returnType + "\n" +
                "\tparamenters=" + parameters + "\n" +
                "\tlocalVariables=\n" + varsString +
                "\t}";
    }
}
