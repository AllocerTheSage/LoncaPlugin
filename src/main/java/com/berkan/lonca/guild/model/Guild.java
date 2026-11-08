
package com.berkan.lonca.guild.model;

public class Guild {

    private final long id;
    private final String name;
    private final String tag;

    private final int level;
    private final long xp;
    private final int capacity;

    private final String description;
    private final boolean applicationsOpen;
    private final String createdAt;

    public Guild(
            long id,
            String name,
            String tag,
            int level,
            long xp,
            int capacity,
            String description,
            boolean applicationsOpen,
            String createdAt
    ) {
        this.id = id;
        this.name = name;
        this.tag = tag;
        this.level = level;
        this.xp = xp;
        this.capacity = capacity;
        this.description = description;
        this.applicationsOpen = applicationsOpen;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTag() {
        return tag;
    }

    public int getLevel() {
        return level;
    }

    public long getXp() {
        return xp;
    }

    public int getCapacity() {
        return capacity;
    }

    public String getDescription() {
        return description;
    }

    public boolean isApplicationsOpen() {
        return applicationsOpen;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
