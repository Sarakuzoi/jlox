package lox;

import java.util.List;
import java.util.Optional;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    private static class BreakException extends RuntimeException {}

    private static class ContinueException extends RuntimeException {}

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    private Environment environment = new Environment();
    private boolean repl = false;

    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    void interpret(List<Stmt> statements, boolean repl) {
        this.repl = repl;
        interpret(statements);
    }

    private void execute (Stmt stmt) {
        if (repl && stmt instanceof Stmt.Expression expr) {
            System.out.println(stringify(expr.expression.accept(this)));
            return;
        }
        stmt.accept(this);
    }

    void executeBlock(List<Stmt> statements,
                      Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;

            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }


    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case MINUS:
                checkNumberOperand(expr.operator, left, right);
                return (double)left - (double)right;
            case SLASH:
                checkNumberOperand(expr.operator, left, right);
                if ((Double)right == 0d)
                    throw new RuntimeError(expr.operator,
                            "Attempt to divide by 0");
                return (double)left / (double)right;
            case STAR:
                checkNumberOperand(expr.operator, left, right);
                return (double)left * (double)right;
            case MODULO:
                checkNumberOperand(expr.operator, left, right);
                if ((Double)right == 0d)
                    throw new RuntimeError(expr.operator,
                            "Attempt to divide by 0");
                return (double)left % (double)right;
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double)left + (double)right;
                }
                if (left instanceof String) {
                    if (right instanceof String || right instanceof Boolean || (right instanceof Double && (Double)right != ((Double) right).intValue()))
                        return (String)left + right;
                    if (right instanceof Double)
                        return (String)left + ((Double) right).intValue();
                    throw new RuntimeError(expr.operator,
                            "Operands must be string-convertable.");
                }
                if (right instanceof String) {
                    if (left instanceof Boolean || (left instanceof Double && (Double)left != ((Double) left).intValue()))
                        return left + (String)right;
                    if (left instanceof Double)
                        return (String)right + ((Double) left).intValue();
                    throw new RuntimeError(expr.operator,
                            "Operands must be string-convertable.");
                }
                throw new RuntimeError(expr.operator,
                        "Operands must be two numbers or two strings.");
            case GREATER:
                if (left instanceof Double && right instanceof Double) {
                    return (double)left > (double)right;
                }
                if (left instanceof String && right instanceof String) {
                    return ((String) left).compareTo((String) right) > 0;
                }
                throw new RuntimeError(expr.operator,
                        "Operands must be two numbers or two strings.");
            case GREATER_EQUAL:
                if (left instanceof Double && right instanceof Double) {
                    return (double)left >= (double)right;
                }
                if (left instanceof String && right instanceof String) {
                    return ((String) left).compareTo((String) right) >= 0;
                }
                throw new RuntimeError(expr.operator,
                        "Operands must be two numbers or two strings.");
            case LESS:
                if (left instanceof Double && right instanceof Double) {
                    return (double)left < (double)right;
                }
                if (left instanceof String && right instanceof String) {
                    return ((String) left).compareTo((String) right) < 0;
                }
                throw new RuntimeError(expr.operator,
                        "Operands must be two numbers or two strings.");
            case LESS_EQUAL:
                if (left instanceof Double && right instanceof Double) {
                    return (double)left <= (double)right;
                }
                if (left instanceof String && right instanceof String) {
                    return ((String) left).compareTo((String) right) <= 0;
                }
                throw new RuntimeError(expr.operator,
                        "Operands must be two numbers or two strings.");
            case BANG_EQUAL: return !isEqual(left, right);
            case EQUAL_EQUAL: return isEqual(left, right);
        }
        return null;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);

        if (expr.operator.type == TokenType.OR) {
            if (isTruthy(left)) return left;
        } else {
            if (!isTruthy(left)) return left;
        }

        return evaluate(expr.right);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case MINUS -> {
                checkNumberOperand(expr.operator, right);
                return -(double)right;
            }
            case BANG -> {
                return !isTruthy(right);
            }
        }
        return null;
    }

    @Override
    public Object visitTernaryExpr(Expr.Ternary expr) {
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if(isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null){
            execute(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        try {
            while (isTruthy(evaluate(stmt.condition))) {
                try {
                    execute(stmt.body);
                } catch (ContinueException ex) {
                    // Do Nothing
                }
            }
        } catch (BreakException ex) {
            // Do Nothing
        }
        return null;
    }

    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        throw new BreakException();
    }

    @Override
    public Void visitContinueStmt(Stmt.Continue stmt) {
        if (stmt.forIncrement != null) {
            evaluate(stmt.forIncrement);
        }
        throw new ContinueException();
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = Optional.empty();
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }

        environment.define(stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        environment.assign(expr.name, value);
        return value;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.get(expr.name);
    }



    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    private boolean isTruthy(Object o) {
        if (o == null) return false;
        if (o.equals(0.0)) return false;
        if (o instanceof Boolean) return (boolean) o;
        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;

        return a.equals(b);
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number");
    }

    private void checkNumberOperand(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(operator, "Operands must be numbers");
    }

    public String stringify(Object o) {
        if (o == null) return "nil";

        if (o instanceof Double) {
            String text = o.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return  text;
        }

        return o.toString();
    }
}
