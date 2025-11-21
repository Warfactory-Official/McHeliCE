package com.norwood.mcheli.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.*;

import static com.norwood.mcheli.core.MCHCore.coreLogger;
import static org.objectweb.asm.Opcodes.*;

public class EntityTrackerEntryTransformer implements IClassTransformer {
    private static final ObfSafeName IS_VISIBLE_TO = new ObfSafeName("isVisibleTo", "func_180233_c");
    private static final ObfSafeName TRACKED_ENTITY = new ObfSafeName("trackedEntity", "field_73132_a");
    private static final ObfSafeName IS_PLAYER_WATCHING = new ObfSafeName("isPlayerWatchingThisChunk", "func_73121_d");

    private static boolean patchIsVisibleTo(MethodNode method, String ownerInternalName) {
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn.getOpcode() == INVOKESTATIC && insn instanceof MethodInsnNode minCall) {
                if ("java/lang/Math".equals(minCall.owner) && "min".equals(minCall.name) && "(II)I".equals(minCall.desc)) {
                    int localRange = method.maxLocals;
                    int localMaxRange = method.maxLocals + 1;
                    method.maxLocals += 2;
                    InsnList inject = new InsnList();
                    inject.add(new VarInsnNode(ISTORE, localMaxRange));//maxRange
                    inject.add(new VarInsnNode(ISTORE, localRange));//range
                    inject.add(new VarInsnNode(ALOAD, 0));
                    inject.add(new FieldInsnNode(GETFIELD, ownerInternalName, TRACKED_ENTITY.getName(), "Lnet/minecraft/entity/Entity;"));
                    inject.add(new VarInsnNode(ILOAD, localRange));
                    inject.add(new VarInsnNode(ILOAD, localMaxRange));
                    inject.add(new MethodInsnNode(INVOKESTATIC, "com/norwood/mcheli/core/TrackerHook", "getRenderDistance", "(Lnet/minecraft/entity/Entity;II)I", false));
                    method.instructions.insert(minCall, inject);
                    method.instructions.remove(minCall);
                    return true;
                }
            }
        }
        return false;
    }

    private static void patchIsPlayerWatching(MethodNode method, String ownerInternalName) {
        InsnList inject = new InsnList();
        LabelNode continueLabel = new LabelNode();
        inject.add(new VarInsnNode(ALOAD, 0));
        inject.add(new FieldInsnNode(GETFIELD, ownerInternalName, TRACKED_ENTITY.getName(), "Lnet/minecraft/entity/Entity;"));
        inject.add(new MethodInsnNode(INVOKESTATIC, "com/norwood/mcheli/core/TrackerHook", "shouldForceWatch", "(Lnet/minecraft/entity/Entity;)Z", false));
        inject.add(new JumpInsnNode(IFEQ, continueLabel));
        inject.add(new InsnNode(ICONST_1));
        inject.add(new InsnNode(IRETURN));
        inject.add(continueLabel);
        inject.add(new FrameNode(F_SAME, 0, null, 0, null));
        method.instructions.insert(method.instructions.getFirst(), inject);
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (!"net.minecraft.entity.EntityTrackerEntry".equals(transformedName)) {
            return basicClass;
        }

        coreLogger.info("Patching class {} / {}", transformedName, name);

        try {
            ClassNode classNode = new ClassNode();
            ClassReader classReader = new ClassReader(basicClass);
            classReader.accept(classNode, 0);

            for (MethodNode method : classNode.methods) {
                if (IS_VISIBLE_TO.matches(method.name) && "(Lnet/minecraft/entity/player/EntityPlayerMP;)Z".equals(method.desc)) {
                    coreLogger.info("Patching method: {} / {}", IS_VISIBLE_TO.mcp, method.name);
                    if (!patchIsVisibleTo(method, classNode.name)) {
                        throw new IllegalStateException("Did not find Math.min call to patch in EntityTrackerEntry#isVisibleTo");
                    }
                }

                if (IS_PLAYER_WATCHING.matches(method.name) && "(Lnet/minecraft/entity/player/EntityPlayerMP;)Z".equals(method.desc)) {
                    coreLogger.info("Patching method: {} / {}", IS_PLAYER_WATCHING.mcp, method.name);
                    patchIsPlayerWatching(method, classNode.name);
                }
            }

            ClassWriter writer = new W_ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);
            classNode.accept(writer);
            return writer.toByteArray();
        } catch (Throwable t) {
            MCHCore.fail("net.minecraft.entity.EntityTrackerEntry", t);
            return basicClass;
        }
    }
}
