package com.norwood.mcheli.core;


import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ListIterator;

import static com.norwood.mcheli.core.MCHCore.coreLogger;
import static org.objectweb.asm.Opcodes.*;

public class EntityRenderHooks implements IClassTransformer {
    private static final ObfSafeName RENDER_WORLD_PASS = new ObfSafeName("renderWorldPass","func_175068_a");

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (!transformedName.equals("net.minecraft.client.renderer.EntityRenderer")) {
            return basicClass;
        }
        coreLogger.info("Patching class {} / {}", transformedName, name);
        try {
            ClassNode classNode = new ClassNode();
            ClassReader classReader = new ClassReader(basicClass);
            classReader.accept(classNode, 0);

            for (MethodNode method : classNode.methods) {
                if (RENDER_WORLD_PASS.matches(method.name)
                        && method.desc.equals("(IFJ)V")) {
                    coreLogger.info("Patching method: {} / {}", RENDER_WORLD_PASS.mcp, method.name);
                    patchRender(method);
                }
            }

            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            classNode.accept(writer);
            return writer.toByteArray();
        } catch (Throwable t) {
            MCHCore.fail("net.minecraft.client.renderer.EntityRenderer", t);
            return basicClass;
        }
    }
    private void patchRender(MethodNode method) {
        ListIterator<AbstractInsnNode> iter = method.instructions.iterator();

        while (iter.hasNext()) {
            AbstractInsnNode insn = iter.next();
            if (insn instanceof MethodInsnNode) {
                //Looks for any renderglobal.renderEntities(entity, icamera, partialTicks) call
                MethodInsnNode renderCall = (MethodInsnNode) insn;
                if (renderCall.getOpcode() == Opcodes.INVOKEVIRTUAL &&
                        renderCall.owner.equals("net/minecraft/client/renderer/RenderGlobal") &&
                        renderCall.name.equals("renderEntities") &&
                        renderCall.desc.equals("(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/renderer/culling/ICamera;F)V")) {

                    method.instructions.insert(createRenderVehicleHook());
                }
            }
        }
    }
    private InsnList createRenderVehicleHook() {
        InsnList toInject = new InsnList();
        toInject.add(new FieldInsnNode(
                Opcodes.GETSTATIC,
                "com/norwood/mcheli/core/VehicleRenderHook",
                "INSTANCE",
                "Lcom/norwood/mcheli/core/VehicleRenderHook;")); //access the instance
        toInject.add(new VarInsnNode(ALOAD, 9)); // renderViewEntity
        toInject.add(new VarInsnNode(ALOAD, 8)); // camera
        toInject.add(new VarInsnNode(FLOAD, 2)); // partialTicks
        toInject.add(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                "com/norwood/mcheli/core/VehicleRenderHook",
                "renderVehicles",
                "(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/renderer/culling/ICamera;F)V",
                false)); //execute
        return toInject;
    }


}
