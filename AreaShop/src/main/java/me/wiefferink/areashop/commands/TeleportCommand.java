package me.wiefferink.areashop.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.GeneralRegion;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class TeleportCommand extends CommandAreaShop {

	private final IFileManager fileManager;

	@Inject
	public TeleportCommand(@Nonnull MessageBridge messageBridge, @Nonnull IFileManager fileManager) {
		super(messageBridge);
		this.fileManager = fileManager;
	}
	
	@Override
	public String getCommandStart() {
		return "areashop tp";
	}

	@Override
	public String getHelp(CommandSender target) {
		if(target.hasPermission("areashop.teleportall") || target.hasPermission("areashop.teleport")) {
			return "help-teleport";
		}
		return null;
	}

	/**
	 * Check if a person can teleport to the region (assuming he is not teleporting to a sign).
	 * @param person The person to check
	 * @param region The region to check for
	 * @return true if the person can teleport to it, otherwise false
	 */
	public static boolean canUse(CommandSender person, GeneralRegion region) {
		if(!(person instanceof Player player)) {
			return false;
		}
		return player.hasPermission("areashop.teleportall")
				|| region.isOwner(player) && player.hasPermission("areashop.teleport")
				|| region.isAvailable() && player.hasPermission("areashop.teleportavailable")
				|| region.getFriendsFeature().getFriends().contains(player.getUniqueId()) && player.hasPermission("areashop.teleportfriend");
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		if(!sender.hasPermission("areashop.teleport") && !sender.hasPermission("areashop.teleportall") && !sender.hasPermission("areashop.teleportavailable") && !sender.hasPermission("areashop.teleportavailablesign") && !sender.hasPermission("areashop.teleportsign") && !sender.hasPermission("areashop.teleportsignall") && !sender.hasPermission("areashop.teleportfriend") && !sender.hasPermission("teleportfriendsign")) {
			messageBridge.message(sender, "teleport-noPermission");
			return;
		}
		if(!(sender instanceof Player player)) {
			messageBridge.message(sender, "cmd-onlyByPlayer");
			return;
		}
		if(args.length <= 1 || args[1] == null) {
			messageBridge.message(sender, "teleport-help");
			return;
		}
		GeneralRegion region = fileManager.getRegion(args[1]);
		if(region == null) {
			messageBridge.message(player, "teleport-noRentOrBuy", args[1]);
			return;
		}

		boolean toSign = args.length >= 3 && (
				args[2].equalsIgnoreCase("sign")
						|| args[2].equalsIgnoreCase("yes")
						|| args[2].equalsIgnoreCase("true")
		);
		region.getTeleportFeature().teleportPlayer(player, toSign);
	}

	@Override
	public List<String> getTabCompleteList(int toComplete, String[] start, CommandSender sender) {
		ArrayList<String> result = new ArrayList<>();
		if(toComplete == 2) {
			result.addAll(fileManager.getRegionNames());
		} else if(toComplete == 3) {
			result.add("sign");
		}
		return result;
	}

}
