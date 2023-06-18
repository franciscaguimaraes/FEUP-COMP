package pt.up.fe.comp2023.jasmin;

import org.specs.comp.ollir.*;

import java.util.*;

import static pt.up.fe.comp2023.jasmin.JasminUtils.*;
import static pt.up.fe.comp2023.jasmin.JasminLimits.*;


public class JasminBuilder {
    static ClassUnit classUnit;
    String superClass;
    int localLimit;
    static int stackMaxSize;
    static int currentTotal;
    int countStack;


    public String JasminBuilder(ClassUnit classUnit) {
        JasminBuilder.classUnit = classUnit;
        StringBuilder code = new StringBuilder();
        code.append(".class public ").append(classUnit.getClassName()).append("\n");
        superClass = classUnit.getSuperClass();
        if (superClass == null || getImpClass(superClass, classUnit).equals("Object")) superClass = "java/lang/Object";
        code.append(".super ").append(getImpClass(superClass, classUnit)).append("\n\n");

        for (Field field : classUnit.getFields()) {
            code.append(fieldBuild(field));
        }
        for (Method method : classUnit.getMethods()) {
            code.append(getMethod(method));
        }
        return code.toString();
    }

    public String fieldBuild(Field field) {
        StringBuilder code = new StringBuilder();
        AccessModifiers accessModifiers = field.getFieldAccessModifier();
        StringBuilder accessModName =  new StringBuilder();
        if (accessModifiers != AccessModifiers.DEFAULT) {
            accessModName.append(accessModifiers.name().toLowerCase()).append(" ");
        }
        if (field.isStaticField()){
            accessModName.append("static ");
        }
        if (field.isFinalField()){
            accessModName.append("final ");
        }
        code.append(".field ").append(accessModName).append(field.getFieldName()).append(" ").append(getJasminType(field.getFieldType()));
        if (field.isInitialized()){
            code.append(" = ").append(field.getInitialValue());
        }
        code.append("\n");
        return code.toString();
    }


    private String getMethod(Method method) {
        StringBuilder stringBuilder = new StringBuilder();
        AccessModifiers accessModifiers = method.getMethodAccessModifier();
        stringBuilder.append(".method ");
        if (accessModifiers != AccessModifiers.DEFAULT) {
            stringBuilder.append(accessModifiers.name().toLowerCase()).append(" ");
        }

        if (method.isStaticMethod()) {
            stringBuilder.append("static ");
        }
        if (method.isFinalMethod()) {
            stringBuilder.append("final ");
        }
        if (method.isConstructMethod()) {
            stringBuilder.append("<init>");
        } else {
            stringBuilder.append(method.getMethodName());
        }


        stringBuilder.append("(");
        if(method.isStaticMethod()){
            stringBuilder.append("[Ljava/lang/String;");
        } else {
            for (Element element : method.getParams()) {
                stringBuilder.append(getJasminType(element.getType()));
            }
        }
        stringBuilder.append(")");
        stringBuilder.append(getJasminType(method.getReturnType())).append("\n");

        currentTotal = 0;
        stackMaxSize = 0;
        localLimit = updateLocalLimit(method);

        String instructionsString = getInstructionsString(method);

        if (!((method.getInstructions().size() > 0) && (method.getInstructions().get(method.getInstructions().size() - 1).getInstType() == InstructionType.RETURN))) {
            if (method.getReturnType().getTypeOfElement() == ElementType.VOID) {
                instructionsString += "\treturn\n";
            }
        }

        stringBuilder.append("\t.limit stack ").append(getStackMaxSixe()).append("\n").append("\t.limit locals ").append(localLimit).append("\n");

        stringBuilder.append(instructionsString);

        stringBuilder.append(".end method\n\n");
        return stringBuilder.toString();
    }

    private String getInstructionsString(Method method) {
        StringBuilder newStringBuilder = new StringBuilder();

        List<Instruction> instructions = method.getInstructions();

        for (Instruction instruction : instructions) {
            for (Map.Entry<String, Instruction> label : method.getLabels().entrySet()) {
                if (label.getValue().equals(instruction)) {
                    newStringBuilder.append(label.getKey()).append(":\n");
                }
            }
            newStringBuilder.append(getInstruction(instruction, method.getVarTable()));

            if (instruction.getInstType() == InstructionType.CALL) {
                CallInstruction inst = (CallInstruction) instruction;
                ElementType elementType = inst.getReturnType().getTypeOfElement();

                if (elementType != ElementType.VOID) {
                    newStringBuilder.append("\tpop\n");
                    updateStackLimit(-1);
                }
            }
        }
        return newStringBuilder.toString();
    }


    private String getInstruction(Instruction instruction, HashMap<String, Descriptor> varTable) {
        if (instruction.getInstType() == InstructionType.CALL) {
            return dealWithCall((CallInstruction) instruction, varTable);
        } else if (instruction.getInstType() == InstructionType.GOTO) {
            return dealWithGoTo((GotoInstruction) instruction);
        } else if (instruction.getInstType() == InstructionType.BRANCH) {
            return dealWithBranch((CondBranchInstruction) instruction, varTable);
        } else if (instruction.getInstType() == InstructionType.NOPER) {
            return loadStack(((SingleOpInstruction) instruction).getSingleOperand(), varTable);
        } else if (instruction.getInstType() == InstructionType.ASSIGN) {
            return dealWithAssign((AssignInstruction) instruction, varTable);
        } else if (instruction.getInstType() == InstructionType.RETURN) {
            return dealWithReturn((ReturnInstruction) instruction, varTable);
        } else if (instruction.getInstType() == InstructionType.GETFIELD) {
            return dealWithGetField((GetFieldInstruction) instruction, varTable);
        } else if (instruction.getInstType() == InstructionType.PUTFIELD) {
            return dealWithPutField((PutFieldInstruction) instruction, varTable);
        } else if (instruction.getInstType() == InstructionType.UNARYOPER) {
            return dealWithUnaryOper((UnaryOpInstruction) instruction, varTable);
        } else if (instruction.getInstType() == InstructionType.BINARYOPER) {
            return dealWithBinaryOper((BinaryOpInstruction) instruction, varTable);
        }
        return "";
    }

    private String loadStack(Element element, HashMap<String, Descriptor> varTable) {
        StringBuilder inst = new StringBuilder();
        if (element instanceof Operand operand) {
            if (operand.getName().equals("false")) {
                inst.append("\ticonst_0").append("\n");
                updateStackLimit(1);
                return inst.toString();
            } else if (operand.getName().equals("true")) {
                inst.append("\ticonst_1").append("\n");
                updateStackLimit(1);
                return inst.toString();
            }
        }

        if (element instanceof LiteralElement) {
            String literal = ((LiteralElement) element).getLiteral();

            if ((element.getType().getTypeOfElement() == ElementType.INT32) ||
                    element.getType().getTypeOfElement() == ElementType.BOOLEAN) {

                int parser = Integer.parseInt(literal);

                if (parser >= -1 && parser <= 5) inst.append("\ticonst_");

                else if (parser >= -128 && parser <= 127) inst.append("\tbipush ");

                else if (parser >= -32768 && parser <= 32767) inst.append("\tsipush ");

                else inst.append("\tldc ");

                if (parser == -1) inst.append("m1");
                else inst.append(parser);
            }
            else inst.append("\tldc ").append(literal);

            updateStackLimit(1);
        }

        else if (element instanceof ArrayOperand op) {

            inst.append("\taload").append(this.getVarRegister(op.getName(), varTable)).append("\n");

            updateStackLimit(1);

            inst.append(loadStack(op.getIndexOperands().get(0), varTable));
            inst.append("\tiaload");

            updateStackLimit(-1);
        }
        else if (element instanceof Operand operand) {

            switch (operand.getType().getTypeOfElement()) {
                case INT32, BOOLEAN -> inst.append("\tiload").append(this.getVarRegister(operand.getName(), varTable));
                case OBJECTREF, STRING, ARRAYREF -> inst.append("\taload").append(this.getVarRegister(operand.getName(), varTable));
                case THIS -> inst.append("\taload_0");
                default -> inst.append("Error: SingleOperand ").append(operand.getType().getTypeOfElement()).append("\n");
            }
            updateStackLimit(1);
        }
        else inst.append("Error: SingleOperand not recognized\n");

        inst.append("\n");
        return inst.toString();
    }


    private String getVarRegister(String name, HashMap<String, Descriptor> varTable) {
        StringBuilder stringBuilder = new StringBuilder();
        if (name.equals("this")) return "_0";
        int reg = varTable.get(name).getVirtualReg();
        if (reg < 4) stringBuilder.append("_");
        else stringBuilder.append(" ");
        stringBuilder.append(reg);
        return stringBuilder.toString();
    }

    private String dealWithBinaryOper(BinaryOpInstruction binaryOpInstruction, HashMap<String, Descriptor> varTable) {
        StringBuilder stringBuilder = new StringBuilder();

        Element left = binaryOpInstruction.getLeftOperand();
        Element right = binaryOpInstruction.getRightOperand();

        OperationType op = binaryOpInstruction.getOperation().getOpType();

        if (op == OperationType.LTH) {

            if (left.isLiteral()) {
                int value = Integer.parseInt(((LiteralElement) left).getLiteral());
                if (value == 0) {
                    stringBuilder.append(this.loadStack(right, varTable)).append("\t").append("ifgt");
                    stringBuilder.append(printOperation()).append("\n");

                    updateStackLimit(-1);

                    return stringBuilder.toString();
                }
            }

            if (right.isLiteral()) {
                int value = Integer.parseInt(((LiteralElement) right).getLiteral());
                if (value == 0) {
                    stringBuilder.append(this.loadStack(left, varTable)).append("\t").append("iflt");
                    stringBuilder.append(printOperation()).append("\n");

                    updateStackLimit(-1);

                    return stringBuilder.toString();
                }


            }
                stringBuilder.append(this.loadStack(left, varTable))
                        .append(this.loadStack(right, varTable)).append("\t")
                        .append(printOpType(binaryOpInstruction.getOperation().getOpType()));

            stringBuilder.append(printOperation()).append("\n");

            return stringBuilder.toString();

        } else if (op == OperationType.GTE) {
            if (left.isLiteral()) {
                int value = Integer.parseInt(((LiteralElement) left).getLiteral());
                if (value == 0) {
                    stringBuilder.append(this.loadStack(right, varTable)).append("\t").append("ifle");
                    stringBuilder.append(printOperation()).append("\n");

                    updateStackLimit(-1);
                    return stringBuilder.toString();
                }
            }

            if (right.isLiteral()) {
                int value = Integer.parseInt(((LiteralElement) right).getLiteral());
                if (value == 0) {
                    stringBuilder.append(this.loadStack(left, varTable)).append("\t").append("ifge");
                    stringBuilder.append(printOperation()).append("\n");

                    updateStackLimit(-1);
                    return stringBuilder.toString();
                }


            }

            stringBuilder.append(this.loadStack(left, varTable))
                    .append(this.loadStack(right, varTable)).append("\t")
                    .append(printOpType(binaryOpInstruction.getOperation().getOpType()));


            stringBuilder.append(printOperation()).append("\n");

            return stringBuilder.toString();
        }

        stringBuilder.append(this.loadStack(left, varTable))
                .append(this.loadStack(right, varTable)).append("\t")
                .append(printOpType(binaryOpInstruction.getOperation().getOpType()));

        updateStackLimit(-1);
        stringBuilder.append("\n");

        return stringBuilder.toString();
    }

    private String printOpType(OperationType operationType) {
        if (operationType == OperationType.ADD) {
            return "iadd";
        } else if (operationType == OperationType.SUB) {
            return "isub";
        } else if (operationType == OperationType.MUL) {
            return "imul";
        } else if (operationType == OperationType.LTH) {
            return "if_icmplt";
        } else if (operationType == OperationType.ANDB) {
            return "iand";
        } else if (operationType == OperationType.NOTB) {
            return "ifeq";
        } else if (operationType == OperationType.DIV) {
            return "idiv";
        } else if( operationType == OperationType.GTE) {
            return "if_icmpge";
        } else {
            return "Error: operation not recognized:" + operationType + "\n";
        }
    }


    private String dealWithUnaryOper(UnaryOpInstruction unaryOpInstruction, HashMap<String, Descriptor> varTable) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.loadStack(unaryOpInstruction.getOperand(), varTable)).append("\t").append(printOpType(unaryOpInstruction.getOperation().getOpType()));
        if (unaryOpInstruction.getOperation().getOpType() == OperationType.NOTB) {
            stringBuilder.append(this.printOperation());
        }
        else stringBuilder.append("Error: invalid UnaryOper\n");
        stringBuilder.append("\n");
        return stringBuilder.toString();
    }

    private String printOperation() {
        return " TRUE" + this.countStack + "\n" + "\ticonst_0\n" + "\tgoto NEXT" + this.countStack + "\n" + "TRUE"
                + this.countStack + ":\n" + "\ticonst_1\n" + "NEXT" + this.countStack++ + ":";
    }

    private String dealWithPutField(PutFieldInstruction putFieldInstruction, HashMap<String, Descriptor> varTable) {
        updateStackLimit(-2);
        return loadStack(putFieldInstruction.getFirstOperand(), varTable) + loadStack(putFieldInstruction.getThirdOperand(), varTable)
                + "\tputfield " + getImpClass(((Operand) putFieldInstruction.getFirstOperand()).getName(), classUnit) + "/" + ((Operand)
                putFieldInstruction.getSecondOperand()).getName() + " " + getJasminType(putFieldInstruction.getSecondOperand().getType()) + "\n";
    }

    private String dealWithGetField(GetFieldInstruction getFieldInstruction, HashMap<String, Descriptor> varTable) {
        return loadStack(getFieldInstruction.getFirstOperand(), varTable) + "\tgetfield " + getImpClass(((Operand)
                getFieldInstruction.getFirstOperand()).getName(), classUnit) + "/" + ((Operand) getFieldInstruction.getSecondOperand()).getName() + " " +
                getJasminType(getFieldInstruction.getSecondOperand().getType()) + "\n";
    }
    private String dealWithReturn(ReturnInstruction returnInstruction, HashMap<String, Descriptor> varTable) {
        StringBuilder stringBuilder = new StringBuilder();

        if (returnInstruction.hasReturnValue()) stringBuilder.append(loadStack(returnInstruction.getOperand(), varTable));
        stringBuilder.append("\t");

        if (returnInstruction.getOperand() != null) {
            ElementType elementType = returnInstruction.getOperand().getType().getTypeOfElement();

            if (elementType == ElementType.INT32 || elementType == ElementType.BOOLEAN) stringBuilder.append("i");
            else stringBuilder.append("a");
        }
        stringBuilder.append("return\n");

        return stringBuilder.toString();
    }

    private String dealWithAssign(AssignInstruction assignInstruction, HashMap<String, Descriptor> varTable) {
        StringBuilder stringBuilder = new StringBuilder();
        Operand destination = (Operand) assignInstruction.getDest();

        if (destination instanceof ArrayOperand opDest) {
            updateStackLimit(1);
            stringBuilder.append("\taload").append(this.getVarRegister(opDest.getName(), varTable)).append("\n").append(loadStack(opDest.getIndexOperands().get(0), varTable));
        }
        else {
            if (assignInstruction.getRhs().getInstType() == InstructionType.BINARYOPER) {
                BinaryOpInstruction inst = (BinaryOpInstruction) assignInstruction.getRhs();

                if (inst.getOperation().getOpType() == OperationType.ADD) {


                    LiteralElement literal = null;
                    Operand op = null;

                    if (!inst.getLeftOperand().isLiteral() && inst.getRightOperand().isLiteral()) {
                        literal = (LiteralElement) inst.getRightOperand();
                        op = (Operand) inst.getLeftOperand();
                    }
                    else if (inst.getLeftOperand().isLiteral() && !inst.getRightOperand().isLiteral()) {
                        literal = (LiteralElement) inst.getLeftOperand();
                        op = (Operand) inst.getRightOperand();
                    }
                    if (literal != null && op != null) {
                        if (op.getName().equals(destination.getName())) {
                            int literalValue = Integer.parseInt((literal).getLiteral());
                            if (literalValue <= 127) {
                                updateStackLimit(1);
                                return "\tiinc " + varTable.get(op.getName()).getVirtualReg() + " " + literalValue + "\n";
                            } else {
                                dealWithBinaryOper(inst, varTable);
                            }
                        }
                    }
                }
                if (inst.getOperation().getOpType() == OperationType.SUB) {

                    String sign = "-";
                    LiteralElement literal = null;
                    Operand op = null;

                    if (!inst.getLeftOperand().isLiteral() && inst.getRightOperand().isLiteral()) {
                        literal = (LiteralElement) inst.getRightOperand();
                        op = (Operand) inst.getLeftOperand();
                    }
                    else if (inst.getLeftOperand().isLiteral() && !inst.getRightOperand().isLiteral()) {
                        dealWithBinaryOper(inst, varTable);
                    }
                    if (literal != null && op != null) {
                        
                        if (op.getName().equals(destination.getName())) {

                            int literalValue = Integer.parseInt((literal).getLiteral());
                            if ((literalValue <= 128)) {
                                updateStackLimit(1);
                                return "\tiinc " + varTable.get(op.getName()).getVirtualReg() + " -" + literalValue + "\n";
                            } else {
                                dealWithBinaryOper(inst, varTable);
                            }
                        }
                    }
                }
            }
        }
        stringBuilder.append(this.getInstruction(assignInstruction.getRhs(), varTable));

        switch (destination.getType().getTypeOfElement()) {
            case OBJECTREF, THIS, STRING, ARRAYREF:
                stringBuilder.append("\tastore").append(this.getVarRegister(destination.getName(), varTable)).append("\n");
                updateStackLimit(-1);
                break;
            case INT32, BOOLEAN:
                if (varTable.get(destination.getName()).getVarType().getTypeOfElement() == ElementType.ARRAYREF) {
                    stringBuilder.append("\tiastore").append("\n");
                    updateStackLimit(-3);
                } else {
                    stringBuilder.append("\tistore").append(this.getVarRegister(destination.getName(), varTable)).append("\n");
                    updateStackLimit(-1);
                }
                break;
            default:
                stringBuilder.append("Error: couldn't store instruction\n");
                break;
        }
        return stringBuilder.toString();
    }

    private String dealWithBranch(CondBranchInstruction instruction, HashMap<String, Descriptor> varTable) {
        StringBuilder strInst = new StringBuilder();
        String op;

        Instruction brancher;
        if (instruction instanceof SingleOpCondInstruction singleOpCondInstruction) {
            brancher = singleOpCondInstruction.getCondition();
        }
        else if (instruction instanceof OpCondInstruction opCondInstruction) {
            brancher = opCondInstruction.getCondition();
        }
        else {
            return "Error: Instruction branch error\n";
        }
        switch (brancher.getInstType()) {
            case UNARYOPER -> {
                assert brancher instanceof UnaryOpInstruction;
                UnaryOpInstruction unaryOpInstruction = (UnaryOpInstruction) brancher;
                if (unaryOpInstruction.getOperation().getOpType() == OperationType.NOTB) {
                    strInst.append(this.loadStack(unaryOpInstruction.getOperand(), varTable));
                    op = "ifeq";
                }
                else {
                    strInst.append("Error: invalid UnaryOper\n");
                    strInst.append(this.getInstruction(brancher, varTable));
                    op = "ifne";
                }
            }
            case BINARYOPER -> {
                assert brancher instanceof BinaryOpInstruction;
                BinaryOpInstruction binaryOpInstruction = (BinaryOpInstruction) brancher;
                switch (binaryOpInstruction.getOperation().getOpType()) {
                    case LTH -> {
                        Integer counter = null;
                        Element other = null;

                        Element left = binaryOpInstruction.getLeftOperand();
                        Element right = binaryOpInstruction.getRightOperand();
                        op = "if_icmplt";

                        if (left instanceof LiteralElement) {
                            String lit = ((LiteralElement) left).getLiteral();
                            counter = Integer.parseInt(lit);
                            other = right;
                            op = "ifgt";
                        }
                        else if (right instanceof LiteralElement) {
                            String lit = ((LiteralElement) right).getLiteral();
                            counter = Integer.parseInt(lit);
                            other = left;
                            op = "iflt";
                        }
                        if (counter != null && counter == 0) {
                            strInst.append((this.loadStack(other, varTable)));
                        }
                        else {
                            strInst.append(this.loadStack(left, varTable)).append(this.loadStack(right, varTable));
                            op = "if_icmplt";
                        }
                    }
                    case ANDB -> {
                        strInst.append(this.getInstruction(brancher, varTable));
                        op = "ifne";
                    }
                    case GTE -> {
                        Element  left = binaryOpInstruction.getLeftOperand();
                        Element right = binaryOpInstruction.getRightOperand();

                        if (right instanceof LiteralElement && ((LiteralElement) right).getLiteral().equals("0")) {
                            op = "if_icmpge";
                        }
                        else {
                            strInst.append(this.loadStack(right, varTable)).append(this.loadStack(left, varTable));
                            op = "ifge";
                        }
                    }
                    case LTE -> {
                        Element  left = binaryOpInstruction.getLeftOperand();
                        Element right = binaryOpInstruction.getRightOperand();

                        if (left instanceof LiteralElement && ((LiteralElement) left).getLiteral().equals("0")) {
                            op = "if_icmple";
                        }
                        else {
                            strInst.append(this.loadStack(left, varTable)).append(this.loadStack(right, varTable));
                            op = "ifle";
                        }
                    }
                    default -> {
                        strInst.append("Error: BinaryOper not recognized\n");
                        op = "ifne";
                    }
                }
            }
            default -> {
                strInst.append(this.getInstruction(brancher, varTable));
                op = "ifne";
            }
        }
        strInst.append("\t").append(op).append(" ").append(instruction.getLabel()).append("\n");

        if (op.equals("if_icmplt") || op.equals("if_icmple") || op.equals("if_icmpge")) updateStackLimit(-2);
        else updateStackLimit(-1);

        return strInst.toString();
    }


    private String dealWithGoTo(GotoInstruction gotoInstruction) {
        return "\tgoto " + gotoInstruction.getLabel() + "\n";
    }

    private String dealWithCall(CallInstruction instruction, HashMap<String, Descriptor> varTable) {
        StringBuilder strInst = new StringBuilder();
        int popper = 0;

        switch (instruction.getInvocationType()) {
            case arraylength -> strInst.append(loadStack(instruction.getFirstArg(), varTable)).append("\tarraylength\n");

            case NEW -> {
                popper -= 1;
                ElementType type =instruction.getReturnType().getTypeOfElement();
                if (type == ElementType.OBJECTREF) {
                    for (Element element : instruction.getListOfOperands()) {
                        strInst.append(this.loadStack(element, varTable));
                        popper++;
                    }
                    strInst.append("\tnew ").append(getImpClass(((Operand) instruction.getFirstArg()).getName(), classUnit)).append("\n");
                }
                else if (type == ElementType.ARRAYREF) {
                    for (Element element : instruction.getListOfOperands()) {
                        strInst.append(this.loadStack(element, varTable));
                        popper++;
                    }
                    strInst.append("\tnewarray ");
                    if (instruction.getListOfOperands().get(0).getType().getTypeOfElement() == ElementType.INT32) strInst.append("int\n");

                    else strInst.append("Error: Array type not supported\n");

                }
                else strInst.append("Error: Type not supported\n");

            }

            case ldc -> strInst.append(loadStack(instruction.getFirstArg(), varTable));

            case invokestatic -> {

                for (Element element : instruction.getListOfOperands()) {
                    strInst.append(this.loadStack(element, varTable));
                    popper++;
                }
                strInst.append("\tinvokestatic ").append(getImpClass(((Operand) instruction.getFirstArg()).getName(), classUnit)).
                        append("/").append(((LiteralElement) instruction.getSecondArg()).getLiteral().replace("\"", "")).append("(");

                for (Element element : instruction.getListOfOperands()) {
                    strInst.append(getJasminType(element.getType()));
                }
                strInst.append(")").append(getJasminType(instruction.getReturnType())).append("\n");

                if (instruction.getReturnType().getTypeOfElement() != ElementType.VOID) popper--;
            }
            case invokespecial -> {
                popper = 1;
                strInst.append(loadStack(instruction.getFirstArg(), varTable));

                strInst.append("\tinvokespecial ");

                if (instruction.getFirstArg().getType().getTypeOfElement() == ElementType.THIS) strInst.append(superClass);
                else {
                    String className = getImpClass(((ClassType) instruction.getFirstArg().getType()).getName(), classUnit);
                    strInst.append(className);
                }
                strInst.append("/" + "<init>(");

                for (Element element : instruction.getListOfOperands()) {
                    strInst.append(getJasminType(element.getType()));
                }
                strInst.append(")").append(getJasminType(instruction.getReturnType())).append("\n");
                if (instruction.getReturnType().getTypeOfElement() != ElementType.VOID) popper--;
            }
            case invokevirtual -> {
                popper = 1;
                strInst.append(loadStack(instruction.getFirstArg(), varTable));

                for (Element element : instruction.getListOfOperands()) {
                    strInst.append(loadStack(element, varTable));
                    popper++;
                }
                strInst.append("\tinvokevirtual ").append(getImpClass(((ClassType) instruction.getFirstArg().getType()).getName(), classUnit)).
                        append("/").append(((LiteralElement) instruction.getSecondArg()).getLiteral().replace("\"", "")).append("(");

                for (Element element : instruction.getListOfOperands()) {
                    strInst.append(getJasminType(element.getType()));
                }

                strInst.append(")").append(getJasminType(instruction.getReturnType())).append("\n");

                if (instruction.getReturnType().getTypeOfElement() != ElementType.VOID) popper--;
            }

            default -> strInst.append("Error: call instruction not processed.");
        }
        updateStackLimit(-popper);
        return strInst.toString();
    }
}
