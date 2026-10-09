import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

/** Runs real Forge serializers in an isolated LaunchWrapper JVM, never a game instance. */
public final class ForgeNbtTestLauncher {
    public static void main(String[] args) throws Exception {
        File home = new File(args[0]).getCanonicalFile(); home.mkdirs();
        String[] paths = System.getProperty("java.class.path").split(File.pathSeparator);
        URL[] urls = new URL[paths.length];
        for (int i = 0; i < paths.length; i++) urls[i] = new File(paths[i]).toURI().toURL();
        Launch.minecraftHome = home; Launch.assetsDir = new File(home, "assets");
        Launch.blackboard = new HashMap<String,Object>();
        Launch.classLoader = new LaunchClassLoader(urls);
        Thread.currentThread().setContextClassLoader(Launch.classLoader);
        Class<?> log = Class.forName("cpw.mods.fml.relauncher.FMLRelaunchLog", true, Launch.classLoader);
        java.lang.reflect.Field side = log.getDeclaredField("side"); side.setAccessible(true);
        Class<?> sideType = Class.forName("cpw.mods.fml.relauncher.Side", true, Launch.classLoader);
        side.set(null, sideType.getField("SERVER").get(null));
        Class<?> loader = Class.forName("cpw.mods.fml.common.Loader", true, Launch.classLoader);
        loader.getMethod("injectData", Object[].class).invoke(null, (Object)new Object[]{
            "7", "10", "99", "99", "1.7.10", "9.05", home, new ArrayList<String>()});
        Class.forName("net.minecraft.init.Bootstrap", true, Launch.classLoader)
            .getMethod("func_151354_b").invoke(null);
        Class.forName("org.junit.runner.JUnitCore", true, Launch.classLoader)
            .getMethod("main", String[].class).invoke(null, (Object)new String[]{"dev.fluxarc.TilePersistenceTest", "dev.fluxarc.ForgeDependencyLoadingTest"});
    }
}
