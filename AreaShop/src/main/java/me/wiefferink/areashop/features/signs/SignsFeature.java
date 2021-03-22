package me.wiefferink.areashop.features.signs;

import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.wiefferink.areashop.AreaShop;
import me.wiefferink.areashop.events.ask.AddingRegionEvent;
import me.wiefferink.areashop.features.RegionFeature;
import me.wiefferink.areashop.interfaces.BlockBehaviourHelper;
import me.wiefferink.areashop.managers.FileManager;
import me.wiefferink.areashop.regions.BuyRegion;
import me.wiefferink.areashop.regions.GeneralRegion;
import me.wiefferink.areashop.regions.RentRegion;
import me.wiefferink.areashop.tools.Materials;
import me.wiefferink.areashop.tools.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.*;

public class SignsFeature extends RegionFeature {

    public static final SignCache signCache = new SignCache();

    private static final BlockBehaviourHelper behaviourHelper = AreaShop.getInstance().getNMSHelper().behaviourHelper();

    static {
        signCache.registerListeners();
    }

    public static void shutdownGlobalState() {
        signCache.shutdown();
    }

    private Map<String, RegionSign> signs;

    public SignsFeature() {

    }

    /**
     * Constructor.
     *
     * @param region The region to bind to
     */
    public SignsFeature(GeneralRegion region) {
        setRegion(region);
        signs = new HashMap<>();
        // Setup current signs
        ConfigurationSection signSection = region.getConfig().getConfigurationSection("general.signs");
        if (signSection != null) {
            for (String signKey : signSection.getKeys(false)) {
                RegionSign sign = new RegionSign(this, signKey);
                Location location = sign.getLocation();
                if (location == null) {
                    AreaShop.warn("Sign with key " + signKey + " of region " + region.getName() + " does not have a proper location");
                    continue;
                }
                signs.put(sign.getStringLocation(), sign);
                signCache.addSign(sign);
            }
        }
    }

    /**
     * Convert a location to a string to use as map key.
     *
     * @param location The location to get the key for
     * @return A string to use in a map for a location
     */
    public static String locationToString(Location location) {
        return location.getWorld().getName() + ";" + location.getBlockX() + ";" + location.getBlockY() + ";" + location.getBlockZ();
    }

    /**
     * Convert a chunk to a string to use as map key.
     *
     * @param location The location to get the key for
     * @return A string to use in a map for a chunk
     */
    public static String chunkToString(Location location) {
        return location.getWorld().getName() + ";" + (location.getBlockX() >> 4) + ";" + (location.getBlockZ() >> 4);
    }

    /**
     * Convert a chunk to a string to use as map key.
     * Use a Location argument to prevent chunk loading!
     *
     * @param chunk The location to get the key for
     * @return A string to use in a map for a chunk
     */
    public static String chunkToString(Chunk chunk) {
        return chunk.getWorld().getName() + ";" + chunk.getX() + ";" + chunk.getZ();
    }

    /**
     * Get a sign by a location.
     *
     * @param location The location to get the sign for
     * @return The RegionSign that is at the location, or null if none
     */
    public static RegionSign getSignByLocation(Location location) {
        return signCache.getSignByLocation(location);
    }

    /**
     * Get the map with all signs.
     *
     * @return Map with all signs: locationString -&gt; RegionSign
     */
    public static Map<String, RegionSign> getAllSigns() {
        return signCache.getAllSigns();
    }

    /**
     * Get the map with signs by chunk.
     *
     * @return Map with signs by chunk: chunkString -&gt; List&lt;RegionSign&gt;
     */
    public static Map<String, List<RegionSign>> getSignsByChunk() {
        return signCache.getSignsByChunk();
    }

    @Override
    public void shutdown() {
        // Deregister signs from the registry
        if (signs != null) {
            for (Map.Entry<String, RegionSign> entry : signs.entrySet()) {
                signCache.removeSign(entry.getValue());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSignBreak(BlockBreakEvent event) {
        if (getRegion().invalidated()) {
            AreaShop.warn("SignsFeature: Sign feature for an invalidated region is still active!");
        }
        Block block = event.getBlock();
        // Check if it is a sign
        if (Materials.isSign(block.getType())) {
            // Check if the rent sign is really the same as a saved rent
            RegionSign regionSign = SignsFeature.getSignByLocation(block.getLocation());
            if (regionSign == null) {
                return;
            }
            if (regionSign.getRegion().invalidated()) {
                AreaShop.warn("SignsFeature: Sign feature for an invalidated region object is still active!");
            }
            // Remove the sign of the rental region if the player has permission
            if (event.getPlayer().hasPermission("areashop.delsign")) {
                regionSign.remove();
                plugin.message(event.getPlayer(), "delsign-success", regionSign.getRegion());
            } else { // Cancel the breaking of the sign
                event.setCancelled(true);
                plugin.message(event.getPlayer(), "delsign-noPermission", regionSign.getRegion());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onIndirectSignBreak(BlockPhysicsEvent event) {
        if (getRegion().invalidated()) {
            AreaShop.warn("SignsFeature: Sign feature for an invalidated region is still active!");
        }
        // Check if the block is a sign
        if (!Materials.isSign(event.getBlock().getType())) {
            return;
        }
        final Block block = event.getBlock();

        // Check if the sign is really the same as a saved rent
        RegionSign regionSign = SignsFeature.getSignByLocation(event.getBlock().getLocation());
        if (regionSign == null) {
            return;
        }
        // If the block is in an invalid position then we remove the sign
        if (!behaviourHelper.isBlockValid(block)) {
            regionSign.remove();
        }

        // Remove the sign so that it does not fall on the floor as an item (next region update will place it back when possible)
        AreaShop.debug("onIndirectSignBreak: Removed block of sign for", regionSign.getRegion().getName(), "at", regionSign.getStringLocation());
        event.getBlock().setType(Material.AIR);
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSignClick(PlayerInteractEvent event) {
        if (getRegion().invalidated()) {
            AreaShop.warn("SignsFeature: Sign feature for an invalidated region is still active!");
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        // Only listen to left and right clicks on blocks
        if (!(event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK)) {
            return;
        }

        // Only care about clicking blocks
        if (!Materials.isSign(block.getType())) {
            return;
        }

        // Check if this sign belongs to a region
        RegionSign regionSign = SignsFeature.getSignByLocation(block.getLocation());
        if (regionSign == null) {
            return;
        }

        // Ignore players that are in sign link mode (which will handle the event itself)
        Player player = event.getPlayer();
        if (plugin.getSignlinkerManager().isInSignLinkMode(player)) {
            return;
        }

        // Get the clicktype
        GeneralRegion.ClickType clickType = null;
        if (player.isSneaking() && event.getAction() == Action.LEFT_CLICK_BLOCK) {
            clickType = GeneralRegion.ClickType.SHIFTLEFTCLICK;
        } else if (!player.isSneaking() && event.getAction() == Action.LEFT_CLICK_BLOCK) {
            clickType = GeneralRegion.ClickType.LEFTCLICK;
        } else if (player.isSneaking() && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            clickType = GeneralRegion.ClickType.SHIFTRIGHTCLICK;
        } else if (!player.isSneaking() && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            clickType = GeneralRegion.ClickType.RIGHTCLICK;
        }

        boolean ran = regionSign.runSignCommands(player, clickType);

        // Only cancel event if at least one command has been executed
        event.setCancelled(ran);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        if (getRegion().invalidated()) {
            AreaShop.warn("SignsFeature: Sign feature for an invalidated region is still active!");
        }
        Player player = event.getPlayer();
        if (!plugin.isReady()) {
            plugin.message(player, "general-notReady");
            return;
        }

        final BlockFace rotation;
        final BlockData data = event.getBlock().getBlockData();
        if (data instanceof Sign) {
            rotation = ((Sign) data).getRotation();
        } else if (data instanceof WallSign) {
            rotation = ((WallSign) data).getFacing();
        } else {
            rotation = null;
        }

        final String line0 = event.getLine(0) == null ? "" : event.getLine(0);

        // Check if the sign is meant for this plugin
        if (line0.contains(plugin.getConfig().getString("signTags.rent"))) {
            if (!player.hasPermission("areashop.createrent")
                    && !player.hasPermission("areashop.createrent.member")
                    && !player.hasPermission("areashop.createrent.owner")) {
                plugin.message(player, "setup-noPermissionRent");
                return;
            }

            // Get the other lines
            String secondLine = event.getLine(1);
            String thirdLine = event.getLine(2);
            String fourthLine = event.getLine(3);

            // Get the regionManager for accessing regions
            RegionManager regionManager = plugin.getRegionManager(event.getPlayer().getWorld());

            // If the secondLine does not contain a name try to find the region by location
            if (secondLine == null || secondLine.isEmpty()) {
                Set<ProtectedRegion> regions = plugin.getWorldGuardHandler().getApplicableRegionsSet(event.getBlock().getLocation());
                if (regions != null) {
                    boolean first = true;
                    ProtectedRegion candidate = null;
                    for (ProtectedRegion pr : regions) {
                        if (first) {
                            candidate = pr;
                            first = false;
                        } else {
                            if (pr.getPriority() > candidate.getPriority()) {
                                candidate = pr;
                            } else if (pr.getParent() != null && pr.getParent().equals(candidate)) {
                                candidate = pr;
                            } else {
                                plugin.message(player, "setup-couldNotDetect", candidate.getId(), pr.getId());
                                return;
                            }
                        }
                    }
                    if (candidate != null) {
                        secondLine = candidate.getId();
                    }
                }
            }

            boolean priceSet = fourthLine != null && !fourthLine.isEmpty();
            boolean durationSet = thirdLine != null && !thirdLine.isEmpty();
            // check if all the lines are correct
            if (secondLine == null || secondLine.isEmpty()) {
                plugin.message(player, "setup-noRegion");
                return;
            }
            ProtectedRegion region = regionManager.getRegion(secondLine);
            if (region == null) {
                plugin.message(player, "cmd-noRegion", secondLine);
                return;
            }

            FileManager.AddResult addResult = plugin.getFileManager().checkRegionAdd(player, regionManager.getRegion(secondLine), event.getPlayer().getWorld(), GeneralRegion.RegionType.RENT);

            switch (addResult) {
                case BLACKLISTED:
                    plugin.message(player, "setup-blacklisted", secondLine);
                    break;
                case ALREADYADDED:
                    plugin.message(player, "setup-alreadyRentSign");
                    break;
                case ALREADYADDEDOTHERWORLD:
                    plugin.message(player, "setup-alreadyOtherWorld");
                    break;
                case NOPERMISSION:
                    plugin.message(player, "setup-noPermission", secondLine);
                    break;
                default:
                    if (thirdLine != null && !thirdLine.isEmpty() && !Utils.checkTimeFormat(thirdLine)) {
                        plugin.message(player, "setup-wrongDuration");
                        break;
                    }
                    double price = 0.0;
                    if (priceSet) {
                        // Check the fourth line
                        try {
                            price = Double.parseDouble(fourthLine);
                        } catch (NumberFormatException e) {
                            plugin.message(player, "setup-wrongPrice");
                            return;
                        }
                    }

                    // Add rent to the FileManager
                    final RentRegion rent = new RentRegion(secondLine, event.getPlayer().getWorld());
                    boolean isMember = plugin.getWorldGuardHandler().containsMember(rent.getRegion(), player.getUniqueId());
                    boolean isOwner = plugin.getWorldGuardHandler().containsOwner(rent.getRegion(), player.getUniqueId());
                    boolean landlord = (!player.hasPermission("areashop.createrent")
                            && ((player.hasPermission("areashop.createrent.owner") && isOwner)
                            || (player.hasPermission("areashop.createrent.member") && isMember)));

                    if (landlord) {
                        rent.setLandlord(player.getUniqueId(), player.getName());
                    }
                    if (priceSet) {
                        rent.setPrice(price);
                    }
                    if (durationSet) {
                        rent.setDuration(thirdLine);
                    }
                    rent.getSignsFeature().addSign(event.getBlock().getLocation(), event.getBlock().getType(), rotation, null);

                    AddingRegionEvent addingRegionEvent = plugin.getFileManager().addRegion(rent);
                    if (addingRegionEvent.isCancelled()) {
                        plugin.message(player, "general-cancelled", addingRegionEvent.getReason());
                        return;
                    }

                    rent.handleSchematicEvent(GeneralRegion.RegionEvent.CREATED);
                    plugin.message(player, "setup-rentSuccess", rent);
                    // Update the region after the event has written its lines
                    Bukkit.getScheduler().runTask(plugin, rent::update);

            }
        } else if (line0.contains(plugin.getConfig().getString("signTags.buy"))) {
            // Check for permission
            if (!player.hasPermission("areashop.createbuy") && !player.hasPermission("areashop.createbuy.member") && !player.hasPermission("areashop.createbuy.owner")) {
                plugin.message(player, "setup-noPermissionBuy");
                return;
            }

            // Get the other lines
            String secondLine = event.getLine(1);
            String thirdLine = event.getLine(2);

            // Get the regionManager for accessing regions
            RegionManager regionManager = plugin.getRegionManager(event.getPlayer().getWorld());

            // If the secondLine does not contain a name try to find the region by location
            if (secondLine == null || secondLine.isEmpty()) {
                Set<ProtectedRegion> regions = plugin.getWorldGuardHandler().getApplicableRegionsSet(event.getBlock().getLocation());
                if (regions != null) {
                    boolean first = true;
                    ProtectedRegion candidate = null;
                    for (ProtectedRegion pr : regions) {
                        if (first) {
                            candidate = pr;
                            first = false;
                        } else {
                            if (pr.getPriority() > candidate.getPriority()) {
                                candidate = pr;
                            } else if (pr.getParent() != null && pr.getParent().equals(candidate)) {
                                candidate = pr;
                            } else {
                                plugin.message(player, "setup-couldNotDetect", candidate.getId(), pr.getId());
                                return;
                            }
                        }
                    }
                    if (candidate != null) {
                        secondLine = candidate.getId();
                    }
                }
            }

            boolean priceSet = thirdLine != null && !thirdLine.isEmpty();
            // Check if all the lines are correct
            if (secondLine == null || secondLine.isEmpty()) {
                plugin.message(player, "setup-noRegion");
                return;
            }
            ProtectedRegion region = regionManager.getRegion(secondLine);
            if (region == null) {
                plugin.message(player, "cmd-noRegion", secondLine);
                return;
            }
            FileManager.AddResult addResult = plugin.getFileManager().checkRegionAdd(player, region, event.getPlayer().getWorld(), GeneralRegion.RegionType.BUY);
            if (addResult == FileManager.AddResult.BLACKLISTED) {
                plugin.message(player, "setup-blacklisted", secondLine);
            } else if (addResult == FileManager.AddResult.ALREADYADDED) {
                plugin.message(player, "setup-alreadyRentSign");
            } else if (addResult == FileManager.AddResult.ALREADYADDEDOTHERWORLD) {
                plugin.message(player, "setup-alreadyOtherWorld");
            } else if (addResult == FileManager.AddResult.NOPERMISSION) {
                plugin.message(player, "setup-noPermission", secondLine);
            } else {
                double price = 0.0;
                if (priceSet) {
                    // Check the fourth line
                    try {
                        price = Double.parseDouble(thirdLine);
                    } catch (NumberFormatException e) {
                        plugin.message(player, "setup-wrongPrice");
                        return;
                    }
                }

                // Add buy to the FileManager
                final BuyRegion buy = new BuyRegion(secondLine, event.getPlayer().getWorld());
                boolean isMember = plugin.getWorldGuardHandler().containsMember(buy.getRegion(), player.getUniqueId());
                boolean isOwner = plugin.getWorldGuardHandler().containsOwner(buy.getRegion(), player.getUniqueId());
                boolean landlord = (!player.hasPermission("areashop.createbuy")
                        && ((player.hasPermission("areashop.createbuy.owner") && isOwner)
                        || (player.hasPermission("areashop.createbuy.member") && isMember)));

                if (landlord) {
                    buy.setLandlord(player.getUniqueId(), player.getName());
                }
                if (priceSet) {
                    buy.setPrice(price);
                }
                buy.getSignsFeature().addSign(event.getBlock().getLocation(), event.getBlock().getType(), rotation, null);

                AddingRegionEvent addingRegionEvent = plugin.getFileManager().addRegion(buy);
                if (addingRegionEvent.isCancelled()) {
                    plugin.message(player, "general-cancelled", addingRegionEvent.getReason());
                    return;
                }

                buy.handleSchematicEvent(GeneralRegion.RegionEvent.CREATED);
                plugin.message(player, "setup-buySuccess", buy);
                // Update the region after the event has written its lines
                Bukkit.getScheduler().runTask(plugin, buy::update);
            }
        } else if (event.getLine(0).contains(plugin.getConfig().getString("signTags.add"))) {
            // Check for permission
            if (!player.hasPermission("areashop.addsign")) {
                plugin.message(player, "addsign-noPermission");
                return;
            }

            // Get the other lines
            String secondLine = event.getLine(1);
            String thirdLine = event.getLine(2);

            GeneralRegion region;
            if (secondLine != null && !secondLine.isEmpty()) {
                // Get region by secondLine of the sign
                region = plugin.getFileManager().getRegion(secondLine);
                if (region == null) {
                    plugin.message(player, "addSign-notRegistered", secondLine);
                    return;
                }
            } else {
                // Get region by sign position
                List<GeneralRegion> regions = Utils.getImportantRegions(event.getBlock().getLocation());
                if (regions.isEmpty()) {
                    plugin.message(player, "addsign-noRegions");
                    return;
                } else if (regions.size() > 1) {
                    plugin.message(player, "addsign-couldNotDetectSign", regions.get(0).getName(), regions.get(1).getName());
                    return;
                }
                region = regions.get(0);
            }
            String profile = thirdLine == null || thirdLine.isEmpty() ? null : thirdLine;
            region.getSignsFeature().addSign(event.getBlock().getLocation(), event.getBlock().getType(), rotation, profile);
            if (profile == null) {
                plugin.message(player, "addsign-success", region);
            } else {
                plugin.message(player, "addsign-successProfile", thirdLine, region);
            }

            // Update the region later because this event will do it first
            Bukkit.getScheduler().runTask(plugin, region::update);
        }
    }

    /**
     * Update all signs connected to this region.
     *
     * @return true if all signs are updated correctly, false if one or more updates failed
     */
    public boolean update() {
        boolean result = true;
        for (RegionSign sign : signs.values()) {
            result &= sign.update();
        }
        return result;
    }

    /**
     * Check if any of the signs need periodic updating.
     *
     * @return true if one or more of the signs need periodic updating, otherwise false
     */
    public boolean needsPeriodicUpdate() {
        for (RegionSign sign : signs.values()) {
            if (sign.needsPeriodicUpdate()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the signs of this region.
     *
     * @return List of signs
     */
    public List<RegionSign> getSigns() {
        return new ArrayList<>(signs.values());
    }

    /**
     * Get the signs of this region.
     *
     * @return Map with signs: locationString -&gt; RegionSign
     */
    Map<String, RegionSign> getSignsRef() {
        return signs;
    }

    /**
     * Get a list with all sign locations.
     *
     * @return A List with all sign locations
     */
    public List<Location> getSignLocations() {
        List<Location> result = new ArrayList<>();
        for (RegionSign sign : signs.values()) {
            result.add(sign.getLocation());
        }
        return result;
    }

    /**
     * Add a sign to this region.
     *
     * @param location The location of the sign
     * @param signType The type of the sign (WALL_SIGN or SIGN_POST)
     * @param facing   The orientation of the sign
     * @param profile  The profile to use with this sign (null for default)
     */
    public void addSign(Location location, Material signType, BlockFace facing, String profile) {
        int i = 0;
        while (getRegion().getConfig().isSet("general.signs." + i)) {
            i++;
        }
        String signPath = "general.signs." + i + ".";
        final GeneralRegion region = getRegion();
        region.setSetting(signPath + "location", Utils.locationToConfig(location));
        region.setSetting(signPath + "facing", facing != null ? facing.name() : null);
        region.setSetting(signPath + "signType", signType != null ? signType.name() : null);
        if (profile != null && !profile.isEmpty()) {
            region.setSetting(signPath + "profile", profile);
        }
        // Add to the map
        RegionSign sign = new RegionSign(this, String.valueOf(i));
        signs.put(sign.getStringLocation(), sign);
        signCache.addSign(sign);
    }

}
