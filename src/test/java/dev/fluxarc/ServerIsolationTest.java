package dev.fluxarc;

import java.io.InputStream;
import java.util.List;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import static org.junit.Assert.*;

/** Bytecode dependency audit, not a substitute for a dedicated-server launch. */
public class ServerIsolationTest {
    @Test public void commonBytecodeHasNoUnstrippedClientDependencies() throws Exception {
        String[] names = {"FluxArc", "CommonProxy", "FluxConfig", "tile/TileMachine", "block/ArcBlock",
            "block/ArcItemBlock", "item/ArcItems", "gui/GuiHandler", "gui/ContainerMachine",
            "challenge/ArcSentinel", "challenge/ChallengeSession", "challenge/ChallengeHooks",
            "recipe/Recipe", "recipe/RecipeCatalog", "recipe/ItemIngredient", "recipe/Step", "structure/StructureSpec"};
        for (String name : names) {
            InputStream stream = getClass().getClassLoader().getResourceAsStream("dev/fluxarc/" + name + ".class");
            assertNotNull("Missing compiled production class " + name, stream);
            ClassNode cls = new ClassNode();
            try { new ClassReader(stream).accept(cls, 0); } finally { stream.close(); }
            for (Object entry : cls.fields) {
                FieldNode field = (FieldNode) entry;
                if (!clientOnly(field.visibleAnnotations) && !clientOnly(field.invisibleAnnotations))
                    rejectClient(name + "." + field.name, field.desc);
            }
            for (Object entry : cls.methods) {
                MethodNode method = (MethodNode) entry;
                if (clientOnly(method.visibleAnnotations) || clientOnly(method.invisibleAnnotations)) continue;
                String context = name + "." + method.name;
                rejectClient(context, method.desc);
                for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (insn instanceof MethodInsnNode) {
                        MethodInsnNode call = (MethodInsnNode)insn; rejectClient(context, call.owner); rejectClient(context, call.desc);
                    } else if (insn instanceof FieldInsnNode) {
                        FieldInsnNode access = (FieldInsnNode)insn; rejectClient(context, access.owner); rejectClient(context, access.desc);
                    } else if (insn instanceof TypeInsnNode) rejectClient(context, ((TypeInsnNode)insn).desc);
                }
            }
        }
    }
    private static boolean clientOnly(List annotations) {
        if (annotations == null) return false;
        for (Object entry : annotations) {
            AnnotationNode annotation = (AnnotationNode) entry;
            if (!"Lcpw/mods/fml/relauncher/SideOnly;".equals(annotation.desc) || annotation.values == null) continue;
            for (Object value : annotation.values) if (value instanceof String[]) {
                String[] pair = (String[])value;
                if (pair.length == 2 && "CLIENT".equals(pair[1])) return true;
            }
        }
        return false;
    }
    private static void rejectClient(String context, String descriptor) {
        assertFalse(context + " references client type " + descriptor,
            descriptor.contains("net/minecraft/client/") || descriptor.contains("org/lwjgl/") || descriptor.contains("dev/fluxarc/client/"));
    }
}
