package generation;

import analysis.AstUtils;
import analysis.MySymbolTable;
import pt.up.fe.comp.jmm.JmmNode;
import analysis.Method;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.*;
import java.util.function.BiFunction;

import static generation.OllirAssistant.addAllAuxCode;
import static generation.OllirAssistant.convertTypeToString;

public class OllirVisitor {

    private final Map<String, BiFunction<JmmNode, List<OllirAssistant>, OllirAssistant>> visitMap;

    private final MySymbolTable symbolTable;

    private int auxVarCounter;

    public OllirVisitor(SymbolTable symbolTable) {
        this.symbolTable = (MySymbolTable) symbolTable;
        this.visitMap = new HashMap<>();
        this.visitMap.put("Value", this::handleValue);
        this.visitMap.put("BinaryOp", this::handleBinaryOp);
        this.visitMap.put("Statement", this::handleStatement);
        this.visitMap.put("Indexing", this::handleIndexing);
        this.visitMap.put("MethodCall", this::handleMethodCall);
        this.visitMap.put("Method", this::handleMethod);
        this.visitMap.put("Args", this::handleArgs);
        this.visitMap.put("ClassDeclaration", this::handleClassDeclaration);
        this.visitMap.put("VarDeclaration", this::handleVarDeclaration);
        this.visitMap.put("MethodDeclaration", this::handleMethodDeclaration);
        this.visitMap.put("MethodHeader", this::handleMethodHeader);
        this.visitMap.put("MethodBody", this::handleMethodBody);
        this.visitMap.put("Return", this::handleReturn);
        this.visitMap.put("Array", this::handleArray);
        this.visitMap.put("Length", this::handleLength);
        this.visitMap.put("UnaryOp", this::handleUnaryOp);
        this.visitMap.put("IterationStatement", this::handleIterationStatement);
        this.visitMap.put("CompoundStatement", this::handleCompoundStatement);
        this.visitMap.put("SelectionStatement", this::handleSelectionStatement);
        this.visitMap.put("MainBody", this::handleMethodBody);
        this.visitMap.put("Main", this::handleMain);
        this.visitMap.put("ClassObj", this::handleClassObj);


        auxVarCounter = 0;
    }

    private OllirAssistant handleValue(JmmNode node, List<OllirAssistant> childrenResults) {


        String value = "";
        Type type = AstUtils.getValueType(node, symbolTable);
        String name = node.get("object");

        Optional<JmmNode> ancestorOpt = node.getAncestor("MethodDeclaration");
        if(ancestorOpt.isEmpty()) {
            throw new RuntimeException("Value not inside method");
        }

        JmmNode ancestor = ancestorOpt.get();
        String methodName = AstUtils.getMethodName(ancestor);
        JmmNode methodHeader = ancestor.getChildren().get(0);

        for (int i = 1; i < methodHeader.getChildren().size(); i++) {
            if (methodHeader.getChildren().get(i).get("name").equals(name)) {
                value += "$" + i + ".";
                break;
            }
        }

        OllirAssistantType oat = OllirAssistantType.VALUE;
        if(AstUtils.isVariable(node))
        {
            boolean notFound = true;
            Method method = symbolTable.getMethod(methodName);
            List<Symbol> symbolList = method.getLocalVariables();
            symbolList.addAll(method.getParameters());
            for(Symbol s : symbolList)
            {
                if (s.getName().equals(name)) {
                    notFound = false;
                    break;
                }
            }
            if(notFound)
                oat = OllirAssistantType.FIELD;
        }


        System.out.println(oat);
        if(oat == OllirAssistantType.FIELD)
        {
            System.out.println(name);
        }

        value += name + convertTypeToString(type);
        OllirAssistant result = new OllirAssistant(oat, value, "", type);
        System.out.println(result);

        return result;
    }

    private OllirAssistant handleClassDeclaration(JmmNode node, List<OllirAssistant> childrenResults) {
        StringBuilder value = new StringBuilder();
        value.append(symbolTable.getClassName()).append(" {\n");
        boolean inMethods = false;

        for (OllirAssistant child : childrenResults) {
            if (child.getType() == OllirAssistantType.METHOD_DECLARATION) {
                if(!inMethods) {
                    value.append("\n\t.construct ").append(symbolTable.getClassName()).append("().V {\n");
                    value.append("\t\tinvokespecial(this, \"<init>\").V;\n\t}\n");
                    value.append("\n");
                    inMethods = true;
                }
            }

            value.append("\t").append(child.getValue());
            value.append("\n");
        }
        value.append("}");

        OllirAssistant result = new OllirAssistant(OllirAssistantType.CLASS_DECLARATION, value.toString(), "", null);
        System.out.println("\n\n\n");
        System.out.println(value);
        return result;
    }

    private OllirAssistant handleVarDeclaration(JmmNode node, List<OllirAssistant> childrenResults) {

        Optional<JmmNode> ancestorOpt = node.getAncestor("MethodDeclaration");
        if (ancestorOpt.isPresent())
            return null;

        Symbol childSymbol = AstUtils.getChildSymbol(node, 0);
        String value = ".field private " + childSymbol.getName() + convertTypeToString(childSymbol.getType()) + ";";
        OllirAssistant result = new OllirAssistant(OllirAssistantType.VAR_DECLARATION, value, "", childSymbol.getType());

        
        return result;
    }

    private OllirAssistant handleMethodDeclaration(JmmNode node, List<OllirAssistant> childrenResults) {
        String value = childrenResults.get(0).getValue() + childrenResults.get(1).getValue();

        OllirAssistant result = new OllirAssistant(OllirAssistantType.METHOD_DECLARATION, value, "", null);
        

        return result;
    }

    private OllirAssistant handleMethodHeader(JmmNode node, List<OllirAssistant> childrenResults) {
        StringBuilder value = new StringBuilder(".method public ");
        Method method = symbolTable.getMethod(node.getChildren().get(0).get("name"));

        value.append(method.getName()).append("(");
        for (Symbol s : method.getParameters()) {
            value.append(s.getName()).append(convertTypeToString(s.getType()));
        }

        value.append(")").append(convertTypeToString(method.getReturnType()));
        OllirAssistant result = new OllirAssistant(OllirAssistantType.METHOD_HEADER, value.toString(), "", null);
        
        return result;
    }

    private OllirAssistant handleMethodBody(JmmNode node, List<OllirAssistant> childrenResults) {
        StringBuilder value = new StringBuilder();
        value.append(" {\n");

        for (OllirAssistant oa : childrenResults) {
            if (oa == null) {
                continue;
            }

            for (String s : oa.getAuxCode().split("\n")) {
                value.append("\t\t").append(s).append("\n");
            }
            for(String s : oa.getValue().split("\n")) {
                value.append("\t\t").append(s).append("\n");
            }
        }
        value.append("\t}\n");

        System.out.println("\n\n\n");
        OllirAssistant result = new OllirAssistant(OllirAssistantType.METHOD_BODY, value.toString(), "", null);
        
        return result;
    }

    private OllirAssistant handleMain(JmmNode node, List<OllirAssistant> childrenResults) {
        StringBuilder value = new StringBuilder(".method public static main(args.array.String).V ");

        OllirAssistant result = new OllirAssistant(OllirAssistantType.METHOD_BODY, value.toString(), "", null);
        

        return result;
    }

    private OllirAssistant handleIndexing(JmmNode node, List<OllirAssistant> childrenResults) {
        StringBuilder auxCode = new StringBuilder();
        StringBuilder value = new StringBuilder();

        OllirAssistant childVar = childrenResults.get(0);
        OllirAssistant childIndex = childrenResults.get(1);

        OllirAssistant.addAllAuxCode(auxCode, childrenResults);

        String varValue = childrenResults.get(0).getValue().split("\\.")[0];

        switch (childVar.getType()) {
            case VALUE:
                value.append(varValue);
                break;
            case FIELD:
                String auxVarF = createGetFieldAux(childrenResults.get(0), auxCode);
                value.append(auxVarF);
                break;
            case METHODCALL:
                String auxVar = createAux(varValue, childrenResults.get(0).getVarType(), childVar.getType(), auxCode);
                value.append(auxVar);
                break;
        }

        value.append("[");

        switch (childIndex.getType()) {
            case FIELD:
                String auxVarF = createGetFieldAux(childrenResults.get(0), auxCode);
                value.append(auxVarF);
                break;
            case VALUE:
                value.append(childIndex.getValue());
                break;
            case METHODCALL:
            case BIN_OP:
            case LENGTH:
                String auxVar = createAux(childIndex.getValue(), childIndex.getVarType(), childIndex.getType(), auxCode);
                value.append(auxVar);
                break;
        }

        Type arrayValueType = new Type(childVar.getVarType().getName(), false);
        value.append("]").append(convertTypeToString(arrayValueType));

        OllirAssistant result = new OllirAssistant(OllirAssistantType.INDEXING, value.toString(), auxCode.toString(), arrayValueType);
        
        return result;
    }

    private OllirAssistant handleMethodCall(JmmNode node, List<OllirAssistant> childrenResults) {
        StringBuilder auxCode = new StringBuilder();
        StringBuilder value = new StringBuilder("invokevirtual(");

        OllirAssistant.addAllAuxCode(auxCode, childrenResults);

        Type methodVarType = childrenResults.get(1).getVarType();

        switch (childrenResults.get(0).getType()) {
            case FIELD:
                String auxVarF = createGetFieldAux(childrenResults.get(0), auxCode);
                value.append(auxVarF);
                break;
            case VALUE:
                value.append(childrenResults.get(0).getValue());
                break;
            case UN_OP_NEW_OBJ:
            case METHODCALL:
                String auxVar = createAux(childrenResults.get(0).getValue(),
                        childrenResults.get(0).getVarType(), childrenResults.get(0).getType(), auxCode);
                value.append(auxVar);
                break;
        }
        value.append(childrenResults.get(1).getValue()).append(")").append(convertTypeToString(methodVarType));

        OllirAssistant result = new OllirAssistant(OllirAssistantType.METHODCALL, value.toString(), auxCode.toString(), methodVarType);
        
        return result;
    }

    private OllirAssistant handleMethod(JmmNode node, List<OllirAssistant> childrenResults) {
        OllirAssistant result;
        Type returnType = symbolTable.getReturnType(node.get("methodName"));
        if (childrenResults.size() > 0) {
            OllirAssistant child = childrenResults.get(0);
            StringBuilder auxCode = new StringBuilder();
            OllirAssistant.addAllAuxCode(auxCode, childrenResults);

            result = new OllirAssistant(OllirAssistantType.METHOD,
                    ", " + "\"" + node.get("methodName") + "\"" + child.getValue(),
                    auxCode.toString(), returnType);
            
        } else {
            result = new OllirAssistant(OllirAssistantType.METHOD,
                    ", " + "\"" + node.get("methodName") + "\"", "", returnType);
        }
        return result;
    }

    private OllirAssistant handleReturn(JmmNode node, List<OllirAssistant> childrenResults) {
        StringBuilder value = new StringBuilder("ret");
        StringBuilder auxCode = new StringBuilder();
        OllirAssistant.addAllAuxCode(auxCode, childrenResults);
        OllirAssistant returnChild = childrenResults.get(0);

        value.append(convertTypeToString(returnChild.getVarType())).append(" ");

        switch (returnChild.getType()) {
            case FIELD:
            case VALUE:
            case LENGTH:
            case INDEXING:
                value.append(returnChild.getValue());
                break;
            case BIN_OP:
            case UN_OP_NEG:
            case UN_OP_NEW_ARRAY:
            case UN_OP_NEW_OBJ:
            case METHODCALL:
                String auxVar = createAux(returnChild.getValue(), returnChild.getVarType(),
                        returnChild.getType(), auxCode);
                value.append(auxVar);
                break;
        }
        value.append(";");

        OllirAssistant result = new OllirAssistant(OllirAssistantType.RETURN, value.toString(), auxCode.toString(), returnChild.getVarType());
        

        return result;
    }

    private OllirAssistant handleArgs(JmmNode node, List<OllirAssistant> childrenResults) {
        StringBuilder sb = new StringBuilder();
        Type opType = null;
        StringBuilder auxCode = new StringBuilder();
        OllirAssistant.addAllAuxCode(auxCode, childrenResults);

        for (OllirAssistant childResult : childrenResults) {
            opType = childResult.getVarType();
            sb.append(", ");
            switch (childResult.getType()) {
                case UN_OP_NEG:
                case UN_OP_NEW_ARRAY:
                case UN_OP_NEW_OBJ:
                case LENGTH:
                case METHODCALL:
                case BIN_OP :
                    String auxVar = createAux(childResult.getValue(), childResult.getVarType(),
                            childResult.getType(), auxCode);
                    sb.append(auxVar);
                    break;
                case FIELD:
                    String auxVarF = createGetFieldAux(childrenResults.get(0), auxCode);
                    sb.append(auxVarF);
                    break;
                case VALUE :
                    sb.append(childResult.getValue());
                    break;
            }
        }

        OllirAssistant result = new OllirAssistant(OllirAssistantType.METHOD_ARGS,
                sb.toString(),
                auxCode.toString(),
                opType);

        
        return result;
    }

    private OllirAssistant handleBinaryOp(JmmNode node, List<OllirAssistant> childrenResults) {
        List<String> values = new ArrayList<>();
        Type opType = null;
        StringBuilder auxCode = new StringBuilder();
        OllirAssistant.addAllAuxCode(auxCode, childrenResults);

        for (OllirAssistant childResult : childrenResults) {
            if (opType == null)
                opType = childResult.getVarType();

            switch (childResult.getType()) {
                case UN_OP_NEG:
                case BIN_OP:
                case METHODCALL:
                case LENGTH:
                    String auxVar = createAux(childResult.getValue(), childResult.getVarType(), childResult.getType(), auxCode);
                    values.add(auxVar);
                    break;
                    //TODO: see how OLLIR NEG

                case FIELD:
                    String auxVarF = createGetFieldAux(childrenResults.get(0), auxCode);
                    values.add(auxVarF);
                    break;
                case VALUE:
                    values.add(childResult.getValue());
                    break;
            }
        }
        OllirAssistant result = new OllirAssistant(OllirAssistantType.BIN_OP,
                OllirAssistant.biOpToString(values, opType, node.get("op")),
                auxCode.toString(),
                opType);

        System.out.println(result);
        return result;
    }

    private OllirAssistant handleUnaryOp(JmmNode node, List<OllirAssistant> childrenResults) {
        String value = childrenResults.get(0).getValue();
        StringBuilder auxCode = new StringBuilder();
        OllirAssistantType opType;

        addAllAuxCode(auxCode, childrenResults);

        if(!node.get("op").equals("NEG")) {
            if(childrenResults.get(0).getType() == OllirAssistantType.CLASSOBJ) {
                opType = OllirAssistantType.UN_OP_NEW_OBJ;
            } else
                opType = OllirAssistantType.UN_OP_NEW_ARRAY;

        } else {
            opType = OllirAssistantType.UN_OP_NEG;
        }

        OllirAssistant result = new OllirAssistant(opType, value, auxCode.toString(), childrenResults.get(0).getVarType());

        System.out.println(result);
        return result;
    }

    private OllirAssistant handleLength(JmmNode node, List<OllirAssistant> childrenResults) {
        StringBuilder value = new StringBuilder();
        StringBuilder auxCode = new StringBuilder();

        OllirAssistant child = childrenResults.get(0);

        switch (child.getType()) {
            case FIELD:
                String auxVarF = createGetFieldAux(childrenResults.get(0), auxCode);
                value.append(auxVarF);
                break;
            case VALUE:
                value.append("arraylength(").append(child.getValue()).append(")");
                break;
            case METHODCALL:
                String auxVar = createAux(childrenResults.get(0).getValue(), childrenResults.get(0).getVarType(),
                        child.getType(), auxCode);
                value.append("arraylength(").append(auxVar).append(").i32");
                break;
        }

        OllirAssistant result = new OllirAssistant(OllirAssistantType.LENGTH, value.toString(), auxCode.toString(), new Type("int", false));

        System.out.println(result);
        return result;
    }

    private OllirAssistant handleArray(JmmNode node, List<OllirAssistant> childrenResults) {
        StringBuilder value = new StringBuilder("new(array, ");
        StringBuilder auxCode = new StringBuilder();

        switch (childrenResults.get(0).getType()) {
            case BIN_OP:
            case METHODCALL:
            case LENGTH:
            case INDEXING:
                String auxVar = createAux(childrenResults.get(0).getValue(), childrenResults.get(0).getVarType(),
                        childrenResults.get(0).getType(), auxCode);
                value.append(auxVar);
                break;
            case FIELD:
            case VALUE:
                value.append(childrenResults.get(0).getValue());
                break;
        }

        value.append(").array.i32");

        OllirAssistant result = new OllirAssistant(OllirAssistantType.ARRAY, value.toString(), auxCode.toString(), new Type("int", true));

        System.out.println(result);
        return result;
    }

    private OllirAssistant handleClassObj(JmmNode node, List<OllirAssistant> childrenResults) {
        StringBuilder value = new StringBuilder("new(");
        StringBuilder auxCode = new StringBuilder();

        String objClassName = node.get("classObj");

        value.append(objClassName).append(").").append(objClassName);

        OllirAssistant result = new OllirAssistant(OllirAssistantType.CLASSOBJ, value.toString(), "", new Type(objClassName, false));

        System.out.println(result);
        return result;
    }

    private OllirAssistant handleStatement(JmmNode node, List<OllirAssistant> childrenResults) {
        OllirAssistantType statementType;
        StringBuilder value = new StringBuilder();
        StringBuilder auxCode = new StringBuilder();

        OllirAssistant.addAllAuxCode(auxCode, childrenResults);

        if (node.getChildren().size() == 2) {

            statementType = OllirAssistantType.STATEMENT_ASSIGN;
            String typeString = convertTypeToString(childrenResults.get(0).getVarType());

            if(childrenResults.get(0).getType() == OllirAssistantType.FIELD)
            {
                String auxVar;
                if (childrenResults.get(1).getType() == OllirAssistantType.FIELD) {
                    auxVar = createGetFieldAux(childrenResults.get(1), auxCode);
                } else {
                    if(childrenResults.get(1).getType() != OllirAssistantType.VALUE)
                        auxVar = createAux(childrenResults.get(1).getValue(), childrenResults.get(1).getVarType(),
                                childrenResults.get(1).getType(), auxCode);
                    else
                        auxVar = childrenResults.get(1).getValue();
                }
                value.append("putfield(this, ").append(childrenResults.get(0).getValue()).append(", ")
                        .append(auxVar).append(").V");
            }
            else {
                value.append(childrenResults.get(0).getValue()).append(" :=").append(typeString).append(" ");


                if (childrenResults.get(1).getType() == OllirAssistantType.FIELD) {
                    value.append("getfield(this, ")
                            .append(childrenResults.get(1).getValue()).append(")").append(typeString);
                } else
                    value.append(childrenResults.get(1).getValue());

                if (childrenResults.get(0).getType() != OllirAssistantType.FIELD)
                    if (childrenResults.get(1).getType() == OllirAssistantType.UN_OP_NEW_OBJ)
                        value.append(";\ninvocespecial(").append(childrenResults.get(0).getValue())
                                .append(", \"<init>\").V");
            }
        } else {
            statementType = OllirAssistantType.STATEMENT;
            value.append(childrenResults.get(0).getValue());
        }

        value.append(";");

        OllirAssistant result = new OllirAssistant(statementType, value.toString(), auxCode.toString(), null);


        System.out.println(result);
        return result;

    }

    private OllirAssistant handleIterationStatement(JmmNode node, List<OllirAssistant> childrenResults) {
        StringBuilder value = new StringBuilder();
        StringBuilder auxCode = new StringBuilder();

        OllirAssistant childExpression = childrenResults.get(0);
        OllirAssistant compoundStatement = childrenResults.get(1);

        value.append("Loop:\n");

        if (!childExpression.getAuxCode().equals("")) {
            for (String s : childExpression.getAuxCode().split("\n")) {
                value.append("\t").append(s).append("\n");
            }
        }

        value.append("\tif (").append(childExpression.getValue()).append(") goto Body;\n\tgoto EndLoop;\nBody:\n");
        value.append(compoundStatement.getValue());
        value.append("\tgoto Loop;\n").append("EndLoop:\n");



        OllirAssistant result = new OllirAssistant(OllirAssistantType.ITERATION_STATEMENT, value.toString(), auxCode.toString(), null);


        System.out.println(result);
        return result;
    }

    private OllirAssistant handleSelectionStatement(JmmNode node, List<OllirAssistant> childrenResults) {
        StringBuilder value = new StringBuilder();
        StringBuilder auxCode = new StringBuilder();

        OllirAssistant expression = childrenResults.get(0);
        OllirAssistant thenStatement = childrenResults.get(1);
        OllirAssistant elseStatement = childrenResults.get(2);

        if (!expression.getAuxCode().equals("")) {
            for (String s : expression.getAuxCode().split("\n")) {
                auxCode.append(s).append("\n");
            }
        }

        value.append("if(").append(expression.getValue()).append(") goto Then;\n");
        value.append("\tgoto Else;\n");

        value.append("Then:\n").append(thenStatement.getValue()).append("\tgoto Endif;\n");
        value.append("Else:\n").append(elseStatement.getValue());
        value.append("Endif:");

        OllirAssistant result = new OllirAssistant(OllirAssistantType.SELECTION_STATEMENT, value.toString(), auxCode.toString(), null);

        System.out.println(result);
        return result;
    }

    private OllirAssistant handleCompoundStatement(JmmNode node, List<OllirAssistant> childrenResults) {
        StringBuilder value = new StringBuilder();

        for (OllirAssistant oa : childrenResults) {
            if (oa == null) {
                continue;
            }

            if (!oa.getAuxCode().equals("")) {
                for (String s : oa.getAuxCode().split("\n")) {
                    value.append("\t").append(s).append("\n");
                }
            }
            value.append("\t").append(oa.getValue()).append("\n");
        }

        OllirAssistant result = new OllirAssistant(OllirAssistantType.COMPOUND_STATEMENT, value.toString(), "", null);

        System.out.println(result);
        return result;
    }

    private String createAux(String value, Type type, OllirAssistantType auxType, StringBuilder auxCode) {
        String typeString = convertTypeToString(type);
        String auxVar = "t" + auxVarCounter++ + typeString;
        auxCode.append(auxVar).append(" :=").append(typeString).append(" ").append(value).append(";\n");

        if(auxType == OllirAssistantType.UN_OP_NEW_OBJ) {
            auxCode.append("invocespecial(").append(auxVar).append(", \"<init>\").V;\n");
        }
        return auxVar;
    }

    private String createGetFieldAux(OllirAssistant oa, StringBuilder auxCode) {

        String typeString = convertTypeToString(oa.getVarType());
        String auxVar = "t" + auxVarCounter++ + typeString;
        auxCode.append(auxVar).append(" :=").append(typeString).append(" getfield(this, ")
                .append(oa.getValue()).append(")").append(typeString).append(";\n");

        return auxVar;
    }


    public OllirAssistant visit(JmmNode node) {
        SpecsCheck.checkNotNull(node, () -> "Node should not be null");

        OllirAssistant result = null;
        BiFunction<JmmNode, List<OllirAssistant>, OllirAssistant> visit = this.visitMap.get(node.getKind());

        List<OllirAssistant> returnsChildren = new ArrayList<>();

        // Postorder: first, visit each children
        for (var child : node.getChildren()) {
            returnsChildren.add(visit(child));
        }

        // Postorder: then, visit the node
        if (visit != null) {
            result = visit.apply(node, returnsChildren);
        }

        return result;
    }
}
