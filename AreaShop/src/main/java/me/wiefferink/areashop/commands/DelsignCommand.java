package me.wiefferink.areashop.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.features.signs.RegionSign;
import me.wiefferink.areashop.features.signs.SignManager;
import me.wiefferink.areashop.tools.Materials;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Singleton
public class DelsignCommand extends CommandAreaShop {

	private final SignManager signManager;
	@Inject
	public DelsignCommand(@Nonnull MessageBridge messageBridge, @Nonnull SignManager signManager) {
		super(messageBridge);
		this.signManager = signManager;
	}
	@Override
	public String getCommandStart() {
		return "areashop delsign";
	}

	@Override
	public String getHelp(CommandSender target) {
		if(target.hasPermission("areashop.delsign")) {
			return "help-delsign";
		}
		return null;
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		if(!sender.hasPermission("areashop.delsign")) {
			messageBridge.message(sender, "delsign-noPermission");
			return;
		}
		if(!(sender instanceof Player player)) {
			messageBridge.message(sender, "cmd-onlyByPlayer");
			return;
		}

		// Get the sign
		Block block = null;
		BlockIterator blockIterator = new BlockIterator(player, 100);
		while(blockIterator.hasNext() && block == null) {
			Block next = blockIterator.next();
			if(!next.getType().isAir()) {
				block = next;
			}
		}
		if(block == null || !Materials.isSign(block.getType())) {
			messageBridge.message(sender, "delsign-noSign");
			return;
		}
		Optional<RegionSign> optionalSign = signManager.removeSign(block.getLocation());
		if(optionalSign.isEmpty()) {
			messageBridge.message(sender, "delsign-noRegion");
			return;
		}
		RegionSign regionSign = optionalSign.get();
		messageBridge.message(sender, "delsign-success", regionSign.getRegion());
		regionSign.remove();
		// Sometimes the RegionSign data is corrupted. Forcefully set the block to air
		block.setType(Material.AIR);
	}

	@Override
	public List<String> getTabCompleteList(int toComplete, String[] start, CommandSender sender) {
		return new ArrayList<>();
	}

}










