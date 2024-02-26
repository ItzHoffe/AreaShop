package me.wiefferink.areashop.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.RentRegion;
import me.wiefferink.areashop.tools.Utils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class RentCommand extends CommandAreaShop {

	private final IFileManager fileManager;

	@Inject
	public RentCommand(@Nonnull MessageBridge messageBridge, @Nonnull IFileManager fileManager) {
		super(messageBridge);
		this.fileManager = fileManager;
	}
	
	@Override
	public String getCommandStart() {
		return "areashop rent";
	}

	@Override
	public String getHelp(CommandSender target) {
		if(target.hasPermission("areashop.rent")) {
			return "help-rent";
		}
		return null;
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		if(!sender.hasPermission("areashop.rent")) {
			messageBridge.message(sender, "rent-noPermission");
			return;
		}
		if(!(sender instanceof Player player)) {
			messageBridge.message(sender, "cmd-onlyByPlayer");
			return;
		}
		if(args.length > 1 && args[1] != null) {
			RentRegion rent = fileManager.getRent(args[1]);
			if(rent == null) {
				messageBridge.message(sender, "rent-notRentable", args[1]);
			} else {
				rent.rent(player);
			}
		} else {
			// get the region by location
			List<RentRegion> regions = Utils.getImportantRentRegions(((Player)sender).getLocation());
			if(regions.isEmpty()) {
				messageBridge.message(sender, "cmd-noRegionsAtLocation");
			} else if(regions.size() > 1) {
				messageBridge.message(sender, "cmd-moreRegionsAtLocation");
			} else {
				regions.get(0).rent(player);
			}
		}
	}

	@Override
	public List<String> getTabCompleteList(int toComplete, String[] start, CommandSender sender) {
		ArrayList<String> result = new ArrayList<>();
		if(toComplete == 2) {
			for(RentRegion region : fileManager.getRentsRef()) {
				if(!region.isRented()) {
					result.add(region.getName());
				}
			}
		}
		return result;
	}
}
