package com.example.dungeonhud;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;

/** Ищет в инвентаре лучшее лечение: зелье лечения > зачарованное золотое яблоко > золотое яблоко. */
public final class HealFinder {
    private HealFinder() {}

    public record Result(ItemStack stack, int count, int hotbarSlot) {}

    private static int priority(ItemStack s) {
        Item it = s.getItem();
        if (it == Items.POTION) {
            Potion p = PotionUtils.getPotion(s);
            return (p == Potions.HEALING || p == Potions.STRONG_HEALING) ? 3 : -1;
        }
        if (it == Items.ENCHANTED_GOLDEN_APPLE) return 2;
        if (it == Items.GOLDEN_APPLE) return 1;
        return -1;
    }

    public static Result find(Inventory inv) {
        int best = -1;
        ItemStack bestStack = ItemStack.EMPTY;
        int slot = -1;
        for (int i = 0; i < inv.items.size(); i++) {
            ItemStack s = inv.items.get(i);
            if (s.isEmpty()) continue;
            int pr = priority(s);
            if (pr < 0) continue;
            if (pr > best) {
                best = pr;
                bestStack = s;
                slot = i < 9 ? i : -1;
            } else if (pr == best && slot < 0 && i < 9) {
                slot = i;
            }
        }
        if (best < 0) return null;
        int total = 0;
        for (ItemStack s : inv.items) {
            if (!s.isEmpty() && priority(s) == best) total += s.getCount();
        }
        return new Result(bestStack, total, slot);
    }
}
