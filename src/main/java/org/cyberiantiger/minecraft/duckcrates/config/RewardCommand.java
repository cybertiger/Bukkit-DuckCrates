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
public class RewardCommand extends Reward {
    private boolean asOP = true;
    private String command;

    public boolean isAsOP() {
        return asOP;
    }

    public String getCommand() {
        return command;
    }

    @Override
    public boolean validate(Main main, List<String> errors) {
        boolean result = super.validate(main, errors);
        if (command == null) {
            errors.add("Command missing command text");
            result = false;
        }
        return result;
    }

    @Override
    public void doReward(Main main, Player player, List<ItemStack> rewardItems) {
        String cmd = String.format(command, player.getName());
        if (isAsOP()) {
            main.getServer().dispatchCommand(main.getServer().getConsoleSender(), cmd);
        } else {
            // This is kinda useless, player could just run the command themselves.
            main.getServer().dispatchCommand(player, cmd);
        }
    }
}
