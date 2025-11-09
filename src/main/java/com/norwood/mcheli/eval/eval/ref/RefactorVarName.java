package com.norwood.mcheli.eval.eval.ref;

public class RefactorVarName extends RefactorAdapter {

    protected final Class<?> targetClass;
    protected final String oldName;
    protected final String newName;

    public RefactorVarName(Class<?> targetClass, String oldName, String newName) {
        this.targetClass = targetClass;
        this.oldName = oldName;
        this.newName = newName;
        if (oldName == null || newName == null) {
            throw new NullPointerException();
        }
    }

    @Override
    public String getNewName(Object target, String name) {
        if (name.equals(this.oldName)) {
            if (this.targetClass == null) {
                if (target == null) {
                    return this.newName;
                }
            } else if (target != null && this.targetClass.isAssignableFrom(target.getClass())) {
                return this.newName;
            }

        }
        return null;
    }
}
