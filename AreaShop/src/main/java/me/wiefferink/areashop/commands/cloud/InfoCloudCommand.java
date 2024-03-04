package me.wiefferink.areashop.commands.cloud;

import io.github.bakedlibs.dough.blocks.BlockPosition;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.adapters.platform.OfflinePlayerHelper;
import me.wiefferink.areashop.commands.DelfriendCommand;
import me.wiefferink.areashop.commands.SellCommand;
import me.wiefferink.areashop.commands.SetteleportCommand;
import me.wiefferink.areashop.commands.TeleportCommand;
import me.wiefferink.areashop.commands.UnrentCommand;
import me.wiefferink.areashop.commands.util.AreaShopCommandException;
import me.wiefferink.areashop.commands.util.RegionGroupParser;
import me.wiefferink.areashop.commands.util.RegionInfoUtil;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.BuyRegion;
import me.wiefferink.areashop.regions.GeneralRegion;
import me.wiefferink.areashop.regions.RegionGroup;
import me.wiefferink.areashop.regions.RentRegion;
import me.wiefferink.areashop.tools.BukkitSchedulerExecutor;
import me.wiefferink.areashop.tools.SimpleMessageBridge;
import me.wiefferink.areashop.tools.Utils;
import me.wiefferink.interactivemessenger.processing.Message;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.parser.flag.CommandFlag;
import org.incendo.cloud.parser.standard.EnumParser;
import org.incendo.cloud.parser.standard.IntegerParser;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Singleton
public class InfoCloudCommand extends CloudCommandBean {

    private static final CommandFlag<Integer> FLAG_PAGE = CommandFlag.builder("page")
            .withComponent(IntegerParser.integerParser(1))
            .build();
    private static final CloudKey<String> KEY_FILTER_ARG = CloudKey.of("filterArg", String.class);
    private static final CloudKey<RegionStateFilterType> KEY_TYPE = CloudKey.of("type", RegionStateFilterType.class);

    private final MessageBridge messageBridge;
    private final IFileManager fileManager;
    private final Server server;
    private final OfflinePlayerHelper offlinePlayerHelper;
    private final BukkitSchedulerExecutor executor;

    private final CommandFlag<RegionGroup> filterGroupFlag;

    @Inject
    public InfoCloudCommand(
            @Nonnull MessageBridge messageBridge,
            @Nonnull IFileManager fileManager,
            @Nonnull Server server,
            @Nonnull OfflinePlayerHelper offlinePlayerHelper,
            @Nonnull BukkitSchedulerExecutor executor
    ) {
        this.messageBridge = messageBridge;
        this.fileManager = fileManager;
        this.server = server;
        this.filterGroupFlag = CommandFlag.builder("group")
                .withComponent(ParserDescriptor.of(new RegionGroupParser<>(fileManager, "info-noFiltergroup"),
                        RegionGroup.class))
                .build();
        this.offlinePlayerHelper = offlinePlayerHelper;
        this.executor = executor;
    }

    @Override
    public String stringDescription() {
        return null;
    }

    @Override
    public String getHelpKey(CommandSender target) {
        if(target.hasPermission("areashop.info")) {
            return "help-info";
        }
        return null;
    }

    @Override
    protected @Nonnull Command.Builder<? extends CommandSender> configureCommand(@Nonnull Command.Builder<CommandSender> builder) {
        return builder.literal("info")
                .required(KEY_TYPE, EnumParser.enumParser(RegionStateFilterType.class))
                .optional(KEY_FILTER_ARG, StringParser.stringParser(), this::suggestFilterArg)
                .flag(FLAG_PAGE)
                .handler(this::handleCommand);
    }

    @Override
    protected @Nonnull CommandProperties properties() {
        return CommandProperties.of("info");
    }

    private CompletableFuture<Iterable<Suggestion>> suggestFilterArg(
            @Nonnull CommandContext<CommandSender> context,
            @Nonnull CommandInput commandInput) {
        Optional<RegionStateFilterType> filterType = context.optional(KEY_TYPE);
        if (filterType.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        String text = commandInput.peekString();
        Stream<String> stream = switch (filterType.get()) {
            case PLAYER -> this.server.getOnlinePlayers().stream().map(Player::getName);
            case REGION -> this.fileManager.getRegionNames().stream();
            default -> Stream.empty();
        };
        List<Suggestion> suggestions = stream.filter(value -> value.startsWith(text)).map(Suggestion::simple).toList();
        return CompletableFuture.completedFuture(suggestions);
    }

    private void handleCommand(@Nonnull CommandContext<CommandSender> context) {
        CommandSender sender = context.sender();
        if (!sender.hasPermission("areashop.info")) {
            messageBridge.message(sender, "info-noPermission");
            return;
        }
        RegionStateFilterType filterType = context.get(KEY_TYPE);
        if (filterType == RegionStateFilterType.REGION) {
            processWithRegionFilter(context);
        } else if (filterType == RegionStateFilterType.PLAYER) {
            processWithPlayerFilter(context);
        } else {
            processOtherFilters(context, filterType);
        }
    }

    private void processOtherFilters(@Nonnull CommandContext<CommandSender> context,
                                     @Nonnull RegionStateFilterType filterType) {
        RegionGroup filterGroup = context.flags().get(this.filterGroupFlag);
        int page = context.flags().getValue(FLAG_PAGE).orElse(1);
        Stream<? extends GeneralRegion> toShow = switch (filterType) {
            case ALL -> fileManager.getRegionsRef().stream();
            case SOLD -> fileManager.getBuysRef().stream().filter(BuyRegion::isSold);
            case RENTED -> fileManager.getRentsRef().stream().filter(RentRegion::isRented);
            case FORRENT -> fileManager.getRentsRef().stream().filter(RentRegion::isAvailable);
            case FORSALE -> fileManager.getBuysRef().stream().filter(BuyRegion::isAvailable);
            case RESELLING -> fileManager.getBuysRef().stream().filter(Predicate.not(BuyRegion::isInResellingMode));
            case NOGROUP -> {
                List<GeneralRegion> regions = new LinkedList<>(fileManager.getRegions());
                for (RegionGroup group : fileManager.getGroups()) {
                    regions.removeAll(group.getMemberRegions());
                }
                yield regions.stream();
            }
            default -> Stream.empty();
        };
        String header = switch (filterType) {
            case ALL -> "info-allHeader";
            case SOLD -> "info-soldHeader";
            case RENTED -> "info-rentedHeader";
            case FORRENT -> "info-forrentHeader";
            case FORSALE -> "info-forsaleHeader";
            case RESELLING -> "info-reselling";
            case NOGROUP -> "info-nogroupHeader";
            default -> "";
        };
        String baseCommand = switch (filterType) {
            case ALL -> "info all";
            case SOLD -> "info sold";
            case RENTED -> "info rented";
            case FORRENT -> "info forrent";
            case FORSALE -> "info forsale";
            case RESELLING -> "info reselling";
            case NOGROUP -> "info nogroup";
            default -> "";
        };
        showSortedPagedList(context.sender(), toShow, filterGroup, header, page, baseCommand);
    }

    private void processWithPlayerFilter(@Nonnull CommandContext<CommandSender> context) {
        CommandSender sender = context.sender();
        Optional<String> optionalArg = context.optional(KEY_FILTER_ARG);
        if (optionalArg.isEmpty()) {
            throw new AreaShopCommandException("info-playerHelp");
        }
        String playerName = optionalArg.get();
        this.offlinePlayerHelper.lookupOfflinePlayerAsync(playerName).thenAcceptAsync(offlinePlayer -> {
            if (!offlinePlayer.hasPlayedBefore()) {
                // Don't throw an exception here, just send the error message directly
                this.messageBridge.message(sender, "me-noPlayer", playerName);
                return;
            }
            RegionInfoUtil.showRegionInfo(this.messageBridge, this.fileManager, sender, offlinePlayer);
        }, executor);

    }

    private void processWithRegionFilter(@Nonnull CommandContext<CommandSender> context) {
        CommandSender sender = context.sender();
        // Region info
        Optional<String> optionalArg = context.optional(KEY_FILTER_ARG);
        GeneralRegion region;
        if (optionalArg.isPresent()) {
            region = this.fileManager.getRegion(optionalArg.get());
            if (region == null) {
                throw new AreaShopCommandException("info-regionNotExisting", optionalArg);
            }
        } else if (sender instanceof Player player) {
            // get the region by location
            List<GeneralRegion> regions = Utils.getImportantRegions(player.getLocation());
            if (regions.isEmpty()) {
                throw new AreaShopCommandException("cmd-noRegionsAtLocation");
            } else if (regions.size() > 1) {
                throw new AreaShopCommandException("cmd-moreRegionsAtLocation");
            } else {
                region = regions.get(0);
            }
        } else {
            throw new AreaShopCommandException("cmd-automaticRegionOnlyByPlayer");
        }
        if (region instanceof RentRegion rent) {
            handleRent(sender, rent);
        } else if (region instanceof BuyRegion buy) {
            handleBuy(sender, buy);
        }
    }

    private void handleBuy(@Nonnull CommandSender sender, @Nonnull BuyRegion buy) {
        messageBridge.message(sender, "info-regionHeaderBuy", buy);
        if (buy.isSold()) {
            if (buy.isInResellingMode()) {
                messageBridge.messageNoPrefix(sender, "info-regionReselling", buy);
                messageBridge.messageNoPrefix(sender, "info-regionReselPrice", buy);
            } else {
                messageBridge.messageNoPrefix(sender, "info-regionBought", buy);
            }
            // Money back
            if (!buy.getBooleanSetting("buy.sellDisabled")) {
                if (SellCommand.canUse(sender, buy)) {
                    messageBridge.messageNoPrefix(sender, "info-regionMoneyBackBuyClick", buy);
                } else {
                    messageBridge.messageNoPrefix(sender, "info-regionMoneyBackBuy", buy);
                }
            }
            // Friends
            if (!buy.getFriendsFeature().getFriendNames().isEmpty()) {
                String messagePart = "info-friend";
                if (DelfriendCommand.canUse(sender, buy)) {
                    messagePart = "info-friendRemove";
                }
                messageBridge.messageNoPrefix(sender,
                        "info-regionFriends",
                        buy,
                        Utils.combinedMessage(buy.getFriendsFeature().getFriendNames(), messagePart));
            }
        } else {
            messageBridge.messageNoPrefix(sender, "info-regionCanBeBought", buy);
        }
        if (buy.getLandlord() != null) {
            messageBridge.messageNoPrefix(sender, "info-regionLandlord", buy);
        }
        if (buy.getInactiveTimeUntilSell() != -1) {
            messageBridge.messageNoPrefix(sender, "info-regionInactiveSell", buy);
        }
        // Restoring
        if (buy.isRestoreEnabled()) {
            messageBridge.messageNoPrefix(sender, "info-regionRestoringBuy", buy);
        }
        // Restrictions
        if (!buy.isSold()) {
            if (buy.restrictedToRegion()) {
                messageBridge.messageNoPrefix(sender, "info-regionRestrictedRegionBuy", buy);
            } else if (buy.restrictedToWorld()) {
                messageBridge.messageNoPrefix(sender, "info-regionRestrictedWorldBuy", buy);
            }
        }
        messageBridge.messageNoPrefix(sender, "info-regionFooterBuy", buy);
    }

    private void handleRent(@Nonnull CommandSender sender, @Nonnull RentRegion rent) {
        messageBridge.message(sender, "info-regionHeaderRent", rent);
        if (rent.isRented()) {
            messageBridge.messageNoPrefix(sender, "info-regionRented", rent);
            messageBridge.messageNoPrefix(sender, "info-regionExtending", rent);
            // Money back
            if (UnrentCommand.canUse(sender, rent)) {
                messageBridge.messageNoPrefix(sender, "info-regionMoneyBackRentClick", rent);
            } else {
                messageBridge.messageNoPrefix(sender, "info-regionMoneyBackRent", rent);
            }
            // Friends
            if (!rent.getFriendsFeature().getFriendNames().isEmpty()) {
                String messagePart = "info-friend";
                if (DelfriendCommand.canUse(sender, rent)) {
                    messagePart = "info-friendRemove";
                }
                messageBridge.messageNoPrefix(sender,
                        "info-regionFriends",
                        rent,
                        Utils.combinedMessage(rent.getFriendsFeature().getFriendNames(), messagePart));
            }
        } else {
            messageBridge.messageNoPrefix(sender, "info-regionCanBeRented", rent);
        }
        if (rent.getLandlordName() != null) {
            messageBridge.messageNoPrefix(sender, "info-regionLandlord", rent);
        }
        // Maximum extends
        if (rent.getMaxExtends() != -1) {
            if (rent.getMaxExtends() == 0) {
                messageBridge.messageNoPrefix(sender, "info-regionNoExtending", rent);
            } else if (rent.isRented()) {
                messageBridge.messageNoPrefix(sender, "info-regionExtendsLeft", rent);
            } else {
                messageBridge.messageNoPrefix(sender, "info-regionMaxExtends", rent);
            }
        }
        // If maxExtends is zero it does not make sense to show this message
        if (rent.getMaxRentTime() != -1 && rent.getMaxExtends() != 0) {
            messageBridge.messageNoPrefix(sender, "info-regionMaxRentTime", rent);
        }
        if (rent.getInactiveTimeUntilUnrent() != -1) {
            messageBridge.messageNoPrefix(sender, "info-regionInactiveUnrent", rent);
        }
        displayMiscInfo(sender, rent);
        // Restoring
        if (rent.isRestoreEnabled()) {
            messageBridge.messageNoPrefix(sender, "info-regionRestoringRent", rent);
        }
        // Restrictions
        if (!rent.isRented()) {
            if (rent.restrictedToRegion()) {
                messageBridge.messageNoPrefix(sender, "info-regionRestrictedRegionRent", rent);
            } else if (rent.restrictedToWorld()) {
                messageBridge.messageNoPrefix(sender, "info-regionRestrictedWorldRent", rent);
            }
        }
        messageBridge.messageNoPrefix(sender, "info-regionFooterRent", rent);
    }

    private void displayMiscInfo(@Nonnull CommandSender sender, @Nonnull GeneralRegion region) {
        displayTeleportInfo(sender, region);
        displaySignInfo(sender, region);
        displayGroupInfo(sender, region);
    }

    private void displayGroupInfo(@Nonnull CommandSender sender, @Nonnull GeneralRegion region) {// Groups
        if (sender.hasPermission("areashop.groupinfo") && !region.getGroupNames().isEmpty()) {
            messageBridge.messageNoPrefix(sender,
                    "info-regionGroups",
                    Utils.createCommaSeparatedList(region.getGroupNames()));
        }
    }

    private void displaySignInfo(@Nonnull CommandSender sender, @Nonnull GeneralRegion region) {
        // Signs
        List<String> signLocations = new ArrayList<>();
        for (BlockPosition location : region.getSignsFeature().signManager().allSignLocations()) {
            signLocations.add(Message.fromKey("info-regionSignLocation")
                    .replacements(location.getWorld().getName(),
                            location.getX(),
                            location.getY(),
                            location.getZ())
                    .getPlain());
        }
        if (!signLocations.isEmpty()) {
            messageBridge.messageNoPrefix(sender,
                    "info-regionSigns",
                    Utils.createCommaSeparatedList(signLocations));
        }
    }

    private void displayTeleportInfo(@Nonnull CommandSender sender, @Nonnull GeneralRegion region) {
        // Teleport
        Message tp = Message.fromKey("info-prefix");
        boolean foundSomething = false;
        if (TeleportCommand.canUse(sender, region)) {
            foundSomething = true;
            tp.append(Message.fromKey("info-regionTeleport").replacements(region));
        }
        if (SetteleportCommand.canUse(sender, region)) {
            if (foundSomething) {
                tp.append(", ");
            }
            foundSomething = true;
            tp.append(Message.fromKey("info-setRegionTeleport").replacements(region));
        }
        if (foundSomething) {
            tp.append(".");
            SimpleMessageBridge.send(tp, sender);
        }
    }


    /**
     * Display a page of a list of regions.
     *
     * @param sender       The CommandSender to send the messages to
     * @param regionStream The regions to display
     * @param filterGroup  The group to filter the regions by
     * @param keyHeader    The header to print above the page
     * @param pageInput    The page number, if any
     * @param baseCommand  The command to execute for next/previous page (/areashop will be added)
     */
    private void showSortedPagedList(
            CommandSender sender,
            Stream<? extends GeneralRegion> regionStream,
            RegionGroup filterGroup,
            String keyHeader,
            int pageInput,
            String baseCommand
    ) {
        int page = pageInput;
        int maximumItems = 20;
        int itemsPerPage = maximumItems - 2;
        List<? extends GeneralRegion> regions;
        if (filterGroup != null) {
            regions = regionStream.filter(generalRegion -> !filterGroup.isMember(generalRegion)).toList();
        } else {
            regions = regionStream.toList();
        }
        if (regions.isEmpty()) {
            messageBridge.message(sender, "info-noRegions");
            return;
        }
        // First sort by type, then by name
        regions = regions.stream().sorted((one, two) -> {
            int typeCompare = getTypeOrder(two).compareTo(getTypeOrder(one));
            if (typeCompare != 0) {
                return typeCompare;
            } else {
                return one.getName().compareTo(two.getName());
            }
        }).toList();
        // Header
        Message limitedToGroup = Message.empty();
        if (filterGroup != null) {
            limitedToGroup = Message.fromKey("info-limitedToGroup").replacements(filterGroup.getName());
        }
        messageBridge.message(sender, keyHeader, limitedToGroup);
        // Page entries
        int totalPages = (int) Math.ceil(regions.size() / (double) itemsPerPage); // Clip page to correct boundaries, not much need to tell the user
        if (regions.size() == itemsPerPage + 1) { // 19 total items is mapped to 1 page of 19
            itemsPerPage++;
            totalPages = 1;
        }
        page = Math.max(1, Math.min(totalPages, page));
        int linesPrinted = 1; // header
        for (int i = (page - 1) * itemsPerPage; i < page * itemsPerPage && i < regions.size(); i++) {
            String state;
            GeneralRegion region = regions.get(i);
            if (region.getType() == GeneralRegion.RegionType.RENT) {
                if (region.getOwner() == null) {
                    state = "Forrent";
                } else {
                    state = "Rented";
                }
            } else {
                if (region.getOwner() == null) {
                    state = "Forsale";
                } else if (!((BuyRegion) region).isInResellingMode()) {
                    state = "Sold";
                } else {
                    state = "Reselling";
                }
            }
            messageBridge.messageNoPrefix(sender, "info-entry" + state, region);
            linesPrinted++;
        }
        Message footer = Message.empty();
        // Previous button
        if (page > 1) {
            footer.append(Message.fromKey("info-pagePrevious").replacements(baseCommand + " " + (page - 1)));
        } else {
            footer.append(Message.fromKey("info-pageNoPrevious"));
        }
        // Page status
        if (totalPages > 1) {
            StringBuilder pageString = new StringBuilder("" + page);
            for (int i = pageString.length(); i < (totalPages + "").length(); i++) {
                pageString.insert(0, "0");
            }
            footer.append(Message.fromKey("info-pageStatus").replacements(page, totalPages));
            if (page < totalPages) {
                footer.append(Message.fromKey("info-pageNext").replacements(baseCommand + " " + (page + 1)));
            } else {
                footer.append(Message.fromKey("info-pageNoNext"));
            }
            // Fill up space if the page is not full (aligns header nicely)
            for (int i = linesPrinted; i < maximumItems - 1; i++) {
                sender.sendMessage(" ");
            }
            SimpleMessageBridge.send(footer, sender);
        }
    }

    /**
     * Get an integer to order by type, usable for Comparators.
     *
     * @param region The region to get the order for
     * @return An integer for sorting by type
     */
    private Integer getTypeOrder(GeneralRegion region) {
        if (region.getType() == GeneralRegion.RegionType.RENT) {
            if (region.getOwner() == null) {
                return 1;
            } else {
                return 2;
            }
        } else {
            if (region.getOwner() == null) {
                return 3;
            } else if (!((BuyRegion) region).isInResellingMode()) {
                return 4;
            } else {
                return 5;
            }
        }
    }


    private enum RegionStateFilterType {
        ALL,
        RENTED,
        FORRENT,
        SOLD,
        FORSALE,
        RESELLING,
        NOGROUP,
        PLAYER,
        REGION
    }

}


























