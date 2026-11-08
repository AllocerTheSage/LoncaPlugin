
package com.berkan.lonca.guild.service;

import com.berkan.lonca.guild.model.Guild;
import com.berkan.lonca.guild.model.GuildFounderRank;
import com.berkan.lonca.guild.repository.GuildRepository;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class GuildService {

    private final GuildRepository guildRepository;

    public GuildService(GuildRepository guildRepository) {
        this.guildRepository = guildRepository;
    }

    // Lonca adı kurallarını kontrol eder.
    private boolean isValidName(String name) {
        return name != null
                && name.matches("[A-Za-z0-9 _.-]{4,16}");
    }


    // Lonca oluşturma işlemini yönetir.
    public CompletableFuture<Guild> createGuild(
            String name,
            UUID leaderUuid,
            GuildFounderRank founderRank
    ) {

        if (!isValidName(name)) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException(
                            "Lonca adi 4-16 karakter olmali ve sadece Ingilizce harf, rakam, bosluk, _, - ve . icerebilir!"
                    )
            );
        }

        if (founderRank == null) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Kurucu rutbesi belirlenemedi!")
            );
        }

        return guildRepository.isPlayerInGuild(leaderUuid)
                .thenCompose(inGuild -> {

                    if (inGuild) {
                        return CompletableFuture.failedFuture(
                                new IllegalStateException(
                                        "Zaten bir loncaya uyessin!"
                                )
                        );
                    }

                    return guildRepository.existsByName(name);
                })
                .thenCompose(nameExists -> {

                    if (nameExists) {
                        return CompletableFuture.failedFuture(
                                new IllegalStateException(
                                        "Bu lonca adi zaten kullaniliyor!"
                                )
                        );
                    }

                    int capacity = founderRank.getStartingCapacity();

                    return guildRepository.create(
                            name,
                            leaderUuid,
                            capacity
                    );
                });
    }
}
