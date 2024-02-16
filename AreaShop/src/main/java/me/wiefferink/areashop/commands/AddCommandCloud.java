package me.wiefferink.areashop.commands;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.AreaShop;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.commands.util.WorldGuardRegionParser;
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
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.bukkit.parser.WorldParser;
import org.incendo.cloud.execution.CommandExecutionHandler;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.parser.standard.EnumParser;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

@Singleton
public class AddCommandCloud extends CloudCommandBean {

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
        ParserDescriptor<CommandSender, ProtectedRegion> wgRegionParser = ParserDescriptor.of(new WorldGuardRegionParser<>("world", this.worldGuardInterface), ProtectedRegion.class);
        return builder
                .literal("add")
                .senderType(Player.class)
                .required("regionType", EnumParser.enumParser(GeneralRegion.RegionType.class))
                .required("region", wgRegionParser)
                .optional("world", WorldParser.worldParser())
                .handler(context -> {
                    Player player = context.sender();
                    final GeneralRegion.RegionType regionType = context.get("regionType");
                    World world = context.getOrDefault("world", player.getWorld());
                    ProtectedRegion inputRegion = context.get("region");
                    Map<String, ProtectedRegion> regions = new HashMap<>();
                    regions.put(inputRegion.getId(), inputRegion);
                    final boolean isRent = regionType == GeneralRegion.RegionType.RENT;
                    final Player finalPlayer = player;
                    AreaShop.debug("Starting add task with " + regions.size() + " regions");

                    TreeSet<GeneralRegion> regionsSuccess = new TreeSet<>();
                    TreeSet<GeneralRegion> regionsAlready = new TreeSet<>();
                    TreeSet<GeneralRegion> regionsAlreadyOtherWorld = new TreeSet<>();
                    TreeSet<GeneralRegion> regionsRentCancelled = new TreeSet<>(); // Denied by an event listener
                    TreeSet<GeneralRegion> regionsBuyCancelled = new TreeSet<>(); // Denied by an event listener
                    TreeSet<String> namesBlacklisted = new TreeSet<>();
                    TreeSet<String> namesNoPermission = new TreeSet<>();
                    TreeSet<String> namesAddCancelled = new TreeSet<>(); // Denied by an event listener
                    Do.forAll(
                            plugin.getConfig().getInt("adding.regionsPerTick"),
                            regions.entrySet(),
                            regionEntry -> {
                                String regionName = regionEntry.getKey();
                                ProtectedRegion region = regionEntry.getValue();
                                // Determine if the player is an owner or member of the region
                                boolean isMember = worldGuardInterface.containsMember(region,
                                                                        finalPlayer.getUniqueId());
                                boolean isOwner = worldGuardInterface.containsOwner(region,
                                                                        finalPlayer.getUniqueId());
                                String type;
                                if (isRent) {
                                    type = "rent";
                                } else {
                                    type = "buy";
                                }
                                IFileManager.AddResult result = plugin.getFileManager()
                                        .checkRegionAdd(player,
                                                region,
                                                world,
                                                isRent ? GeneralRegion.RegionType.RENT : GeneralRegion.RegionType.BUY);
                                if (result == IFileManager.AddResult.ALREADYADDED) {
                                    regionsAlready.add(plugin.getFileManager().getRegion(regionName));
                                } else if (result == IFileManager.AddResult.ALREADYADDEDOTHERWORLD) {
                                    regionsAlreadyOtherWorld.add(plugin.getFileManager().getRegion(regionName));
                                } else if (result == IFileManager.AddResult.BLACKLISTED) {
                                    namesBlacklisted.add(regionName);
                                } else if (result == IFileManager.AddResult.NOPERMISSION) {
                                    namesNoPermission.add(regionName);
                                } else {
                                    // Check if the player should be landlord
                                    boolean landlord = (!player.hasPermission("areashop.create" + type)
                                            && ((player.hasPermission("areashop.create" + type + ".owner") && isOwner)
                                            || (player.hasPermission("areashop.create" + type + ".member") && isMember)));
                                    List<UUID> existing = new ArrayList<>();
                                    existing.addAll(worldGuardInterface.getOwners(region).asUniqueIdList());
                                    existing.addAll(worldGuardInterface.getMembers(region).asUniqueIdList());

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

                                    if (isRent) {
                                        RentRegion rent = regionFactory.createRentRegion(regionName, world);
                                        // Set landlord
                                        if (landlord) {
                                            rent.setLandlord(finalPlayer.getUniqueId(), finalPlayer.getName());
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
                                    } else {
                                        BuyRegion buy = regionFactory.createBuyRegion(regionName, world);
                                        // Set landlord
                                        if (landlord) {
                                            buy.setLandlord(finalPlayer.getUniqueId(), finalPlayer.getName());
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
                                }
                            },
                            () -> {
                                if (!regionsSuccess.isEmpty()) {
                                    messageBridge.message(player,
                                            "add-success",
                                            inputRegion.getId(),
                                            Utils.combinedMessage(regionsSuccess, "region"));
                                }
                                if (!regionsAlready.isEmpty()) {
                                    messageBridge.message(player,
                                            "add-failed",
                                            Utils.combinedMessage(regionsAlready, "region"));
                                }
                                if (!regionsAlreadyOtherWorld.isEmpty()) {
                                    messageBridge.message(player,
                                            "add-failedOtherWorld",
                                            Utils.combinedMessage(regionsAlreadyOtherWorld, "region"));
                                }
                                if (!regionsRentCancelled.isEmpty()) {
                                    messageBridge.message(player,
                                            "add-rentCancelled",
                                            Utils.combinedMessage(regionsRentCancelled, "region"));
                                }
                                if (!regionsBuyCancelled.isEmpty()) {
                                    messageBridge.message(player,
                                            "add-buyCancelled",
                                            Utils.combinedMessage(regionsBuyCancelled, "region"));
                                }
                                if (!namesBlacklisted.isEmpty()) {
                                    messageBridge.message(player,
                                            "add-blacklisted",
                                            Utils.createCommaSeparatedList(namesBlacklisted));
                                }
                                if (!namesNoPermission.isEmpty()) {
                                    messageBridge.message(player,
                                            "add-noPermissionRegions",
                                            Utils.createCommaSeparatedList(namesNoPermission));
                                    messageBridge.message(player, "add-noPermissionOwnerMember");
                                }
                                if (!namesAddCancelled.isEmpty()) {
                                    messageBridge.message(player,
                                            "add-rentCancelled",
                                            Utils.createCommaSeparatedList(namesAddCancelled));
                                }
                            }
                    );
                });
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


    public List<String> getTabCompleteList(int toComplete, String[] start, CommandSender sender) {
        List<String> result = new ArrayList<>();
        if (toComplete == 2) {
            if (sender.hasPermission("areashop.createrent")) {
                result.add("rent");
            }
            if (sender.hasPermission("areashop.createbuy")) {
                result.add("buy");
            }
        } else if (toComplete == 3) {
            if (sender instanceof Player player) {
                if (sender.hasPermission("areashop.createrent") || sender.hasPermission("areashop.createbuy")) {
                    for (ProtectedRegion region : plugin.getRegionManager(player.getWorld()).getRegions().values()) {
                        result.add(region.getId());
                    }
                }
            }
        }
        return result;
    }

    @Override
    protected @NonNull CommandProperties properties() {
        return CommandProperties.of("add");
    }
}










