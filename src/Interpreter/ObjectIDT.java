package Interpreter;

import AST.ClassNode;

import java.util.HashMap;

public class ObjectIDT implements InterpreterDataType {
    public final HashMap<String,InterpreterDataType> members = new HashMap<>();
    public final ClassNode astNode;

    public ObjectIDT(ClassNode astNode) {
        this.astNode = astNode;
    }

    @Override
    public void Assign(InterpreterDataType in) {
        if (in instanceof ObjectIDT objectIDT) {
            if (!this.astNode.name.equals(objectIDT.astNode.name)) {
                throw new RuntimeException("Cannot assign an object of type " + objectIDT.astNode.name + " to " + this.astNode.name);
            }
            this.members.clear();
            this.members.putAll(objectIDT.members);
        } else {
            throw new RuntimeException("Trying to assign to an object IDT from a " + in.getClass());
        }
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        for (var m : members.entrySet())
            out.append(m.getKey()).append(" : ").append(m.getValue().toString()).append("\n");
        return out.toString();
    }
}
