package me.wiefferink.areashop.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.RegionGroup;
import me.wiefferink.areashop.tools.Utils;
import org.bukkit.command.CommandSender;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Singleton
public class GroupinfoCommand extends CommandAreaShop {

	private final IFileManager fileManager;

	@Inject
	public GroupinfoCommand(@Nonnull MessageBridge messageBridge, @Nonnull IFileManager fileManager) {
		super(messageBridge);
		this.fileManager = fileManager;
	}
	
	@Override
	public String getCommandStart() {
		return "areashop groupinfo";
	}

	@Override
	public String getHelp(CommandSender target) {
		if(target.hasPermission("areashop.groupinfo")) {
			return "help-groupinfo";
		}
		return null;
	}


	@Override
	public void execute(CommandSender sender, String[] args) {
		if(!sender.hasPermission("areashop.groupinfo")) {
			messageBridge.message(sender, "groupinfo-noPermission");
			return;
		}
		if(args.length < 2 || args[1] == null) {
			messageBridge.message(sender, "groupinfo-help");
			return;
		}
		RegionGroup group = fileManager.getGroup(args[1]);
		if(group == null) {
			messageBridge.message(sender, "groupinfo-noGroup", args[1]);
			return;
		}
		Set<String> members = group.getMembers();
		if(members.isEmpty()) {
			messageBridge.message(sender, "groupinfo-noMembers", group.getName());
		} else {
			messageBridge.message(sender, "groupinfo-members", group.getName(), Utils.createCommaSeparatedList(members));
		}
	}

	@Override
	public List<String> getTabCompleteList(int toComplete, String[] start, CommandSender sender) {
		List<String> result = new ArrayList<>();
		if(toComplete == 2) {
			result = fileManager.getGroupNames();
		}
		return result;
	}

}










