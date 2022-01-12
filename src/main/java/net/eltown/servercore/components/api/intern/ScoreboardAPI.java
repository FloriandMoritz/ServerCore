package net.eltown.servercore.components.api.intern;

import net.eltown.servercore.ServerCore;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashMap;

public record ScoreboardAPI(ServerCore serverCore) {

    public static HashMap<String, Scoreboard> cachedScoreboards = new HashMap<>();

}
