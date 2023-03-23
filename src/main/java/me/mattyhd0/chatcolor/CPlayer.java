package me.mattyhd0.chatcolor;

import me.mattyhd0.chatcolor.configuration.SimpleYMLConfiguration;
import me.mattyhd0.chatcolor.pattern.api.BasePattern;
import me.mattyhd0.chatcolor.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CPlayer {

    public Player player;
    public static Map<UUID, String> lastMessages = new HashMap<>();

    public CPlayer(Player player){
        this.player = player;
    }

    public void setPattern(BasePattern pattern){
        if(ChatColorPlugin.getInstance().getMysqlConnection() == null) {
            SimpleYMLConfiguration data = ChatColorPlugin.getInstance().getConfigurationManager().getData();
            data.set("data." + player.getUniqueId(), pattern.getName(false));
            data.save();
        } else {
            try {

                Statement statement = ChatColorPlugin.getInstance().getMysqlConnection().createStatement();

                statement.execute(
                        formatQuery("INSERT INTO playerdata(uuid, pattern) VALUES('{uuid}', '{pattern}') ON DUPLICATE KEY UPDATE pattern= VALUES(pattern);", pattern)
                );

                statement.close();

            } catch (SQLException e){

                Bukkit.getServer().getConsoleSender().sendMessage(
                        Util.color(
                                formatQuery("&c[ChatColor] An error occurred while trying to set the pattern of {uuid} ({player}) via MySQL")
                                )
                );
                e.printStackTrace();

            }
        }
    }

    public void disablePattern(){
        ChatColorPlugin plugin = ChatColorPlugin.getInstance();
        if(plugin.getMysqlConnection() == null) {
            SimpleYMLConfiguration data = ChatColorPlugin.getInstance().getConfigurationManager().getData();
            data.set("data." + player.getUniqueId(), null);
            data.save();
        } else {
            try {

                if(plugin.getMysqlConnection().isClosed()){
                    plugin.openMysqlConnection();
                }

                Statement statement = ChatColorPlugin.getInstance().getMysqlConnection().createStatement();

                statement.execute(
                        formatQuery("DELETE FROM playerdata WHERE uuid = '{uuid}';")
                );

                statement.close();

            } catch (SQLException e){

                Bukkit.getServer().getConsoleSender().sendMessage(
                        Util.color(
                                formatQuery("&c[ChatColor] An error occurred while trying to remove the pattern from {uuid} ({player}) via MySQL")
                                )
                );
                e.printStackTrace();

            }
        }
    }

    public BasePattern getPattern(){
        String pattern = "";
        if(ChatColorPlugin.getInstance().getMysqlConnection() == null) {
            SimpleYMLConfiguration data = ChatColorPlugin.getInstance().getConfigurationManager().getData();
            pattern = data.getString("data." + player.getUniqueId());
        } else {
            try {

                Statement statement = ChatColorPlugin.getInstance().getMysqlConnection().createStatement();

                ResultSet resultSet = statement.executeQuery(
                        formatQuery("SELECT * FROM playerdata WHERE uuid = '{uuid}';")
                );

                while (resultSet.next()) {
                    pattern = resultSet.getString("pattern");
                }


            } catch (SQLException e){

                Bukkit.getServer().getConsoleSender().sendMessage(
                        Util.color(
                                formatQuery("&c[ChatColor] An error occurred while trying to get the pattern of {uuid} ({player}) via MySQL")
                                )
                );
                e.printStackTrace();

            }
        }
        return ChatColorPlugin.getInstance().getPatternManager().getPatternByName(pattern);
    }

    public boolean canUsePattern(BasePattern pattern){
        return (pattern.getPermission() == null || player.hasPermission(pattern.getPermission()));
    }

    public void setLastMessages(String lastMessages) {
        CPlayer.lastMessages.put(this.player.getUniqueId(), lastMessages);
    }

    public String getLastMessages() {
        String message = CPlayer.lastMessages.get(player.getUniqueId());
        return message != null ? message : "";
    }

    private String formatQuery(String string, BasePattern pattern){

        String uuid = player.getUniqueId().toString();
        String name = player.getName();

        string = pattern == null ? string : string.replaceAll("\\{pattern}", pattern.getName(false));

        return string
                .replaceAll("\\{uuid}", uuid)
                .replaceAll("\\{player}", name);

    }

    private String formatQuery(String string){
        return formatQuery(string, null);
    }

}
