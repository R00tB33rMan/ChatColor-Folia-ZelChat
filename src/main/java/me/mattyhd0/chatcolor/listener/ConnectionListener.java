package me.mattyhd0.chatcolor.listener;

import me.mattyhd0.chatcolor.CPlayer;
import me.mattyhd0.chatcolor.ChatColorPlugin;
import me.mattyhd0.chatcolor.configuration.SimpleYMLConfiguration;
import me.mattyhd0.chatcolor.pattern.api.BasePattern;
import me.mattyhd0.chatcolor.updatechecker.UpdateChecker;
import me.mattyhd0.chatcolor.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import me.nahu.scheduler.wrapper.WrappedJavaPlugin;
import me.nahu.scheduler.wrapper.WrappedScheduler;
import me.nahu.scheduler.wrapper.task.WrappedTask;
import me.nahu.scheduler.wrapper.runnable.WrappedRunnable;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.UUID;

public class ConnectionListener implements Listener {
    private ChatColorPlugin plugin;
    private WrappedTask task;
    private HashMap<UUID, WrappedTask> playersBeingLoaded = new HashMap<>();

    public ConnectionListener(ChatColorPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();

        if (player.hasPermission("chatcolor.updatenotify") && ChatColorPlugin.getInstance().getConfigurationManager().getConfig().getBoolean("config.update-checker")) {
            ChatColorPlugin.plugin.getScheduler().runTaskAsynchronously(() -> {
                UpdateChecker updateChecker = new UpdateChecker(ChatColorPlugin.getInstance(), 93186);
        
                if (!player.isOnline()) return;
        
                if (updateChecker.requestIsValid()) {
                    if (!updateChecker.isRunningLatestVersion()) {
                        String message = ChatColor.translateAlternateColorCodes('&', "&8[&4&lC&c&lh&6&la&e&lt&2&lC&a&lo&b&ll&3&lo&1&lr&8] &7You are using version &a" + updateChecker.getVersion() + "&7 and the latest version is &a" + updateChecker.getLatestVersion());
                        String message2 = ChatColor.translateAlternateColorCodes('&', "&8[&4&lC&c&lh&6&la&e&lt&2&lC&a&lo&b&ll&3&lo&1&lr&8] &7You can download the latest version at: &a" + updateChecker.getSpigotResource().getDownloadUrl());
                        player.sendMessage(message);
                        player.sendMessage(message2);
                    }
                } else {
                    String message = ChatColor.translateAlternateColorCodes('&', "&8[&4&lC&c&lh&6&la&e&lt&2&lC&a&lo&b&ll&3&lo&1&lr&8] &7Could not verify if you are using the latest version of ChatColor :(");
                    String message2 = ChatColor.translateAlternateColorCodes('&', "&8[&4&lC&c&lh&6&la&e&lt&2&lC&a&lo&b&ll&3&lo&1&lr&8] &7You can disable update checker in config.yml file");
                    player.sendMessage(message);
                    player.sendMessage(message2);
                }
            });
        }
        if (playersBeingLoaded.containsKey(player.getUniqueId())) {
            playersBeingLoaded.remove(event.getPlayer().getUniqueId()).cancel();
        }
        int delay = Math.max(0, plugin.getConfigurationManager().getConfig().getInt("config.data-delay", 30));
        task = new WrappedRunnable() {
            @Override
            public void run() {
                if (ChatColorPlugin.getInstance().getConnectionPool() == null) {
                    SimpleYMLConfiguration data = ChatColorPlugin.getInstance().getConfigurationManager().getData();
                    BasePattern basePattern = plugin.getPatternManager().getPatternByName(data.getString("data." + player.getUniqueId()));
                    plugin.getDataMap().put(player.getUniqueId(), new CPlayer(player, basePattern));
                } else {
                    try {
                        Connection connection = ChatColorPlugin.getInstance().getConnectionPool().getConnection();
                        PreparedStatement statement = connection.prepareStatement("SELECT * FROM playerdata WHERE uuid=?");
                        statement.setString(1, player.getUniqueId().toString());
                        ResultSet resultSet = statement.executeQuery();
                        if (resultSet.next()) {
                            BasePattern basePattern = plugin.getPatternManager().getPatternByName(resultSet.getString("pattern"));
                            plugin.getDataMap().put(player.getUniqueId(), new CPlayer(player, basePattern));
                        } else plugin.getDataMap().put(player.getUniqueId(), new CPlayer(player, null));
                        statement.close();
                        connection.close();
                    } catch (SQLException e) {
                        Bukkit.getServer().getConsoleSender().sendMessage(
                                Util.color(
                                        formatQuery(player, "&c[ChatColor] An error occurred while trying to get the pattern of {uuid} ({player}) via MySQL")
                                )
                        );
                        e.printStackTrace();
                    }
                }
                playersBeingLoaded.remove(player.getUniqueId());
            }
        }.runTaskLaterAsynchronously(ChatColorPlugin.plugin.getScheduler(), delay);
        playersBeingLoaded.put(player.getUniqueId(), task);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (playersBeingLoaded.containsKey(event.getPlayer().getUniqueId())) {
            playersBeingLoaded.remove(event.getPlayer().getUniqueId()).cancel();
            return;
        }
        CPlayer cPlayer = plugin.getDataMap().get(event.getPlayer().getUniqueId());
        if (plugin.getDataMap().containsKey(event.getPlayer().getUniqueId())) {
            new WrappedRunnable() {
                @Override
                public void run() {
                    cPlayer.saveData();
                }
            }.runTaskAsynchronously(ChatColorPlugin.plugin.getScheduler());
        }
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuitMonitor(PlayerQuitEvent event) {
        plugin.getDataMap().remove(event.getPlayer().getUniqueId());
    }


    private String formatQuery(Player player, String string) {
        return formatQuery(player, string, null);
    }

    private String formatQuery(Player player, String string, BasePattern pattern) {

        String uuid = player.getUniqueId().toString();
        String name = player.getName();

        string = pattern == null ? string : string.replaceAll("\\{pattern}", pattern.getName(false));

        return string
                .replaceAll("\\{uuid}", uuid)
                .replaceAll("\\{player}", name);

    }
}
