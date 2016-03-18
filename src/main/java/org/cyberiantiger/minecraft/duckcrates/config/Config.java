/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cyberiantiger.minecraft.duckcrates.config;

import java.util.List;
import java.util.Map;
import org.cyberiantiger.minecraft.duckcrates.Main;

/**
 *
 * @author antony
 */
public class Config {
    
    private String dbUrl;
    private String dbUser;
    private String dbPassword;
    private Map<String, CrateGroup> groups;

    public String getDbUrl() {
        return dbUrl;
    }

    public String getDbUser() {
        return dbUser;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public Map<String, CrateGroup> getGroups() {
        return groups;
    }

    public boolean validate(Main main, List<String> errors) {
        boolean result = true;
        for (CrateGroup group : groups.values()) {
            if (!group.validate(main, errors)) result = false;
        }
        return result;
    }
}