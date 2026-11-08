
package com.berkan.lonca.database.connection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DatabaseExecutor {

    private final DatabaseManager databaseManager;
    private final ExecutorService executor;

    public DatabaseExecutor(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;

        this.executor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "Lonca-Database-Thread");
            thread.setDaemon(true);
            return thread;
        });
    }

    public <T> CompletableFuture<T> submit(DatabaseTask<T> task) {

        return CompletableFuture.supplyAsync(() -> {

            try {
                Connection connection = databaseManager.getConnection();

                return task.execute(connection);

            } catch (SQLException exception) {
                throw new CompletionException(exception);
            }

        }, executor);
    }



    public boolean shutdown() {

        executor.shutdown();

        try {
            if (executor.awaitTermination(
                    30,
                    java.util.concurrent.TimeUnit.SECONDS
            )) {
                return true;
            }

            executor.shutdownNow();

            return executor.awaitTermination(
                    5,
                    java.util.concurrent.TimeUnit.SECONDS
            );

        } catch (InterruptedException exception) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
            return false;
        }
    }


    @FunctionalInterface
    public interface DatabaseTask<T> {

        T execute(Connection connection) throws SQLException;
    }
}
