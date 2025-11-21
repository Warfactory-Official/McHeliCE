package com.norwood.mcheli.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.*;

import static com.norwood.mcheli.core.MCHCore.coreLogger;
import static org.objectweb.asm.Opcodes.*;

// patch it for metallolom. i donno how a checkcast fixed it either..
public class AviatorCodeGeneratorTransformer implements IClassTransformer {
    private static final String TARGET_CLASS_NAME = "com.googlecode.aviator.code.asm.ASMCodeGenerator";
    private static final String TARGET_METHOD = "genNewLambdaCode";
    private static final String ASM_MV_OWNER = "com/googlecode/aviator/asm/MethodVisitor";

    private static boolean patchGenNewLambdaCode(MethodNode method) {
        AbstractInsnNode targetNode = null;
        boolean foundLdcName = false;

        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn.getOpcode() == LDC && insn instanceof LdcInsnNode ldc) {
                if ("newLambda".equals(ldc.cst)) {
                    foundLdcName = true;
                }
            }
            if (foundLdcName && insn.getOpcode() == INVOKEVIRTUAL && insn instanceof MethodInsnNode min) {
                if (ASM_MV_OWNER.equals(min.owner) && "visitMethodInsn".equals(min.name)) {
                    targetNode = insn;
                    break;
                }
            }
        }

        if (targetNode != null) {
            InsnList inject = new InsnList();
            inject.add(new VarInsnNode(ALOAD, 0));
            inject.add(new FieldInsnNode(GETFIELD, "com/googlecode/aviator/code/asm/ASMCodeGenerator", "mv", "Lcom/googlecode/aviator/asm/MethodVisitor;"));
            inject.add(new IntInsnNode(SIPUSH, 192));// CHECKCAST
            inject.add(new LdcInsnNode("com/googlecode/aviator/runtime/type/AviatorObject"));
            inject.add(new MethodInsnNode(INVOKEVIRTUAL, ASM_MV_OWNER, "visitTypeInsn", "(ILjava/lang/String;)V", false));
            method.instructions.insert(targetNode, inject);
            return true;
        }

        return false;
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (!TARGET_CLASS_NAME.equals(transformedName)) {
            return basicClass;
        }

        coreLogger.info("Patching class {} / {}", transformedName, name);

        try {
            ClassNode classNode = new ClassNode();
            ClassReader classReader = new ClassReader(basicClass);
            classReader.accept(classNode, 0);

            boolean patched = false;

            for (MethodNode method : classNode.methods) {
                if (TARGET_METHOD.equals(method.name)) {
                    coreLogger.info("Patching method: {}", method.name);
                    if (patchGenNewLambdaCode(method)) {
                        patched = true;
                    }
                    break;
                }
            }

            if (!patched) {
                throw new IllegalStateException("Did not find 'visitMethodInsn' call for 'newLambda' in " + TARGET_METHOD);
            }

            ClassWriter writer = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);
            classNode.accept(writer);
            return writer.toByteArray();

        } catch (Throwable t) {
            MCHCore.fail(TARGET_CLASS_NAME, t);
            return basicClass;
        }
    }
}