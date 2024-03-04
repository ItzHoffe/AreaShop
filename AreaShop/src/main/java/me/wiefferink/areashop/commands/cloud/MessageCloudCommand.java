package me.wiefferink.areashop.commands.cloud;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.commands.CommandAreaShop;
import me.wiefferink.areashop.tools.SimpleMessageBridge;
import me.wiefferink.areashop.tools.Utils;
import me.wiefferink.interactivemessenger.processing.Message;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.bukkit.parser.PlayerParser;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.parser.flag.CommandFlag;
import org.incendo.cloud.parser.standard.StringParser;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class MessageCloudCommand extends CloudCommandBean {

	private static final CloudKey<Player> KEY_PLAYER = CloudKey.of("player", Player.class);
	private static final CloudKey<String> KEY_MESSAGE = CloudKey.of("message", String.class);

	private final MessageBridge messageBridge;

	@Inject
	public MessageCloudCommand(@Nonnull MessageBridge messageBridge) {
		this.messageBridge = messageBridge;
	}

	public String getHelp(CommandSender target) {
		// Internal command, no need to show in the help list
		return null;
	}

	@Override
	public String stringDescription() {
		return null;
	}

	@NotNull
	@Override
	protected Command.Builder<? extends CommandSender> configureCommand(@NotNull Command.Builder<CommandSender> builder) {
		return builder.literal("message")
				.permission("areashop.message")
				.required(KEY_PLAYER, PlayerParser.playerParser())
				.required(KEY_MESSAGE, StringParser.greedyStringParser())
				.handler(this::handleCommand);
	}

	@Override
	protected @NonNull CommandProperties properties() {
		return CommandProperties.of("message");
	}

	private void handleCommand(@Nonnull CommandContext<CommandSender> context) {
		CommandSender sender = context.sender();
		if(!sender.hasPermission("areashop.message")) {
			this.messageBridge.message(sender, "message-noPermission");
			return;
		}
		Player player = context.get(KEY_PLAYER);
		String message = context.get(KEY_MESSAGE);
		Message m = Message.fromString(message);
		SimpleMessageBridge.send(m, player);
	}

}










