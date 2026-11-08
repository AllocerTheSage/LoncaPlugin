
package com.berkan.lonca.guild.model;

public enum GuildFounderRank {

    NORMAL(8),
    VIP(10),
    VIP_PLUS(12),
    SPONSOR(15);

    private final int startingCapacity;

    GuildFounderRank(int startingCapacity) {
        this.startingCapacity = startingCapacity;
    }

    public int getStartingCapacity() {
        return startingCapacity;
    }
}
