/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cyberiantiger.minecraft.duckcrates;

import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.cyberiantiger.minecraft.duckcrates.config.Config;
import org.cyberiantiger.minecraft.duckcrates.config.CrateGroup;
import org.cyberiantiger.minecraft.duckcrates.config.PermissionGrant;
import org.cyberiantiger.minecraft.duckcrates.config.Reward;
import org.cyberiantiger.minecraft.nbt.CompoundTag;
import org.cyberiantiger.minecraft.unsafe.CBShim;
import org.cyberiantiger.minecraft.unsafe.NBTTools;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormat;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.introspector.BeanAccess;

/**
 *
 * @author antony
 */
public class Main extends JavaPlugin implements Listener {
    private static final String CONFIG = "config.yml";
    private static final String MESSAGES = "locale.properties";
    
    private NBTTools tools;
    private Config config;
    private final Properties messages = new Properties();
    private final Database database;

    public Main() {
        this.database = new Database(this);
    }

    public NBTTools getTools() {
        return tools;
    }
    
    private boolean copyDefault(String source, String dest) {
        File destFile = new File(getDataFolder(), dest);
        if (!destFile.exists()) {
            try {
                destFile.getParentFile().mkdirs();
                InputStream in = getClass().getClassLoader().getResourceAsStream(source);
                if (in != null) {
                    try {
                        try (OutputStream out = new FileOutputStream(destFile)) {
                            ByteStreams.copy(in, out);
                        }
                    } finally {
                        in.close();
                    }
                    return true;
                }
            } catch (IOException ex) {
                getLogger().log(Level.WARNING, "Error copying default " + dest, ex);
            }
        }
        return false;
    }
    
    public File getDataFile(String name) {
        return new File(getDataFolder(), name);
    }

    public Reader openFile(File file) throws IOException {
        return new InputStreamReader(new BufferedInputStream(new FileInputStream(file)), Charsets.UTF_8);
    }

    public Writer writeFile(File file) throws IOException {
        return new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(file)), Charsets.UTF_8);
    }

    public Reader openDataFile(String file) throws IOException {
        return openFile(getDataFile(file));
    }

    public Writer writeDataFile(String file) throws IOException {
        return writeFile(getDataFile(file));
    }

    public Reader openResource(String resource) throws IOException {
        InputStream in = getClass().getClassLoader().getResourceAsStream(resource);
        if (in == null) { 
            throw new FileNotFoundException(resource);
        }
        return new InputStreamReader(new BufferedInputStream(in), Charsets.UTF_8);
    }

    private void loadConfig() {
        config = new Config();
        try {
            Yaml configLoader = new Yaml(new CustomClassLoaderConstructor(Config.class, getClass().getClassLoader()));
            configLoader.setBeanAccess(BeanAccess.FIELD);
            try (Reader in = openDataFile(CONFIG)) { 
                config = configLoader.loadAs(in, Config.class);
                List<String> errors = new ArrayList<>();
                if (!config.validate(this, errors)) {
                    getLogger().log(Level.WARNING, "Errors found in configuration file");
                    errors.stream().forEach((s) -> {
                        getLogger().log(Level.WARNING, s);
                    });
                }
            }
        } catch (IOException | YAMLException ex) {
            getLogger().log(Level.SEVERE, "Error loading config.yml", ex);
            getLogger().severe("Your config.yml has fatal errors, using defaults.");
        }
    }

    private void loadMessages() {
        try {
            messages.clear();
            messages.load(openDataFile(MESSAGES));
        } catch (IOException ex) {
            getLogger().log(Level.WARNING, "Could not load locale.properties", ex);
            try {
                messages.clear();
                messages.load(openResource(MESSAGES));
            } catch (IOException ex1) {
                getLogger().log(Level.SEVERE, "Could not load default locale.properties", ex);
            }
        }
    }

    @Override
    public void onEnable() {
        try {
            tools = CBShim.createShim(NBTTools.class, this);
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Exception initialising nbt tools", e);
            setEnabled(false);
            return;
        }
        copyDefault(CONFIG, CONFIG);
        copyDefault(MESSAGES, MESSAGES);
        loadConfig();
        loadMessages();
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        database.close();
    }

    private Map<String, CratesAvailable> getAvailable(Player player) {
        Map<String, CratesAvailable> result = new HashMap<>();
        for (Map.Entry<String, CrateGroup> e : getDCConfig().getGroups().entrySet()) {
            String key = e.getKey();
            for (PermissionGrant p : e.getValue().getRewardCount()) {
                if (player.hasPermission(p.getPermission())) {
                    result.put(e.getKey(), new CratesAvailable(e.getValue(), p.getDaily(), p.getOnce()));
                    break;
                }
            }
        }
        return result;
    }

    private void openCrate(Player player, CrateGroup group) {
        List<ItemStack> rewardItems = new ArrayList<>();
        if (group.getMessage() != null) {
            player.sendMessage(group.getMessage());
        }
        group.getAllRewards().stream().filter((reward) -> (Math.random() < reward.getChance())).map((reward) -> {
            if (reward.getMessage() != null) {
                player.sendMessage(reward.getMessage());
            }
            return reward;
        }).forEach((reward) -> {
            reward.doReward(this, player, rewardItems);
        });
        HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(rewardItems.toArray(new ItemStack[rewardItems.size()]));
        overflow.values().stream().forEach((stack) -> {
            player.getWorld().dropItem(player.getLocation(), stack);
        });
    }

    private ItemStack getCrateItemStack(String crate, String name, byte count, short damage, CompoundTag tag) {
        CompoundTag itemTag = new CompoundTag();
        itemTag.setString("id", name);
        itemTag.setByte("Count", count);
        itemTag.setShort("Damage", damage);
        if (tag == null) {
            tag = new CompoundTag();
        }
        itemTag.setCompound("tag", tag);
        tag.setString("DuckCrates", crate);
        return tools.createItemStack(itemTag);
    }

    private void deliver(Player player, String crate, CrateGroup group, int amount) {
        ItemStack item = getCrateItemStack(crate, group.getName(), (byte)1, group.getDamage(), group.getParsedTag());
        item.getMaxStackSize();

        List<ItemStack> rewards = new ArrayList<>();
        int count = 0;
        while (count < amount) {
            ItemStack nextStack = item.clone();
            if (amount - count >= item.getMaxStackSize()) {
                nextStack.setAmount(item.getMaxStackSize());
            } else {
                nextStack.setAmount(amount - count);
            }
            rewards.add(nextStack);
            count += nextStack.getAmount();
        }
        HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(rewards.toArray(new ItemStack[rewards.size()]));
        overflow.values().stream().forEach((stack) -> {
            player.getWorld().dropItem(player.getLocation(), stack);
        });
    }

    private void deliverDaily(Player player, Map<String, CratesAvailable> available) {
        available.entrySet().stream().filter((e) -> (e.getValue().getDaily() > 0)).forEach((e) -> {
            deliver(player, e.getKey(), getDCConfig().getGroups().get(e.getKey()), e.getValue().getDaily());
        });
    }

    private void deliverSpecific(Player player, String group, int count) {
        deliver(player, group, getDCConfig().getGroups().get(group), count);
    }

    private String findCrateType(String base) {
        for (String s : getDCConfig().getGroups().keySet()) {
            if (s.equalsIgnoreCase(base)) {
                return s;
            }
        }
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // TODO
        return null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            args = new String[] {"inspect"};
        }
        if ("daily".equalsIgnoreCase(args[0]) && args.length == 1) {
            if (! (sender instanceof Player) ) {
                sender.sendMessage(getMessage("claim.error.not_player"));
                return true;
            }
            final Player player = (Player) sender;
            database.claimDaily(player.getUniqueId(),
                    (ClaimDailyStatus t) -> {
                        if (t.isSuccess()) {
                            // FUTURE: Improve the message to describe what crates
                            // where delivered.
                            player.sendMessage(getMessage("claim.daily.success"));
                            Map<String, CratesAvailable> available = getAvailable(player);
                            deliverDaily(player, available);
                        } else {
                            Period period = new Period(t.getRemainingTime());
                            player.sendMessage(getMessage("claim.daily.wait", PeriodFormat.getDefault().print(period)));
                        }
                    }, (Exception t) -> {
                        player.sendMessage(getMessage("claim.error.exception"));
                        getLogger().log(Level.WARNING, "Exception handling command", t);
                    });
            return true;
        } else if ("reload".equalsIgnoreCase(args[0]) && args.length == 1) {
            if (!sender.hasPermission("duckcrates.reload")) {
                sender.sendMessage(getMessage("claim.permission", "duckcrates.reload"));
                return true;
            }
            loadConfig();
            loadMessages();
            database.close();
            sender.sendMessage(getMessage("claim.reload"));
            return true;
        } else if ("reset".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission("duckcrates.reset")) {
                sender.sendMessage(getMessage("claim.permission", "duckcrates.reset"));
                return true;
            }
            Player target;
            if (args.length == 1) {
                if (sender instanceof Player) {
                    target = (Player) sender;
                } else {
                    sender.sendMessage(getMessage("claim.error.not_player"));
                    return true;
                }
            } else if (args.length == 2) {
                target = getServer().getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(getMessage("claim.reset.unknown_target", args[1]));
                    return true;
                }
            } else {
                return false;
            }
            database.resetClaims(target.getUniqueId(),
                    (Void t) -> {
                        sender.sendMessage(getMessage("claim.reset.success", target.getDisplayName()));
                    }, 
                    (Exception t) -> {
                        sender.sendMessage(getMessage("claim.error.exception"));
                        getLogger().log(Level.WARNING, "Exception handling command", t);
                    });
            return true;
        } else if ("inspect".equalsIgnoreCase(args[0])) {
            Player target;
            if (args.length == 1) {
                if (sender instanceof Player) {
                    target = (Player) sender;
                } else {
                    sender.sendMessage(getMessage("claim.error.not_player"));
                    return true;
                }
            } else if (args.length == 2) {
                target = getServer().getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(getMessage("claim.inspect.unknown_target", args[1]));
                    return true;
                }
            } else {
                return false;
            }
            if (target != sender && !sender.hasPermission("duckcrates.inspect")) {
                sender.sendMessage(getMessage("claim.permission", "duckcrates.inspect"));
                return true;
            }
            final Map<String, CratesAvailable> available = getAvailable(target);
            database.getClaimStatus(target.getUniqueId(),
                    (ClaimStatus t) -> {
                        if (sender == target) {
                            sender.sendMessage(getMessage("claim.inspect.header_self"));
                        } else {
                            sender.sendMessage(getMessage("claim.inspect.header_other", target.getDisplayName()));
                        }
                        long now = System.currentTimeMillis();
                        if (t.getLastDaily() + Database.ONE_DAY <= now) {
                            sender.sendMessage(getMessage("claim.inspect.daily_available"));
                        } else {
                            Period period = new Period(t.getLastDaily() + Database.ONE_DAY - now);
                            sender.sendMessage(getMessage("claim.inspect.daily_unavailable", PeriodFormat.getDefault().print(period)));
                        }
                        Map<String, Integer> claimed = t.getClaimed();
                        available.entrySet().stream().forEach((e) -> {
                            int once = e.getValue().getOnce();
                            if (once > 0) {
                                int delivered = 0;
                                if (claimed.containsKey(e.getKey())) {
                                    delivered = claimed.get(e.getKey());
                                }
                                int remaining = once - delivered;
                                if (remaining < 0) {
                                    remaining = 0;
                                }
                                sender.sendMessage(getMessage("claim.inspect.once_available", e.getKey(), remaining, once));
                            }
                        });
                    },
                    (Exception t) -> {
                        target.sendMessage(getMessage("claim.error.exception"));
                        getLogger().log(Level.WARNING, "Exception handling command", t);
                    });
            return true;
        } else if ("give".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission("duckcrates.give")) {
                sender.sendMessage(getMessage("claim.permission", "duckcrates.give"));
                return true;
            }
            if (args.length == 1) {
                return false;
            }
            String type = findCrateType(args[1]);
            int count = 1;
            Player target = (sender instanceof Player) ? (Player) sender : null;
            if (args.length >= 3) {
                try { 
                    count = Integer.parseInt(args[2]);
                } catch (NumberFormatException ex) {
                    return false;
                }
            }
            if (args.length >= 4) {
                target = getServer().getPlayer(args[3]);
            }
            if (type == null) {
                sender.sendMessage(getMessage("claim.give.crate_not_found", args[1]));
                return true;
            }
            if (count < 1) {
                sender.sendMessage(getMessage("claim.give.amount_too_small", count));
                return true;
            }
            if (count > 1024) {
                sender.sendMessage(getMessage("claim.give.amount_too_big", count));
                return true;
            }
            if (target == null) {
                sender.sendMessage(getMessage("claim.give.target_not_found", args[3]));
                return true;
            }
            sender.sendMessage(getMessage("claim.give.success", target.getDisplayName(), type, count));
            deliverSpecific(target, type, count);
            return true;
        } else if (args.length == 1) {
            if (! (sender instanceof Player) ) {
                sender.sendMessage(getMessage("claim.error.not_player"));
                return true;
            }
            final Player player = (Player) sender;
            String type = findCrateType(args[0]);
            if (type == null) {
                sender.sendMessage(getMessage("claim.specific.unknown_type", args[0]));
                return true;
            }
            final Map<String, CratesAvailable> available = getAvailable(player);
            if (!available.containsKey(type) || available.get(type).getOnce() == 0) {
                sender.sendMessage(getMessage("claim.once.not_available", type));
                return true;
            }
            database.claimOnce(player.getUniqueId(), type, available.get(type).getOnce(),
                    (Integer amount) -> {
                        if (amount > 0) {
                            sender.sendMessage(getMessage("claim.once.success", type, amount));
                            deliverSpecific(player, type, amount);
                        } else {
                            sender.sendMessage(getMessage("claim.once.already_claimed", type));
                        }
                    },
                    (Exception t) -> {
                        player.sendMessage(getMessage("claim.error.exception"));
                        getLogger().log(Level.WARNING, "Exception handling command", t);
                    }
            );
            return true;
        }
        return false;
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Player player = e.getPlayer();
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item != null) {
                CompoundTag itemNbt = tools.readItemStack(item);
                if (itemNbt != null) {
                    if (itemNbt.containsKey("tag")) {
                        CompoundTag tag = itemNbt.getCompound("tag");
                        if (tag.containsKey("DuckCrates")) {
                            String crateType = tag.getString("DuckCrates");
                            CrateGroup group = getDCConfig().getGroups().get(crateType);
                            if (group != null) {
                                e.setCancelled(true);
                                if (item.getAmount() > 1) {
                                    item.setAmount(item.getAmount()-1);
                                    player.getInventory().setItemInMainHand(item);
                                } else {
                                    player.getInventory().setItemInMainHand(null);
                                }
                                openCrate(player, group);
                            }
                        }
                    }
                }
            }
        }
    }
    
    @EventHandler(ignoreCancelled = false, priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent e) {
        // TODO: Show available crates on join
    }

    public String getMessage(String msg, Object... args) {
        String result = messages.getProperty(msg);
        if (result == null) {
            return String.format("Unknown message %s with arguments %s", msg, Arrays.toString(args));
        }
        return String.format(result, args);
    }

    public Config getDCConfig() {
        return config;
    }
}