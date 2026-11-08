package com.berkan.lonca.guild.model;

public class GuildCapacityResult {

    private final long guildId;
    private final int capacity;

    public GuildCapacityResult(long guildId, int capacity) {
        this.guildId = guildId;
        this.capacity = capacity;
    }

    public long getGuildId() {
        return guildId;
    }

    public int getCapacity() {
        return capacity;
    }
}