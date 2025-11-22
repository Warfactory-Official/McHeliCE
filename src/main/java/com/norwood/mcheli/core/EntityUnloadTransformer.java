package com.norwood.mcheli.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import static com.norwood.mcheli.core.MCHCore.coreLogger;

public class EntityUnloadTransformer implements IClassTransformer {

    ObfSafeName UNLOAD_ENTITES = new ObfSafeName("unloadEntities", "func_175681_c");

    private static void patchUnloadEntites(MethodNode method) {

        for (AbstractInsnNode insn : method.instructions.toArray()) {

            if (insn.getOpcode() == Opcodes.ALOAD && ((VarInsnNode) insn).var == 1) {

                InsnList inject = new InsnList();

                inject.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "com/norwood/mcheli/core/WorldUnloadHook",
                        "onEntityUnload",
                        "(Ljava/util/Collection;)Ljava/util/Collection;",
                        false
                ));


                method.instructions.insert(insn, inject);
                break;
            }
        }


    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] clazz) {

        if (!"net.minecraft.world.World".equals(transformedName)) {
            return clazz;
        }
        coreLogger.info("Patching class {} / {}", transformedName, name);

        try {
            ClassNode classNode = new ClassNode();
            ClassReader classReader = new ClassReader(clazz);
            classReader.accept(classNode, 0);

            for (MethodNode method : classNode.methods) {
                if (UNLOAD_ENTITES.matches(method.name) && "(Ljava/util/Collection;)V".equals(method.desc)) {
                    coreLogger.info("Patching method: {} / {}", UNLOAD_ENTITES.mcp, method.name);
                    patchUnloadEntites(method);
                }
            }
            ClassWriter writer = new W_ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);
            classNode.accept(writer);
            return writer.toByteArray();
        } catch (Throwable t) {
            MCHCore.fail("net.minecraft.entity.EntityTrackerEntry", t);
            return clazz;


        }
    }


}
