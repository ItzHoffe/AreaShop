package me.wiefferink.areashop.commands.cloud;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.commands.util.AreaShopCommandException;
import me.wiefferink.areashop.commands.util.RegionFlagUtil;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.GeneralRegion;
import me.wiefferink.areashop.regions.RegionFactory;
import me.wiefferink.areashop.regions.RegionGroup;
import me.wiefferink.areashop.tools.Utils;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.parser.flag.CommandFlag;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;

@Singleton
public class GroupAddCloudCommand extends CloudCommandBean {

    private static final CloudKey<String> KEY_GROUP = CloudKey.of("group", String.class);

    private final IFileManager fileManager;
    private final RegionFactory regionFactory;
    private final MessageBridge messageBridge;
    private final CommandFlag<GeneralRegion> regionFlag;

    @Inject
    public GroupAddCloudCommand(
            @Nonnull MessageBridge messageBridge,
            @Nonnull IFileManager fileManager,
            @Nonnull RegionFactory regionFactory

    ) {
        this.messageBridge = messageBridge;
        this.fileManager = fileManager;
        this.regionFactory = regionFactory;
        this.regionFlag = RegionFlagUtil.createDefault(fileManager);
    }

    @Override
    public String stringDescription() {
        return null;
    }

    @Override
    protected @Nonnull Command.Builder<? extends CommandSender> configureCommand(@Nonnull Command.Builder<CommandSender> builder) {
        return builder.literal("groupadd")
                .required(KEY_GROUP, StringParser.stringParser(), this::suggestGroupNames)
                .flag(this.regionFlag)
                .handler(this::handleCommand);
    }

    @Override
    protected @Nonnull CommandProperties properties() {
        return CommandProperties.of("groupadd");
    }

    private void handleCommand(@Nonnull CommandContext<CommandSender> context) {
        CommandSender sender = context.sender();
        if (!sender.hasPermission("areashop.groupadd")) {
            throw new AreaShopCommandException("groupadd-noPermission");
        }
        String rawGroup = context.get(KEY_GROUP);
        RegionGroup group = fileManager.getGroup(rawGroup);
        if (group == null) {
            group = regionFactory.createRegionGroup(rawGroup);
            fileManager.addGroup(group);
        }
        GeneralRegion declaredRegion = context.flags().get(this.regionFlag);
        if (declaredRegion != null) {
            if (!group.addMember(declaredRegion)) {
                throw new AreaShopCommandException("groupadd-failed", group.getName(), declaredRegion);
            }
            declaredRegion.update();
            this.messageBridge.message(sender,
                    "groupadd-success",
                    group.getName(),
                    group.getMembers().size(),
                    declaredRegion);
            return;
        }

        Collection<GeneralRegion> regions = RegionFlagUtil.getOrParseRegionsInSel(context, this.regionFlag);
        Set<GeneralRegion> regionsSuccess = new TreeSet<>();
        Set<GeneralRegion> regionsFailed = new TreeSet<>();
        for (GeneralRegion region : regions) {
            if (group.addMember(region)) {
                regionsSuccess.add(region);
            } else {
                regionsFailed.add(region);
            }
        }
        if (!regionsSuccess.isEmpty()) {
            messageBridge.message(sender,
                    "groupadd-weSuccess",
                    group.getName(),
                    Utils.combinedMessage(regionsSuccess, "region"));
        }
        if (!regionsFailed.isEmpty()) {
            messageBridge.message(sender,
                    "groupadd-weFailed",
                    group.getName(),
                    Utils.combinedMessage(regionsFailed, "region"));
        }
        // Update all regions, this does it in a task, updating them without lag
        fileManager.updateRegions(List.copyOf(regionsSuccess), sender);
    }

    private CompletableFuture<Iterable<Suggestion>> suggestGroupNames(
            @Nonnull CommandContext<CommandSender> context,
            @Nonnull CommandInput input
    ) {
        String text = input.peekString();
        List<Suggestion> suggestions = this.fileManager.getGroupNames().stream()
                .filter(name -> name.startsWith(text))
                .map(Suggestion::simple)
                .toList();
        return CompletableFuture.completedFuture(suggestions);
    }
}










