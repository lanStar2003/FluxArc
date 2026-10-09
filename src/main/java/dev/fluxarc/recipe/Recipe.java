package dev.fluxarc.recipe;

import net.minecraft.item.ItemStack;

public final class Recipe {
    public final String id, name, requiredCard, fluidName, outputFluid;
    public final int type, fluidAmount, outputFluidAmount;
    public final ItemIngredient[] inputs;
    public final ItemStack[] outputs;
    public final Step[] steps;
    public Recipe(String id, int type, String name, String requiredCard, ItemIngredient[] inputs,
            String fluidName, int fluidAmount, ItemStack[] outputs, String outputFluid,
            int outputFluidAmount, Step... steps) {
        if (id == null || inputs == null || outputs == null || steps == null || steps.length == 0)
            throw new IllegalArgumentException("Incomplete recipe");
        for (ItemStack output : outputs) if (output == null || output.stackSize < 1)
            throw new IllegalArgumentException("Missing recipe output: " + id);
        this.id = id; this.type = type; this.name = name; this.requiredCard = requiredCard;
        this.inputs = inputs.clone(); this.fluidName = fluidName; this.fluidAmount = fluidAmount;
        this.outputs = new ItemStack[outputs.length];
        for (int i = 0; i < outputs.length; i++) this.outputs[i] = outputs[i].copy();
        this.outputFluid = outputFluid; this.outputFluidAmount = outputFluidAmount; this.steps = steps.clone();
    }
    public int totalTicks() { int sum = 0; for (Step step : steps) sum += step.duration; return sum; }
    public long totalEu() { long sum = 0; for (Step step : steps) sum += (long)step.duration * step.euPerTick; return sum; }
    public Step stepAt(int tick) {
        for (Step step : steps) { if (tick < step.duration) return step; tick -= step.duration; }
        return steps[steps.length - 1];
    }
}
