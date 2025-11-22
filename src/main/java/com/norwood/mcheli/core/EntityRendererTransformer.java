package com.norwood.mcheli.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.*;

import static org.objectweb.asm.Opcodes.*;

public class EntityRendererTransformer implements IClassTransformer {
    private static final ObfSafeName RENDER_WORLD_PASS = new ObfSafeName("renderWorldPass", "func_175068_a");
    private static final ObfSafeName SHADE_MODEL = new ObfSafeName("shadeModel", "func_179103_j");

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (!"net.minecraft.client.renderer.EntityRenderer".equals(transformedName)) {
            return basicClass;
        }

        try {
            ClassNode classNode = new ClassNode();
            ClassReader classReader = new ClassReader(basicClass);
            classReader.accept(classNode, 0);

            for (MethodNode method : classNode.methods) {
                if (RENDER_WORLD_PASS.matches(method.name)) {
                    patchRenderWorldPass(method);
                }
            }

            ClassWriter writer = new W_ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);
            classNode.accept(writer);
            return writer.toByteArray();
        } catch (Throwable t) {
            MCHCore.fail("net.minecraft.client.renderer.EntityRenderer", t);
            return basicClass;
        }
    }

    private static void patchRenderWorldPass(MethodNode method) {
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn.getOpcode() == INVOKESTATIC && insn instanceof MethodInsnNode minsn) {
                if (minsn.owner.contains("GlStateManager") && SHADE_MODEL.matches(minsn.name)) {
                    AbstractInsnNode prev = insn.getPrevious();
                    if (prev != null && prev.getOpcode() == SIPUSH && ((IntInsnNode) prev).operand == 7425) {

                        InsnList inject = new InsnList();
                        inject.add(new VarInsnNode(ALOAD, 0));
                        inject.add(new VarInsnNode(FLOAD, 2));
                        inject.add(new MethodInsnNode(INVOKESTATIC, "com/norwood/mcheli/core/EntityRendererHook", "renderFarPass", "(Lnet/minecraft/client/renderer/EntityRenderer;F)V", false));
                        method.instructions.insert(insn, inject);
                        MCHCore.coreLogger.info("patched renderWorldPass");
                        return;
                    }
                }
            }
        }
    }
}
