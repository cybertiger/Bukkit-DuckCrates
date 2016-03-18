/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cyberiantiger.minecraft.duckcrates;

import java.util.Map;

/**
 *
 * @author antony
 */
public class ClaimStatus {
    private final Map<String, Integer> claimed;
    private final long lastDaily;

    public ClaimStatus(Map<String,Integer> claimed, long lastDaily) {
        this.claimed = claimed;
        this.lastDaily = lastDaily;
    }

    public Map<String, Integer> getClaimed() {
        return claimed;
    }

    public long getLastDaily() {
        return lastDaily;
    }
}
