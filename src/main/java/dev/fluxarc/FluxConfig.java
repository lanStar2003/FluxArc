package dev.fluxarc;
import java.io.File;
import net.minecraftforge.common.config.Configuration;
public final class FluxConfig {
 public static int euPercent=100,timePercent=100;
 public static long capacity=4000000,maxVoltage=8192;
 public static boolean challenges=true;
 public static void load(File file){Configuration c=new Configuration(file);c.load();
 euPercent=c.getInt("euPercent","balance",100,10,1000,"EU/t multiplier; never zero");
 timePercent=c.getInt("timePercent","balance",100,10,1000,"Processing duration multiplier");
 capacity=c.getInt("bufferEU","power",4000000,32768,100000000,"Internal EU buffer");
 maxVoltage=c.getInt("maxVoltage","power",8192,32,131072,"Higher voltage packets are rejected, never converted or exploded");
 challenges=c.getBoolean("enabled","challenges",true,"Enable bounded non-destructive combat tasks");if(c.hasChanged())c.save();}
}
