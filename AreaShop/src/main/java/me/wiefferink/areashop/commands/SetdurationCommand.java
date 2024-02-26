package me.wiefferink.areashop.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.RentRegion;
import me.wiefferink.areashop.tools.Utils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class SetdurationCommand extends CommandAreaShop {

	private final IFileManager fileManager;
	private final Plugin plugin;

	@Inject
	public SetdurationCommand(
			@Nonnull MessageBridge messageBridge,
			@Nonnull IFileManager fileManager,
			@Nonnull Plugin plugin) {
		super(messageBridge);
		this.fileManager = fileManager;
		this.plugin = plugin;
	}
	
	@Override
	public String getCommandStart() {
		return "areashop setduration";
	}

	@Override
	public String getHelp(CommandSender target) {
		if(target.hasPermission("areashop.setduration")) {
			return "help-setduration";
		}
		return null;
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		if(!sender.hasPermission("areashop.setduration") && (!sender.hasPermission("areashop.setduration.landlord") && sender instanceof Player)) {
			messageBridge.message(sender, "setduration-noPermission");
			return;
		}
		if(args.length < 3 || args[1] == null || args[2] == null) {
			messageBridge.message(sender, "setduration-help");
			return;
		}
		int regionArgument = 3;
		if(args.length >= 2 && ("default".equalsIgnoreCase(args[1]) || "reset".equalsIgnoreCase(args[1]))) {
			regionArgument = 2;
		}
		RentRegion rent;
		if(args.length <= regionArgument) {
			if(sender instanceof Player) {
				// get the region by location
				List<RentRegion> regions = Utils.getImportantRentRegions(((Player)sender).getLocation());
				if(regions.isEmpty()) {
					messageBridge.message(sender, "cmd-noRegionsAtLocation");
					return;
				} else if(regions.size() > 1) {
					messageBridge.message(sender, "cmd-moreRegionsAtLocation");
					return;
				} else {
					rent = regions.get(0);
				}
			} else {
				messageBridge.message(sender, "cmd-automaticRegionOnlyByPlayer");
				return;
			}
		} else {
			rent = fileManager.getRent(args[regionArgument]);
		}
		if(rent == null) {
			messageBridge.message(sender, "setduration-notRegistered", args[regionArgument]);
			return;
		}
		if(!sender.hasPermission("areashop.setduration") && !(sender instanceof Player && rent.isLandlord(((Player)sender).getUniqueId()))) {
			messageBridge.message(sender, "setduration-noLandlord", rent);
			return;
		}
		if("default".equalsIgnoreCase(args[1]) || "reset".equalsIgnoreCase(args[1])) {
			rent.setDuration(null);
			rent.update();
			messageBridge.message(sender, "setduration-successRemoved", rent);
			return;
		}
		try {
			Integer.parseInt(args[1]);
		} catch(NumberFormatException e) {
			messageBridge.message(sender, "setduration-wrongAmount", args[1], rent);
			return;
		}
		if(!Utils.checkTimeFormat(args[1] + " " + args[2])) {
			messageBridge.message(sender, "setduration-wrongFormat", args[1] + " " + args[2], rent);
			return;
		}
		rent.setDuration(args[1] + " " + args[2]);
		rent.update();
		messageBridge.message(sender, "setduration-success", rent);
	}

	@Override
	public List<String> getTabCompleteList(int toComplete, String[] start, CommandSender sender) {
		List<String> result = new ArrayList<>();
		if(toComplete == 3) {
			result.addAll(plugin.getConfig().getStringList("minutes"));
			result.addAll(plugin.getConfig().getStringList("hours"));
			result.addAll(plugin.getConfig().getStringList("days"));
			result.addAll(plugin.getConfig().getStringList("months"));
			result.addAll(plugin.getConfig().getStringList("years"));
		} else if(toComplete == 4) {
			result = fileManager.getRentNames();
		}
		return result;
	}

}
