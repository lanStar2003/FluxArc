package dev.fluxarc.recipe;

import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

public final class ItemIngredient {
    public final String ore;
    public final ItemStack exact;
    public final int amount;
    public final boolean rejectNbt;
    private ItemIngredient(String ore, ItemStack exact, int amount, boolean rejectNbt) {
        if (amount < 1 || (ore == null && exact == null)) throw new IllegalArgumentException("Invalid ingredient");
        this.ore = ore;
        this.exact = exact == null ? null : exact.copy();
        this.amount = amount;
        this.rejectNbt = rejectNbt;
    }
    public static ItemIngredient ore(String name, int amount) { return new ItemIngredient(name, null, amount, false); }
    public static ItemIngredient exact(ItemStack stack, int amount, boolean rejectNbt) {
        return new ItemIngredient(null, stack, amount, rejectNbt);
    }
    /** Count is deliberately checked by the transaction planner, not by this predicate. */
    public boolean matches(ItemStack stack) {
        if (stack == null || stack.stackSize <= 0 || (rejectNbt && stack.hasTagCompound())) return false;
        if (exact != null) return stack.getItem() == exact.getItem()
            && stack.getItemDamage() == exact.getItemDamage()
            && (rejectNbt || ItemStack.areItemStackTagsEqual(exact, stack));
        for (int id : OreDictionary.getOreIDs(stack)) if (ore.equals(OreDictionary.getOreName(id))) return true;
        return false;
    }
    public ItemStack displayStack() {
        ItemStack source = exact;
        if (source == null) {
            List<ItemStack> options = OreDictionary.getOres(ore);
            if (options.isEmpty()) return null;
            source = options.get(0);
        }
        ItemStack result = source.copy();
        result.stackSize = amount;
        return result;
    }
}
