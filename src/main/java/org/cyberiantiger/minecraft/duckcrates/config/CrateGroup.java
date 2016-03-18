/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cyberiantiger.minecraft.duckcrates.config;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.cyberiantiger.minecraft.duckcrates.Main;
import org.cyberiantiger.minecraft.nbt.CompoundTag;
import org.cyberiantiger.minecraft.nbt.MojangsonParser;
import org.cyberiantiger.minecraft.nbt.TagTuple;
import org.cyberiantiger.minecraft.nbt.TagType;

/**
 *
 * @author antony
 */
public class CrateGroup {
    private String name;
    private short damage = 0;
    private String tag = null;
    private String message = null;
    private transient CompoundTag parsedTag = null;
    private List<PermissionGrant> rewardCount = Collections.emptyList();
    private List<RewardItem> items = Collections.emptyList();
    private List<RewardCommand> commands = Collections.emptyList();
    private List<RewardSpawn> spawns = Collections.emptyList();
    private transient List<Reward> allRewards;

    public String getName() {
        return name;
    }

    public short getDamage() {
        return damage;
    }

    public String getTag() {
        return tag;
    }

    public String getMessage() {
        return message;
    }

    public List<PermissionGrant> getRewardCount() {
        return rewardCount;
    }

    public List<RewardItem> getItems() {
        return items;
    }

    public List<RewardCommand> getCommands() {
        return commands;
    }

    public List<RewardSpawn> getSpawns() {
        return spawns;
    }

    public CompoundTag getParsedTag() {
        return parsedTag;
    }

    public List<Reward> getAllRewards() {
        return allRewards;
    }

    public boolean validate(Main main, List<String> errors) {
        boolean result = true;
        if (rewardCount == null) {
            rewardCount = Collections.emptyList();
        }
        if (items == null) {
            items = Collections.emptyList();
        }
        if (commands == null) {
            commands = Collections.emptyList();
        }
        if (spawns == null) {
            spawns = Collections.emptyList();
        }
        allRewards = new ArrayList<>();
        allRewards.addAll(items);
        allRewards.addAll(commands);
        allRewards.addAll(spawns);
        if (tag != null) {
            try {
                TagTuple parse = new MojangsonParser(new StringReader(tag)).parse();
                if (parse.getValue().getType() != TagType.COMPOUND) {
                    errors.add("Tag must be a compound: " + tag);
                    result = false;
                } else {
                    parsedTag = (CompoundTag) parse.getValue();
                }
            } catch (IOException ex) {
                errors.add("Invalid NBT: " + tag);
                result = false;
            }
        }
        for(PermissionGrant g : rewardCount) {
            if (!g.validate(main, errors)) result = false;
        }
        for (RewardItem g : items) {
            if (!g.validate(main, errors)) result = false;
        }
        for (RewardCommand g : commands) {
            if (!g.validate(main, errors)) result = false;
        }
        for (RewardSpawn g : spawns) {
            if (!g.validate(main, errors)) result = false;
        }
        return result;
    }
} 