/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cyberiantiger.minecraft.duckcrates.config;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.cyberiantiger.minecraft.duckcrates.Main;
import org.cyberiantiger.minecraft.nbt.CompoundTag;
import org.cyberiantiger.minecraft.nbt.MojangsonParser;
import org.cyberiantiger.minecraft.nbt.TagTuple;
import org.cyberiantiger.minecraft.nbt.TagType;

/**
 *
 * @author antony
 */
public class RewardItem extends Reward {
    private String name;
    private short damage = 0;
    private int count = 1;
    private String tag;
    private transient List<ItemStack> reward;

    public String getName() {
        return name;
    }

    public short getDamage() {
        return damage;
    }

    public int getCount() {
        return count;
    }

    public String getTag() {
        return tag;
    }

    public List<ItemStack> getReward() {
        return reward;
    }

    @Override
    public boolean validate(Main main, List<String> errors) {
        boolean result = super.validate(main, errors);
        if (name == null) {
            errors.add("Reward item missing item name");
            result = false;
        }
        CompoundTag parsedTag = null;
        if (tag != null) {
            try {
                TagTuple parse = new MojangsonParser(new StringReader(tag)).parse();
                if (parse.getValue().getType() != TagType.COMPOUND) {
                    errors.add("Tag must be a compound: " + tag);
                } else {
                    parsedTag = (CompoundTag) parse.getValue();
                }
            } catch (IOException ex) {
                errors.add("Invalid NBT: " + tag);
                result = false;
            }
        }
        reward = new ArrayList<>();
        try {
            CompoundTag item = new CompoundTag();
            item.setString("id", name);
            item.setByte("Count", (byte)1);
            item.setShort("Damage", damage);
            if (parsedTag != null) {
                item.setCompound("tag", parsedTag);
            }
            ItemStack stack = main.getTools().createItemStack(item);
            int full = count / stack.getMaxStackSize();
            stack.setAmount(stack.getMaxStackSize());
            for (int i = 0; i < full; i++) {
                reward.add(stack.clone());
            }
            int remainder = count % stack.getMaxStackSize();
            if (remainder != 0) {
                stack.setAmount(remainder);
                reward.add(stack);
            }
        } catch (Exception e) {
            result = false;
            errors.add(e.getClass().getName() + ": " + e.getMessage() + " in " + toString());
        }
        return result;
    }

    @Override
    public void doReward(Main main, Player player, List<ItemStack> rewardItems) {
        rewardItems.addAll(reward);
    }

    @Override
    public String toString() {
        return "RewardItem{" + "name=" + name + ", damage=" + damage + ", count=" + count + ", tag=" + tag + '}';
    }
}