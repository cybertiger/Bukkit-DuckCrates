/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cyberiantiger.minecraft.duckcrates.config;

import java.util.List;
import org.cyberiantiger.minecraft.duckcrates.Main;

/**
 *
 * @author antony
 */
public class PermissionGrant {
    private String permission;
    private int once = 0;
    private int daily = 0;

    public String getPermission() {
        return permission;
    }

    public int getOnce() {
        return once;
    }

    public int getDaily() {
        return daily;
    }

    public boolean validate(Main main, List<String> errors) {
        boolean result = true;
        if (permission == null) {
            errors.add("Missing permission in rewardCount section");
            result = false;
        }
        return result;
    }
}
