package dev.fluxarc;

import cpw.mods.fml.common.*;
import cpw.mods.fml.common.discovery.ContainerType;
import cpw.mods.fml.common.discovery.ModCandidate;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.jar.JarFile;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import static org.junit.Assert.*;

/** Exercises Forge's actual dependency gate; does not initialize a full GTNH game. */
public class ForgeDependencyLoadingTest {
    private File runtimeJar() {
        File jar = new File(System.getProperty("fluxarc.gtRuntimeJar"));
        assertTrue("Run tools/bootstrap-dependencies.py to stage the official production GT jar", jar.isFile());
        return jar;
    }

    private Map<String,Object> annotation(InputStream input) throws Exception {
        assertNotNull(input);
        ClassNode node = new ClassNode();
        try { new ClassReader(input).accept(node, ClassReader.SKIP_CODE); } finally { input.close(); }
        for (Object value : node.visibleAnnotations) {
            AnnotationNode a = (AnnotationNode)value;
            if (!a.desc.equals("Lcpw/mods/fml/common/Mod;")) continue;
            Map<String,Object> result = new HashMap<String,Object>();
            for (int i=0; i<a.values.size(); i+=2) result.put((String)a.values.get(i), a.values.get(i+1));
            return result;
        }
        throw new AssertionError("Missing @Mod annotation");
    }

    private FMLModContainer container(String className, File source, Map<String,Object> descriptor,
                                       MetadataCollection metadata) {
        FMLModContainer result = new FMLModContainer(className,
            new ModCandidate(source, source, ContainerType.JAR), descriptor);
        result.bindMetadata(metadata);
        return result;
    }

    private FMLModContainer gt(String className) throws Exception {
        try (JarFile jar = new JarFile(runtimeJar())) {
            Map<String,Object> descriptor = annotation(jar.getInputStream(jar.getJarEntry(className.replace('.', '/') + ".class")));
            MetadataCollection metadata = MetadataCollection.from(jar.getInputStream(jar.getJarEntry("mcmod.info")), "official GT runtime");
            return container(className, runtimeJar(), descriptor, metadata);
        }
    }

    private FMLModContainer flux(String dependencyOverride) throws Exception {
        ClassLoader cl = getClass().getClassLoader();
        Map<String,Object> descriptor = annotation(cl.getResourceAsStream("dev/fluxarc/FluxArc.class"));
        if (dependencyOverride != null) descriptor.put("dependencies", dependencyOverride);
        return container("dev.fluxarc.FluxArc", new File("FluxArc-test.jar"), descriptor,
            MetadataCollection.from(cl.getResourceAsStream("mcmod.info"), "FluxArc mcmod.info"));
    }

    private DummyModContainer external(String id, String version) {
        ModMetadata md = new ModMetadata(); md.modId=id; md.name=id; md.version=version;
        return new DummyModContainer(md) {
            @Override public File getSource() { return new File("external-metadata-fixture.jar"); }
        };
    }

    private List<ModContainer> available(FMLModContainer flux) throws Exception {
        FMLModContainer legacy = gt("gregtech.GTMod"), modern = gt("gregtech.GTNHMod");
        // External GT lifecycle is intentionally outside this dependency-gate fixture.
        // IDs and effective versions come from FML binding of the official production jar.
        return new ArrayList<ModContainer>(Arrays.<ModContainer>asList(flux,
            external(legacy.getModId(), legacy.getVersion()), external(modern.getModId(), modern.getVersion()),
            external("Forge", net.minecraftforge.common.ForgeVersion.getVersion())));
    }

    private Field field(String name) throws Exception {
        Field f = Loader.class.getDeclaredField(name); f.setAccessible(true); return f;
    }

    private void runForgeGate(List<ModContainer> mods) throws Exception {
        Loader loader = Loader.instance();
        Field list=field("mods"), names=field("namedMods"), controller=field("modController");
        Object oldList=list.get(loader), oldNames=names.get(loader), oldController=controller.get(loader);
        try {
            ModAPIManager.INSTANCE.registerDataTableAndParseAPI(new cpw.mods.fml.common.discovery.ASMDataTable());
            LoadController state = new LoadController(loader);
            state.getActiveModList().addAll(mods);
            Map<String,ModContainer> index = new HashMap<String,ModContainer>();
            for (ModContainer mod : mods) index.put(mod.getModId(), mod);
            list.set(loader, new ArrayList<ModContainer>(mods)); names.set(loader,index); controller.set(loader,state);
            Method gate = Loader.class.getDeclaredMethod("sortModList"); gate.setAccessible(true);
            try { gate.invoke(loader); }
            catch (InvocationTargetException error) {
                Throwable cause=error.getCause();
                if (cause instanceof Exception) throw (Exception)cause;
                throw error;
            }
        } finally { list.set(loader,oldList); names.set(loader,oldNames); controller.set(loader,oldController); }
    }

    private void rejected(List<ModContainer> mods) throws Exception {
        try { runForgeGate(mods); fail("Forge must reject this incompatible or missing dependency"); }
        catch (MissingModsException expected) { /* actual Forge startup exception */ }
    }

    @Test public void officialProductionJarExposesTwoDifferentVersionIdentities() throws Exception {
        assertEquals("gregtech",gt("gregtech.GTMod").getModId());
        assertEquals("MC1710",gt("gregtech.GTMod").getVersion());
        assertEquals("5.09.51.482",gt("gregtech.GTMod").getMetadata().version);
        assertEquals("gregtech_nh",gt("gregtech.GTNHMod").getModId());
        assertEquals("5.09.51.482",gt("gregtech.GTNHMod").getVersion());
    }

    @Test public void oldReleaseReproducesForgeMissingModsFailure() throws Exception {
        rejected(available(flux("required-after:gregtech@[5.09.51.482];required-after:Forge@[10.13.4.1614,)")));
    }

    @Test public void currentProductionDescriptorPassesRealForgeGate() throws Exception {
        FMLModContainer mod = flux(null);
        assertEquals("0.1.1",mod.getVersion());
        assertEquals(mod.getVersion(),mod.getMetadata().version);
        assertEquals("10.13.4.1614",net.minecraftforge.common.ForgeVersion.getVersion());
        runForgeGate(available(mod));
    }

    @Test public void differentGtBuildIsStillRejected() throws Exception {
        for (String version : Arrays.asList("5.09.51.481","5.09.51.483")) {
            List<ModContainer> mods=available(flux(null)); mods.set(2,external("gregtech_nh",version)); rejected(mods);
        }
    }

    @Test public void missingRequiredIdsAndOldForgeAreStillRejected() throws Exception {
        for (int index : new int[]{1,2,3}) {
            List<ModContainer> mods=available(flux(null)); mods.remove(index); rejected(mods);
        }
        List<ModContainer> mods=available(flux(null)); mods.set(3,external("Forge","10.13.4.1558")); rejected(mods);
    }
}
