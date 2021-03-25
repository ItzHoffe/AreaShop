package me.wiefferink.areashop.features;

import me.wiefferink.areashop.AreaShop;
import me.wiefferink.areashop.regions.LegacyGeneralRegion;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

/*
Possible future feature classes:
- RestrictedToRegion feature
	- rent/buy restricted to the region itself
- RestrictedToWorld feature
	- rent/buy restricted to the world of the region
- CountLimitsFeature:
	- Check region limits
- TimeLimitsFeatures:
	- maxExtends
	- maxRentTime
- SchematicFeature
	- save/load schematics
- CommandsFeature
	- execute commands after extend/rent/etc
- LandlordFeature
	- manage landlords
- ExpirationWarningsFeature
	- sending rental expiration warnings
 */

public abstract class RegionFeature implements Listener {
	public static final AreaShop plugin = AreaShop.getInstance();

	protected YamlConfiguration config = plugin.getConfig();
	private LegacyGeneralRegion region;

	/**
	 * Set the region for this feature.
	 * @param region Feature region
	 */
	public void setRegion(LegacyGeneralRegion region) {
		this.region = region;
	}

	/**
	 * Get the region of this feature.
	 * @return region of this feature, or null if generic
	 */
	public LegacyGeneralRegion getRegion() {
		return region;
	}

	/**
	 * Start listening to events.
	 */
	public void listen() {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Destroy the feature and deregister everything.
	 */
	public void shutdownFeature() {
		HandlerList.unregisterAll(this);
		shutdown();
	}

	/**
	 * Dummy method a RegionFeature implementation can override.
	 */
	public void shutdown() {
	}

}
