package me.wiefferink.areashop.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.interfaces.WorldEditInterface;
import me.wiefferink.areashop.interfaces.WorldEditSelection;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.GeneralRegion;
import me.wiefferink.areashop.regions.RegionGroup;
import me.wiefferink.areashop.tools.Utils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

@Singleton
public class GroupdelCommand extends CommandAreaShop {

	private final IFileManager fileManager;
	private final WorldEditInterface worldEditInterface;

	@Inject
	public GroupdelCommand(
			@Nonnull MessageBridge messageBridge,
			@Nonnull IFileManager fileManager,
			@Nonnull WorldEditInterface worldEditInterface
	) {
		super(messageBridge);
		this.fileManager = fileManager;
		this.worldEditInterface = worldEditInterface;
	}


	@Override
	public String getCommandStart() {
		return "areashop groupdel";
	}

	@Override
	public String getHelp(CommandSender target) {
		if(target.hasPermission("areashop.groupdel")) {
			return "help-groupdel";
		}
		return null;
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		if(!sender.hasPermission("areashop.groupdel")) {
			messageBridge.message(sender, "groupdel-noPermission");
			return;
		}
		if(args.length < 2 || args[1] == null) {
			messageBridge.message(sender, "groupdel-help");
			return;
		}
		RegionGroup group = fileManager.getGroup(args[1]);
		if(group == null) {
			messageBridge.message(sender, "groupdel-wrongGroup", args[1]);
			return;
		}

		if(args.length == 2) {
			if(!(sender instanceof Player player)) {
				messageBridge.message(sender, "cmd-weOnlyByPlayer");
				return;
			}
			WorldEditSelection selection = worldEditInterface.getPlayerSelection(player);
			if(selection == null) {
				messageBridge.message(player, "cmd-noSelection");
				return;
			}
			List<GeneralRegion> regions = Utils.getRegionsInSelection(selection);
			if(regions.isEmpty()) {
				messageBridge.message(player, "cmd-noRegionsFound");
				return;
			}
			TreeSet<GeneralRegion> regionsSuccess = new TreeSet<>();
			TreeSet<GeneralRegion> regionsFailed = new TreeSet<>();
			for(GeneralRegion region : regions) {
				if(group.removeMember(region)) {
					regionsSuccess.add(region);
				} else {
					regionsFailed.add(region);
				}
			}
			if(!regionsSuccess.isEmpty()) {
				messageBridge.message(player, "groupdel-weSuccess", group.getName(), Utils.combinedMessage(regionsSuccess, "region"));
			}
			if(!regionsFailed.isEmpty()) {
				messageBridge.message(player, "groupdel-weFailed", group.getName(), Utils.combinedMessage(regionsFailed, "region"));
			}
			// Update all regions, this does it in a task, updating them without lag
			fileManager.updateRegions(regionsSuccess, player);
			group.saveRequired();
		} else {
			GeneralRegion region = fileManager.getRegion(args[2]);
			if(region == null) {
				messageBridge.message(sender, "cmd-notRegistered", args[2]);
				return;
			}
			if(group.removeMember(region)) {
				region.update();
				messageBridge.message(sender, "groupdel-success", group.getName(), group.getMembers().size(), region);
			} else {
				messageBridge.message(sender, "groupdel-failed", group.getName(), region);
			}
		}
	}

	@Override
	public List<String> getTabCompleteList(int toComplete, String[] start, CommandSender sender) {
		List<String> result = new ArrayList<>();
		if(toComplete == 2) {
			result = fileManager.getGroupNames();
		} else if(toComplete == 3) {
			result = fileManager.getRegionNames();
		}
		return result;
	}

}










