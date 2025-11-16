package com.norwood.mcheli.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;

import static com.norwood.mcheli.core.MCHCore.coreLogger;
import static org.objectweb.asm.Opcodes.*;

public class EntityRenderHooks implements IClassTransformer {

    private static final ObfSafeName RENDER_WORLD_PASS = new ObfSafeName("renderWorldPass", "func_175068_a");
    private static final ObfSafeName RENDER_ENTITES = new ObfSafeName("renderEntities", "func_180446_a");

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
                if (RENDER_WORLD_PASS.matches(method.name) && method.desc.equals("(IFJ)V")) {
                    coreLogger.info("Patching method: {} / {}", RENDER_WORLD_PASS.mcp, method.name);
                    patchRender(method);
                }
            }

            ClassWriter writer = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            classNode.accept(writer);
            return writer.toByteArray();
        } catch (Throwable t) {
            MCHCore.fail("net.minecraft.client.renderer.EntityRenderer", t);
            return basicClass;
        }
    }

    private static void patchRender(MethodNode method) {
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn.getOpcode() == INVOKEVIRTUAL && insn instanceof MethodInsnNode renderCall) {
                // Looks for any renderglobal.renderEntities(entity, icamera, partialTicks) methodNode
                if (renderCall.getOpcode() == INVOKEVIRTUAL &&
                        renderCall.owner.equals("net/minecraft/client/renderer/RenderGlobal") &&
                       RENDER_ENTITES.matches(renderCall.name) &&
                        renderCall.desc.equals(
                                "(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/renderer/culling/ICamera;F)V")) {
                    List<AbstractInsnNode> stackArgs = new ArrayList<>();
                    AbstractInsnNode cursor = renderCall.getPrevious();

                    while (cursor != null && stackArgs.size() < 3) {
                        int op = cursor.getOpcode();
                        if (op >= ILOAD && op <= ALOAD || op == LDC || (op >= ICONST_M1 && op <= DCONST_1)) {
                            stackArgs.add(0, cursor);
                        }
                        cursor = cursor.getPrevious();
                    }
                    int entityVar = ((VarInsnNode) stackArgs.get(0)).var;
                    int cameraVar = ((VarInsnNode) stackArgs.get(1)).var;
                    int partialVar = ((VarInsnNode) stackArgs.get(2)).var;
                    InsnList toInject = createRenderVehicleHook(entityVar,cameraVar,partialVar);
                    method.instructions.insert(renderCall, toInject);
                }
            }
        }
    }

    private static InsnList createRenderVehicleHook(int entityVar, int camVar, int partialVar ) {
        InsnList toInject = new InsnList();
        toInject.add(new FieldInsnNode(
                GETSTATIC,
                "com/norwood/mcheli/core/VehicleRenderHook",
                "INSTANCE",
                "Lcom/norwood/mcheli/core/VehicleRenderHook;")); // access the instance
        toInject.add(new VarInsnNode(ALOAD, entityVar)); // renderViewEntity
        toInject.add(new VarInsnNode(ALOAD, camVar)); // camera
        toInject.add(new VarInsnNode(FLOAD, partialVar)); // partialTicks
        toInject.add(new MethodInsnNode(
                INVOKEVIRTUAL,
                "com/norwood/mcheli/core/VehicleRenderHook",
                "renderVehicles",
                "(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/renderer/culling/ICamera;F)V",
                false)); // execute
        return toInject;
    }
}
