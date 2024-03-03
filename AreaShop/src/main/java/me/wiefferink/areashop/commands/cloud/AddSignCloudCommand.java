package me.wiefferink.areashop.commands.cloud;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.commands.util.RegionFlagUtil;
import me.wiefferink.areashop.commands.util.SignProfileUtil;
import me.wiefferink.areashop.features.signs.RegionSign;
import me.wiefferink.areashop.features.signs.SignManager;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.GeneralRegion;
import me.wiefferink.areashop.tools.Materials;
import me.wiefferink.areashop.tools.SignUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BlockIterator;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.flag.CommandFlag;

import javax.annotation.Nonnull;
import java.util.Optional;

@Singleton
public class AddSignCloudCommand extends CloudCommandBean {
	private final SignManager signManager;
	private final Plugin plugin;

	private final MessageBridge messageBridge;

	private final CommandFlag<GeneralRegion> regionFlag;

    @Inject
    public AddSignCloudCommand(
            @Nonnull MessageBridge messageBridge,
            @Nonnull IFileManager fileManager,
            @Nonnull SignManager signManager,
            @Nonnull Plugin plugin
    ) {
		this.messageBridge = messageBridge;
        this.signManager = signManager;
        this.plugin = plugin;
		this.regionFlag = RegionFlagUtil.createDefault(fileManager);
    }

	@Override
	public String stringDescription() {
		return "Allows you to add signs to existing regions";
	}

	@Override
	protected @Nonnull Command.Builder<? extends CommandSender> configureCommand(@Nonnull Command.Builder<CommandSender> builder) {
		return builder.literal("addsign")
				.senderType(Player.class)
				.permission("areashop.addsign")
				.flag(this.regionFlag)
				.flag(SignProfileUtil.DEFAULT_FLAG)
				.handler(this::handleCommand);
	}

	private void handleCommand(@Nonnull CommandContext<Player> context) {
		Player sender = context.sender();
		// Get the sign
		Block block = null;
		BlockIterator blockIterator = new BlockIterator(sender, 100);
		while(blockIterator.hasNext() && block == null) {
			Block next = blockIterator.next();
			if(next.getType() != Material.AIR) {
				block = next;
			}
		}
		if(block == null || !Materials.isSign(block.getType())) {
			messageBridge.message(sender, "addsign-noSign");
			return;
		}

		GeneralRegion region = RegionFlagUtil.getOrParseRegion(context, this.regionFlag);
		String profile = SignProfileUtil.getOrParseProfile(context, this.plugin);
		Optional<RegionSign> optionalRegionSign = this.signManager.signFromLocation(block.getLocation());
		if(optionalRegionSign.isPresent()) {
			RegionSign regionSign = optionalRegionSign.get();
			messageBridge.message(sender, "addsign-alreadyRegistered", regionSign.getRegion());
			return;
		}

		region.getSignsFeature().addSign(block.getLocation(), block.getType(), SignUtils.getSignFacing(block), profile);
		if(profile == null) {
			messageBridge.message(sender, "addsign-success", region);
		} else {
			messageBridge.message(sender, "addsign-successProfile", profile, region);
		}
		region.update();
	}


    @Override
    protected @Nonnull CommandProperties properties() {
		return CommandProperties.of("addsign");
	}
}










