/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cyberiantiger.minecraft.duckcrates.config;

import java.io.IOException;
import java.io.StringReader;
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
public class RewardSpawn extends Reward {
    private double xOffset = 0d;
    private double yOffset = 0d;
    private double zOffset = 0d;
    private String entity;
    private transient CompoundTag parsedEntity;

    public double getxOffset() {
        return xOffset;
    }

    public double getyOffset() {
        return yOffset;
    }

    public double getzOffset() {
        return zOffset;
    }

    public String getEntity() {
        return entity;
    }

    public CompoundTag getParsedEntity() {
        return parsedEntity;
    }

    @Override
    public boolean validate(Main main, List<String> errors) {
        boolean result = super.validate(main, errors);
        if (entity == null) {
            errors.add("entity nbt not set");
            result = false;
        } else {
            try {
                TagTuple parse = new MojangsonParser(new StringReader(entity)).parse();
                if (parse.getValue().getType() != TagType.COMPOUND) {
                    errors.add("Tag must be a compound: " + entity);
                    result = false;
                } else {
                    parsedEntity = (CompoundTag) parse.getValue();
                }
            } catch (IOException ex) {
                errors.add("Invalid NBT: " + entity);
                result = false;
            }
        }
        return result;
    }

    @Override
    public void doReward(Main main, Player player, List<ItemStack> rewardItems) {
        // TODO: Not Yet Implemented in NBTTools
    }
}
