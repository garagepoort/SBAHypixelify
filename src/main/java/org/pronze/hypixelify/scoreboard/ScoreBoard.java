package org.pronze.hypixelify.scoreboard;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.pronze.hypixelify.Configurator;
import org.pronze.hypixelify.SBAHypixelify;
import org.pronze.hypixelify.arena.Arena;
import org.pronze.hypixelify.message.Messages;
import org.pronze.hypixelify.utils.ScoreboardUtil;
import org.screamingsandals.bedwars.Main;
import org.screamingsandals.bedwars.api.RunningTeam;
import org.screamingsandals.bedwars.api.Team;
import org.screamingsandals.bedwars.api.game.Game;
import org.screamingsandals.bedwars.api.game.GameStatus;
import org.screamingsandals.bedwars.api.statistics.PlayerStatistic;

import java.text.SimpleDateFormat;
import java.util.*;

public class ScoreBoard {

    private final Game game;
    private final Arena arena;
    private final String date;
    private final Map<String, String> teamstatus;
    private final List<String> animatedTitle;

    private int ticks = 0;
    private int currentTitlePos = 0;


    public ScoreBoard(Arena arena) {
        this.arena = arena;
        date = new SimpleDateFormat(Configurator.date).format(new Date());
        animatedTitle = SBAHypixelify.getConfigurator().config.getStringList("lobby-scoreboard.title");
        game = arena.getGame();
        teamstatus = new HashMap<>();
        new BukkitRunnable() {
            public void run() {
                if (game.getStatus() == GameStatus.RUNNING) {
                    ticks += 5;
                    updateCustomObj();
                    if (ticks % 20 == 0)
                        updateScoreboard();
                } else {
                    cancel();
                }
            }
        }.runTaskTimer(SBAHypixelify.getInstance(), 0L, 5L);
    }

    public void updateCustomObj() {
        for (Player pl : game.getConnectedPlayers()) {
            ScoreboardUtil.updateCustomObjective(pl, game);
        }
    }

    public void updateScoreboard() {
        List<String> scoreboard_lines;
        final List<String> lines = new ArrayList<>();

        if(currentTitlePos >= animatedTitle.size())
            currentTitlePos = 0;

        final String currentTitle  = animatedTitle.get(currentTitlePos);

        if (game.countAvailableTeams() >= 5 && Configurator.Scoreboard_Lines.containsKey("5")) {
            scoreboard_lines = Configurator.Scoreboard_Lines.get("5");
        } else if (Configurator.Scoreboard_Lines.containsKey("default")) {
            scoreboard_lines = Configurator.Scoreboard_Lines.get("default");
        } else {
            scoreboard_lines = Arrays.asList("", "{team_status}", "");
        }

        for (Player player : game.getConnectedPlayers()) {
            ChatColor chatColor = null;
            final RunningTeam playerTeam = game.getTeamOfPlayer(player);
            lines.clear();
            String totalKills;
            String currentKills;
            String finalKills;
            String currentDeaths;
            String currentBedDestroys;

            final PlayerStatistic statistic = Main.getPlayerStatisticsManager().getStatistic(player);
            try {
                totalKills = String.valueOf(statistic.getCurrentKills() + statistic.getKills());
                currentKills = String.valueOf(statistic.getCurrentKills());
                finalKills = String.valueOf(statistic.getKills());
                currentDeaths = String.valueOf(statistic.getCurrentDeaths());
                currentBedDestroys = String.valueOf(statistic.getCurrentDestroyedBeds());
            } catch (Throwable e) {
                final String nullscore = "0";
                totalKills = nullscore;
                currentKills = nullscore;
                finalKills = nullscore;
                currentDeaths = nullscore;
                currentBedDestroys = nullscore;
            }

            String teamSize = "";
            String teamName = "";
            String teamStatus = "";

            if (playerTeam != null && playerTeam.countConnectedPlayers() > 0) {
                chatColor = org.screamingsandals.bedwars.game.TeamColor.valueOf(playerTeam.getColor().name()).chatColor;
                teamSize = String.valueOf(playerTeam.getConnectedPlayers().size());
                teamName = playerTeam.getName();
                teamStatus = getTeamBedStatus(playerTeam);
            }


            for (String ls : scoreboard_lines) {
                if (ls.contains("{team_status}")) {
                    for (Team t : game.getAvailableTeams()) {
                        String you = "";
                        if (playerTeam != null)
                            if (playerTeam.getName().equals(t.getName())) {
                                you = Messages.message_you;
                            } else {
                                you = "";
                            }
                        if (teamstatus.containsKey(t.getName())) {
                            lines.add(teamstatus.get(t.getName()).replace("{you}", you));
                            continue;
                        }
                        lines.add(ls.replace("{team_status}",
                                getTeamStatusFormat(t).replace("{you}", you)));
                    }
                    continue;
                }

                String addline = ls
                        .replace("{team}", teamName).replace("{beds}", currentBedDestroys).replace("{dies}", currentDeaths)
                        .replace("{totalkills}", totalKills).replace("{finalkills}", finalKills).replace("{kills}", currentKills)
                        .replace("{time}", Main.getGame(game.getName()).getFormattedTimeLeft())
                        .replace("{formattime}", Main.getGame(game.getName()).getFormattedTimeLeft())
                        .replace("{game}", game.getName()).replace("{date}", date)
                        .replace("{team_bed_status}", teamStatus)
                        .replace("{tier}", arena.gameTask.getTier().replace("-", " ")
                                + " in §a" + arena.gameTask.getFormattedTimeLeft());

                if (game.isPlayerInAnyTeam(player) && chatColor != null) {
                    addline = addline.replace("{team_peoples}", teamSize).replace("{player_name}", player.getName())
                            .replace("{teams}", String.valueOf(game.getRunningTeams().size())).replace("{color}", chatColor.toString());
                }

                for (RunningTeam t : game.getRunningTeams()) {
                    if (addline.contains("{team_" + t.getName() + "_status}")) {
                        String stf = getTeamStatusFormat(t);
                        if (game.getTeamOfPlayer(player) == null) {
                            stf = stf.replace("{you}", "");
                        } else if (game.getTeamOfPlayer(player) == t) {
                            stf = stf.replace("{you}", Messages.message_you);
                        } else {
                            stf = stf.replace("{you}", "");
                        }
                        addline = addline.replace("{team_" + t.getName() + "_status}", stf);
                    }
                    if (addline.contains("{team_" + t.getName() + "_bed_status}"))
                        addline = addline.replace("{team_" + t.getName() + "_bed_status}",
                                getTeamBedStatus(t));
                    if (addline.contains("{team_" + t.getName() + "_peoples}"))
                        addline = addline.replace("{team_" + t.getName() + "_peoples}", String.valueOf(t.getConnectedPlayers().size()));
                }
                if (lines.contains(addline)) {
                    lines.add(conflict(lines, addline));
                    continue;
                }
                lines.add(addline);
            }
            String title = currentTitle;
            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI"))
                title = PlaceholderAPI.setPlaceholders(player, title);
            List<String> elements = new ArrayList<>();
            elements.add(title);
            elements.addAll(lines);
            if (elements.size() < 16) {
                int es = elements.size();
                for (int i = 0; i < 16 - es; i++)
                    elements.add(1, null);
            }
            List<String> ncelements = elementsPro(elements);
            String[] scoreboardelements = ncelements.toArray(new String[0]);
            ScoreboardUtil.setGameScoreboard(player, scoreboardelements, this.game);

        }
    }


    private List<String> elementsPro(List<String> lines) {
        ArrayList<String> nclines = new ArrayList<>();
        for (String ls : lines) {
            String l = ls;
            if (l != null) {
                if (nclines.contains(l)) {
                    for (int i = 0; i == 0; ) {
                        l = l + "§r";
                        if (!nclines.contains(l)) {
                            nclines.add(l);
                            break;
                        }
                    }
                    continue;
                }
                nclines.add(l);
                continue;
            }
            nclines.add(l);
        }
        return nclines;
    }

    private String conflict(List<String> lines, String line) {
        String l = line;
        for (int i = 0; i == 0; ) {
            l = l + "§r";
            if (!lines.contains(l))
                return l;
        }
        return l;
    }


    private String getTeamBedStatus(RunningTeam team) {
        return team.isDead() ? "§c\u2717" :
                "§a\u2713";
    }

    private String getTeamStatusFormat(RunningTeam team) {
        String alive = "{color} {team} §a\u2713 §8{you}";
        String destroyed = "{color} {team} §a§f{players}§8 {you}";

        String status = team.isTargetBlockExists() ? alive : destroyed;

        if (team.isDead() && team.getConnectedPlayers().size() <= 0)
            status = "{color} {team} §c\u2717 {you}";

        String formattedTeam = org.screamingsandals.bedwars.game.TeamColor.valueOf(team.getColor().name()).chatColor.toString()
                + team.getName().charAt(0);
        return status.replace("{bed_status}", getTeamBedStatus(team))
                .replace("{color}", formattedTeam)
                .replace("{team}", ChatColor.WHITE.toString() + team.getName() + ":")
                .replace("{players}", ChatColor.GREEN.toString() + team.getConnectedPlayers().size());
    }

    private String getTeamStatusFormat(Team team) {
        boolean alive = false;
        RunningTeam rt = null;

        for (RunningTeam t : game.getRunningTeams()) {
            if (t.getName().equalsIgnoreCase(team.getName())) {
                alive = true;
                rt = t;
            }
        }

        if (alive) {
            return getTeamStatusFormat(rt);
        }

        String destroyed = "{color} {team} §c\u2718 {you}";


        String formattedTeam = org.screamingsandals.bedwars.game.TeamColor.valueOf(team.getColor().name()).chatColor.toString()
                + team.getName().charAt(0);

        return destroyed.replace("{bed_status}", "§c\u2718")
                .replace("{color}", formattedTeam)
                .replace("{team}", ChatColor.WHITE.toString() + team.getName() + ":");
    }
}