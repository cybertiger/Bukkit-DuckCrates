/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cyberiantiger.minecraft.duckcrates;

import org.cyberiantiger.minecraft.duckcrates.config.CrateGroup;

/**
 *
 * @author antony
 */
public class CratesAvailable {
    private final CrateGroup group;
    private final int daily;
    private final int once;

    public CratesAvailable(CrateGroup group, int daily, int once) {
        this.group = group;
        this.daily = daily;
        this.once = once;
    }

    public CrateGroup getGroup() {
        return group;
    }

    public int getDaily() {
        return daily;
    }

    public int getOnce() {
        return once;
    }

}
