
package com.berkan.lonca.player.model;

import java.util.UUID;

public class PlayerData {

    private final UUID uuid;
    private String lastKnownName;

    private int totalKills;
    private int totalDeaths;
    private int warWins;
    private int warLosses;

    public PlayerData(
            UUID uuid,
            String lastKnownName,
            int totalKills,
            int totalDeaths,
            int warWins,
            int warLosses
    ) {
        this.uuid = uuid;
        this.lastKnownName = lastKnownName;
        this.totalKills = totalKills;
        this.totalDeaths = totalDeaths;
        this.warWins = warWins;
        this.warLosses = warLosses;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getLastKnownName() {
        return lastKnownName;
    }

    public int getTotalKills() {
        return totalKills;
    }

    public int getTotalDeaths() {
        return totalDeaths;
    }

    public int getWarWins() {
        return warWins;
    }

    public int getWarLosses() {
        return warLosses;
    }
}
