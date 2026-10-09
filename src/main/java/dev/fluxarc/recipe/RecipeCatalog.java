package dev.fluxarc.recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import cpw.mods.fml.common.registry.GameRegistry;
import dev.fluxarc.FluxArc;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

/** Explicit whitelist; never infers a reverse recipe from the crafting registry. */
public final class RecipeCatalog {
    private static final Map<String, Recipe> ALL = new LinkedHashMap<String, Recipe>();
    private static final List<String> SKIPPED = new ArrayList<String>();
    private static boolean initialized;
    private RecipeCatalog() {}
    public static Recipe find(String id) { return ALL.get(id); }
    public static List<Recipe> forMachine(int type) {
        List<Recipe> list = new ArrayList<Recipe>();
        for (Recipe r : ALL.values()) if (r.type == type) list.add(r);
        return Collections.unmodifiableList(list);
    }
    public static List<String> skippedRecipes() { return Collections.unmodifiableList(SKIPPED); }
    private static ItemStack local(String id, int amount) { return FluxArc.stack(id, amount); }
    private static ItemStack oreStack(String name, int amount) {
        for (ItemStack s : OreDictionary.getOres(name)) {
            if (s != null && s.getItemDamage() != OreDictionary.WILDCARD_VALUE && !s.hasTagCompound()) {
                ItemStack result = s.copy(); result.stackSize = amount; return result;
            }
        }
        return null;
    }
    private static ItemIngredient o(String name, int amount) { return ItemIngredient.ore(name, amount); }
    private static ItemIngredient l(String id, int amount) { return ItemIngredient.exact(local(id, 1), amount, true); }
    private static Step s(String name, int eu, int ticks) { return new Step(name, eu, ticks); }
    private static void add(String id, int type, String name, String card, ItemIngredient[] in,
            String fluid, int amount, ItemStack[] out, String outFluid, int outAmount, Step... steps) {
        for (ItemIngredient i : in) if (i == null || i.displayStack() == null) { SKIPPED.add(id); return; }
        for (ItemStack i : out) if (i == null) { SKIPPED.add(id); return; }
        if ((fluid != null && FluidRegistry.getFluid(fluid) == null)
            || (outFluid != null && FluidRegistry.getFluid(outFluid) == null)) { SKIPPED.add(id); return; }
        if (ALL.containsKey(id)) throw new IllegalStateException("Duplicate recipe " + id);
        ALL.put(id, new Recipe(id, type, name, card, in, fluid, amount, out, outFluid, outAmount, steps));
    }
    public static synchronized void init() {
        if (initialized) return;
        initialized = true;
        crafting();
        forging("iron_gear", "铁齿轮", "ingotIron", 4, "gearGtIron", 1, 32);
        forging("steel_gear", "钢齿轮", "ingotSteel", 4, "gearGtSteel", 1, 128);
        forging("steel_plate", "钢板", "ingotSteel", 1, "plateSteel", 1, 64);
        forging("iron_plate", "铁板", "ingotIron", 1, "plateIron", 1, 32);
        add("bronze_alloy", 0, "铜锡合金锻造", null,
            new ItemIngredient[]{o("ingotCopper",3),o("ingotTin",1)}, "water",250,
            new ItemStack[]{oreStack("ingotBronze",4)},null,0,
            s("发生器展开",32,40),s("金属熔融悬浮",128,160),s("液滴汇合",128,100),
            s("约束环压制",128,80),s("冷却出料",32,100));
        add("swarm_relay",1,"蜂群继电组件","blueprint_basic",
            new ItemIngredient[]{o("plateIron",2),o("wireGt01Copper",2),o("dustRedstone",2)},null,0,
            new ItemStack[]{local("relay",1)},null,0,s("机械臂展开",16,40),s("线框铺设",32,160),s("层叠装配",32,240));
        add("swarm_precision",1,"精密装配核心","blueprint_precision",
            new ItemIngredient[]{l("relay",2),o("plateSteel",4),o("circuitBasic",2)},"water",250,
            new ItemStack[]{local("precision_core",1)},null,0,s("机械臂定位",64,80),s("线框校准",128,200),s("精密层叠",128,320));
        add("swarm_quantum",1,"量子约束核心","blueprint_quantum",
            new ItemIngredient[]{l("precision_core",2),l("deep_sample",1),o("plateTitanium",4),o("circuitAdvanced",2)},"water",500,
            new ItemStack[]{local("quantum_core",1)},null,0,s("蜂群展开",128,80),s("多层线框",512,240),s("约束核心封装",512,400));
        add("fold_steel",2,"渗碳—锻炼—水冷","process_steel",
            new ItemIngredient[]{o("ingotIron",1),o("dustCoal",1)},"water",100,
            new ItemStack[]{oreStack("ingotSteel",1)},"steam",16000,
            s("渗碳预热",128,200),s("折叠锻炼",128,800),s("受控水冷",32,100));
        add("fold_copper",2,"粉末混合—合金化—浇铸","process_copper",
            new ItemIngredient[]{o("dustCopper",3),o("dustTin",1)},"water",100,
            new ItemStack[]{oreStack("ingotBronze",4)},"steam",16000,
            s("粉末混合",32,120),s("合金熔炼",64,300),s("成型水冷",32,120));
        add("fold_ore",2,"碎矿洗选—还原—浇铸","process_recycle",
            new ItemIngredient[]{o("crushedIron",1),o("dustCoal",1)},"water",100,
            new ItemStack[]{oreStack("ingotIron",1),oreStack("dustStone",1)},"steam",16000,
            s("碎矿洗选",32,160),s("碳热还原",64,360),s("分离浇铸",32,160));
        recycle("recycle_plate_normal","普通钢板回收",null,"plateSteel", "nuggetSteel",6,32);
        recycle("recycle_plate_precision","精密钢板回收","blueprint_precision","plateSteel","nuggetSteel",8,128);
        recycle("recycle_gear_normal","普通钢齿轮回收",null,"gearGtSteel","ingotSteel",2,32);
        recycle("recycle_gear_precision","精密钢齿轮回收","blueprint_precision","gearGtSteel","ingotSteel",3,128);
        add("probe_orbit",4,"近轨道电磁测绘","blueprint_basic",
            new ItemIngredient[]{l("probe",1),l("sensor_payload",1)},null,0,
            new ItemStack[]{local("research_data",2)},null,0,
            s("轨道充能",128,200),s("探针发射",128,80),s("近轨道测绘",32,1800),s("数据回传",32,120));
        add("probe_samples",4,"外层空间样本回收","blueprint_precision",
            new ItemIngredient[]{l("probe",1),l("sample_payload",1),l("research_data",1)},null,0,
            new ItemStack[]{local("orbital_sample",2)},null,0,
            s("分段轨道充能",128,240),s("采样探针发射",128,80),s("样本采集",64,3600),s("回收舱捕获",64,200));
        add("probe_deep",4,"深空约束研究","blueprint_quantum",
            new ItemIngredient[]{l("probe",1),l("deep_payload",1),l("orbital_sample",1)},null,0,
            new ItemStack[]{local("deep_sample",2)},null,0,
            s("深空轨道充能",512,320),s("深空探针发射",512,80),s("远域研究",128,6000),s("研究舱回收",128,240));
        add("sentinel_trial",5,"战术任务：双哨兵","blueprint_basic",
            new ItemIngredient[]{l("field_key",1)},null,0,new ItemStack[]{local("tactical_data",1)},null,0,
            s("双哨兵挑战 / 60秒",32,1200));
        add("guardian_trial",5,"战术任务：四重防线","blueprint_precision",
            new ItemIngredient[]{l("field_key",2),l("research_data",1)},null,0,
            new ItemStack[]{local("tactical_data",3)},null,0,s("四重防线 / 90秒",128,1800));
    }
    private static void forging(String id,String name,String input,int count,String output,int outCount,int eu) {
        add(id,0,name,null,new ItemIngredient[]{o(input,count)},"water",250,
            new ItemStack[]{oreStack(output,outCount)},null,0,s("发生器展开",16,40),s("金属熔融悬浮",eu,160),
            s("液滴汇合",eu,80),s("约束环压制",eu,80),s("冷却出料",16,120));
    }
    private static void recycle(String id,String name,String card,String input,String output,int amount,int eu) {
        ItemStack accepted = oreStack(input,1);
        if (accepted == null) { SKIPPED.add(id); return; }
        add(id,3,name,card,new ItemIngredient[]{ItemIngredient.exact(accepted,1,true)},null,0,
            new ItemStack[]{oreStack(output,amount)},null,0,s("材质扫描",eu,100),s("白名单分离",eu,180),s("回收封装",eu,80));
    }
    private static void shaped(String output,int count,Object... recipe) {
        GameRegistry.addRecipe(new ShapedOreRecipe(local(output,count),recipe));
    }
    private static void shapeless(String output,int count,Object... recipe) {
        GameRegistry.addRecipe(new ShapelessOreRecipe(local(output,count),recipe));
    }
    private static void crafting() {
        // All bootstrap blocks use GT materials directly; no controller requires its own output.
        Object[] selectors = {Blocks.piston, Items.quartz, Items.ender_pearl, Items.flint,
            Items.compass, Items.diamond};
        for (int machine = 0; machine < 6; machine++) {
            GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(FluxArc.controller,1,machine),
                "SCS","GEG","SPS",'S',"plateSteel",'C',machine < 2 ? "circuitBasic" : "circuitAdvanced",
                'G',local("gravity_coil",1),'E',"gearGtSteel",'P',selectors[machine]));
        }
        GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(FluxArc.component,4,0),
            "S S"," S ","S S",'S',"stickSteel"));
        GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(FluxArc.component,4,1),
            "CCC","CFC","CCC",'C',local("ceramic_plate",1),'F',new ItemStack(FluxArc.component,1,0)));
        GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(FluxArc.component,2,2),
            " C ","GFG"," C ",'C',local("ceramic_plate",1),'G',local("gravity_coil",1),'F',new ItemStack(FluxArc.component,1,0)));
        GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(FluxArc.component,4,3),
            "S S","SCS","S S",'S',"stickSteel",'C',local("ceramic_plate",1)));
        GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(FluxArc.component,4,4),
            "SSS","GCG","SSS",'S',"plateSteel",'G',local("gravity_coil",1),'C',local("ceramic_plate",1)));
        shaped("ceramic_plate",4,"CCC","CRC","CCC",'C',Items.clay_ball,'R',"dustRedstone");
        shaped("gravity_coil",1,"WCW","CRC","WCW",'W',"wireGt01Copper",'C',local("ceramic_plate",1),'R',"ingotIron");
        shaped("blueprint_basic",1,"PGP","PRP","PPP",'P',Items.paper,'G',"ingotGold",'R',"dustRedstone");
        shapeless("blueprint_precision",1,local("blueprint_basic",1),local("research_data",1),local("research_data",1),local("relay",1),Items.paper);
        shapeless("blueprint_quantum",1,local("blueprint_precision",1),local("orbital_sample",1),local("precision_core",1),Items.paper);
        shapeless("process_steel",1,Items.paper,"ingotIron","dustCoal","dustRedstone");
        shapeless("process_copper",1,Items.paper,"dustCopper","dustTin","dustRedstone");
        shapeless("process_recycle",1,Items.paper,"crushedIron","dustRedstone");
        shaped("probe",1," P ","CRC"," P ",'P',"plateSteel",'C',local("ceramic_plate",1),'R',local("relay",1));
        shaped("sensor_payload",1," G ","WRW"," G ",'G',Blocks.glass,'W',"wireGt01Copper",'R',"dustRedstone");
        shapeless("sample_payload",1,local("sensor_payload",1),"plateSteel",Items.bucket,local("relay",1));
        shapeless("deep_payload",1,local("sample_payload",1),local("precision_core",1),"plateTitanium");
        shapeless("field_key",1,local("ceramic_plate",1),"dustRedstone","ingotIron");
        // Tactical data has an explicit use without becoming a resource multiplier.
        shapeless("research_data",1,local("tactical_data",1),local("tactical_data",1),Items.paper);
        GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(FluxArc.component,16,4),
            "FFF","FCF","FFF",'F',new ItemStack(FluxArc.component,1,0),'C',local("precision_core",1)));
        GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(FluxArc.component,32,2),
            "FFF","FCF","FFF",'F',new ItemStack(FluxArc.component,1,0),'C',local("quantum_core",1)));
    }
}
