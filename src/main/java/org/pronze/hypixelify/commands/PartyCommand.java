package org.pronze.hypixelify.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.pronze.hypixelify.SBAHypixelify;
import org.pronze.hypixelify.api.database.PlayerDatabase;
import org.pronze.hypixelify.api.party.Party;
import org.pronze.hypixelify.api.party.PartyManager;
import org.pronze.hypixelify.manager.DatabaseManager;
import org.pronze.hypixelify.message.Messages;
import org.pronze.hypixelify.utils.ShopUtil;

import java.util.Arrays;
import java.util.List;

public class PartyCommand extends AbstractCommand {

    public PartyCommand() {
        super(null, false, "party");
    }

    @Override
    public boolean onPreExecute(CommandSender sender, String[] args) {
        if (!SBAHypixelify.getConfigurator().config.getBoolean("party.enabled", true)) {
            sender.sendMessage("§cCannot access command, party system is disabled.");
            return false;
        }
        return true;
    }

    @Override
    public void onPostExecute() {

    }

    @Override
    public void execute(String[] args, CommandSender sender) {
        Player player = (Player) sender;

        if (args == null || args.length == 0 || args.length > 2) {
            ShopUtil.sendMessage(player, Messages.message_invalid_command);
            return;
        }

        final DatabaseManager databaseManager = SBAHypixelify.getDatabaseManager();
        PlayerDatabase playerDatabase = databaseManager.getDatabase(player);
        final PartyManager partyManager = SBAHypixelify.getPartyManager();


        if (playerDatabase != null && playerDatabase.isInParty()
                && playerDatabase.getPartyLeader() != null
                && !playerDatabase.getPartyLeader().isOnline()) {
            player.sendMessage("§cPlease wait until the leader comes back online..., or party disbands");
            return;
        }

        else if (args[0].equalsIgnoreCase("invite")) {
            if (args.length != 2) {
                ShopUtil.sendMessage(player, Messages.message_invalid_command);
                return;
            }

            //Party size limit
            int max_sz = SBAHypixelify.getConfigurator().config.getInt("party.size", 4);
            if (playerDatabase != null &&
                    playerDatabase.isInParty() &&
                    partyManager.getParty(playerDatabase.getPartyLeader()) != null &&
                    partyManager.getParty(playerDatabase.getPartyLeader()).getCompleteSize() >= max_sz) {
                player.sendMessage("§cParty has reached maximum Size.");
                return;
            }


            //check if player argument is online
            Player invited = Bukkit.getPlayerExact(args[1].toLowerCase());
            if (invited == null) {
                ShopUtil.sendMessage(player, Messages.message_player_not_found);
                return;
            }


            if (invited.getUniqueId().equals(player.getUniqueId())) {
                ShopUtil.sendMessage(player, Messages.message_cannot_invite_yourself);
                return;
            }

            if (playerDatabase == null)
                SBAHypixelify.getDatabaseManager().createDatabase(player);

            PlayerDatabase invitedData = databaseManager.getDatabase(invited);
            if (invitedData == null)
                databaseManager.createDatabase(invited);

            if (databaseManager.getDatabase(player).isInvited()) {
                ShopUtil.sendMessage(player, Messages.message_decline_inc);
                return ;
            }

            Party party;
            if (!playerDatabase.isInParty()) {
                party = partyManager.getParty(player);
                if (party == null) {
                    party = partyManager.createParty(player);
                    playerDatabase.setIsInParty(true);
                    playerDatabase.setPartyLeader(player);
                }
            } else {
                party = partyManager.getParty(player);
            }

            if (invitedData.isInParty()) {
                ShopUtil.sendMessage(player, Messages.message_cannot_invite);
                return ;
            }

            if (invitedData.isInvited()) {
                ShopUtil.sendMessage(player, Messages.message_already_invited);
                return ;
            }

            if (party.canAnyoneInvite() || player.equals(party.getLeader())) {
                partyManager.getParty(party.getLeader()).addInvitedMember(invited);
                for (String message : SBAHypixelify.getConfigurator().config.getStringList("party.message.invite")) {
                    invited.sendMessage(ShopUtil.translateColors(message).replace("{player}", player.getDisplayName()));
                }
                for (String message : SBAHypixelify.getConfigurator().config.getStringList("party.message.invited")) {
                    player.sendMessage(ShopUtil.translateColors(message).replace("{player}", invited.getDisplayName()));
                }
                return ;
            }

        }

        //check if player does not do other commands on his newly created party.
        else if (playerDatabase != null && playerDatabase.isInParty() &&
                partyManager.getParty(playerDatabase.getPartyLeader()) != null &&
                partyManager.getParty(player).getPlayers() == null) {
            ShopUtil.sendMessage(player, Messages.message_no_other_commands);
            return ;
        } else if (args[0].equalsIgnoreCase("accept")) {

            if (playerDatabase == null) {
                player.sendMessage("§cAn error has occurred..");
                return ;
            }

            if (!playerDatabase.isInvited()) {
                ShopUtil.sendMessage(player, Messages.message_not_invited);
                return ;
            }

            if (playerDatabase.isInParty() || playerDatabase.getInvitedParty() == null) {
                ShopUtil.sendMessage(player, Messages.message_invalid_command);
                return ;
            }

            Party pParty = playerDatabase.getInvitedParty();
            partyManager.addToParty(player, pParty);
            return ;

        } else if (args[0].equalsIgnoreCase("leave")) {

            if (args.length != 1) {
                ShopUtil.sendMessage(player, Messages.message_invalid_command);
                return ;
            }

            if (playerDatabase == null) return ;

            if (!playerDatabase.isInParty()) {
                ShopUtil.sendMessage(player, Messages.message_not_in_party);
                return ;
            }
            if (playerDatabase.isPartyLeader()) {
                player.sendMessage("§cYou have to disband the party first!");
                return ;
            }

            if (playerDatabase.getPartyLeader() == null || partyManager.getParty(playerDatabase.getPartyLeader()) == null)
                return ;

            Party party = partyManager.getParty(playerDatabase.getPartyLeader());
            if (party == null) return ;
            partyManager.removeFromParty(player, party);
            ShopUtil.sendMessage(player, Messages.message_party_left);
        } else if (args[0].equalsIgnoreCase("decline")) {

            if (playerDatabase == null) return ;

            if (!playerDatabase.isInvited()) {
                ShopUtil.sendMessage(player, Messages.message_not_invited);
                return ;
            }

            Party invitedParty = playerDatabase.getInvitedParty();

            if (invitedParty == null || invitedParty.getLeader() == null) return ;

            partyManager.getParty(invitedParty.getLeader()).removeInvitedMember(player);

            ShopUtil.sendMessage(player, Messages.message_decline_user);

            if (invitedParty.getAllPlayers() != null && !invitedParty.getAllPlayers().isEmpty()) {
                for (Player pl : invitedParty.getAllPlayers()) {
                    if (pl != null && pl.isOnline()) {
                        for (String st : Messages.message_declined) {
                            pl.sendMessage(ShopUtil.translateColors(st.replace("{player}", player.getDisplayName())));
                        }
                    }
                }
            }
            playerDatabase.setInvited(false);
            playerDatabase.setExpiredTimeTimeout(60);
            playerDatabase.setInvitedParty(null);
            return ;

        } else if (args[0].equalsIgnoreCase("list")) {

            if (playerDatabase == null) return ;
            if (!playerDatabase.isInParty()) {
                ShopUtil.sendMessage(player, Messages.message_not_in_party);
                return;
            }

            Player leader = playerDatabase.getPartyLeader();
            if (leader == null) return ;
            player.sendMessage("Players: ");
            if (partyManager.getParty(leader).getPlayers() == null) return ;
            for (Player pl : partyManager.getParty(leader).getAllPlayers()) {
                player.sendMessage(pl.getDisplayName());
            }
            return ;
        } else if (args[0].equalsIgnoreCase("disband")) {

            if (playerDatabase == null || partyManager.getParty(playerDatabase.getPartyLeader()) == null) {
                ShopUtil.sendMessage(player, Messages.message_not_in_party);
                return ;
            }

            if (!playerDatabase.getPartyLeader().equals(player)) {
                ShopUtil.sendMessage(player, Messages.message_access_denied);
                return ;
            }
            partyManager.disband(player);
            return ;
        } else if (args[0].equalsIgnoreCase("kick")) {
            if (!playerDatabase.isInParty() || playerDatabase.getPartyLeader() == null) {
                ShopUtil.sendMessage(player, Messages.message_not_in_party);
                return ;
            }

            if (!playerDatabase.getPartyLeader().equals(player)) {
                ShopUtil.sendMessage(player, Messages.message_access_denied);
                return ;
            }
            if (args.length != 2) {
                ShopUtil.sendMessage(player, Messages.message_invalid_command);
                return ;
            }

            Player invited = Bukkit.getPlayerExact(args[1].toLowerCase());
            if (invited == null) {
                ShopUtil.sendMessage(player, Messages.message_player_not_found);
                return ;
            }

            if (invited.equals(player)) {
                for (String str : SBAHypixelify.getConfigurator().config.getStringList("party.message.cannot-blank-yourself")) {
                    player.sendMessage(ShopUtil.translateColors(str).replace("{blank}", "kick"));
                }
                return ;
            }

            if (partyManager.getParty(player).getPlayers() == null ||
                    partyManager.getParty(player).getPlayers().isEmpty()) return ;

            if (!partyManager.getParty(player).getAllPlayers().contains(invited)) {
                ShopUtil.sendMessage(player, Messages.message_player_not_found);
                return ;
            }

            partyManager.kickFromParty(invited);
            return ;

        } else if (args[0].equalsIgnoreCase("warp")) {

            if (args.length != 1) {
                ShopUtil.sendMessage(player, Messages.message_invalid_command);
                return ;
            }

            if (!player.isOnline()) {
                return ;
            }

            if (!partyManager.isInParty(player)) {
                ShopUtil.sendMessage(player, Messages.message_not_in_party);
                return ;
            }

            if (partyManager.getParty(player) == null) {
                player.sendMessage("§cAn error has occured");
                return ;
            }

            if (playerDatabase == null || playerDatabase.getPartyLeader() == null) {
                player.sendMessage("§cSomething went wrong, reload Addon or BedWars to fix this issue");
                return ;
            }
            if (!playerDatabase.getPartyLeader().equals(player)) {
                ShopUtil.sendMessage(player, Messages.message_access_denied);
                return ;
            }
            partyManager.warpPlayersToLeader(player);

        } else if (args[0].equalsIgnoreCase("chat")) {

            if (playerDatabase == null) return ;

            if (!playerDatabase.isInParty()) {
                ShopUtil.sendMessage(player, Messages.message_not_in_party);
                return ;
            }

            if (args.length != 2 || (!args[1].equalsIgnoreCase("on") && !args[1].equalsIgnoreCase("off"))) {
                ShopUtil.sendMessage(player, Messages.message_invalid_command);
                return ;
            }

            playerDatabase.setPartyChatEnabled(args[1].equalsIgnoreCase("on"));

            String mode = args[1].equals("on") ? "enabled" : "disabled";

            for (String st : SBAHypixelify.getConfigurator().config.getStringList("party.message.chat-enable-disabled")) {
                player.sendMessage(ShopUtil.translateColors(st).replace("{mode}", mode));
            }
            return ;

        } else {
            ShopUtil.sendMessage(player, Messages.message_invalid_command);
            return ;
        }
    }

    @Override
    public void displayHelp(CommandSender sender) {
        Player player = (Player) sender;
        ShopUtil.sendMessage(player, Messages.message_party_help);
    }

    @Override
    public List<String> tabCompletion(String[] strings, CommandSender commandSender) {
        if (!(commandSender instanceof Player))
            return null;
        Player player = (Player) commandSender;

        final PlayerDatabase playerDatabase = SBAHypixelify.getDatabaseManager().getDatabase(player);

        if (playerDatabase != null && playerDatabase.isInvited()) {
            return Arrays.asList("accept", "decline");
        }
        if (strings.length == 1) {
            if (playerDatabase != null && playerDatabase.isInParty()
                    && playerDatabase.getPartyLeader().equals(player))
                return Arrays.asList("invite", "list", "disband", "kick", "warp");


            return Arrays.asList("invite", "list", "help", "chat");
        }

        if (strings.length == 2 && strings[0].equalsIgnoreCase("chat")) {
            return Arrays.asList("on", "off");
        }

        return null;
    }
}
