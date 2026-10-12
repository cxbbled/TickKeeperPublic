package com.example.tickcountstorer.client;

import com.example.tickcountstorer.client.mixin.HudAccessor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class TickCountStorage {
    private static final Path FILE = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("tick-count-storer.properties");

    private static final Properties SAVED_COUNTS = new Properties();

    private static String activeWorldKey;
    private static Integer pendingRestoreCount;
    private static String pendingRestoreKey;

    private static int ticksSinceSave;

    private TickCountStorage() {
    }

    public static void register() {
        loadFile();

        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, level) -> {
            if (level == null) {
                saveActiveWorld(client);

                activeWorldKey = null;
                pendingRestoreCount = null;
                pendingRestoreKey = null;
                ticksSinceSave = 0;
                return;
            }

            String newWorldKey = getWorldKey(client);

            if (newWorldKey == null) {
                return;
            }

            if (newWorldKey.equals(activeWorldKey)) {
                if (pendingRestoreCount == null) {
                    String saved = SAVED_COUNTS.getProperty(activeWorldKey);

                    if (saved != null) {
                        try {
                            pendingRestoreCount = Integer.parseInt(saved);
                            pendingRestoreKey = activeWorldKey;
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }

                return;
            }

            saveActiveWorld(client);

            activeWorldKey = newWorldKey;
            pendingRestoreCount = null;
            pendingRestoreKey = null;
            ticksSinceSave = 0;

            String saved = SAVED_COUNTS.getProperty(activeWorldKey);

            if (saved != null) {
                try {
                    pendingRestoreCount = Integer.parseInt(saved);
                    pendingRestoreKey = activeWorldKey;
                } catch (NumberFormatException ignored) {
                }
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level == null || client.player == null || client.gui == null) {
                return;
            }

            if (pendingRestoreCount != null) {
                if (!pendingRestoreKey.equals(activeWorldKey)) {
                    pendingRestoreCount = null;
                    pendingRestoreKey = null;
                } else {
                    int count = pendingRestoreCount;

                    ((HudAccessor) client.gui.hud)
                            .tickCountStorer$setTickCount(count);

                    pendingRestoreCount = null;
                    pendingRestoreKey = null;
                    ticksSinceSave = 0;
                }
            }

            if (activeWorldKey == null) {
                activeWorldKey = getWorldKey(client);

                if (activeWorldKey == null) {
                    return;
                }
            }

            ticksSinceSave++;

            if (ticksSinceSave >= 20) {
                ticksSinceSave = 0;
                saveActiveWorld(client);
            }
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(
                TickCountStorage::saveActiveWorld);
    }

    private static String getWorldKey(Minecraft client) {
        IntegratedServer server = client.getSingleplayerServer();

        if (server != null) {
            Path worldPath = server.getWorldPath(LevelResource.ROOT);

            return "singleplayer:"
                    + worldPath.toAbsolutePath().normalize();
        }

        if (client.getCurrentServer() != null) {
            var multiplayerServer = client.getCurrentServer();

            String address = multiplayerServer.ip
                    .trim()
                    .toLowerCase(java.util.Locale.ROOT);

            return "multiplayer:" + address;
        }

        return null;
    }

    private static void saveActiveWorld(Minecraft client) {
        if (activeWorldKey == null || client.gui == null) {
            return;
        }

        int count = ((HudAccessor) client.gui.hud)
                .tickCountStorer$getTickCount();

        SAVED_COUNTS.setProperty(activeWorldKey, Integer.toString(count));

        saveFile();
    }

    private static void loadFile() {
        if (!Files.exists(FILE)) {
            return;
        }

        try (var input = Files.newInputStream(FILE)) {
            SAVED_COUNTS.load(input);
        } catch (IOException ignored) {
        }
    }

    private static void saveFile() {
        try {
            Files.createDirectories(FILE.getParent());

            try (var output = Files.newOutputStream(FILE)) {
                SAVED_COUNTS.store(output, null);
            }
        } catch (IOException ignored) {
        }
    }
}