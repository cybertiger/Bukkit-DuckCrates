/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cyberiantiger.minecraft.duckcrates;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.cyberiantiger.minecraft.duckcrates.config.Config;

/**
 *
 * @author antony
 */
public class Database {
    // TODO: Refactor this to a saner place, or replace with an existing constant
    public static final long ONE_DAY = 1000 * 60 * 60 * 24;
    private static final int DB_VERSION = 1;
    private static final String GET_VERSION_SQL = "SELECT id FROM version";
    private static final String GET_CLAIMED_SQL = "SELECT crate_type, amount FROM claimed WHERE uuid = ?";
    private static final String GET_LAST_CLAIM_SQL = "SELECT claim_time FROM last_daily WHERE uuid = ?";
    private static final String UPDATE_LAST_CLAIM_SQL = "INSERT INTO last_daily (uuid, claim_time) VALUES (?, ?) ON DUPLICATE KEY UPDATE claim_time = ?";
    private static final String GET_CLAIMED_TYPE_SQL = "SELECT amount FROM claimed WHERE uuid = ? AND crate_type = ?";
    private static final String UPDATE_CLAIMED_TYPE_SQL = "INSERT INTO claimed (uuid, crate_type, amount) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE amount = amount + ?";
    private static final String RESET_LAST_CLAIM = "DELETE FROM claimed WHERE uuid = ?";
    private static final String RESET_CLAIMED_TYPE = "DELETE FROM last_daily WHERE uuid = ?";
    private Connection conn;
    private final Main outer;

    Database(final Main outer) {
        this.outer = outer;
    }

    private synchronized Connection getConnection() throws SQLException {
        if (conn != null) {
            if (conn.isValid(1000)) {
                return conn;
            } else {
                conn.close();
                conn = null;
            }
        }
        Config config = outer.getDCConfig();
        conn = DriverManager.getConnection(config.getDbUrl(), config.getDbUser(), config.getDbPassword());
        conn.setAutoCommit(true); // Who needs transactions anyway.
        try (
                Statement testVersion = conn.createStatement();
                ResultSet rs = testVersion.executeQuery(GET_VERSION_SQL)
                ) {
            if (rs.next()) {
                int version = rs.getInt(1);
                if (version != DB_VERSION) throw new SQLException("Database version does not match expected version");
            } else {
                throw new SQLException("Database version field not set");
            }
        } catch (SQLException ex) {
            outer.getLogger().log(Level.INFO, "Error initialising database: {0}", ex.getMessage());
            try (
                    Statement batch = conn.createStatement();
                    Reader in = outer.openResource("schema.sql");
                    ) {
                BufferedReader inn = new BufferedReader(in);
                StringBuilder query = new StringBuilder();
                String line;
                while ( (line = inn.readLine()) != null ) {
                    if (line.endsWith(";")) {
                        query.append(line.substring(0, line.length() - 1));
                        batch.addBatch(query.toString());
                        query.setLength(0);
                    } else {
                        query.append(line);
                        query.append(System.lineSeparator());
                    }
                }
                batch.executeBatch();
            } catch (IOException ex2) {
                throw new SQLException("Fatal error initialising database", ex2);
            }
        }
        return conn;
    }

    public synchronized void close() {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException ex) {
                outer.getLogger().log(Level.WARNING, "Error closing SQL connection", ex);
            }
            conn = null;
        }
    }

    private static ClaimStatus getClaimStatusSync(Connection conn, UUID player) throws SQLException {
        try (
                PreparedStatement getClaimed = conn.prepareStatement(GET_CLAIMED_SQL);
                PreparedStatement getLastClaim = conn.prepareStatement(GET_LAST_CLAIM_SQL)
                )
        {
            getClaimed.setString(1, player.toString());
            getLastClaim.setString(1, player.toString());
            try (
                    ResultSet claimed = getClaimed.executeQuery();
                    ResultSet lastClaim = getLastClaim.executeQuery();
                    )
            {
                Map<String, Integer> claimedMap = new HashMap<>();
                long lastClaimTime = 0L;
                while (claimed.next()) {
                    claimedMap.put(claimed.getString(1), claimed.getInt(2));
                }
                if (lastClaim.next()) {
                    lastClaimTime = lastClaim.getLong(1);
                }
                return new ClaimStatus(claimedMap, lastClaimTime);
            }
        }
    }


    private static ClaimDailyStatus claimDailySync(Connection conn, UUID player) throws SQLException {
        try (
                PreparedStatement getLastClaim = conn.prepareStatement(GET_LAST_CLAIM_SQL)
                )
        {
            getLastClaim.setString(1, player.toString());
            try (
                    ResultSet lastClaim = getLastClaim.executeQuery()
                    )
            {
                long now = System.currentTimeMillis();
                long lastClaimTime = 0L;
                if (lastClaim.next()) {
                    lastClaimTime = lastClaim.getLong(1);
                }
                if (now >= lastClaimTime + ONE_DAY) {
                    try (
                            PreparedStatement updateLastClaim = conn.prepareStatement(UPDATE_LAST_CLAIM_SQL);
                            )
                    {
                        updateLastClaim.setString(1, player.toString());
                        updateLastClaim.setLong(2, now);
                        updateLastClaim.setLong(3, now);
                        updateLastClaim.executeUpdate();
                    }
                    return new ClaimDailyStatus(true, 0L);
                } else {
                    return new ClaimDailyStatus(false, lastClaimTime + ONE_DAY - now);
                }
            }
        }
    }

    private static int claimOnceSync(Connection conn, UUID player, String type, int maxAvailable) throws SQLException {
        try (
                PreparedStatement getClaimedType = conn.prepareStatement(GET_CLAIMED_TYPE_SQL);
                )
        {
            getClaimedType.setString(1, player.toString());
            getClaimedType.setString(2, type);
            try (
                    ResultSet claimed = getClaimedType.executeQuery();
                    )
            {
                int amount = 0;
                if (claimed.next()) {
                    amount = claimed.getInt(1);
                }
                int result = 0;
                if (amount < maxAvailable) {
                    result = maxAvailable - amount;
                    try (
                            PreparedStatement updateClaimedType = conn.prepareStatement(UPDATE_CLAIMED_TYPE_SQL);
                            )
                    {
                        updateClaimedType.setString(1, player.toString());
                        updateClaimedType.setString(2, type);
                        updateClaimedType.setInt(3, result);
                        updateClaimedType.setInt(4, result);
                        updateClaimedType.executeUpdate();
                        
                    }
                }
                return result;
            }
        }
    }

    private static void resetClaimsSync(Connection conn, UUID player) throws SQLException {
        try (
                PreparedStatement resetClaimedType = conn.prepareStatement(RESET_CLAIMED_TYPE);
                PreparedStatement resetLastClaim = conn.prepareStatement(RESET_LAST_CLAIM);
                )
        {
            resetClaimedType.setString(1, player.toString());
            resetLastClaim.setString(1, player.toString());
            resetClaimedType.executeUpdate();
            resetLastClaim.executeUpdate();
        }
    }

    public void getClaimStatus(final UUID player, final Consumer<ClaimStatus> handler, final Consumer<Exception> errorHandler) {
        if (Bukkit.isPrimaryThread()) {
            outer.getServer().getScheduler().runTaskAsynchronously(outer, () -> {
                getClaimStatus(player, new CallSyncConsumer<>(handler, outer), new CallSyncConsumer<>(errorHandler, outer));
            });
            return;
        }
        try {
            handler.accept(getClaimStatusSync(getConnection(), player));
        } catch (SQLException ex) {
            errorHandler.accept(ex);
        }
    }

    public void claimDaily(final UUID player, final Consumer<ClaimDailyStatus> handler, final Consumer<Exception> errorHandler) {
        if (Bukkit.isPrimaryThread()) {
            outer.getServer().getScheduler().runTaskAsynchronously(outer, () -> {
                claimDaily(player, new CallSyncConsumer<>(handler, outer), new CallSyncConsumer<>(errorHandler, outer));
            });
            return;
        }
        try {
            handler.accept(claimDailySync(getConnection(), player));
        } catch (SQLException ex) {
            errorHandler.accept(ex);
        }
    }

    public void claimOnce(final UUID player, final String type, final int maxAvailable, final Consumer<Integer> handler, final Consumer<Exception> errorHandler) {
        if (Bukkit.isPrimaryThread()) {
            outer.getServer().getScheduler().runTaskAsynchronously(outer, () -> {
                claimOnce(player, type, maxAvailable, new CallSyncConsumer<>(handler, outer), new CallSyncConsumer<>(errorHandler, outer));
            });
            return;
        }
        try {
            handler.accept(claimOnceSync(getConnection(), player, type, maxAvailable));
        } catch (SQLException ex) {
            errorHandler.accept(ex);
        }
    }

    public void resetClaims(final UUID player, final Consumer<Void> handler, final Consumer<Exception> errorHandler) {
        if (Bukkit.isPrimaryThread()) {
            outer.getServer().getScheduler().runTaskAsynchronously(outer, () -> {
                resetClaims(player, new CallSyncConsumer<>(handler, outer), new CallSyncConsumer<>(errorHandler, outer));
            });
            return;
        }
        try {
            resetClaimsSync(getConnection(), player);
            handler.accept(null);
        } catch (SQLException ex) {
            errorHandler.accept(ex);
        }
    }
}
