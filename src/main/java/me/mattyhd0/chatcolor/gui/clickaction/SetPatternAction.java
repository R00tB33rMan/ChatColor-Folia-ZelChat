package me.mattyhd0.chatcolor.gui.clickaction;

import me.mattyhd0.chatcolor.CPlayer;
import me.mattyhd0.chatcolor.ChatColorPlugin;
import me.mattyhd0.chatcolor.gui.clickaction.api.GuiClickAction;
import me.mattyhd0.chatcolor.pattern.api.BasePattern;
import org.bukkit.entity.Player;

public class SetPatternAction implements GuiClickAction {

    private final String patternName;

    public SetPatternAction(String patternName){
        this.patternName = patternName;
    }

    @Override
    public void execute(Player player) {

        BasePattern pattern = ChatColorPlugin.getInstance().getPatternManager().getPatternByName(patternName);
        CPlayer cPlayer = (player != null) ? ChatColorPlugin.getInstance().getDataMap().get(player.getUniqueId()) : null;
        if(cPlayer != null) cPlayer.setPattern(pattern);
    }
}
