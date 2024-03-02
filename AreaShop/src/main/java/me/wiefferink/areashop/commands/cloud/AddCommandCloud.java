package me.wiefferink.areashop.commands.cloud;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.AreaShop;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.commands.util.AreaShopCommandException;
import me.wiefferink.areashop.commands.util.WorldFlagUtil;
import me.wiefferink.areashop.commands.util.WorldGuardRegionParser;
import me.wiefferink.areashop.commands.util.WorldSelection;
import me.wiefferink.areashop.events.ask.AddingRegionEvent;
import me.wiefferink.areashop.events.ask.BuyingRegionEvent;
import me.wiefferink.areashop.events.ask.RentingRegionEvent;
import me.wiefferink.areashop.events.notify.BoughtRegionEvent;
import me.wiefferink.areashop.events.notify.RentedRegionEvent;
import me.wiefferink.areashop.interfaces.WorldEditInterface;
import me.wiefferink.areashop.interfaces.WorldEditSelection;
import me.wiefferink.areashop.interfaces.WorldGuardInterface;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.BuyRegion;
import me.wiefferink.areashop.regions.GeneralRegion;
import me.wiefferink.areashop.regions.RegionFactory;
import me.wiefferink.areashop.regions.RentRegion;
import me.wiefferink.areashop.tools.Utils;
import me.wiefferink.bukkitdo.Do;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.caption.CaptionVariable;
import org.incendo.cloud.caption.StandardCaptionKeys;
import org.incendo.cloud.caption.StandardCaptionsProvider;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.parser.standard.EnumParser;
import org.yaml.snakeyaml.parser.ParserException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

@Singleton
public class AddCommandCloud extends CloudCommandBean {

    private static final CloudKey<GeneralRegion.RegionType> KEY_REGION_TYPE = CloudKey.of("regionType",
            GeneralRegion.RegionType.class);
    private static final CloudKey<ProtectedRegion> KEY_REGION = CloudKey.of("region", ProtectedRegion.class);
    private final MessageBridge messageBridge;
    private final AreaShop plugin;
    private final WorldEditInterface worldEditInterface;
    private final WorldGuardInterface worldGuardInterface;
    private final RegionFactory regionFactory;

    @Inject
    public AddCommandCloud(
            @NonNull MessageBridge messageBridge,
            @NonNull AreaShop plugin,
            @NonNull WorldEditInterface worldEditInterface,
            @NonNull WorldGuardInterface worldGuardInterface,
            @NonNull RegionFactory regionFactory) {
        this.messageBridge = messageBridge;
        this.plugin = plugin;
        this.worldEditInterface = worldEditInterface;
        this.worldGuardInterface = worldGuardInterface;
        this.regionFactory = regionFactory;
    }

    @Override
    protected Command.@NonNull Builder<? extends CommandSender> configureCommand(Command.@NonNull Builder<CommandSender> builder) {
        // /as add <rent|buy> [region] [world]
        ParserDescriptor<Entity, ProtectedRegion> wgRegionParser = ParserDescriptor.of(new WorldGuardRegionParser<>(
                WorldFlagUtil.DEFAULT_WORLD_FLAG,
                this.worldGuardInterface), ProtectedRegion.class);
        return builder
                .literal("add")
                .senderType(Player.class)
                .required(KEY_REGION_TYPE, EnumParser.enumParser(GeneralRegion.RegionType.class))
                .optional(KEY_REGION, wgRegionParser)
                .flag(WorldFlagUtil.DEFAULT_WORLD_FLAG)
                .handler(this::handleCommand);
    }

    private void handleCommand(CommandContext<Player> context) {
        Player player = context.sender();
        final GeneralRegion.RegionType regionType = context.get(KEY_REGION_TYPE);
        World world = WorldFlagUtil.parseOrDetectWorld(context);
        Map<String, ProtectedRegion> regions;
        Optional<ProtectedRegion> inputRegion = context.optional(KEY_REGION);
        if (inputRegion.isPresent()) {
            regions = new HashMap<>();
            regions.put(inputRegion.get().getId(), inputRegion.get());
        } else {
            WorldSelection selection = WorldSelection.fromPlayer(context.sender(), this.worldEditInterface);
            regions = Utils.getWorldEditRegionsInSelection(selection.selection()).stream()
                    .collect(Collectors.toMap(ProtectedRegion::getId, region -> region));
        }
        if (regions.isEmpty()) {
            throw new AreaShopCommandException("cmd-noWERegionsFound");
        }
        AreaShop.debug("Starting add task with " + regions.size() + " regions");
        AddTaskState state = createState(player, regionType, world);
        int batchSize = plugin.getConfig().getInt("adding.regionsPerTick");
        Do.forAll(
                batchSize,
                regions.entrySet(),
                regionEntry -> processEntry(regionEntry, state),
                () -> onCompletion(state)
        );
    }

    private AddTaskState createState(
            @NonNull Player player,
            GeneralRegion.@NonNull RegionType regionType,
            @NonNull World world
    ) {
        Set<GeneralRegion> regionsSuccess = new TreeSet<>();
        Set<GeneralRegion> regionsAlready = new TreeSet<>();
        Set<GeneralRegion> regionsAlreadyOtherWorld = new TreeSet<>();
        Set<GeneralRegion> regionsRentCancelled = new TreeSet<>(); // Denied by an event listener
        Set<GeneralRegion> regionsBuyCancelled = new TreeSet<>(); // Denied by an event listener
        Set<String> namesBlacklisted = new TreeSet<>();
        Set<String> namesNoPermission = new TreeSet<>();
        Set<String> namesAddCancelled = new TreeSet<>(); // Denied by an event listener
        return new AddTaskState(
                player,
                regionType,
                regionsSuccess,
                regionsAlready,
                regionsAlreadyOtherWorld,
                regionsRentCancelled,
                regionsBuyCancelled,
                namesBlacklisted,
                namesNoPermission,
                namesAddCancelled,
                world
        );
    }

    private void processEntry(Map.@NonNull Entry<String, ProtectedRegion> regionEntry,
                              AddCommandCloud.@NonNull AddTaskState taskState) {
        Player player = taskState.sender();
        Set<GeneralRegion> regionsAlready = taskState.regionsAlready();
        Set<GeneralRegion> regionsAlreadyOtherWorld = taskState.regionsAlreadyOtherWorld();
        Set<String> namesBlacklisted = taskState.namesBlacklisted();
        Set<String> namesNoPermission = taskState.namesNoPermission();
        GeneralRegion.RegionType regionType = taskState.regionType();
        World world = taskState.world();
        String regionName = regionEntry.getKey();
        ProtectedRegion region = regionEntry.getValue();
        // Determine if the player is an owner or member of the region
        boolean isMember = this.worldGuardInterface.containsMember(region, player.getUniqueId());
        boolean isOwner = this.worldGuardInterface.containsOwner(region, player.getUniqueId());
        String type = regionType.toString().toLowerCase(Locale.ENGLISH);
        IFileManager.AddResult result = this.plugin.getFileManager()
                .checkRegionAdd(
                        player,
                        region,
                        world,
                        regionType
                );
        switch (result) {
            case ALREADYADDED -> regionsAlready.add(plugin.getFileManager().getRegion(regionName));
            case ALREADYADDEDOTHERWORLD -> regionsAlreadyOtherWorld.add(plugin.getFileManager().getRegion(regionName));
            case BLACKLISTED -> namesBlacklisted.add(regionName);
            case NOPERMISSION -> namesNoPermission.add(regionName);
            default -> {
                // Check if the player should be landlord
                boolean landlord = (!player.hasPermission("areashop.create" + type)
                        && ((player.hasPermission("areashop.create" + type + ".owner") && isOwner)
                        || (player.hasPermission("areashop.create" + type + ".member") && isMember)));
                List<UUID> existing = new ArrayList<>();
                existing.addAll(worldGuardInterface.getOwners(region).asUniqueIdList());
                existing.addAll(worldGuardInterface.getMembers(region).asUniqueIdList());
                debugResult(player, regionName, landlord, existing, isMember, isOwner, regionType);
                if (regionType == GeneralRegion.RegionType.BUY) {
                    proccessBuy(region, landlord, existing, taskState);
                } else if (regionType == GeneralRegion.RegionType.RENT) {
                    processRent(region, landlord, existing, taskState);
                }
            }
        }
    }

    private void debugResult(
            @NonNull Player player,
            @NonNull String regionName,
            boolean landlord,
            List<UUID> existing,
            boolean isMember,
            boolean isOwner,
            GeneralRegion.@NonNull RegionType regionType
    ) {
        String type = regionType.name().toLowerCase(Locale.ENGLISH);
        AreaShop.debug("regionAddLandlordStatus:",
                regionName,
                "landlord:",
                landlord,
                "existing:",
                existing,
                "isMember:",
                isMember,
                "isOwner:",
                isOwner,
                "createPermission:",
                player.hasPermission("areashop.create" + type),
                "ownerPermission:",
                player.hasPermission("areashop.create" + type + ".owner"),
                "memberPermission:",
                player.hasPermission("areashop.create" + type + ".member"));
    }

    private void onCompletion(AddCommandCloud.@NonNull AddTaskState taskState) {
        CommandSender player = taskState.sender();
        Set<GeneralRegion> regionsSuccess = taskState.regionsSuccess();
        Set<GeneralRegion> regionsAlready = taskState.regionsAlready();
        Set<GeneralRegion> regionsAlreadyOtherWorld = taskState.regionsAlreadyOtherWorld();
        Set<GeneralRegion> regionsRentCancelled = taskState.regionsRentCancelled(); // Denied by an event listener
        Set<GeneralRegion> regionsBuyCancelled = taskState.regionsBuyCancelled(); // Denied by an event listener
        Set<String> namesBlacklisted = taskState.namesBlacklisted();
        Set<String> namesNoPermission = taskState.namesNoPermission();
        Set<String> namesAddCancelled = taskState.namesAddCancelled(); // Denied by an event listener
        if (!regionsSuccess.isEmpty()) {
            for (GeneralRegion added : regionsSuccess) {
                this.messageBridge.message(player,
                        "add-success",
                        added.getName(),
                        Utils.combinedMessage(regionsSuccess, "region"));
            }
        }
        if (!regionsAlready.isEmpty()) {
            this.messageBridge.message(player,
                    "add-failed",
                    Utils.combinedMessage(regionsAlready, "region"));
        }
        if (!regionsAlreadyOtherWorld.isEmpty()) {
            this.messageBridge.message(player,
                    "add-failedOtherWorld",
                    Utils.combinedMessage(regionsAlreadyOtherWorld, "region"));
        }
        if (!regionsRentCancelled.isEmpty()) {
            this.messageBridge.message(player,
                    "add-rentCancelled",
                    Utils.combinedMessage(regionsRentCancelled, "region"));
        }
        if (!regionsBuyCancelled.isEmpty()) {
            this.messageBridge.message(player,
                    "add-buyCancelled",
                    Utils.combinedMessage(regionsBuyCancelled, "region"));
        }
        if (!namesBlacklisted.isEmpty()) {
            this.messageBridge.message(player,
                    "add-blacklisted",
                    Utils.createCommaSeparatedList(namesBlacklisted));
        }
        if (!namesNoPermission.isEmpty()) {
            this.messageBridge.message(player,
                    "add-noPermissionRegions",
                    Utils.createCommaSeparatedList(namesNoPermission));
            this.messageBridge.message(player, "add-noPermissionOwnerMember");
        }
        if (!namesAddCancelled.isEmpty()) {
            this.messageBridge.message(player,
                    "add-rentCancelled",
                    Utils.createCommaSeparatedList(namesAddCancelled));
        }
    }

    private void processRent(
            @NonNull ProtectedRegion region,
            boolean landlord,
            List<UUID> existing,
            @NonNull AddTaskState taskState
    ) {
        Player player = taskState.sender();
        Set<GeneralRegion> regionsSuccess = taskState.regionsSuccess();
        Set<GeneralRegion> regionsRentCancelled = taskState.regionsRentCancelled(); // Denied by an event listener
        Set<String> namesAddCancelled = taskState.namesAddCancelled(); // Denied by an event listener
        World world = taskState.world();
        String regionName = region.getId();
        RentRegion rent = regionFactory.createRentRegion(regionName, world);
        // Set landlord
        if (landlord) {
            rent.setLandlord(player.getUniqueId(), player.getName());
        }

        AddingRegionEvent event = plugin.getFileManager().addRegion(rent);
        if (event.isCancelled()) {
            namesAddCancelled.add(rent.getName());
            return;
        }
        rent.handleSchematicEvent(GeneralRegion.RegionEvent.CREATED);
        rent.update();

        // Add existing owners/members if any
        if (!landlord && !existing.isEmpty()) {
            UUID rentBy = existing.remove(0);
            OfflinePlayer rentByPlayer = Bukkit.getOfflinePlayer(rentBy);

            RentingRegionEvent rentingRegionEvent = new RentingRegionEvent(rent,
                    rentByPlayer,
                    false);
            Bukkit.getPluginManager().callEvent(rentingRegionEvent);
            if (rentingRegionEvent.isCancelled()) {
                regionsRentCancelled.add(rent);
            } else {
                // Add values to the rent and send it to FileManager
                rent.setRentedUntil(Calendar.getInstance()
                        .getTimeInMillis() + rent.getDuration());
                rent.setRenter(rentBy);
                rent.updateLastActiveTime();

                // Fire schematic event and updated times extended
                rent.handleSchematicEvent(GeneralRegion.RegionEvent.RENTED);

                // Add others as friends
                for (UUID friend : existing) {
                    rent.getFriendsFeature().addFriend(friend, null);
                }

                rent.notifyAndUpdate(new RentedRegionEvent(rent, false));
            }
        }

        regionsSuccess.add(rent);
    }

    private void proccessBuy(
            @NonNull ProtectedRegion region,
            boolean landlord,
            List<UUID> existing,
            @NonNull AddTaskState taskState
    ) {
        Player player = taskState.sender();
        Set<GeneralRegion> regionsSuccess = taskState.regionsSuccess();
        Set<GeneralRegion> regionsBuyCancelled = taskState.regionsBuyCancelled(); // Denied by an event listener
        Set<String> namesAddCancelled = taskState.namesAddCancelled(); // Denied by an event listener
        World world = taskState.world();
        String regionName = region.getId();
        BuyRegion buy = regionFactory.createBuyRegion(regionName, world);
        // Set landlord
        if (landlord) {
            buy.setLandlord(player.getUniqueId(), player.getName());
        }

        AddingRegionEvent event = plugin.getFileManager().addRegion(buy);
        if (event.isCancelled()) {
            namesAddCancelled.add(buy.getName());
            return;
        }

        buy.handleSchematicEvent(GeneralRegion.RegionEvent.CREATED);
        buy.update();

        // Add existing owners/members if any
        if (!landlord && !existing.isEmpty()) {
            UUID buyBy = existing.remove(0);
            OfflinePlayer buyByPlayer = Bukkit.getOfflinePlayer(buyBy);

            BuyingRegionEvent buyingRegionEvent = new BuyingRegionEvent(buy,
                    buyByPlayer);
            Bukkit.getPluginManager().callEvent(buyingRegionEvent);
            if (buyingRegionEvent.isCancelled()) {
                regionsBuyCancelled.add(buy);
            } else {
                // Set the owner
                buy.setBuyer(buyBy);
                buy.updateLastActiveTime();

                // Update everything
                buy.handleSchematicEvent(GeneralRegion.RegionEvent.BOUGHT);

                // Add others as friends
                for (UUID friend : existing) {
                    buy.getFriendsFeature().addFriend(friend, null);
                }

                buy.notifyAndUpdate(new BoughtRegionEvent(buy));
            }
        }

        regionsSuccess.add(buy);
    }

    @Override
    public String stringDescription() {
        return "Add a region";
    }

    public String getHelp(CommandSender target) {
        if (target.hasPermission("areashop.createrent")
                || target.hasPermission("areashop.createrent.member")
                || target.hasPermission("areashop.createrent.owner")

                || target.hasPermission("areashop.createbuy")
                || target.hasPermission("areashop.createbuy.member")
                || target.hasPermission("areashop.createbuy.owner")) {
            return "help-add";
        }
        return null;
    }

    @Override
    protected @NonNull CommandProperties properties() {
        return CommandProperties.of("add");
    }

    private record AddTaskState(
            @NonNull Player sender,
            GeneralRegion.@NonNull RegionType regionType,
            @NonNull Set<GeneralRegion> regionsSuccess,
            @NonNull Set<GeneralRegion> regionsAlready,
            @NonNull Set<GeneralRegion> regionsAlreadyOtherWorld,
            @NonNull Set<GeneralRegion> regionsRentCancelled,
            @NonNull Set<GeneralRegion> regionsBuyCancelled,
            @NonNull Set<String> namesBlacklisted,
            @NonNull Set<String> namesNoPermission,
            @NonNull Set<String> namesAddCancelled,
            @NonNull World world) {
    }
}










