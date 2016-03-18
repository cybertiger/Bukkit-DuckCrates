/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cyberiantiger.minecraft.duckcrates;

/**
 *
 * @author antony
 */
public class ClaimDailyStatus {
    private final boolean success;
    private final long remainingTime;

    public ClaimDailyStatus(boolean success, long remainingTime) {
        this.success = success;
        this.remainingTime = remainingTime;
    }

    public boolean isSuccess() {
        return success;
    }

    public long getRemainingTime() {
        return remainingTime;
    }
}
