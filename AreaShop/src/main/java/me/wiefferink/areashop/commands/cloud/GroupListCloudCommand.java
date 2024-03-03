package me.wiefferink.areashop.commands.cloud;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.tools.Utils;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.context.CommandContext;

import javax.annotation.Nonnull;
import java.util.List;

@Singleton
public class GroupListCloudCommand extends CloudCommandBean {

	private final MessageBridge messageBridge;
	private final IFileManager fileManager;

	@Inject
	public GroupListCloudCommand(@Nonnull MessageBridge messageBridge, @Nonnull IFileManager fileManager) {
		this.messageBridge = messageBridge;
		this.fileManager = fileManager;
	}

	@Override
	public String stringDescription() {
		return null;
	}

	@Override
	protected @Nonnull Command.Builder<? extends CommandSender> configureCommand(@Nonnull Command.Builder<CommandSender> builder) {
		return builder.literal("grouplist", "groups")
				.permission("areashop.grouplist")
				.handler(this::handleCommand);
	}

    @Override
    protected @Nonnull CommandProperties properties() {
		return CommandProperties.of("grouplist", "groups");
	}

	private void handleCommand(@Nonnull CommandContext<CommandSender> context) {
		List<String> groups = this.fileManager.getGroupNames();
		CommandSender sender = context.sender();
		if(groups.isEmpty()) {
			messageBridge.message(sender, "grouplist-noGroups");
		} else {
			messageBridge.message(sender, "grouplist-success", Utils.createCommaSeparatedList(groups));
		}
	}

}










