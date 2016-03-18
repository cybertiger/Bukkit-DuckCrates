/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cyberiantiger.minecraft.duckcrates.config;

import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.cyberiantiger.minecraft.duckcrates.Main;

/**
 *
 * @author antony
 */
public abstract class Reward {
    private double chance = 1d;
    private String message = null;

    public double getChance() {
        return chance;
    }

    public String getMessage() {
        return message;
    }

    public boolean validate(Main main, List<String> errors) {
        boolean result = true;
        if (chance < 0d) {
            errors.add("Chance was less than zero");
            result = false;
        }
        if (chance > 1d) {
            errors.add("Chance was greater than 1");
            result = false;
        }
        return result;
    }

    public abstract void doReward(Main main, Player player, List<ItemStack> rewardItems);
}
